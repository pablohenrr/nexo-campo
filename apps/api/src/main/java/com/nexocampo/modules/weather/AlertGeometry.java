package com.nexocampo.modules.weather;

import java.util.List;

public record AlertGeometry(
        Double latitude,
        Double longitude,
        List<double[]> polygon,
        Double circleLatitude,
        Double circleLongitude,
        Double circleRadiusKm,
        List<List<double[]>> rings) {

    public AlertGeometry {
        polygon = polygon == null ? List.of() : List.copyOf(polygon);
        rings = rings == null || rings.isEmpty()
                ? (polygon.size() >= 3 ? List.of(polygon) : List.of())
                : List.copyOf(rings);
    }

    public static AlertGeometry point(double latitude, double longitude) {
        return new AlertGeometry(latitude, longitude, List.of(), null, null, null, List.of());
    }

    public static AlertGeometry polygon(List<double[]> ring) {
        return new AlertGeometry(null, null, ring, null, null, null, List.of(ring));
    }

    public static AlertGeometry polygons(List<List<double[]>> rings) {
        List<double[]> first = rings.isEmpty() ? List.of() : rings.get(0);
        return new AlertGeometry(null, null, first, null, null, null, rings);
    }

    public static AlertGeometry circle(double latitude, double longitude, double radiusKm) {
        return new AlertGeometry(latitude, longitude, List.of(), latitude, longitude, radiusKm, List.of());
    }

    public static AlertGeometry none() {
        return new AlertGeometry(null, null, List.of(), null, null, null, List.of());
    }

    public String serialized() {
        if (polygon.size() < 3) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (double[] point : polygon) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(point[0]).append(',').append(point[1]);
        }
        String value = builder.toString();
        return value.length() <= 20000 ? value : value.substring(0, 20000);
    }
}
