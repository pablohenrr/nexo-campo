package com.nexocampo.modules.farm;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlotRepository extends JpaRepository<Plot, UUID> {
    List<Plot> findAllByFarmIdAndFarmOwnerIdOrderByName(UUID farmId, UUID ownerId);
    java.util.Optional<Plot> findByIdAndFarmOwnerId(UUID plotId, UUID ownerId);
}
