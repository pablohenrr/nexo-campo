package com.nexocampo.modules.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(prefix = "app.alertas-climaticos", name = "fontes", havingValue = "reais")
public class OpenMeteoWeatherAlertSource implements WeatherAlertSource {

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final boolean hideFailureDetail;
    private final String apiKey;

    public OpenMeteoWeatherAlertSource(WeatherAlertProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.client = WeatherHttp.client(properties.meteoUrl(), properties.timeout());
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.apiKey = properties.meteoApiKey();
        this.hideFailureDetail = apiKey != null && !apiKey.isBlank();
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public List<IncomingAlert> official() {
        return List.of();
    }

    @Override
    public List<IncomingAlert> forecast(double latitude, double longitude) {
        String body = WeatherHttp.get(client, uri(latitude, longitude), "open_meteo_falhou", hideFailureDetail);
        return OpenMeteoForecast.interpret(body, latitude, longitude, OffsetDateTime.now(clock), objectMapper);
    }

    private String uri(double latitude, double longitude) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/v1/forecast")
                .queryParam("latitude", String.format(Locale.US, "%.4f", latitude))
                .queryParam("longitude", String.format(Locale.US, "%.4f", longitude))
                .queryParam("hourly", "temperature_2m,precipitation,wind_gusts_10m,weather_code,relative_humidity_2m")
                .queryParam("forecast_days", 2)
                .queryParam("timezone", "America/Sao_Paulo")
                .queryParam("wind_speed_unit", "kmh")
                .queryParam("precipitation_unit", "mm");
        if (hideFailureDetail) {
            builder.queryParam("apikey", apiKey);
        }
        return builder.build(false).toUriString();
    }
}
