package com.nexocampo.modules.ingestion;

/** Resultado do processamento de uma mensagem. */
public enum FieldMessageStatus {
    /** Virou uma atividade ou ocorrência no banco. */
    REGISTRADA,
    /** Entendida, mas sem talhão identificado; o produtor precisa reenviar citando o talhão. */
    SEM_TALHAO,
    /** Não continha atividade nem ocorrência (saudação, pergunta...). */
    IGNORADA,
    /** O provedor de IA falhou; nada foi registrado. */
    FALHOU
}
