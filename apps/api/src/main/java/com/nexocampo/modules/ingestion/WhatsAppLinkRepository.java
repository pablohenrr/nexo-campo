package com.nexocampo.modules.ingestion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsAppLinkRepository extends JpaRepository<WhatsAppLink, UUID> {

    /** Já traz o usuário: a mensagem é processada fora de transação. */
    @EntityGraph(attributePaths = "user")
    List<WhatsAppLink> findByPhoneIn(Collection<String> phones);

    Optional<WhatsAppLink> findByUserId(UUID userId);
}
