package com.nexocampo.modules.farm;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlotRepository extends JpaRepository<Plot, UUID> {
    List<Plot> findAllByFarmIdOrderByName(UUID farmId);
}
