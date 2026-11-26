package com.nexocampo.modules.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class OpenMeteoForecast {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int HOURS = 24;

    private OpenMeteoForecast() {}

    static List<IncomingAlert> interpret(String json, double latitude, double longitude, OffsetDateTime now, ObjectMapper mapper) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode hourly = mapper.readTree(json).path("hourly");
            double precipitation = sum(hourly.path("precipitation"));
            double gust = max(hourly.path("wind_gusts_10m"));
            double minTemperature = min(hourly.path("temperature_2m"));
            double minHumidity = min(hourly.path("relative_humidity_2m"));
            Set<Integer> codes = codes(hourly.path("weather_code"));
            if (Double.isNaN(minTemperature)) {
                minTemperature = 99;
            }
            if (Double.isNaN(minHumidity)) {
                minHumidity = 100;
            }
            Set<CategoriaAlertaClimatico> categories =
                    WeatherAlertRule.fromForecast(precipitation, gust, minTemperature, minHumidity, codes);
            if (categories.isEmpty()) {
                return List.of();
            }
            LocalDate day = now.atZoneSameInstant(ZONE).toLocalDate();
            String cell = cell(latitude) + ":" + cell(longitude);
            AlertGeometry geometry = AlertGeometry.point(latitude, longitude);
            List<IncomingAlert> alerts = new ArrayList<>();
            for (CategoriaAlertaClimatico category : categories) {
                alerts.add(new IncomingAlert(
                        FonteAlertaClimatico.OPEN_METEO.name(),
                        "open-meteo:" + category.name() + ":" + day + ":" + cell,
                        category,
                        now,
                        now.plusHours(HOURS),
                        geometry,
                        null));
            }
            return List.copyOf(alerts);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static String cell(double value) {
        double rounded = Math.round(value * 2.0) / 2.0;
        return String.format(Locale.US, "%.1f", rounded);
    }

    private static double sum(JsonNode node) {
        double total = 0;
        int count = 0;
        for (JsonNode item : node) {
            if (count++ >= HOURS) {
                break;
            }
            if (item.isNumber()) {
                total += item.asDouble();
            }
        }
        return total;
    }

    private static double max(JsonNode node) {
        double best = 0;
        int count = 0;
        for (JsonNode item : node) {
            if (count++ >= HOURS) {
                break;
            }
            if (item.isNumber()) {
                best = Math.max(best, item.asDouble());
            }
        }
        return best;
    }

    private static double min(JsonNode node) {
        double best = Double.NaN;
        int count = 0;
        for (JsonNode item : node) {
            if (count++ >= HOURS) {
                break;
            }
            if (item.isNumber()) {
                double value = item.asDouble();
                best = Double.isNaN(best) ? value : Math.min(best, value);
            }
        }
        return best;
    }

    private static Set<Integer> codes(JsonNode node) {
        Set<Integer> codes = new HashSet<>();
        int count = 0;
        for (JsonNode item : node) {
            if (count++ >= HOURS) {
                break;
            }
            if (item.isNumber()) {
                codes.add(item.asInt());
            }
        }
        return codes;
    }
}
