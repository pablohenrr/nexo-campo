package com.nexocampo.modules.identity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "senha_hash")
    private String passwordHash;

    @Column(name = "provedor", length = 30)
    private String provider;

    @Column(name = "provedor_id", length = 255)
    private String providerSubject;

    @Enumerated(EnumType.STRING)
    @Column(name = "perfil", nullable = false, length = 30)
    private Role role = Role.PRODUTOR;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email.toLowerCase(); }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderSubject() { return providerSubject; }
    public void setProviderSubject(String providerSubject) { this.providerSubject = providerSubject; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
