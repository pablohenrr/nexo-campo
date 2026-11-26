package com.nexocampo.modules.weather;

import com.nexocampo.modules.identity.User;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherAlertController {

    private final WeatherAlertService weatherAlertService;

    public WeatherAlertController(WeatherAlertService weatherAlertService) {
        this.weatherAlertService = weatherAlertService;
    }

    @GetMapping("/api/alertas-climaticos")
    public WeatherAlertDtos.AlertasClimaticosResponse list(@AuthenticationPrincipal User user) {
        return weatherAlertService.list(user);
    }

    @PostMapping("/api/alertas-climaticos/{id}/leitura")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        weatherAlertService.markRead(user, id);
    }

    @GetMapping("/api/fazendas/{fazendaId}/alertas-climaticos")
    public WeatherAlertDtos.PreferenciaAlertaResponse preference(
            @PathVariable UUID fazendaId, @AuthenticationPrincipal User user) {
        return weatherAlertService.preference(user, fazendaId);
    }

    @PutMapping("/api/fazendas/{fazendaId}/alertas-climaticos")
    public WeatherAlertDtos.PreferenciaAlertaResponse update(
            @PathVariable UUID fazendaId,
            @Valid @RequestBody WeatherAlertDtos.PreferenciaAlertaRequest request,
            @AuthenticationPrincipal User user) {
        return weatherAlertService.update(user, fazendaId, request);
    }
}
