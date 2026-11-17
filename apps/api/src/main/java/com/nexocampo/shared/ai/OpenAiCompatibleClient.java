package com.nexocampo.shared.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente HTTP para APIs no formato da OpenAI ({@code /chat/completions} e {@code /audio/transcriptions}).
 *
 * <p>Esse formato é aceito por OpenAI, Gemini, Groq, OpenRouter e Ollama, então trocar de provedor
 * é só mudar URL, chave e modelo no ambiente. Não use esta classe direto nos módulos: crie uma porta
 * (interface) no módulo e um adaptador que delega para cá.
 */
public class OpenAiCompatibleClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);
    private static final int MAX_LOGGED_BODY = 500;
    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final Duration DEFAULT_RETRY_BACKOFF = Duration.ofSeconds(1);

    private final RestClient chatClient;
    private final RestClient transcriptionClient;
    private final AiProperties.Chat chat;
    private final AiProperties.Transcription transcription;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;
    private final Duration retryBackoff;

    public OpenAiCompatibleClient(RestClient.Builder builder, AiProperties properties, ObjectMapper objectMapper) {
        this.chat = require(properties.chat(), "app.ai.chat");
        this.transcription = require(properties.transcription(), "app.ai.transcription");
        this.objectMapper = objectMapper;
        this.maxAttempts = properties.maxAttempts() > 0 ? properties.maxAttempts() : DEFAULT_MAX_ATTEMPTS;
        this.retryBackoff = properties.retryBackoff() != null ? properties.retryBackoff() : DEFAULT_RETRY_BACKOFF;
        this.chatClient = client(builder, chat.baseUrl(), chat.apiKey(), "AI_CHAT_BASE_URL", "AI_CHAT_API_KEY");
        this.transcriptionClient = client(builder, transcription.baseUrl(), transcription.apiKey(),
                "AI_TRANSCRIPTION_BASE_URL", "AI_TRANSCRIPTION_API_KEY");
    }

    /**
     * Envia uma conversa e devolve o JSON gerado pelo modelo, já sem cercas de Markdown.
     *
     * @param userContent partes da mensagem do usuário; use {@link #textPart} e {@link #imagePart}
     */
    public String chatJson(String systemPrompt, List<Map<String, Object>> userContent, String schemaName, JsonNode schema) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", chat.model());
        body.put("messages", List.of(
                Map.of("role", "system", "content", chat.jsonSchema() ? systemPrompt : withInlineSchema(systemPrompt, schema)),
                Map.of("role", "user", "content", userContent)));
        body.put("response_format", chat.jsonSchema()
                ? Map.of("type", "json_schema", "json_schema", Map.of("name", schemaName, "schema", schema))
                : Map.of("type", "json_object"));

        JsonNode response = call("chat", () -> chatClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class));
        String content = response == null ? null : response.path("choices").path(0).path("message").path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new AiProviderException("O provedor de IA devolveu uma resposta vazia");
        }
        return stripCodeFence(content);
    }

    /** Transcreve um áudio. O nome do arquivo precisa ter extensão, porque o provedor detecta o formato por ela. */
    public String transcribe(byte[] audio, String fileName, String contentType) {
        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        fileHeaders.setContentDispositionFormData("file", fileName);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new HttpEntity<>(new NamedByteArrayResource(audio, fileName), fileHeaders));
        parts.add("model", transcription.model());
        parts.add("language", "pt");
        parts.add("response_format", "json");

        JsonNode response = call("transcription", () -> transcriptionClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(parts)
                .retrieve()
                .body(JsonNode.class));
        String text = response == null ? null : response.path("text").asText(null);
        if (text == null) {
            throw new AiProviderException("O provedor de transcrição devolveu uma resposta sem texto");
        }
        return text.strip();
    }

    public static Map<String, Object> textPart(String text) {
        return Map.of("type", "text", "text", text);
    }

    public static Map<String, Object> imagePart(byte[] image, String contentType) {
        String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(image);
        return Map.of("type", "image_url", "image_url", Map.of("url", dataUrl));
    }

    /**
     * Executa a chamada repetindo em falhas temporárias (5xx e erro de rede), que são comuns nos planos
     * gratuitos ("model is experiencing high demand"). Erros 4xx, inclusive 429 de cota, não são repetidos.
     */
    private JsonNode call(String operation, Supplier<JsonNode> request) {
        for (int attempt = 1; ; attempt++) {
            try {
                return request.get();
            } catch (RestClientResponseException exception) {
                int status = exception.getStatusCode().value();
                if (exception.getStatusCode().is5xxServerError() && attempt < maxAttempts) {
                    log.info("Provedor de IA indisponível na chamada de {} (status {}), tentativa {} de {}",
                            operation, status, attempt, maxAttempts);
                    pause(attempt);
                    continue;
                }
                log.warn("Provedor de IA recusou a chamada de {} com status {}: {}", operation, status,
                        abbreviate(exception.getResponseBodyAsString()));
                throw new AiProviderException("O provedor de IA respondeu com erro " + status, exception);
            } catch (RestClientException exception) {
                if (attempt < maxAttempts) {
                    log.info("Falha de rede na chamada de {}, tentativa {} de {}", operation, attempt, maxAttempts);
                    pause(attempt);
                    continue;
                }
                throw new AiProviderException("Não foi possível falar com o provedor de IA", exception);
            }
        }
    }

    private void pause(int attempt) {
        try {
            Thread.sleep(retryBackoff.multipliedBy(attempt).toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("Chamada ao provedor de IA interrompida", exception);
        }
    }

    private String withInlineSchema(String systemPrompt, JsonNode schema) {
        try {
            return systemPrompt + "\n\nResponda com um JSON que siga este JSON Schema:\n"
                    + objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Schema de resposta inválido", exception);
        }
    }

    static String stripCodeFence(String content) {
        String trimmed = content.strip();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int closingFence = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || closingFence <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, closingFence).strip();
    }

    private static RestClient client(RestClient.Builder builder, String baseUrl, String apiKey,
                                     String baseUrlVariable, String apiKeyVariable) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Configure " + baseUrlVariable + " para usar o provedor de IA");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Configure " + apiKeyVariable + " para usar o provedor de IA");
        }
        return builder.clone()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    private static <T> T require(T value, String property) {
        if (value == null) {
            throw new IllegalStateException("Configure " + property + " para usar o provedor de IA");
        }
        return value;
    }

    private static String abbreviate(String value) {
        if (value == null || value.length() <= MAX_LOGGED_BODY) {
            return value;
        }
        return value.substring(0, MAX_LOGGED_BODY) + "...";
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String fileName;

        NamedByteArrayResource(byte[] content, String fileName) {
            super(content);
            this.fileName = fileName;
        }

        @Override
        public String getFilename() {
            return fileName;
        }
    }
}
