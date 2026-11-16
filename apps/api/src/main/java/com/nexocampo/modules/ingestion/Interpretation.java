package com.nexocampo.modules.ingestion;

import com.nexocampo.modules.investigation.OccurrenceCategory;
import com.nexocampo.modules.investigation.OccurrenceSeverity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * O que a IA extraiu de uma mensagem de campo. Espelha
 * {@code resources/ai/field-message-interpretation.schema.json}; mude os dois juntos.
 *
 * @param plotName    nome exato de um talhão do produtor, ou {@code null} se não deu para identificar
 * @param activity    preenchido só quando {@code kind} é {@link MessageKind#ATIVIDADE}
 * @param occurrence  preenchido só quando {@code kind} é {@link MessageKind#OCORRENCIA}
 * @param missingFields campos importantes que o produtor não informou (ex.: {@code cost})
 * @param confidence  de 0 a 1
 */
public record Interpretation(
        MessageKind kind,
        String plotName,
        String summary,
        ActivityDraft activity,
        OccurrenceDraft occurrence,
        List<String> missingFields,
        Double confidence) {

    public record ActivityDraft(
            ReportedActivityType type,
            LocalDate date,
            String description,
            String inputs,
            BigDecimal cost) {}

    public record OccurrenceDraft(
            OccurrenceCategory category,
            OccurrenceSeverity severity,
            String description) {}
}
