package com.nexocampo.modules.weather;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "evento_climatico",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_evento_climatico_fonte_externo", columnNames = {"fonte", "id_externo", "categoria"}))
public class WeatherEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String fonte;

    @Column(name = "id_externo", nullable = false, length = 200)
    private String idExterno;

    @Column(nullable = false, length = 30)
    private String categoria;

    @Column(nullable = false)
    private OffsetDateTime inicio;

    @Column(nullable = false)
    private OffsetDateTime fim;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "geometria")
    private String geometria;

    @Column(length = 2000)
    private String texto;

    @Column(nullable = false, length = 400)
    private String frase;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    void onCreate() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getFonte() {
        return fonte;
    }

    public void setFonte(String fonte) {
        this.fonte = fonte;
    }

    public String getIdExterno() {
        return idExterno;
    }

    public void setIdExterno(String idExterno) {
        this.idExterno = idExterno;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public OffsetDateTime getInicio() {
        return inicio;
    }

    public void setInicio(OffsetDateTime inicio) {
        this.inicio = inicio;
    }

    public OffsetDateTime getFim() {
        return fim;
    }

    public void setFim(OffsetDateTime fim) {
        this.fim = fim;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getGeometria() {
        return geometria;
    }

    public void setGeometria(String geometria) {
        this.geometria = geometria;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getFrase() {
        return frase;
    }

    public void setFrase(String frase) {
        this.frase = frase;
    }

    public OffsetDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(OffsetDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
