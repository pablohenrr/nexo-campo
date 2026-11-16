package com.nexocampo.modules.investigation;

import com.nexocampo.modules.farm.Plot;
import com.nexocampo.modules.identity.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Problema observado em um talhão (praga, doença, dano por clima...), ponto de partida das investigações. */
@Entity
@Table(name = "occurrence")
public class Occurrence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plot_id", nullable = false)
    private Plot plot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private OccurrenceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OccurrenceSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OccurrenceStatus status = OccurrenceStatus.ABERTA;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "observed_at", nullable = false)
    private LocalDate observedAt;

    @Column(name = "evidence_id")
    private UUID evidenceId;

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
    public Plot getPlot() { return plot; }
    public void setPlot(Plot plot) { this.plot = plot; }
    public User getReportedBy() { return reportedBy; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }
    public OccurrenceCategory getCategory() { return category; }
    public void setCategory(OccurrenceCategory category) { this.category = category; }
    public OccurrenceSeverity getSeverity() { return severity; }
    public void setSeverity(OccurrenceSeverity severity) { this.severity = severity; }
    public OccurrenceStatus getStatus() { return status; }
    public void setStatus(OccurrenceStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getObservedAt() { return observedAt; }
    public void setObservedAt(LocalDate observedAt) { this.observedAt = observedAt; }
    public UUID getEvidenceId() { return evidenceId; }
    public void setEvidenceId(UUID evidenceId) { this.evidenceId = evidenceId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
