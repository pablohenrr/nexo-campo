package com.nexocampo.modules.weather;

import com.nexocampo.modules.farm.Farm;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

@Entity
@Table(
        name = "evento_climatico_fazenda",
        uniqueConstraints =
                @UniqueConstraint(name = "uk_evento_climatico_fazenda", columnNames = {"evento_id", "fazenda_id"}))
public class WeatherEventFarm {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private WeatherEvent event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fazenda_id", nullable = false)
    private Farm farm;

    public UUID getId() {
        return id;
    }

    public WeatherEvent getEvent() {
        return event;
    }

    public void setEvent(WeatherEvent event) {
        this.event = event;
    }

    public Farm getFarm() {
        return farm;
    }

    public void setFarm(Farm farm) {
        this.farm = farm;
    }
}
