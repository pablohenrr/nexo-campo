package com.nexocampo.modules.weather;

import java.net.URI;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.alertas-climaticos", name = "fontes", havingValue = "reais")
public class InmetWeatherAlertSource implements WeatherAlertSource {

    private static final int MAX_DETAILS = 40;

    private final RestClient client;
    private final Clock clock;
    private final String url;

    public InmetWeatherAlertSource(WeatherAlertProperties properties, Clock clock) {
        this.url = properties.inmetUrl();
        this.client = WeatherHttp.client(properties.inmetUrl(), properties.timeout());
        this.clock = clock;
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public List<IncomingAlert> official() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        String body = WeatherHttp.get(client, url, "inmet_avisos_falhou", false);
        List<IncomingAlert> embedded = InmetCapParser.parse(body, now);
        if (!embedded.isEmpty()) {
            return embedded;
        }
        List<IncomingAlert> alerts = new ArrayList<>();
        for (String link : InmetCapParser.itemLinks(body)) {
            if (alerts.size() >= MAX_DETAILS || !sameHost(link)) {
                continue;
            }
            alerts.addAll(InmetCapParser.parse(WeatherHttp.get(client, link, "inmet_aviso_falhou", false), now));
        }
        return List.copyOf(alerts);
    }

    private boolean sameHost(String link) {
        try {
            URI base = URI.create(url);
            URI target = URI.create(link);
            return "https".equalsIgnoreCase(target.getScheme())
                    && base.getHost() != null
                    && base.getHost().equalsIgnoreCase(target.getHost());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public List<IncomingAlert> forecast(double latitude, double longitude) {
        return List.of();
    }
}
