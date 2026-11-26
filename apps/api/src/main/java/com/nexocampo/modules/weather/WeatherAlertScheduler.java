package com.nexocampo.modules.weather;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.alertas-climaticos", name = "agendamento", havingValue = "true", matchIfMissing = true)
public class WeatherAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(WeatherAlertScheduler.class);

    private final WeatherAlertCycle cycle;

    public WeatherAlertScheduler(WeatherAlertCycle cycle) {
        this.cycle = cycle;
    }

    @Scheduled(cron = "${app.alertas-climaticos.cron:0 */15 * * * *}", zone = "${app.time-zone:America/Sao_Paulo}")
    public void scheduled() {
        run();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        Thread.ofVirtual().start(this::run);
    }

    private void run() {
        try {
            cycle.run();
        } catch (RuntimeException ex) {
            log.error("alertas_climaticos_ciclo_falhou", ex);
        }
    }
}
