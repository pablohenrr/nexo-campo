package com.nexocampo.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nexocampo.modules.identity.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class WhatsAppMessageProcessorTest {

    private static final String PHONE = "5511912345678";
    private static final String MESSAGE_ID = "wamid.HBgLMTU1";

    private FieldMessageService fieldMessageService;
    private WhatsAppLinkService linkService;
    private WhatsAppClient whatsAppClient;
    private WhatsAppMessageProcessor processor;
    private final User producer = new User();

    @BeforeEach
    void setUp() {
        fieldMessageService = mock(FieldMessageService.class);
        linkService = mock(WhatsAppLinkService.class);
        whatsAppClient = mock(WhatsAppClient.class);
        processor = new WhatsAppMessageProcessor(fieldMessageService, linkService, whatsAppClient, Runnable::run);
        when(linkService.findUserByPhone(PHONE)).thenReturn(Optional.of(producer));
        when(fieldMessageService.receive(any(), any())).thenReturn(response("✅ Atividade registrada no Talhão 04"));
    }

    @Test
    void registersTextFromALinkedProducerAndRepliesInTheSameChat() {
        processor.enqueue(List.of(message("text", new WhatsAppWebhookPayload.Text("passei glifosato no talhão 4"), null, null)));

        ArgumentCaptor<IncomingMessage> incoming = ArgumentCaptor.forClass(IncomingMessage.class);
        verify(fieldMessageService).receive(eq(producer), incoming.capture());
        assertThat(incoming.getValue().channel()).isEqualTo(MessageChannel.WHATSAPP);
        assertThat(incoming.getValue().externalId()).isEqualTo(MESSAGE_ID);
        assertThat(incoming.getValue().text()).isEqualTo("passei glifosato no talhão 4");
        verify(whatsAppClient).markAsRead(MESSAGE_ID);
        verify(whatsAppClient).sendText(PHONE, "✅ Atividade registrada no Talhão 04", MESSAGE_ID);
    }

    @Test
    void downloadsPhotoAndKeepsTheCaption() {
        IncomingMedia photo = new IncomingMedia(new byte[] {1, 2, 3}, "image/jpeg", "whatsapp-99.jpg");
        when(whatsAppClient.downloadMedia("99")).thenReturn(photo);

        processor.process(message("image", null, new WhatsAppWebhookPayload.Media("99", "image/jpeg", "ferrugem no talhão 4"), null));

        ArgumentCaptor<IncomingMessage> incoming = ArgumentCaptor.forClass(IncomingMessage.class);
        verify(fieldMessageService).receive(eq(producer), incoming.capture());
        assertThat(incoming.getValue().text()).isEqualTo("ferrugem no talhão 4");
        assertThat(incoming.getValue().media()).isSameAs(photo);
    }

    @Test
    void downloadsVoiceNotes() {
        IncomingMedia audio = new IncomingMedia(new byte[] {1}, "audio/ogg; codecs=opus", "whatsapp-77.ogg");
        when(whatsAppClient.downloadMedia("77")).thenReturn(audio);

        processor.process(message("audio", null, null, new WhatsAppWebhookPayload.Media("77", "audio/ogg; codecs=opus", null)));

        ArgumentCaptor<IncomingMessage> incoming = ArgumentCaptor.forClass(IncomingMessage.class);
        verify(fieldMessageService).receive(eq(producer), incoming.capture());
        assertThat(incoming.getValue().media()).isSameAs(audio);
        assertThat(incoming.getValue().text()).isNull();
    }

    @Test
    void asksUnknownNumbersToLinkTheirWhatsApp() {
        when(linkService.findUserByPhone(PHONE)).thenReturn(Optional.empty());

        processor.process(message("text", new WhatsAppWebhookPayload.Text("oi"), null, null));

        verify(whatsAppClient).sendText(PHONE, WhatsAppMessageProcessor.NOT_LINKED_REPLY, MESSAGE_ID);
        verify(fieldMessageService, never()).receive(any(), any());
    }

    @Test
    void explainsWhichFormatsAreSupported() {
        processor.process(message("sticker", null, null, null));

        verify(whatsAppClient).sendText(PHONE, WhatsAppMessageProcessor.UNSUPPORTED_REPLY, MESSAGE_ID);
        verify(fieldMessageService, never()).receive(any(), any());
    }

    @Test
    void ignoresRedeliveredMessagesWithoutAnsweringAgain() {
        when(fieldMessageService.alreadyReceived(MessageChannel.WHATSAPP, MESSAGE_ID)).thenReturn(true);

        processor.process(message("text", new WhatsAppWebhookPayload.Text("passei glifosato"), null, null));

        verify(fieldMessageService, never()).receive(any(), any());
        verifyNoInteractions(whatsAppClient);
    }

    @Test
    void ignoresTheLoserOfASimultaneousRedelivery() {
        when(fieldMessageService.receive(any(), any())).thenThrow(new DataIntegrityViolationException("uq_mensagem_campo"));

        processor.process(message("text", new WhatsAppWebhookPayload.Text("passei glifosato"), null, null));

        verify(whatsAppClient, never()).sendText(anyString(), anyString(), anyString());
    }

    @Test
    void repliesWithTheValidationMessage() {
        when(fieldMessageService.receive(any(), any()))
                .thenThrow(new IllegalArgumentException("Assinatura do arquivo não reconhecida"));

        processor.process(message("text", new WhatsAppWebhookPayload.Text("passei glifosato"), null, null));

        verify(whatsAppClient).sendText(PHONE,
                "Não consegui registrar sua mensagem: Assinatura do arquivo não reconhecida", MESSAGE_ID);
    }

    @Test
    void apologizesWhenMediaCannotBeDownloaded() {
        when(whatsAppClient.downloadMedia("99")).thenThrow(new IllegalStateException("Meta fora do ar"));

        processor.process(message("image", null, new WhatsAppWebhookPayload.Media("99", "image/jpeg", null), null));

        verify(whatsAppClient).sendText(PHONE, WhatsAppMessageProcessor.FAILURE_REPLY, MESSAGE_ID);
    }

    @Test
    void survivesWhenTheReplyCannotBeSent() {
        doThrow(new IllegalStateException("token vencido")).when(whatsAppClient).sendText(anyString(), anyString(), anyString());
        doThrow(new IllegalStateException("token vencido")).when(whatsAppClient).markAsRead(anyString());

        processor.process(message("text", new WhatsAppWebhookPayload.Text("passei glifosato"), null, null));

        verify(fieldMessageService).receive(eq(producer), any());
    }

    private static WhatsAppWebhookPayload.Message message(
            String type, WhatsAppWebhookPayload.Text text, WhatsAppWebhookPayload.Media image, WhatsAppWebhookPayload.Media audio) {
        return new WhatsAppWebhookPayload.Message(PHONE, MESSAGE_ID, "1763400000", type, text, image, audio);
    }

    private static FieldMessageService.FieldMessageResponse response(String reply) {
        return new FieldMessageService.FieldMessageResponse(UUID.randomUUID(), MessageChannel.WHATSAPP,
                FieldMessageStatus.REGISTRADA, MessageKind.ATIVIDADE, UUID.randomUUID(), "Talhão 04", UUID.randomUUID(),
                null, null, "passei glifosato", null, reply, null, OffsetDateTime.now());
    }
}
