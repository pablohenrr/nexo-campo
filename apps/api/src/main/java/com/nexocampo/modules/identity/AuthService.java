package com.nexocampo.modules.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PhoneNumberService phoneNumberService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PhoneNumberService phoneNumberService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.phoneNumberService = phoneNumberService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }
        String phoneNumber = phoneNumberService.normalizeForSms(request.telefone(), request.notificacoesSmsAtivas());
        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException(PhoneNumberService.PHONE_IN_USE);
        }
        User user = new User();
        user.setName(request.nome());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.senha()));
        user.setPhoneNumber(phoneNumber);
        user.setSmsNotificationsEnabled(request.notificacoesSmsAtivas());
        userRepository.save(user);
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .filter(candidate -> passwordEncoder.matches(request.senha(), candidate.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos"));
        return new AuthResponse(jwtService.generateToken(user), user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public record RegisterRequest(
            @NotBlank String nome,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String senha,
            @Size(max = 40) String telefone,
            boolean notificacoesSmsAtivas) {}

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String senha) {}

    public record AuthResponse(String token, java.util.UUID usuarioId, String nome, String email, Role perfil) {}
}
