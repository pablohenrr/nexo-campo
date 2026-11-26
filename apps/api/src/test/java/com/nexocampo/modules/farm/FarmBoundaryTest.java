package com.nexocampo.modules.farm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class FarmBoundaryTest {

    // Quadrado de ~1,02 km x ~1,11 km perto de Londrina (0,01° de lado).
    private static final List<FarmBoundary.Pin> SQUARE = List.of(
            pin("-23.300", "-51.170"),
            pin("-23.300", "-51.160"),
            pin("-23.310", "-51.160"),
            pin("-23.310", "-51.170"));

    @Test
    void centerIsTheCentroidOfThePinsNotTheFirstPin() {
        FarmBoundary.Pin center = FarmBoundary.fromPins(SQUARE).center();

        assertThat(center.latitude()).isEqualByComparingTo("-23.3050000");
        assertThat(center.longitude()).isEqualByComparingTo("-51.1650000");
        assertThat(center.latitude().scale()).isEqualTo(7);
    }

    @Test
    void areaIsGeodesicInHectares() {
        BigDecimal area = FarmBoundary.fromPins(SQUARE).areaHectares();

        // 0,01° lat ≈ 1111,9 m; 0,01° lon a 23,3°S ≈ 1021,3 m → ≈ 113,6 ha
        assertThat(area.doubleValue()).isBetween(113.0, 114.2);
    }

    @Test
    void keepsPinOrderAndDoesNotRepeatTheClosingPoint() {
        List<FarmBoundary.Pin> pins = FarmBoundary.fromPins(SQUARE).pins();

        assertThat(pins).hasSize(4);
        assertThat(pins.getFirst().latitude()).isEqualByComparingTo("-23.300");
        assertThat(pins.getFirst().longitude()).isEqualByComparingTo("-51.170");
        assertThat(pins.get(1).longitude()).isEqualByComparingTo("-51.160");
    }

    @Test
    void acceptsAnExplicitlyClosedRingAndRepeatedClicks() {
        List<FarmBoundary.Pin> pins = List.of(SQUARE.get(0), SQUARE.get(1), SQUARE.get(1), SQUARE.get(2),
                SQUARE.get(3), SQUARE.get(0));

        assertThat(FarmBoundary.fromPins(pins).pins()).hasSize(4);
    }

    @Test
    void storesLongitudeFirstWithSrid4326() {
        var polygon = FarmBoundary.fromPins(SQUARE).polygon();

        assertThat(polygon.getSRID()).isEqualTo(4326);
        assertThat(polygon.getCoordinates()[0].getX()).isEqualTo(-51.170);
        assertThat(polygon.getCoordinates()[0].getY()).isEqualTo(-23.300);
    }

    @Test
    void rejectsFewerThanThreeDistinctPins() {
        assertThatThrownBy(() -> FarmBoundary.fromPins(List.of(SQUARE.get(0), SQUARE.get(1), SQUARE.get(0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pelo menos 3 pinos");
        assertThatThrownBy(() -> FarmBoundary.fromPins(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCrossingLines() {
        // "Gravata": a ordem dos pinos faz as linhas se cruzarem.
        List<FarmBoundary.Pin> bowTie = List.of(SQUARE.get(0), SQUARE.get(2), SQUARE.get(1), SQUARE.get(3));

        assertThatThrownBy(() -> FarmBoundary.fromPins(bowTie))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("se cruzam");
    }

    @Test
    void rejectsAlignedPins() {
        List<FarmBoundary.Pin> line = List.of(pin("-23.30", "-51.17"), pin("-23.31", "-51.17"), pin("-23.32", "-51.17"));

        assertThatThrownBy(() -> FarmBoundary.fromPins(line))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("alinhados");
    }

    @Test
    void rejectsCoordinatesOutOfRange() {
        List<FarmBoundary.Pin> invalid = List.of(pin("-23.30", "-51.17"), pin("-91", "-51.16"), pin("-23.31", "-51.16"));

        assertThatThrownBy(() -> FarmBoundary.fromPins(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fora dos limites");
    }

    private static FarmBoundary.Pin pin(String latitude, String longitude) {
        return new FarmBoundary.Pin(new BigDecimal(latitude), new BigDecimal(longitude));
    }
}
