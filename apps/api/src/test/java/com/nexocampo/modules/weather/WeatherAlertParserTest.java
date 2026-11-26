package com.nexocampo.modules.weather;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeatherAlertParserTest {

    private final OffsetDateTime now = OffsetDateTime.of(2026, 11, 25, 12, 0, 0, 0, ZoneOffset.ofHours(-3));

    @Test
    void inmetPolygonBecomesOneNormalizedAlert() {
        String xml = """
                <feed xmlns="http://www.w3.org/2005/Atom" xmlns:cap="urn:oasis:names:tc:emergency:cap:1.2">
                  <entry>
                    <id>urn:inmet:chuva-pr</id>
                    <title>Chuva intensa</title>
                    <cap:onset>2026-11-25T10:00:00-03:00</cap:onset>
                    <cap:expires>2026-11-26T10:00:00-03:00</cap:expires>
                    <cap:polygon>-23.0,-51.5 -23.0,-50.8 -23.6,-50.8 -23.6,-51.5 -23.0,-51.5</cap:polygon>
                  </entry>
                  <entry>
                    <id>urn:inmet:sem-geo</id>
                    <title>Chuva sem área</title>
                  </entry>
                </feed>
                """;

        List<IncomingAlert> alerts = InmetCapParser.parse(xml, now);

        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).fonte()).isEqualTo("INMET");
        assertThat(alerts.get(0).categoria()).isEqualTo(CategoriaAlertaClimatico.CHUVA);
        assertThat(alerts.get(0).idExterno()).isEqualTo("urn:inmet:chuva-pr");
        assertThat(WeatherAlertRule.reaches(-23.31, -51.16, alerts.get(0).geometry(), 30)).isTrue();
    }

    @Test
    void capDocumentAndRssIndexAreReadable() {
        String cap = """
                <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
                  <identifier>urn:oid:inmet-1</identifier>
                  <info>
                    <event>Chuva intensa</event>
                    <onset>2026-11-25T10:00:00-03:00</onset>
                    <expires>2026-11-26T10:00:00-03:00</expires>
                    <area><polygon>-23.0,-51.5 -23.0,-50.8 -23.6,-50.8 -23.6,-51.5 -23.0,-51.5</polygon></area>
                  </info>
                </alert>
                """;
        String index = """
                <rss version="2.0"><channel>
                  <link>https://avisos.inmet.gov.br</link>
                  <item><title>Chuva</title><link>https://apiprevmet3.inmet.gov.br/avisos/rss/56507</link></item>
                </channel></rss>
                """;

        assertThat(InmetCapParser.parse(cap, now)).singleElement()
                .extracting(IncomingAlert::idExterno)
                .isEqualTo("urn:oid:inmet-1");
        assertThat(InmetCapParser.itemLinks(index)).containsExactly("https://apiprevmet3.inmet.gov.br/avisos/rss/56507");
    }

    @Test
    void inmetTornadoWordOverridesTheStormLabel() {
        String xml = """
                <feed>
                  <entry>
                    <id>urn:inmet:tornado</id>
                    <title>Tempestade</title>
                    <description>Risco de tornado no oeste</description>
                    <expires>2026-11-26T10:00:00-03:00</expires>
                    <circle>-23.3,-51.2 40</circle>
                  </entry>
                </feed>
                """;

        List<IncomingAlert> alerts = InmetCapParser.parse(xml, now);

        assertThat(alerts).singleElement().extracting(IncomingAlert::categoria).isEqualTo(CategoriaAlertaClimatico.TORNADO);
    }

    @Test
    void forecastDoesNotInventTornado() {
        String rain = """
                {"hourly":{"precipitation":[3,3,3,3,3,3,3,3,3,3],
                "wind_gusts_10m":[10],"temperature_2m":[22],"relative_humidity_2m":[70],"weather_code":[0]}}
                """;
        String storm = """
                {"hourly":{"precipitation":[0],"wind_gusts_10m":[20],"temperature_2m":[18],
                "relative_humidity_2m":[90],"weather_code":[99]}}
                """;

        List<IncomingAlert> rainAlerts = OpenMeteoForecast.interpret(rain, -23.31, -51.16, now, new ObjectMapper());
        List<IncomingAlert> stormAlerts = OpenMeteoForecast.interpret(storm, -23.31, -51.16, now, new ObjectMapper());

        assertThat(rainAlerts).extracting(IncomingAlert::categoria).containsExactly(CategoriaAlertaClimatico.CHUVA);
        assertThat(stormAlerts).extracting(IncomingAlert::categoria)
                .contains(CategoriaAlertaClimatico.TEMPESTADE, CategoriaAlertaClimatico.GRANIZO)
                .doesNotContain(CategoriaAlertaClimatico.TORNADO);
        assertThat(rainAlerts.get(0).idExterno()).isEqualTo("open-meteo:CHUVA:2026-11-25:-23.5:-51.0");
    }
}
