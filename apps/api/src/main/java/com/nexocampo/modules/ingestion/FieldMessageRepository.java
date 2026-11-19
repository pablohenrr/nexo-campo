package com.nexocampo.modules.ingestion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldMessageRepository extends JpaRepository<FieldMessage, UUID> {
    Optional<FieldMessage> findByChannelAndExternalId(MessageChannel channel, String externalId);
    boolean existsByChannelAndExternalId(MessageChannel channel, String externalId);
    List<FieldMessage> findTop50BySenderIdOrderByCreatedAtDesc(UUID senderId);
}
