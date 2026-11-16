package com.nexocampo.shared.health;

import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saude")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("ATIVO", OffsetDateTime.now());
    }

    public record HealthResponse(String situacao, OffsetDateTime dataHora) {
    }
}
