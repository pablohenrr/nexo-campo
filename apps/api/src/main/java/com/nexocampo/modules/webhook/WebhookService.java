package com.nexocampo.modules.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private static final String SIGNATURE_PREFIX = "sha256=";
    private final WebhookEventRepository repository;
    private final byte[] secret;

    public WebhookService(
            WebhookEventRepository repository,
            @Value("${app.webhook.secret}") String secret) {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("WEBHOOK_SECRET deve possuir pelo menos 32 bytes");
        }
        this.repository = repository;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public void receive(String payload, String signature) {
        if (!isValidSignature(payload, signature)) {
            throw new IllegalArgumentException("Assinatura do webhook inválida");
        }
        WebhookEvent event = new WebhookEvent();
        event.setPayload(payload);
        event.setSignature(signature);
        repository.save(event);
    }

    private boolean isValidSignature(String payload, String signature) {
        if (signature == null || !signature.startsWith(SIGNATURE_PREFIX)) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] expected = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            byte[] received = HexFormat.of().parseHex(signature.substring(SIGNATURE_PREFIX.length()));
            return MessageDigest.isEqual(expected, received);
        } catch (Exception exception) {
            return false;
        }
    }
}
