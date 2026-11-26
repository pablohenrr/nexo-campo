package com.nexocampo.modules.weather;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class WeatherAlertDtos {

    private WeatherAlertDtos() {}

    public record PreferenciaAlertaRequest(
            @NotNull @Min(1) @Max(WeatherAlertRule.MAX_RADIUS_KM) Integer raioKm,
            @NotNull Set<CategoriaAlertaClimatico> categorias,
            @NotNull FrequenciaAlertaClimatico frequencia) {}

    public record PreferenciaAlertaResponse(UUID fazendaId, int raioKm, List<String> categorias, String frequencia) {}

    public record FazendaDoAlerta(UUID id, String nome) {}

    public record AlertaClimaticoItem(
            UUID id,
            String categoria,
            String fonte,
            String frase,
            String texto,
            OffsetDateTime inicio,
            OffsetDateTime fim,
            boolean lido,
            List<FazendaDoAlerta> fazendas) {}

    public record AlertasClimaticosResponse(int naoLidos, List<AlertaClimaticoItem> eventos) {}
}
