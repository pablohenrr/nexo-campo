package com.nexocampo.modules.ingestion;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/**
 * Tipos de atividade que a IA pode identificar. O valor é gravado em {@code activity.type}, que hoje
 * aceita texto livre; se o time oficializar uma lista de tipos, ela deve ser esta.
 */
public enum ReportedActivityType {
    PLANTIO,
    ADUBACAO,
    PULVERIZACAO,
    IRRIGACAO,
    CAPINA,
    COLHEITA,
    MONITORAMENTO,
    MANUTENCAO,
    @JsonEnumDefaultValue
    OUTRO
}
