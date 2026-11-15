package com.nexocampo.modules.farm;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "plot")
public class Plot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    private Farm farm;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 120)
    private String crop;

    @Column(precision = 12, scale = 3)
    private BigDecimal areaHectares;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlotStatus status = PlotStatus.ATIVO;

    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon boundary;

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
    public Farm getFarm() { return farm; }
    public void setFarm(Farm farm) { this.farm = farm; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCrop() { return crop; }
    public void setCrop(String crop) { this.crop = crop; }
    public BigDecimal getAreaHectares() { return areaHectares; }
    public void setAreaHectares(BigDecimal areaHectares) { this.areaHectares = areaHectares; }
    public PlotStatus getStatus() { return status; }
    public void setStatus(PlotStatus status) { this.status = status; }
    public Polygon getBoundary() { return boundary; }
    public void setBoundary(Polygon boundary) { this.boundary = boundary; }
}
