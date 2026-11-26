package com.nexocampo.modules.weather;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WeatherAlertRule {

    public static final int DEFAULT_RADIUS_KM = 30;
    public static final int MAX_RADIUS_KM = 80;
    public static final Set<CategoriaAlertaClimatico> DEFAULT_CATEGORIES = EnumSet.of(
            CategoriaAlertaClimatico.CHUVA,
            CategoriaAlertaClimatico.VENTO,
            CategoriaAlertaClimatico.TEMPESTADE);

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double RAIN_MM = 25.0;
    private static final double WIND_KMH = 50.0;
    private static final double FROST_CELSIUS = 3.0;
    private static final double LOW_HUMIDITY = 30.0;

    private WeatherAlertRule() {}

    public static CategoriaAlertaClimatico fromInmetText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = strip(text);
        if (normalized.contains("tornado") || normalized.contains("tromba")) {
            return CategoriaAlertaClimatico.TORNADO;
        }
        if (normalized.contains("granizo")) {
            return CategoriaAlertaClimatico.GRANIZO;
        }
        if (normalized.contains("geada")) {
            return CategoriaAlertaClimatico.GEADA;
        }
        if (normalized.contains("umidade") || normalized.contains("estiagem")) {
            return CategoriaAlertaClimatico.UMIDADE_BAIXA;
        }
        if (normalized.contains("tempestade") || normalized.contains("trovoada")) {
            return CategoriaAlertaClimatico.TEMPESTADE;
        }
        if (normalized.contains("vento") || normalized.contains("vendaval")) {
            return CategoriaAlertaClimatico.VENTO;
        }
        if (normalized.contains("chuva") || normalized.contains("precipitacao")) {
            return CategoriaAlertaClimatico.CHUVA;
        }
        return null;
    }

    public static Set<CategoriaAlertaClimatico> fromForecast(
            double precipitationMm,
            double gustKmh,
            double minTemperature,
            double minHumidity,
            Set<Integer> weatherCodes) {
        EnumSet<CategoriaAlertaClimatico> categories = EnumSet.noneOf(CategoriaAlertaClimatico.class);
        if (precipitationMm >= RAIN_MM || hasCode(weatherCodes, 65, 67, 82)) {
            categories.add(CategoriaAlertaClimatico.CHUVA);
        }
        if (gustKmh >= WIND_KMH) {
            categories.add(CategoriaAlertaClimatico.VENTO);
        }
        if (hasCode(weatherCodes, 95, 97, 99)) {
            categories.add(CategoriaAlertaClimatico.TEMPESTADE);
        }
        if (hasCode(weatherCodes, 96, 99)) {
            categories.add(CategoriaAlertaClimatico.GRANIZO);
        }
        if (minTemperature <= FROST_CELSIUS) {
            categories.add(CategoriaAlertaClimatico.GEADA);
        }
        if (minHumidity <= LOW_HUMIDITY) {
            categories.add(CategoriaAlertaClimatico.UMIDADE_BAIXA);
        }
        return categories;
    }

    public static boolean reaches(double farmLat, double farmLon, AlertGeometry geometry, int radiusKm) {
        if (geometry == null) {
            return false;
        }
        if (geometry.polygon().size() >= 3 || !geometry.rings().isEmpty()) {
            for (List<double[]> ring : geometry.rings()) {
                if (ring.size() < 3) {
                    continue;
                }
                if (contains(farmLat, farmLon, ring) || distanceToRingKm(farmLat, farmLon, ring) <= radiusKm) {
                    return true;
                }
            }
            return false;
        }
        if (geometry.circleLatitude() != null
                && geometry.circleLongitude() != null
                && geometry.circleRadiusKm() != null) {
            double distance = haversineKm(farmLat, farmLon, geometry.circleLatitude(), geometry.circleLongitude());
            if (distance <= geometry.circleRadiusKm()) {
                return true;
            }
            return distance - geometry.circleRadiusKm() <= radiusKm;
        }
        if (geometry.latitude() != null && geometry.longitude() != null) {
            return haversineKm(farmLat, farmLon, geometry.latitude(), geometry.longitude()) <= radiusKm;
        }
        return false;
    }

    public static boolean allows(
            FrequenciaAlertaClimatico frequency, OffsetDateTime last, OffsetDateTime now, ZoneId zone) {
        if (frequency == null || frequency == FrequenciaAlertaClimatico.POR_EVENTO || last == null) {
            return true;
        }
        LocalDate today = now.atZoneSameInstant(zone).toLocalDate();
        LocalDate start = frequency == FrequenciaAlertaClimatico.SEMANAL ? today.with(DayOfWeek.MONDAY) : today;
        OffsetDateTime windowStart = start.atStartOfDay(zone).toOffsetDateTime();
        return last.isBefore(windowStart);
    }

    private static boolean hasCode(Set<Integer> codes, int... expected) {
        if (codes == null) {
            return false;
        }
        for (int code : expected) {
            if (codes.contains(code)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(double lat, double lon, List<double[]> ring) {
        boolean inside = false;
        for (int i = 0, j = ring.size() - 1; i < ring.size(); j = i++) {
            double yi = ring.get(i)[0];
            double xi = ring.get(i)[1];
            double yj = ring.get(j)[0];
            double xj = ring.get(j)[1];
            boolean intersect = ((yi > lat) != (yj > lat)) && (lon < (xj - xi) * (lat - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    private static double distanceToRingKm(double lat, double lon, List<double[]> ring) {
        double[] origin = toKm(lat, lon, lat);
        double best = Double.MAX_VALUE;
        for (int i = 0; i < ring.size(); i++) {
            double[] start = toKm(ring.get(i)[0], ring.get(i)[1], lat);
            double[] next = ring.get((i + 1) % ring.size());
            double[] end = toKm(next[0], next[1], lat);
            best = Math.min(best, distanceToSegment(origin, start, end));
        }
        return best;
    }

    private static double[] toKm(double lat, double lon, double originLat) {
        return new double[] {lat * 110.574, lon * 111.320 * Math.cos(Math.toRadians(originLat))};
    }

    private static double distanceToSegment(double[] point, double[] start, double[] end) {
        double dx = end[0] - start[0];
        double dy = end[1] - start[1];
        double length = dx * dx + dy * dy;
        if (length == 0) {
            return Math.hypot(point[0] - start[0], point[1] - start[1]);
        }
        double ratio = ((point[0] - start[0]) * dx + (point[1] - start[1]) * dy) / length;
        double t = Math.max(0, Math.min(1, ratio));
        return Math.hypot(point[0] - (start[0] + t * dx), point[1] - (start[1] + t * dy));
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2)
                        * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    private static String strip(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
