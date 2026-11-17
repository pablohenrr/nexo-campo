package com.nexocampo.modules.ingestion;

import com.nexocampo.modules.investigation.OccurrenceCategory;
import com.nexocampo.modules.investigation.OccurrenceSeverity;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Interpretação por palavras-chave, sem rede. Serve para o time desenvolver e testar o fluxo completo
 * sem chave de API; não substitui a IA. Todo resumo leva "[modo de teste]" para ninguém confundir.
 */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "stub", matchIfMissing = true)
public class StubFieldMessageInterpreter implements FieldMessageInterpreter {

    private static final String TEST_MODE = "[modo de teste] ";
    private static final Pattern COST = Pattern.compile("(?:r\\$\\s*(\\d+(?:[.,]\\d{1,2})?))|(\\d+(?:[.,]\\d{1,2})?)\\s*(?:reais|conto)");

    private static final List<Map.Entry<String, ReportedActivityType>> ACTIVITY_KEYWORDS = List.of(
            Map.entry("plant", ReportedActivityType.PLANTIO),
            Map.entry("adub", ReportedActivityType.ADUBACAO),
            Map.entry("pulveriz", ReportedActivityType.PULVERIZACAO),
            Map.entry("apliq", ReportedActivityType.PULVERIZACAO),
            Map.entry("irrig", ReportedActivityType.IRRIGACAO),
            Map.entry("capin", ReportedActivityType.CAPINA),
            Map.entry("colh", ReportedActivityType.COLHEITA),
            Map.entry("monitor", ReportedActivityType.MONITORAMENTO),
            Map.entry("manuten", ReportedActivityType.MANUTENCAO));

    private static final List<Map.Entry<String, OccurrenceCategory>> OCCURRENCE_KEYWORDS = List.of(
            Map.entry("praga", OccurrenceCategory.PRAGA),
            Map.entry("lagarta", OccurrenceCategory.PRAGA),
            Map.entry("percevejo", OccurrenceCategory.PRAGA),
            Map.entry("mancha", OccurrenceCategory.DOENCA),
            Map.entry("ferrugem", OccurrenceCategory.DOENCA),
            Map.entry("fungo", OccurrenceCategory.DOENCA),
            Map.entry("daninha", OccurrenceCategory.PLANTA_DANINHA),
            Map.entry("amarel", OccurrenceCategory.DEFICIENCIA_NUTRICIONAL),
            Map.entry("granizo", OccurrenceCategory.CLIMA),
            Map.entry("geada", OccurrenceCategory.CLIMA));

    @Override
    public Interpretation interpret(InterpretationRequest request) {
        String text = request.text() == null ? "" : PlotMatcher.normalize(request.text());
        Optional<String> plotName = PlotMatcher.find(request.text(), request.plotNames());
        List<String> missing = new ArrayList<>();
        if (plotName.isEmpty()) {
            missing.add("nomeTalhao");
        }

        Optional<OccurrenceCategory> occurrenceCategory = firstMatch(text, OCCURRENCE_KEYWORDS);
        Optional<ReportedActivityType> activityType = firstMatch(text, ACTIVITY_KEYWORDS);

        if (occurrenceCategory.isPresent() || (request.image() != null && activityType.isEmpty())) {
            OccurrenceCategory category = occurrenceCategory.orElse(OccurrenceCategory.OUTRO);
            String description = text.isBlank() ? "Foto recebida para análise" : request.text().strip();
            return new Interpretation(MessageKind.OCORRENCIA, plotName.orElse(null),
                    TEST_MODE + "Ocorrência de " + category.name().toLowerCase().replace('_', ' '),
                    null, new Interpretation.OccurrenceDraft(category, OccurrenceSeverity.MEDIA, description),
                    missing, 0.5);
        }

        if (activityType.isPresent()) {
            BigDecimal cost = cost(text);
            if (cost == null) {
                missing.add("custo");
            }
            missing.add("insumos");
            return new Interpretation(MessageKind.ATIVIDADE, plotName.orElse(null),
                    TEST_MODE + "Atividade de " + activityType.get().name().toLowerCase(),
                    new Interpretation.ActivityDraft(activityType.get(), request.today(), request.text().strip(), null, cost),
                    null, missing, 0.5);
        }

        return new Interpretation(MessageKind.OUTRO, null, TEST_MODE + "Mensagem sem atividade ou ocorrência",
                null, null, List.of(), 0.5);
    }

    private static <T> Optional<T> firstMatch(String text, List<Map.Entry<String, T>> keywords) {
        return keywords.stream()
                .filter(entry -> text.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private static BigDecimal cost(String text) {
        Matcher matcher = COST.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        return new BigDecimal(value.replace(',', '.'));
    }
}
