package com.nexocampo.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio da aplicação no fuso das fazendas. Sem isso, um servidor em UTC registraria como "amanhã"
 * uma atividade enviada às 22h no horário de Brasília.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${app.time-zone:America/Sao_Paulo}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
