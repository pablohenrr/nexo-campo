package com.nexocampo.modules.weather;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeatherAlertPreferenceRepository extends JpaRepository<WeatherAlertPreference, UUID> {
    Optional<WeatherAlertPreference> findByFarm_Id(UUID farmId);
}
