package com.nexocampo.modules.webhook;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "evento_webhook")
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "carga_util", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "assinatura", nullable = false, length = 255)
    private String signature;

    @Column(name = "recebido_em", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;

    @PrePersist
    void onCreate() {
        receivedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }
}
