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
 * @param nomeTalhao      nome exato de um talhão do produtor, ou {@code null} se não deu para identificar
 * @param atividade       preenchido só quando {@code tipo} é {@link MessageKind#ATIVIDADE}
 * @param ocorrencia      preenchido só quando {@code tipo} é {@link MessageKind#OCORRENCIA}
 * @param camposFaltantes campos importantes que o produtor não informou (ex.: {@code custo})
 * @param confianca       de 0 a 1
 */
public record Interpretation(
        MessageKind tipo,
        String nomeTalhao,
        String resumo,
        ActivityDraft atividade,
        OccurrenceDraft ocorrencia,
        List<String> camposFaltantes,
        Double confianca) {

    public record ActivityDraft(
            ReportedActivityType tipo,
            LocalDate data,
            String descricao,
            String insumos,
            BigDecimal custo) {}

    public record OccurrenceDraft(
            OccurrenceCategory categoria,
            OccurrenceSeverity severidade,
            String descricao) {}
}
