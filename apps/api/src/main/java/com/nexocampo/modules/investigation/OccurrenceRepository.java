package com.nexocampo.modules.investigation;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OccurrenceRepository extends JpaRepository<Occurrence, UUID> {
    List<Occurrence> findAllByPlotIdOrderByObservedAtDescCreatedAtDesc(UUID plotId);
}
