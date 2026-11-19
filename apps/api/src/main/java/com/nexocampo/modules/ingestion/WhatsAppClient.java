package com.nexocampo.modules.ingestion;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Chamadas à Graph API da Meta: baixar mídia recebida e responder no WhatsApp. */
public class WhatsAppClient {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "audio/ogg", ".ogg",
            "audio/mpeg", ".mp3",
            "audio/mp4", ".m4a",
            "audio/aac", ".aac",
            "audio/amr", ".amr",
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final RestClient restClient;
    private final WhatsAppProperties properties;

    public WhatsAppClient(RestClient.Builder builder, WhatsAppProperties properties) {
        this.properties = properties;
        RestClient.Builder configured = builder.baseUrl(properties.graphBaseUrl());
        if (hasText(properties.accessToken())) {
            configured = configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken());
        }
        this.restClient = configured.build();
    }

    /** Baixa foto ou áudio: a Graph API devolve uma URL temporária, que também exige o token. */
    public IncomingMedia downloadMedia(String mediaId) {
        requireToken();
        JsonNode media = restClient.get().uri("/{mediaId}", mediaId).retrieve().body(JsonNode.class);
        String url = media == null ? null : media.path("url").asText(null);
        if (url == null) {
            throw new IllegalStateException("A Meta não devolveu a URL da mídia " + mediaId);
        }
        String contentType = media.path("mime_type").asText(null);
        byte[] content = restClient.get().uri(URI.create(url)).retrieve().body(byte[].class);
        return new IncomingMedia(content, contentType, "whatsapp-" + mediaId + extensionOf(contentType));
    }

    /** Envia um texto; com {@code replyTo}, a resposta aparece citando a mensagem do produtor. */
    public void sendText(String to, String body, String replyTo) {
        requireSender();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("messaging_product", "whatsapp");
        message.put("recipient_type", "individual");
        message.put("to", to);
        message.put("type", "text");
        message.put("text", Map.of("preview_url", false, "body", body));
        if (replyTo != null) {
            message.put("context", Map.of("message_id", replyTo));
        }
        post(message);
    }

    /** Marca como lida (os dois tiques azuis) enquanto a IA processa. */
    public void markAsRead(String messageId) {
        requireSender();
        post(Map.of("messaging_product", "whatsapp", "status", "read", "message_id", messageId));
    }

    private void post(Map<String, Object> body) {
        restClient.post()
                .uri("/{phoneNumberId}/messages", properties.phoneNumberId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private void requireToken() {
        if (!hasText(properties.accessToken())) {
            throw new IllegalStateException("Configure WHATSAPP_ACCESS_TOKEN para falar com o WhatsApp");
        }
    }

    private void requireSender() {
        requireToken();
        if (!hasText(properties.phoneNumberId())) {
            throw new IllegalStateException("Configure WHATSAPP_PHONE_NUMBER_ID para responder no WhatsApp");
        }
    }

    private static String extensionOf(String contentType) {
        if (contentType == null) {
            return "";
        }
        String baseType = contentType.split(";")[0].strip().toLowerCase(Locale.ROOT);
        return EXTENSIONS.getOrDefault(baseType, "");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
