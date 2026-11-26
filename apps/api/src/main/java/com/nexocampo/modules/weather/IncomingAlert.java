package com.nexocampo.modules.weather;

import java.time.OffsetDateTime;

public record IncomingAlert(
        String fonte,
        String idExterno,
        CategoriaAlertaClimatico categoria,
        OffsetDateTime inicio,
        OffsetDateTime fim,
        AlertGeometry geometry,
        String texto) {

    public IncomingAlert {
        geometry = geometry == null ? AlertGeometry.none() : geometry;
    }
}
