package com.nexocampo.shared.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do provedor de IA.
 *
 * <p>{@code provider} aceita {@code stub} (respostas simuladas, sem rede) ou {@code openai-compatible}
 * (qualquer API no formato da OpenAI: OpenAI, Gemini, Groq, Ollama...). Chat e transcrição têm
 * endpoints separados porque nem todo provedor oferece os dois.
 *
 * @param maxAttempts  tentativas por chamada em falhas temporárias (5xx e rede); padrão 3
 * @param retryBackoff espera antes da 2ª tentativa, multiplicada a cada nova tentativa; padrão 1s
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String provider,
        Duration timeout,
        int maxAttempts,
        Duration retryBackoff,
        Chat chat,
        Transcription transcription) {

    public record Chat(String baseUrl, String apiKey, String model, boolean jsonSchema) {}

    public record Transcription(String baseUrl, String apiKey, String model) {}
}
