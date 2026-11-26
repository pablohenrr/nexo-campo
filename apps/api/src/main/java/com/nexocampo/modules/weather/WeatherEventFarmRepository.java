package com.nexocampo.modules.weather;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherEventFarmRepository extends JpaRepository<WeatherEventFarm, UUID> {
    boolean existsByEvent_IdAndFarm_Id(UUID eventId, UUID farmId);

    @Query("""
            select link from WeatherEventFarm link
            join fetch link.event
            join fetch link.farm
            where link.farm.owner.id = :ownerId
            order by link.event.criadoEm desc
            """)
    List<WeatherEventFarm> findByOwner(@Param("ownerId") UUID ownerId);

    @Query("""
            select link from WeatherEventFarm link
            join fetch link.farm
            where link.event.id = :eventId
            """)
    List<WeatherEventFarm> findByEventId(@Param("eventId") UUID eventId);

    @Query("""
            select max(event.criadoEm) from WeatherEventFarm link
            join link.event event
            where link.farm.id = :farmId and event.categoria = :categoria
            """)
    Optional<OffsetDateTime> findLatest(@Param("farmId") UUID farmId, @Param("categoria") String categoria);

    @Query("""
            select case when count(link) > 0 then true else false end
            from WeatherEventFarm link
            where link.event.id = :eventId and link.farm.owner.id = :ownerId
            """)
    boolean visibleToOwner(@Param("eventId") UUID eventId, @Param("ownerId") UUID ownerId);
}
