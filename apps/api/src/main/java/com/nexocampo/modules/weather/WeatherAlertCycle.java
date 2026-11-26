package com.nexocampo.modules.weather;

import com.nexocampo.modules.farm.Farm;
import com.nexocampo.modules.farm.FarmRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WeatherAlertCycle {

    private static final Logger log = LoggerFactory.getLogger(WeatherAlertCycle.class);

    private final List<WeatherAlertSource> sources;
    private final FarmRepository farms;
    private final WeatherAlertService service;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public WeatherAlertCycle(List<WeatherAlertSource> sources, FarmRepository farms, WeatherAlertService service) {
        this.sources = sources;
        this.farms = farms;
        this.service = service;
    }

    public void run() {
        if (!running.compareAndSet(false, true)) {
            log.info("alertas_climaticos_ciclo_em_andamento");
            return;
        }
        try {
            if (sources.stream().noneMatch(WeatherAlertSource::isLive)) {
                log.info("alertas_climaticos_ciclo fonte=stub");
                return;
            }
            List<IncomingAlert> official = sources.stream().flatMap(source -> official(source).stream()).toList();
            List<Farm> located = farms.findByLatitudeIsNotNullAndLongitudeIsNotNull();
            log.info("alertas_climaticos_ciclo fazendas={} avisosOficiais={}", located.size(), official.size());
            for (Farm farm : located) {
                try {
                    List<IncomingAlert> forecast = sources.stream()
                            .flatMap(source -> forecast(source, farm).stream())
                            .toList();
                    service.apply(farm, official, forecast);
                } catch (RuntimeException ex) {
                    log.error("alertas_climaticos_fazenda_falhou fazendaId={}", farm.getId(), ex);
                }
            }
        } finally {
            running.set(false);
        }
    }

    private List<IncomingAlert> official(WeatherAlertSource source) {
        try {
            return source.official();
        } catch (RuntimeException ex) {
            log.warn("alertas_climaticos_fonte_falhou fonte={}", source.getClass().getSimpleName());
            return List.of();
        }
    }

    private List<IncomingAlert> forecast(WeatherAlertSource source, Farm farm) {
        try {
            return source.forecast(farm.getLatitude().doubleValue(), farm.getLongitude().doubleValue());
        } catch (RuntimeException ex) {
            log.warn("alertas_climaticos_previsao_falhou fazendaId={} fonte={}", farm.getId(), source.getClass().getSimpleName());
            return List.of();
        }
    }
}
