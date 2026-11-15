package com.nexocampo.modules.activity;

import com.nexocampo.modules.farm.Plot;
import com.nexocampo.modules.identity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "activity")
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plot_id", nullable = false)
    private Plot plot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsible_id", nullable = false)
    private User responsible;

    @Column(nullable = false, length = 80)
    private String type;

    @Column(length = 1000)
    private String description;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    private BigDecimal cost;
    private String inputs;

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
    public User getResponsible() { return responsible; }
    public void setResponsible(User responsible) { this.responsible = responsible; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public String getInputs() { return inputs; }
    public void setInputs(String inputs) { this.inputs = inputs; }
}
