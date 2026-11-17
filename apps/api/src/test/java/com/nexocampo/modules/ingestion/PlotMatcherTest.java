package com.nexocampo.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PlotMatcherTest {

    private static final List<String> PLOTS = List.of("Talhão 01", "Talhão 04 - Milho Safrinha", "Pivô Central");

    @Test
    void matchesExactNameIgnoringCaseAndAccents() {
        assertThat(PlotMatcher.find("talhao 04 - milho safrinha", PLOTS)).contains("Talhão 04 - Milho Safrinha");
    }

    @Test
    void matchesNameInsideSentence() {
        assertThat(PlotMatcher.find("irriguei o pivô central hoje cedo", PLOTS)).contains("Pivô Central");
    }

    @Test
    void matchesPlotByNumber() {
        assertThat(PlotMatcher.find("passei veneno no talhão 4 ontem", PLOTS)).contains("Talhão 04 - Milho Safrinha");
        assertThat(PlotMatcher.find("Talhao nº 1", PLOTS)).contains("Talhão 01");
    }

    @Test
    void returnsEmptyWhenNothingOrMoreThanOnePlotMatches() {
        assertThat(PlotMatcher.find("passei veneno hoje", PLOTS)).isEmpty();
        assertThat(PlotMatcher.find("talhão 1", List.of("Talhão 01 Norte", "Talhão 01 Sul"))).isEmpty();
        assertThat(PlotMatcher.find(null, PLOTS)).isEmpty();
    }
}
