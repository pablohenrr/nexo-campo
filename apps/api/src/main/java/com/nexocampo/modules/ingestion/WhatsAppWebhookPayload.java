package com.nexocampo.modules.ingestion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Corpo do webhook da WhatsApp Cloud API, só com os campos que usamos. Os nomes seguem o formato da
 * Meta, não o padrão da nossa API. Dos avisos de entrega ({@code statuses}), só as falhas interessam.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record WhatsAppWebhookPayload(String object, List<Entry> entry) {

    /** Mensagens recebidas em todas as entradas do webhook. */
    List<Message> messages() {
        return values().map(Value::messages).filter(Objects::nonNull).flatMap(List::stream)
                .filter(message -> message.id() != null && message.from() != null)
                .toList();
    }

    /** Respostas nossas que a Meta avisou não ter conseguido entregar. */
    List<Status> failedStatuses() {
        return values().map(Value::statuses).filter(Objects::nonNull).flatMap(List::stream)
                .filter(status -> "failed".equals(status.status()))
                .toList();
    }

    private Stream<Value> values() {
        return Stream.ofNullable(entry).flatMap(List::stream)
                .map(Entry::changes).filter(Objects::nonNull).flatMap(List::stream)
                .map(Change::value).filter(Objects::nonNull);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Entry(String id, List<Change> changes) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Change(String field, Value value) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Value(
            @JsonProperty("messaging_product") String messagingProduct,
            List<Message> messages,
            List<Status> statuses) {}

    /**
     * Aviso de entrega de uma mensagem enviada por nós.
     *
     * @param status {@code sent}, {@code delivered}, {@code read} ou {@code failed}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Status(String id, String status, List<StatusError> errors) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StatusError(Integer code, String title, @JsonProperty("error_data") ErrorData errorData) {

        /** Ex.: {@code 131031 Business Account locked (Business account has been locked.)}. */
        String describe() {
            String details = errorData == null ? null : errorData.details();
            return code + " " + title + (details == null ? "" : " (" + details + ")");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErrorData(String details) {}

    /**
     * @param from telefone de quem enviou, só dígitos e com código do país (o {@code wa_id})
     * @param id   identificador da mensagem ({@code wamid...}), usado para ignorar reenvios
     * @param type {@code text}, {@code image}, {@code audio} ou outro tipo que não tratamos
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String from, String id, String timestamp, String type, Text text, Media image, Media audio) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Text(String body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Media(String id, @JsonProperty("mime_type") String mimeType, String caption) {}
}
