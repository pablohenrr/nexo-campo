package com.nexocampo.modules.weather;

import com.nexocampo.modules.identity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "leitura_alerta_climatico",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_leitura_alerta_climatico", columnNames = {"usuario_id", "evento_id"}))
public class WeatherAlertRead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private WeatherEvent event;

    @Column(name = "lida_em", nullable = false)
    private OffsetDateTime lidaEm;

    @PrePersist
    void onCreate() {
        if (lidaEm == null) {
            lidaEm = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public WeatherEvent getEvent() {
        return event;
    }

    public void setEvent(WeatherEvent event) {
        this.event = event;
    }

    public void setLidaEm(OffsetDateTime lidaEm) {
        this.lidaEm = lidaEm;
    }
}
