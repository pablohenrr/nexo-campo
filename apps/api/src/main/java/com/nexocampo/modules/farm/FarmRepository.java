package com.nexocampo.modules.farm;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmRepository extends JpaRepository<Farm, UUID> {
    List<Farm> findAllByOwnerIdOrderByName(UUID ownerId);
    java.util.Optional<Farm> findByIdAndOwnerId(UUID farmId, UUID ownerId);
}
