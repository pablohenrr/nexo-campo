package com.nexocampo.modules.ingestion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexocampo.modules.activity.ActivityService;
import com.nexocampo.modules.evidence.EvidenceService;
import com.nexocampo.modules.evidence.EvidenceType;
import com.nexocampo.modules.farm.Plot;
import com.nexocampo.modules.farm.PlotRepository;
import com.nexocampo.modules.identity.User;
import com.nexocampo.modules.investigation.OccurrenceCategory;
import com.nexocampo.modules.investigation.OccurrenceService;
import com.nexocampo.modules.investigation.OccurrenceSeverity;
import com.nexocampo.shared.ai.AiProviderException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Recebe mensagens de campo de qualquer canal, interpreta com IA e registra o resultado no talhão citado.
 *
 * <p>A IA é chamada fora da transação (leva segundos); a transação só abre para gravar evidência,
 * atividade ou ocorrência e o histórico da mensagem, tudo junto.
 */
@Service
public class FieldMessageService {

    private static final Logger log = LoggerFactory.getLogger(FieldMessageService.class);
    private static final int MAX_TEXT_LENGTH = 4000;

    static final String FAILURE_REPLY = "Não consegui processar sua mensagem agora. Tente de novo em alguns minutos.";
    static final String IGNORED_REPLY = "Recebi sua mensagem, mas não encontrei uma atividade ou ocorrência para registrar. "
            + "Exemplo: \"Apliquei 2 litros de glifosato no Talhão 01 hoje, gastei 800 reais\".";
    private static final Map<String, String> MISSING_FIELD_LABELS = Map.of(
            "data", "data",
            "insumos", "insumos",
            "custo", "custo",
            "descricao", "descrição");

    private final FieldMessageRepository fieldMessageRepository;
    private final PlotRepository plotRepository;
    private final EvidenceService evidenceService;
    private final ActivityService activityService;
    private final OccurrenceService occurrenceService;
    private final FieldMessageInterpreter interpreter;
    private final AudioTranscriber transcriber;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public FieldMessageService(
            FieldMessageRepository fieldMessageRepository,
            PlotRepository plotRepository,
            EvidenceService evidenceService,
            ActivityService activityService,
            OccurrenceService occurrenceService,
            FieldMessageInterpreter interpreter,
            AudioTranscriber transcriber,
            ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.fieldMessageRepository = fieldMessageRepository;
        this.plotRepository = plotRepository;
        this.evidenceService = evidenceService;
        this.activityService = activityService;
        this.occurrenceService = occurrenceService;
        this.interpreter = interpreter;
        this.transcriber = transcriber;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    public FieldMessageResponse receive(User sender, IncomingMessage message) {
        if (!message.hasText() && message.media() == null) {
            throw new IllegalArgumentException("Envie um texto, um áudio ou uma foto");
        }
        EvidenceType mediaType = mediaTypeOf(message.media());

        Optional<FieldMessageResponse> alreadyReceived = findDuplicate(message);
        if (alreadyReceived.isPresent()) {
            return alreadyReceived.get();
        }

        List<Plot> plots = plotRepository.findAllByFarmOwnerIdOrderByName(sender.getId());
        LocalDate today = LocalDate.now(clock);
        FieldMessage record = newRecord(sender, message, mediaType);

        Interpretation interpretation;
        String fullText;
        try {
            String transcript = mediaType == EvidenceType.AUDIO ? transcriber.transcribe(message.media()) : null;
            record.setTranscript(truncate(transcript, MAX_TEXT_LENGTH));
            fullText = joinText(message.text(), transcript);
            interpretation = interpreter.interpret(new InterpretationRequest(
                    fullText,
                    mediaType == EvidenceType.FOTO ? message.media() : null,
                    plots.stream().map(Plot::getName).toList(),
                    today));
        } catch (AiProviderException exception) {
            log.warn("IA falhou ao processar mensagem do usuário {}: {}", sender.getId(), exception.getMessage());
            record.setStatus(FieldMessageStatus.FALHOU);
            record.setErrorMessage(truncate(exception.getMessage(), 500));
            record.setReply(FAILURE_REPLY);
            return save(record);
        }

        MessageKind kind = interpretation.tipo() == null ? MessageKind.OUTRO : interpretation.tipo();
        record.setKind(kind);
        record.setInterpretation(toJson(interpretation));

        if (kind == MessageKind.OUTRO) {
            record.setStatus(FieldMessageStatus.IGNORADA);
            record.setReply(IGNORED_REPLY);
            return save(record);
        }

        Optional<Plot> plot = resolvePlot(interpretation.nomeTalhao(), fullText, plots);
        if (plot.isEmpty()) {
            record.setStatus(FieldMessageStatus.SEM_TALHAO);
            record.setReply(missingPlotReply(plots));
            return save(record);
        }

        return transactionTemplate.execute(status -> {
            register(record, sender, message, mediaType, interpretation, plot.get(), today);
            return toResponse(fieldMessageRepository.save(record));
        });
    }

    @Transactional(readOnly = true)
    public List<FieldMessageResponse> findMine(User sender) {
        return fieldMessageRepository.findTop50BySenderIdOrderByCreatedAtDesc(sender.getId())
                .stream().map(this::toResponse).toList();
    }

    private void register(FieldMessage record, User sender, IncomingMessage message, EvidenceType mediaType,
                          Interpretation interpretation, Plot plot, LocalDate today) {
        UUID evidenceId = null;
        if (message.media() != null) {
            evidenceId = evidenceService.upload(plot.getId(), mediaType, new InMemoryMultipartFile(message.media()), sender).id();
        }
        String fallbackDescription = firstNonBlank(interpretation.resumo(), message.text(), record.getTranscript());

        if (record.getKind() == MessageKind.ATIVIDADE) {
            Interpretation.ActivityDraft draft = Optional.ofNullable(interpretation.atividade())
                    .orElse(new Interpretation.ActivityDraft(null, null, null, null, null));
            ActivityService.ActivityResponse activity = activityService.create(plot.getId(), new ActivityService.ActivityRequest(
                    Optional.ofNullable(draft.tipo()).orElse(ReportedActivityType.OUTRO).name(),
                    truncate(firstNonBlank(draft.descricao(), fallbackDescription), 1000),
                    Optional.ofNullable(draft.data()).orElse(today),
                    draft.custo() != null && draft.custo().signum() >= 0 ? draft.custo() : null,
                    truncate(draft.insumos(), 500)), sender);
            record.setActivityId(activity.id());
            record.setReply(truncate("✅ Atividade registrada no " + plot.getName() + ": "
                    + firstNonBlank(interpretation.resumo(), draft.descricao(), "atividade")
                    + missingFieldsHint(interpretation.camposFaltantes()), 1000));
        } else {
            Interpretation.OccurrenceDraft draft = Optional.ofNullable(interpretation.ocorrencia())
                    .orElse(new Interpretation.OccurrenceDraft(null, null, null));
            OccurrenceService.OccurrenceResponse occurrence = occurrenceService.register(plot.getId(),
                    new OccurrenceService.OccurrenceRequest(
                            Optional.ofNullable(draft.categoria()).orElse(OccurrenceCategory.OUTRO),
                            Optional.ofNullable(draft.severidade()).orElse(OccurrenceSeverity.MEDIA),
                            truncate(firstNonBlank(draft.descricao(), fallbackDescription, "Ocorrência enviada pelo campo"), 1000),
                            today,
                            evidenceId),
                    sender);
            record.setOccurrenceId(occurrence.id());
            record.setReply(truncate("⚠️ Ocorrência registrada no " + plot.getName() + ": "
                    + firstNonBlank(interpretation.resumo(), draft.descricao(), "ocorrência"), 1000));
        }
        record.setPlot(plot);
        record.setEvidenceId(evidenceId);
        record.setStatus(FieldMessageStatus.REGISTRADA);
    }

    private Optional<FieldMessageResponse> findDuplicate(IncomingMessage message) {
        if (message.externalId() == null) {
            return Optional.empty();
        }
        return transactionTemplate.execute(status -> fieldMessageRepository
                .findByChannelAndExternalId(message.channel(), message.externalId())
                .map(this::toResponse));
    }

    private FieldMessage newRecord(User sender, IncomingMessage message, EvidenceType mediaType) {
        FieldMessage record = new FieldMessage();
        record.setSender(sender);
        record.setChannel(message.channel());
        record.setExternalId(message.externalId());
        record.setText(truncate(message.text(), MAX_TEXT_LENGTH));
        record.setMediaType(mediaType);
        return record;
    }

    private FieldMessageResponse save(FieldMessage record) {
        return transactionTemplate.execute(status -> toResponse(fieldMessageRepository.save(record)));
    }

    /**
     * Usa o talhão escolhido pela IA; se ela não escolheu, procura no próprio texto; se o produtor só
     * tem um talhão e nada foi citado, usa esse.
     */
    static Optional<Plot> resolvePlot(String plotName, String text, List<Plot> plots) {
        List<String> names = plots.stream().map(Plot::getName).toList();
        Optional<String> matched = PlotMatcher.find(plotName, names);
        if (matched.isEmpty() && (plotName == null || plotName.isBlank())) {
            matched = PlotMatcher.find(text, names);
            if (matched.isEmpty() && plots.size() == 1) {
                return Optional.of(plots.get(0));
            }
        }
        return matched.flatMap(name -> plots.stream().filter(plot -> plot.getName().equals(name)).findFirst());
    }

    static EvidenceType mediaTypeOf(IncomingMedia media) {
        if (media == null) {
            return null;
        }
        String contentType = media.contentType() == null ? "" : media.contentType().toLowerCase(Locale.ROOT);
        if (contentType.startsWith("audio/") || contentType.startsWith("application/ogg")) {
            return EvidenceType.AUDIO;
        }
        if (contentType.startsWith("image/")) {
            return EvidenceType.FOTO;
        }
        throw new IllegalArgumentException("Envie apenas texto, áudio ou foto");
    }

    private static String missingPlotReply(List<Plot> plots) {
        if (plots.isEmpty()) {
            return "Você ainda não tem talhões cadastrados. Cadastre um talhão no app do NEXO Campo e envie a mensagem de novo.";
        }
        return truncate("Não identifiquei o talhão. Envie de novo citando um destes: "
                + plots.stream().map(Plot::getName).collect(Collectors.joining(", ")) + ".", 1000);
    }

    private static String missingFieldsHint(List<String> missingFields) {
        if (missingFields == null) {
            return "";
        }
        List<String> labels = missingFields.stream()
                .map(MISSING_FIELD_LABELS::get)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return labels.isEmpty() ? "" : "\nSe quiser, complete pelo app: faltou informar " + String.join(", ", labels) + ".";
    }

    private static String joinText(String text, String transcript) {
        boolean hasText = text != null && !text.isBlank();
        boolean hasTranscript = transcript != null && !transcript.isBlank();
        if (hasText && hasTranscript) {
            return text.strip() + "\n\nÁudio: " + transcript.strip();
        }
        return hasTranscript ? transcript.strip() : hasText ? text.strip() : "";
    }

    private static String firstNonBlank(String... values) {
        return Stream.of(values).filter(value -> value != null && !value.isBlank()).findFirst().map(String::strip).orElse(null);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String toJson(Interpretation interpretation) {
        try {
            return objectMapper.writeValueAsString(interpretation);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar a interpretação", exception);
        }
    }

    private Interpretation fromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Interpretation.class);
        } catch (JsonProcessingException exception) {
            log.warn("Interpretação salva não pôde ser lida: {}", exception.getOriginalMessage());
            return null;
        }
    }

    private FieldMessageResponse toResponse(FieldMessage message) {
        Plot plot = message.getPlot();
        return new FieldMessageResponse(message.getId(), message.getChannel(), message.getStatus(), message.getKind(),
                plot == null ? null : plot.getId(), plot == null ? null : plot.getName(),
                message.getActivityId(), message.getOccurrenceId(), message.getEvidenceId(),
                message.getText(), message.getTranscript(), message.getReply(),
                fromJson(message.getInterpretation()), message.getCreatedAt());
    }

    /**
     * @param resposta texto de confirmação para devolver ao produtor (é o que o bot do WhatsApp responde)
     */
    public record FieldMessageResponse(
            UUID id, MessageChannel canal, FieldMessageStatus situacao, MessageKind tipo,
            UUID talhaoId, String nomeTalhao, UUID atividadeId, UUID ocorrenciaId, UUID evidenciaId,
            String texto, String transcricao, String resposta, Interpretation interpretacao,
            OffsetDateTime criadaEm) {}
}
