package com.nexocampo.modules.weather;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WeatherAlertRuleTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Test
    void tornadoComesOnlyFromInmetWording() {
        assertThat(WeatherAlertRule.fromInmetText("Tempestade com risco de tornado no Paraná"))
                .isEqualTo(CategoriaAlertaClimatico.TORNADO);
        assertThat(WeatherAlertRule.fromInmetText("Possibilidade de tromba d'água"))
                .isEqualTo(CategoriaAlertaClimatico.TORNADO);
        assertThat(WeatherAlertRule.fromInmetText("Chuva intensa")).isEqualTo(CategoriaAlertaClimatico.CHUVA);
        assertThat(WeatherAlertRule.fromForecast(80, 90, 1, 10, Set.of(99)))
                .doesNotContain(CategoriaAlertaClimatico.TORNADO)
                .contains(CategoriaAlertaClimatico.CHUVA, CategoriaAlertaClimatico.VENTO, CategoriaAlertaClimatico.GRANIZO);
    }

    @Test
    void radiusStaysAroundEachFarmPoint() {
        double farmLat = -23.31;
        double farmLon = -51.16;
        AlertGeometry near = AlertGeometry.point(farmLat + 0.18, farmLon);
        AlertGeometry far = AlertGeometry.point(-16.68, -49.25);

        assertThat(WeatherAlertRule.reaches(farmLat, farmLon, near, 30)).isTrue();
        assertThat(WeatherAlertRule.reaches(farmLat, farmLon, far, 80)).isFalse();
        assertThat(WeatherAlertRule.reaches(farmLat, farmLon, AlertGeometry.none(), 80)).isFalse();
    }

    @Test
    void polygonInParanaDoesNotReachGoias() {
        List<double[]> parana = List.of(
                new double[] {-23.0, -51.5},
                new double[] {-23.0, -50.8},
                new double[] {-23.6, -50.8},
                new double[] {-23.6, -51.5});
        AlertGeometry geometry = AlertGeometry.polygon(parana);

        assertThat(WeatherAlertRule.reaches(-23.31, -51.16, geometry, 30)).isTrue();
        assertThat(WeatherAlertRule.reaches(-16.68, -49.25, geometry, 80)).isFalse();
    }

    @Test
    void dailyAndWeeklyWindowsUseSaoPaulo() {
        OffsetDateTime now = OffsetDateTime.of(2026, 11, 25, 12, 0, 0, 0, ZoneOffset.ofHours(-3));
        OffsetDateTime today = now.minusHours(2);
        OffsetDateTime yesterday = now.minusDays(1);
        OffsetDateTime monday = OffsetDateTime.of(2026, 11, 23, 10, 0, 0, 0, ZoneOffset.ofHours(-3));
        OffsetDateTime sunday = OffsetDateTime.of(2026, 11, 22, 10, 0, 0, 0, ZoneOffset.ofHours(-3));

        assertThat(WeatherAlertRule.allows(FrequenciaAlertaClimatico.POR_EVENTO, today, now, SAO_PAULO)).isTrue();
        assertThat(WeatherAlertRule.allows(FrequenciaAlertaClimatico.DIARIO, today, now, SAO_PAULO)).isFalse();
        assertThat(WeatherAlertRule.allows(FrequenciaAlertaClimatico.DIARIO, yesterday, now, SAO_PAULO)).isTrue();
        assertThat(WeatherAlertRule.allows(FrequenciaAlertaClimatico.SEMANAL, monday, now, SAO_PAULO)).isFalse();
        assertThat(WeatherAlertRule.allows(FrequenciaAlertaClimatico.SEMANAL, sunday, now, SAO_PAULO)).isTrue();
    }
}
