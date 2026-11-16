package com.nexocampo.modules.ingestion;

import com.nexocampo.shared.ai.OpenAiCompatibleClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Transcreve áudios pelo endpoint {@code /audio/transcriptions} (Whisper na Groq ou na OpenAI). */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai-compatible")
public class LlmAudioTranscriber implements AudioTranscriber {

    private final OpenAiCompatibleClient client;

    public LlmAudioTranscriber(OpenAiCompatibleClient client) {
        this.client = client;
    }

    @Override
    public String transcribe(IncomingMedia audio) {
        return client.transcribe(audio.content(), fileNameWithExtension(audio), audio.contentType());
    }

    /** O provedor descobre o formato pela extensão; áudios do WhatsApp às vezes chegam sem nome. */
    static String fileNameWithExtension(IncomingMedia audio) {
        String fileName = audio.fileName();
        if (fileName != null && fileName.contains(".")) {
            return fileName;
        }
        String contentType = audio.contentType() == null ? "" : audio.contentType();
        String extension = contentType.startsWith("audio/ogg") ? ".ogg"
                : contentType.startsWith("audio/mpeg") ? ".mp3"
                : contentType.startsWith("audio/wav") ? ".wav"
                : contentType.startsWith("audio/mp4") ? ".m4a"
                : ".ogg";
        return (fileName == null || fileName.isBlank() ? "audio" : fileName) + extension;
    }
}
