package com.nexocampo.modules.ingestion;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

/** O que a IA entendeu da mensagem. */
public enum MessageKind {
    ATIVIDADE,
    OCORRENCIA,
    @JsonEnumDefaultValue
    OUTRO
}
