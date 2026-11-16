package com.nexocampo.modules.ingestion;

/**
 * Porta de transcrição de áudio.
 *
 * <p>Adaptadores: {@link LlmAudioTranscriber} (provedor real) e {@link StubAudioTranscriber}.
 *
 * @throws com.nexocampo.shared.ai.AiProviderException quando o provedor falha
 */
public interface AudioTranscriber {

    String transcribe(IncomingMedia audio);
}
