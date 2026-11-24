package com.nexocampo.modules.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Perfil do usuário logado: dados pessoais, telefone (SMS e WhatsApp) e senha. */
@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final PhoneNumberService phoneNumberService;
    private final PasswordEncoder passwordEncoder;

    public UserProfileService(
            UserRepository userRepository,
            PhoneNumberService phoneNumberService,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.phoneNumberService = phoneNumberService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(User user) {
        return toResponse(user);
    }

    @Transactional
    public ProfileResponse update(User user, ProfileRequest request) {
        String phoneNumber = phoneNumberService.normalizeForSms(request.telefone(), request.notificacoesSmsAtivas());
        if (phoneNumber != null && userRepository.existsByPhoneNumberAndIdNot(phoneNumber, user.getId())) {
            throw new IllegalArgumentException(PhoneNumberService.PHONE_IN_USE);
        }
        user.setName(request.nome().strip());
        user.setPhoneNumber(phoneNumber);
        user.setSmsNotificationsEnabled(request.notificacoesSmsAtivas());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(User user, PasswordChangeRequest request) {
        if (isGoogleAccount(user)) {
            throw new IllegalArgumentException(
                    "Sua conta entra pelo Google e não tem senha no NEXO Campo. A senha é a da sua conta Google.");
        }
        if (!passwordEncoder.matches(request.senhaAtual(), user.getPasswordHash())) {
            throw new IllegalArgumentException("A senha atual não confere");
        }
        if (request.senhaAtual().equals(request.novaSenha())) {
            throw new IllegalArgumentException("A nova senha precisa ser diferente da atual");
        }
        user.setPasswordHash(passwordEncoder.encode(request.novaSenha()));
        userRepository.save(user);
    }

    private static boolean isGoogleAccount(User user) {
        return user.getGoogleSubject() != null;
    }

    private static ProfileResponse toResponse(User user) {
        return new ProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getPhoneNumber(),
                user.isSmsNotificationsEnabled(),
                isGoogleAccount(user),
                user.getCreatedAt());
    }

    public record ProfileRequest(
            @NotBlank @Size(max = 120) String nome,
            @Size(max = 40) String telefone,
            boolean notificacoesSmsAtivas) {}

    public record PasswordChangeRequest(
            @NotBlank String senhaAtual,
            @NotBlank @Size(min = 8, max = 100) String novaSenha) {}

    /**
     * @param telefone    formato internacional ({@code +5511987654321}); é o número que recebe SMS e
     *                    que identifica o produtor quando ele escreve no WhatsApp
     * @param contaGoogle quem entra pelo Google não tem senha própria para trocar
     */
    public record ProfileResponse(
            UUID id,
            String nome,
            String email,
            Role perfil,
            String telefone,
            boolean notificacoesSmsAtivas,
            boolean contaGoogle,
            OffsetDateTime criadoEm) {}
}
