package com.nexocampo.modules.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook da WhatsApp Cloud API. Rota pública (liberada no {@code SecurityConfig}); quem garante que a
 * chamada veio da Meta é a assinatura HMAC, validada sobre o corpo cru antes de qualquer leitura.
 */
@RestController
@RequestMapping("/api/webhooks/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppProperties properties;
    private final WhatsAppMessageProcessor processor;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookController(
            WhatsAppProperties properties,
            WhatsAppMessageProcessor processor,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.processor = processor;
        this.objectMapper = objectMapper;
    }

    /** Confirmação da URL ao cadastrar o webhook no painel da Meta. */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && WhatsAppSignature.sameToken(properties.verifyToken(), verifyToken)) {
            return ResponseEntity.ok(challenge);
        }
        log.warn("Verificação do webhook do WhatsApp recusada (modo {})", mode);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader(name = WhatsAppSignature.HEADER, required = false) String signature,
            @RequestBody byte[] body) {
        if (!WhatsAppSignature.isValid(properties.appSecret(), body, signature)) {
            log.warn("Webhook do WhatsApp com assinatura inválida ou WHATSAPP_APP_SECRET não configurado");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        WhatsAppWebhookPayload payload;
        try {
            payload = objectMapper.readValue(body, WhatsAppWebhookPayload.class);
        } catch (IOException exception) {
            log.warn("Webhook do WhatsApp com corpo inválido: {}", exception.getMessage());
            return ResponseEntity.badRequest().build();
        }
        processor.enqueue(payload.messages());
        payload.failedStatuses().forEach(WhatsAppWebhookController::logFailedDelivery);
        return ResponseEntity.ok().build();
    }

    /** A Meta aceita o envio e só avisa depois, por aqui, quando não consegue entregar. */
    private static void logFailedDelivery(WhatsAppWebhookPayload.Status status) {
        String reasons = status.errors() == null || status.errors().isEmpty()
                ? "sem detalhes"
                : status.errors().stream()
                        .map(WhatsAppWebhookPayload.StatusError::describe)
                        .collect(Collectors.joining("; "));
        log.warn("A Meta não entregou a mensagem {} do WhatsApp: {}", status.id(), reasons);
    }
}
