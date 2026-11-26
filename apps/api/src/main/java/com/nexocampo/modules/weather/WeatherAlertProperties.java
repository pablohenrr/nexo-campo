package com.nexocampo.modules.weather;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.alertas-climaticos")
public record WeatherAlertProperties(
        boolean agendamento,
        String fontes,
        String cron,
        String inmetUrl,
        String meteoUrl,
        String meteoApiKey,
        Duration timeout) {

    public WeatherAlertProperties {
        if (fontes == null || fontes.isBlank()) {
            fontes = "stub";
        }
        if (cron == null || cron.isBlank()) {
            cron = "0 */15 * * * *";
        }
        if (inmetUrl == null || inmetUrl.isBlank()) {
            inmetUrl = "https://apiprevmet3.inmet.gov.br/avisos/rss";
        }
        if (meteoUrl == null || meteoUrl.isBlank()) {
            meteoUrl = "https://api.open-meteo.com";
        }
        if (meteoApiKey == null) {
            meteoApiKey = "";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(15);
        }
    }

    public boolean liveSources() {
        return "reais".equalsIgnoreCase(fontes);
    }
}
