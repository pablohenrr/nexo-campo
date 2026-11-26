package com.nexocampo.modules.weather;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherEventRepository extends JpaRepository<WeatherEvent, UUID> {
    Optional<WeatherEvent> findByFonteAndIdExternoAndCategoria(String fonte, String idExterno, String categoria);
}
