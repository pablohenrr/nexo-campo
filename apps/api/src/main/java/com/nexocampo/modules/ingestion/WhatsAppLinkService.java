package com.nexocampo.modules.ingestion;

import com.nexocampo.modules.identity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppLinkService {

    private final WhatsAppLinkRepository linkRepository;

    public WhatsAppLinkService(WhatsAppLinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    /** Cadastra ou troca o WhatsApp do usuário logado. */
    @Transactional
    public WhatsAppLinkResponse link(User user, WhatsAppLinkRequest request) {
        String phone = PhoneNumbers.normalize(request.telefone());
        boolean takenByOther = linkRepository.findByPhoneIn(PhoneNumbers.variants(phone)).stream()
                .anyMatch(link -> !link.getUser().getId().equals(user.getId()));
        if (takenByOther) {
            throw new IllegalArgumentException("Este número já está vinculado a outra conta");
        }
        WhatsAppLink link = linkRepository.findByUserId(user.getId()).orElseGet(WhatsAppLink::new);
        link.setUser(user);
        link.setPhone(phone);
        return toResponse(linkRepository.save(link));
    }

    @Transactional(readOnly = true)
    public Optional<WhatsAppLinkResponse> find(User user) {
        return linkRepository.findByUserId(user.getId()).map(WhatsAppLinkService::toResponse);
    }

    @Transactional
    public void unlink(User user) {
        linkRepository.findByUserId(user.getId()).ifPresent(linkRepository::delete);
    }

    /** Dono do número que mandou a mensagem, aceitando o celular com ou sem o nono dígito. */
    @Transactional(readOnly = true)
    public Optional<User> findUserByPhone(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return Optional.empty();
        }
        return linkRepository.findByPhoneIn(PhoneNumbers.variants(digits)).stream()
                .findFirst()
                .map(WhatsAppLink::getUser);
    }

    private static WhatsAppLinkResponse toResponse(WhatsAppLink link) {
        return new WhatsAppLinkResponse(link.getPhone(), link.getCreatedAt());
    }

    /** @param telefone com DDD; sem código do país, assume Brasil */
    public record WhatsAppLinkRequest(@NotBlank @Size(max = 30) String telefone) {}

    public record WhatsAppLinkResponse(String telefone, OffsetDateTime criadoEm) {}
}
