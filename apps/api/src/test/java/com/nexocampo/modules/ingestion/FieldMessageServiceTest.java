package com.nexocampo.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import com.nexocampo.modules.investigation.OccurrenceStatus;
import com.nexocampo.shared.ai.AiProviderException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FieldMessageServiceTest {

    // 23h em Brasília já é dia seguinte em UTC: a data precisa sair no fuso da fazenda.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T02:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    @Mock private FieldMessageRepository fieldMessageRepository;
    @Mock private PlotRepository plotRepository;
    @Mock private EvidenceService evidenceService;
    @Mock private ActivityService activityService;
    @Mock private OccurrenceService occurrenceService;
    @Mock private FieldMessageInterpreter interpreter;
    @Mock private AudioTranscriber transcriber;
    @Mock private PlatformTransactionManager transactionManager;

    private FieldMessageService service;
    private User sender;
    private Plot plot01;
    private Plot plot04;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
        service = new FieldMessageService(fieldMessageRepository, plotRepository, evidenceService, activityService,
                occurrenceService, interpreter, transcriber, objectMapper, transactionManager, CLOCK);
        sender = new User();
        ReflectionTestUtils.setField(sender, "id", UUID.randomUUID());
        plot01 = plot("Talhão 01");
        plot04 = plot("Talhão 04 - Milho Safrinha");
        lenient().when(fieldMessageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(plotRepository.findAllByFarmOwnerIdOrderByName(sender.getId())).thenReturn(List.of(plot01, plot04));
    }

    @Test
    void registersActivityOnPlotChosenByAi() {
        when(interpreter.interpret(any())).thenReturn(activity("Talhão 04 - Milho Safrinha", List.of("data")));
        UUID activityId = UUID.randomUUID();
        when(activityService.create(eq(plot04.getId()), any(), eq(sender))).thenReturn(activityResponse(activityId));

        var response = service.receive(sender, IncomingMessage.fromApp("Passei 2 L de glifosato no talhão 4, 800 reais", null));

        ArgumentCaptor<ActivityService.ActivityRequest> request = ArgumentCaptor.forClass(ActivityService.ActivityRequest.class);
        verify(activityService).create(eq(plot04.getId()), request.capture(), eq(sender));
        assertThat(request.getValue().tipo()).isEqualTo("PULVERIZACAO");
        assertThat(request.getValue().dataAtividade()).isEqualTo(TODAY);
        assertThat(request.getValue().custo()).isEqualByComparingTo("800");
        assertThat(request.getValue().insumos()).isEqualTo("Glifosato 2 L");
        assertThat(response.situacao()).isEqualTo(FieldMessageStatus.REGISTRADA);
        assertThat(response.atividadeId()).isEqualTo(activityId);
        assertThat(response.talhaoId()).isEqualTo(plot04.getId());
        assertThat(response.resposta()).contains("Talhão 04 - Milho Safrinha").contains("faltou informar data");
        assertThat(response.interpretacao().tipo()).isEqualTo(MessageKind.ATIVIDADE);
    }

    @Test
    void findsPlotInTextWhenAiDoesNotChooseOne() {
        when(interpreter.interpret(any())).thenReturn(activity(null, List.of()));
        when(activityService.create(eq(plot04.getId()), any(), eq(sender))).thenReturn(activityResponse(UUID.randomUUID()));

        var response = service.receive(sender, IncomingMessage.fromApp("passei veneno no talhao 4", null));

        assertThat(response.talhaoId()).isEqualTo(plot04.getId());
        assertThat(response.situacao()).isEqualTo(FieldMessageStatus.REGISTRADA);
    }

    @Test
    void asksForPlotWhenItCannotBeIdentified() {
        when(interpreter.interpret(any())).thenReturn(activity(null, List.of("nomeTalhao")));

        var response = service.receive(sender, IncomingMessage.fromApp("passei veneno hoje", null));

        assertThat(response.situacao()).isEqualTo(FieldMessageStatus.SEM_TALHAO);
        assertThat(response.resposta()).contains("Talhão 01").contains("Talhão 04 - Milho Safrinha");
        verifyNoInteractions(activityService, occurrenceService, evidenceService);
    }

    @Test
    void doesNotGuessWhenAiNamesAnUnknownPlot() {
        when(plotRepository.findAllByFarmOwnerIdOrderByName(sender.getId())).thenReturn(List.of(plot01));
        when(interpreter.interpret(any())).thenReturn(activity("Talhão 07", List.of()));

        var response = service.receive(sender, IncomingMessage.fromApp("passei veneno no talhão 7", null));

        assertThat(response.situacao()).isEqualTo(FieldMessageStatus.SEM_TALHAO);
        verifyNoInteractions(activityService);
    }

    @Test
    void usesOnlyPlotWhenProducerHasOneAndNoneWasMentioned() {
        when(plotRepository.findAllByFarmOwnerIdOrderByName(sender.getId())).thenReturn(List.of(plot01));
        when(interpreter.interpret(any())).thenReturn(activity(null, List.of()));
        when(activityService.create(eq(plot01.getId()), any(), eq(sender))).thenReturn(activityResponse(UUID.randomUUID()));

        var response = service.receive(sender, IncomingMessage.fromApp("passei veneno hoje", null));

        assertThat(response.talhaoId()).isEqualTo(plot01.getId());
    }

    @Test
    void ignoresMessagesWithoutFieldInformation() {
        when(interpreter.interpret(any())).thenReturn(new Interpretation(
                MessageKind.OUTRO, null, "Saudação", null, null, List.of(), 0.9));

        var response = service.receive(sender, IncomingMessage.fromApp("bom dia!", null));

        assertThat(response.situacao()).isEqualTo(FieldMessageStatus.IGNORADA);
        assertThat(response.resposta()).isEqualTo(FieldMessageService.IGNORED_REPLY);
        verifyNoInteractions(activityService, occurrenceService, evidenceService);
    }

    @Test
    void keepsTheMessageWhenAiProviderFails() {
        when(interpreter.interpret(any())).thenThrow(new AiProviderException("O provedor de IA respondeu com erro 429"));

        var response = service.receive(sender, IncomingMessage.fromApp("passei veneno no talhão 4", null));

        ArgumentCaptor<FieldMessage> saved = ArgumentCaptor.forClass(FieldMessage.class);
        verify(fieldMessageRepository).save(saved.capture());
        assertThat(saved.getValue().getErrorMessage()).contains("429");
        assertThat(saved.getValue().getText()).isEqualTo("passei veneno no talhão 4");
        assertThat(response.situacao()).isEqualTo(FieldMessageStatus.FALHOU);
        assertThat(response.resposta()).isEqualTo(FieldMessageService.FAILURE_REPLY);
    }

    @Test
    void transcribesAudioAndKeepsItAsEvidenceOfTheOccurrence() {
        IncomingMedia audio = new IncomingMedia(new byte[] {1, 2, 3}, "audio/ogg; codecs=opus", "audio.ogg");
        when(transcriber.transcribe(audio)).thenReturn("Apareceu lagarta no talhão 1");
        when(interpreter.interpret(any())).thenReturn(new Interpretation(MessageKind.OCORRENCIA, "Talhão 01",
                "Lagartas no Talhão 01", null,
                new Interpretation.OccurrenceDraft(OccurrenceCategory.PRAGA, OccurrenceSeverity.ALTA, "Lagartas nas folhas"),
                List.of(), 0.8));
        UUID evidenceId = UUID.randomUUID();
        when(evidenceService.upload(eq(plot01.getId()), eq(EvidenceType.AUDIO), any(MultipartFile.class), eq(sender)))
                .thenReturn(new EvidenceService.EvidenceResponse(evidenceId, plot01.getId(), EvidenceType.AUDIO,
                        "audio.ogg", "audio/ogg", 3, "key.ogg", OffsetDateTime.now()));
        UUID occurrenceId = UUID.randomUUID();
        when(occurrenceService.register(eq(plot01.getId()), any(), eq(sender))).thenReturn(occurrenceResponse(occurrenceId));

        var response = service.receive(sender, new IncomingMessage(MessageChannel.WHATSAPP, "wamid.1", null, audio));

        ArgumentCaptor<InterpretationRequest> interpretation = ArgumentCaptor.forClass(InterpretationRequest.class);
        verify(interpreter).interpret(interpretation.capture());
        assertThat(interpretation.getValue().text()).isEqualTo("Apareceu lagarta no talhão 1");
        assertThat(interpretation.getValue().image()).isNull();
        assertThat(interpretation.getValue().today()).isEqualTo(TODAY);

        ArgumentCaptor<OccurrenceService.OccurrenceRequest> occurrence = ArgumentCaptor.forClass(OccurrenceService.OccurrenceRequest.class);
        verify(occurrenceService).register(eq(plot01.getId()), occurrence.capture(), eq(sender));
        assertThat(occurrence.getValue().categoria()).isEqualTo(OccurrenceCategory.PRAGA);
        assertThat(occurrence.getValue().evidenciaId()).isEqualTo(evidenceId);
        assertThat(occurrence.getValue().dataObservacao()).isEqualTo(TODAY);

        assertThat(response.situacao()).isEqualTo(FieldMessageStatus.REGISTRADA);
        assertThat(response.transcricao()).isEqualTo("Apareceu lagarta no talhão 1");
        assertThat(response.evidenciaId()).isEqualTo(evidenceId);
        assertThat(response.ocorrenciaId()).isEqualTo(occurrenceId);
    }

    @Test
    void sendsPhotoToInterpreterWithoutTranscribing() {
        IncomingMedia photo = new IncomingMedia(new byte[] {1, 2, 3}, "image/jpeg", "foto.jpg");
        when(interpreter.interpret(any())).thenReturn(new Interpretation(MessageKind.OUTRO, null, "Foto sem lavoura",
                null, null, List.of(), 0.7));

        service.receive(sender, IncomingMessage.fromApp(null, photo));

        ArgumentCaptor<InterpretationRequest> interpretation = ArgumentCaptor.forClass(InterpretationRequest.class);
        verify(interpreter).interpret(interpretation.capture());
        assertThat(interpretation.getValue().image()).isSameAs(photo);
        verifyNoInteractions(transcriber);
    }

    @Test
    void returnsPreviousResultWhenChannelResendsTheSameMessage() {
        FieldMessage previous = new FieldMessage();
        previous.setChannel(MessageChannel.WHATSAPP);
        previous.setExternalId("wamid.1");
        previous.setStatus(FieldMessageStatus.REGISTRADA);
        when(fieldMessageRepository.findByChannelAndExternalId(MessageChannel.WHATSAPP, "wamid.1"))
                .thenReturn(Optional.of(previous));

        var response = service.receive(sender, new IncomingMessage(MessageChannel.WHATSAPP, "wamid.1", "oi", null));

        assertThat(response.situacao()).isEqualTo(FieldMessageStatus.REGISTRADA);
        verifyNoInteractions(interpreter);
        verify(fieldMessageRepository, never()).save(any());
    }

    @Test
    void rejectsEmptyMessage() {
        assertThatThrownBy(() -> service.receive(sender, IncomingMessage.fromApp("  ", null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(interpreter);
    }

    @Test
    void rejectsMediaThatIsNotAudioOrPhoto() {
        IncomingMedia pdf = new IncomingMedia(new byte[] {1}, "application/pdf", "laudo.pdf");

        assertThatThrownBy(() -> service.receive(sender, IncomingMessage.fromApp(null, pdf)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(interpreter);
    }

    private static Interpretation activity(String plotName, List<String> missingFields) {
        return new Interpretation(MessageKind.ATIVIDADE, plotName, "Pulverização com glifosato",
                new Interpretation.ActivityDraft(ReportedActivityType.PULVERIZACAO, null, "Pulverização com glifosato",
                        "Glifosato 2 L", new BigDecimal("800")),
                null, missingFields, 0.9);
    }

    private ActivityService.ActivityResponse activityResponse(UUID id) {
        return new ActivityService.ActivityResponse(id, plot04.getId(), "PULVERIZACAO", "Pulverização", TODAY,
                new BigDecimal("800"), "Glifosato 2 L", sender.getId());
    }

    private OccurrenceService.OccurrenceResponse occurrenceResponse(UUID id) {
        return new OccurrenceService.OccurrenceResponse(id, plot01.getId(), OccurrenceCategory.PRAGA,
                OccurrenceSeverity.ALTA, OccurrenceStatus.ABERTA, "Lagartas nas folhas", TODAY, null,
                sender.getId(), OffsetDateTime.now());
    }

    private static Plot plot(String name) {
        Plot plot = new Plot();
        plot.setName(name);
        ReflectionTestUtils.setField(plot, "id", UUID.randomUUID());
        return plot;
    }
}
