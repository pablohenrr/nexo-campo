package com.nexocampo.modules.ingestion;

import com.nexocampo.modules.identity.User;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Liga o WhatsApp do produtor à conta dele, para saber de quem é cada mensagem recebida. */
@Entity
@Table(name = "vinculo_whatsapp")
public class WhatsAppLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private User user;

    /** Só dígitos, com código do país. */
    @Column(name = "telefone", nullable = false, unique = true, length = 20)
    private String phone;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
