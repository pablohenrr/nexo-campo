package com.nexocampo.modules.weather;

import com.nexocampo.modules.farm.Farm;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "preferencia_alerta_climatico")
public class WeatherAlertPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fazenda_id", nullable = false, unique = true)
    private Farm farm;

    @Column(name = "raio_km", nullable = false)
    private int raioKm = WeatherAlertRule.DEFAULT_RADIUS_KM;

    @Column(nullable = false, length = 200)
    private String categorias = "CHUVA,VENTO,TEMPESTADE";

    @Column(nullable = false, length = 20)
    private String frequencia = FrequenciaAlertaClimatico.POR_EVENTO.name();

    @Column(name = "criada_em", nullable = false, updatable = false)
    private OffsetDateTime criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private OffsetDateTime atualizadaEm;

    @PrePersist
    void onCreate() {
        criadaEm = OffsetDateTime.now();
        atualizadaEm = criadaEm;
    }

    @PreUpdate
    void onUpdate() {
        atualizadaEm = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }

    public int getRaioKm() {
        return raioKm;
    }

    public void setRaioKm(int raioKm) {
        this.raioKm = raioKm;
    }

    public String getCategorias() {
        return categorias;
    }

    public void setCategorias(String categorias) {
        this.categorias = categorias;
    }

    public String getFrequencia() {
        return frequencia;
    }

    public void setFrequencia(String frequencia) {
        this.frequencia = frequencia;
    }
}
