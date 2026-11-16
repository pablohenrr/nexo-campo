package com.nexocampo.modules.ingestion;

/**
 * Porta da IA que interpreta mensagens de campo.
 *
 * <p>Adaptadores: {@link LlmFieldMessageInterpreter} (provedor real) e
 * {@link StubFieldMessageInterpreter} (respostas simuladas, padrão em desenvolvimento).
 *
 * @throws com.nexocampo.shared.ai.AiProviderException quando o provedor falha
 */
public interface FieldMessageInterpreter {

    Interpretation interpret(InterpretationRequest request);
}
