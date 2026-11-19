package com.nexocampo.modules.ingestion;

/**
 * Mensagem de campo, independente do canal.
 *
 * @param externalId identificador da mensagem no canal de origem (ex.: id do WhatsApp), usado para
 *                   ignorar reenvios; {@code null} quando o canal não tem um
 */
public record IncomingMessage(MessageChannel channel, String externalId, String text, IncomingMedia media) {

    public static IncomingMessage fromApp(String text, IncomingMedia media) {
        return new IncomingMessage(MessageChannel.APP, null, text, media);
    }

    public static IncomingMessage fromWhatsApp(String messageId, String text, IncomingMedia media) {
        return new IncomingMessage(MessageChannel.WHATSAPP, messageId, text, media);
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }
}
