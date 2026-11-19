package com.nexocampo.modules.ingestion;

import com.nexocampo.modules.identity.User;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

/**
 * Processa as mensagens do WhatsApp fora da requisição do webhook: a Meta espera resposta rápida e
 * reenvia o que demora, enquanto transcrição e IA levam segundos.
 */
@Service
public class WhatsAppMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppMessageProcessor.class);

    static final String NOT_LINKED_REPLY = "Olá! Este número ainda não está ligado a uma conta do NEXO Campo. "
            + "Cadastre seu WhatsApp no app e envie a mensagem de novo.";
    static final String UNSUPPORTED_REPLY = "Por enquanto eu entendo texto, áudio e foto. "
            + "Envie sua atividade ou ocorrência em um desses formatos.";
    static final String FAILURE_REPLY = "Não consegui processar sua mensagem agora. Tente de novo em alguns minutos.";

    private final FieldMessageService fieldMessageService;
    private final WhatsAppLinkService linkService;
    private final WhatsAppClient whatsAppClient;
    private final Executor executor;

    @Autowired
    public WhatsAppMessageProcessor(
            FieldMessageService fieldMessageService,
            WhatsAppLinkService linkService,
            WhatsAppClient whatsAppClient) {
        this(fieldMessageService, linkService, whatsAppClient, newExecutor());
    }

    WhatsAppMessageProcessor(
            FieldMessageService fieldMessageService,
            WhatsAppLinkService linkService,
            WhatsAppClient whatsAppClient,
            Executor executor) {
        this.fieldMessageService = fieldMessageService;
        this.linkService = linkService;
        this.whatsAppClient = whatsAppClient;
        this.executor = executor;
    }

    /** Agenda o processamento e volta na hora. */
    public void enqueue(List<WhatsAppWebhookPayload.Message> messages) {
        for (WhatsAppWebhookPayload.Message message : messages) {
            try {
                executor.execute(() -> process(message));
            } catch (RejectedExecutionException exception) {
                log.error("Fila do WhatsApp cheia; mensagem {} descartada", message.id());
            }
        }
    }

    void process(WhatsAppWebhookPayload.Message message) {
        try {
            if (fieldMessageService.alreadyReceived(MessageChannel.WHATSAPP, message.id())) {
                log.info("Mensagem do WhatsApp {} já processada; reenvio ignorado", message.id());
                return;
            }
            Optional<User> sender = linkService.findUserByPhone(message.from());
            if (sender.isEmpty()) {
                reply(message, NOT_LINKED_REPLY);
                return;
            }
            markAsRead(message);
            IncomingMessage incoming = toIncomingMessage(message);
            if (incoming == null) {
                reply(message, UNSUPPORTED_REPLY);
                return;
            }
            FieldMessageService.FieldMessageResponse response = fieldMessageService.receive(sender.get(), incoming);
            reply(message, response.resposta());
        } catch (DataIntegrityViolationException exception) {
            log.info("Mensagem do WhatsApp {} chegou duas vezes ao mesmo tempo; a segunda foi ignorada", message.id());
        } catch (IllegalArgumentException exception) {
            reply(message, "Não consegui registrar sua mensagem: " + exception.getMessage());
        } catch (RuntimeException exception) {
            log.error("Falha ao processar a mensagem do WhatsApp {}", message.id(), exception);
            reply(message, FAILURE_REPLY);
        }
    }

    private IncomingMessage toIncomingMessage(WhatsAppWebhookPayload.Message message) {
        String type = message.type() == null ? "" : message.type();
        return switch (type) {
            case "text" -> message.text() == null || message.text().body() == null || message.text().body().isBlank()
                    ? null
                    : IncomingMessage.fromWhatsApp(message.id(), message.text().body(), null);
            case "image" -> message.image() == null ? null : IncomingMessage.fromWhatsApp(
                    message.id(), message.image().caption(), whatsAppClient.downloadMedia(message.image().id()));
            case "audio" -> message.audio() == null ? null : IncomingMessage.fromWhatsApp(
                    message.id(), null, whatsAppClient.downloadMedia(message.audio().id()));
            default -> null;
        };
    }

    private void markAsRead(WhatsAppWebhookPayload.Message message) {
        try {
            whatsAppClient.markAsRead(message.id());
        } catch (RuntimeException exception) {
            log.debug("Não foi possível marcar a mensagem {} como lida: {}", message.id(), exception.getMessage());
        }
    }

    private void reply(WhatsAppWebhookPayload.Message message, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            whatsAppClient.sendText(message.from(), text, message.id());
        } catch (RuntimeException exception) {
            log.warn("Não foi possível responder a mensagem do WhatsApp {}: {}", message.id(), exception.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        if (executor instanceof ThreadPoolTaskExecutor pool) {
            pool.shutdown();
        }
    }

    /**
     * Fila própria em vez de um bean {@code Executor}: um bean desses desligaria o executor padrão do
     * Spring Boot para o resto da aplicação. Poucas threads para não estourar o limite do plano grátis da IA.
     */
    private static ThreadPoolTaskExecutor newExecutor() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(4);
        pool.setMaxPoolSize(4);
        pool.setQueueCapacity(200);
        pool.setThreadNamePrefix("whatsapp-");
        pool.setWaitForTasksToCompleteOnShutdown(true);
        pool.setAwaitTerminationSeconds(30);
        pool.initialize();
        return pool;
    }
}
