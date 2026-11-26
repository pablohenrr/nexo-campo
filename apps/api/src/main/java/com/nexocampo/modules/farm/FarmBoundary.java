package com.nexocampo.modules.farm;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

/**
 * Contorno da fazenda a partir dos pinos marcados no mapa, na ordem em que formam o contorno.
 * Calcula o centro (gravado em fazenda.latitude/longitude, usado pelos alertas) e a área.
 */
final class FarmBoundary {

    static final int MIN_PINS = 3;
    static final int MAX_PINS = 500;

    private static final double EARTH_RADIUS_M = 6_371_008.8;
    private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);

    private final Polygon polygon;

    private FarmBoundary(Polygon polygon) {
        this.polygon = polygon;
    }

    /** Pinos em ordem, sem repetir o primeiro no fim (se repetir, é aceito). */
    static FarmBoundary fromPins(List<Pin> pins) {
        if (pins == null || pins.isEmpty()) {
            throw new IllegalArgumentException("contorno deve ter pelo menos " + MIN_PINS + " pinos");
        }
        List<Coordinate> ring = new ArrayList<>();
        for (Pin pin : pins) {
            if (pin == null || pin.latitude() == null || pin.longitude() == null) {
                throw new IllegalArgumentException("contorno contém pino sem latitude ou longitude");
            }
            double lat = pin.latitude().doubleValue();
            double lon = pin.longitude().doubleValue();
            if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                throw new IllegalArgumentException("contorno contém coordenadas fora dos limites de latitude/longitude");
            }
            Coordinate coordinate = new Coordinate(lon, lat);
            if (!ring.isEmpty() && ring.getLast().equals2D(coordinate)) {
                continue; // clique duplo no mesmo ponto
            }
            ring.add(coordinate);
        }
        if (ring.size() > 1 && ring.getFirst().equals2D(ring.getLast())) {
            ring.removeLast();
        }
        if (ring.size() < MIN_PINS) {
            throw new IllegalArgumentException("contorno deve ter pelo menos " + MIN_PINS + " pinos distintos");
        }
        if (ring.size() > MAX_PINS) {
            throw new IllegalArgumentException("contorno pode ter no máximo " + MAX_PINS + " pinos");
        }
        ring.add(ring.getFirst().copy());

        Polygon polygon = WGS84.createPolygon(ring.toArray(Coordinate[]::new));
        if (polygon.convexHull().getDimension() < 2) {
            throw new IllegalArgumentException("contorno não pode ter todos os pinos alinhados");
        }
        if (!polygon.isValid()) {
            throw new IllegalArgumentException("contorno não pode ter linhas que se cruzam");
        }
        return new FarmBoundary(polygon);
    }

    static FarmBoundary of(Polygon polygon) {
        return polygon == null ? null : new FarmBoundary(polygon);
    }

    Polygon polygon() {
        return polygon;
    }

    /** Pinos na ordem original, sem o ponto de fechamento. */
    List<Pin> pins() {
        Coordinate[] coordinates = polygon.getExteriorRing().getCoordinates();
        List<Pin> pins = new ArrayList<>(coordinates.length - 1);
        for (int i = 0; i < coordinates.length - 1; i++) {
            pins.add(new Pin(scale7(coordinates[i].getY()), scale7(coordinates[i].getX())));
        }
        return pins;
    }

    /**
     * Centroide do contorno. Em fazenda côncava (formato de "L") ele pode cair fora da área,
     * o que não atrapalha o uso como centro do raio dos alertas.
     */
    Pin center() {
        Point centroid = polygon.getCentroid();
        return new Pin(scale7(centroid.getY()), scale7(centroid.getX()));
    }

    /** Área geodésica aproximada numa esfera (mesma fórmula do Google Maps), em hectares. */
    BigDecimal areaHectares() {
        Coordinate[] c = polygon.getExteriorRing().getCoordinates();
        double sum = 0;
        for (int i = 0; i < c.length - 1; i++) {
            double lon1 = Math.toRadians(c[i].getX());
            double lon2 = Math.toRadians(c[i + 1].getX());
            double lat1 = Math.toRadians(c[i].getY());
            double lat2 = Math.toRadians(c[i + 1].getY());
            sum += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2));
        }
        double squareMeters = Math.abs(sum * EARTH_RADIUS_M * EARTH_RADIUS_M / 2);
        return BigDecimal.valueOf(squareMeters / 10_000).setScale(3, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale7(double value) {
        return BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP);
    }

    record Pin(BigDecimal latitude, BigDecimal longitude) {}
}
