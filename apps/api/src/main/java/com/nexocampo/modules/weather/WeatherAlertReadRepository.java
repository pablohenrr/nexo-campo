package com.nexocampo.modules.weather;

import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherAlertReadRepository extends JpaRepository<WeatherAlertRead, UUID> {
    boolean existsByUser_IdAndEvent_Id(UUID userId, UUID eventId);

    @Query("select leitura.event.id from WeatherAlertRead leitura where leitura.user.id = :userId")
    Set<UUID> findEventIdsByUserId(@Param("userId") UUID userId);
}
