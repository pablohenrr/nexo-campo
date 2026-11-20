package com.nexocampo.modules.identity;

import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAccountService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public GoogleAccountService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Receives only an OIDC principal already signature/issuer/audience/nonce validated by Spring. */
    @Transactional
    public AuthService.AuthResponse login(OidcUser principal) {
        String subject = principal.getSubject();
        String email = principal.getEmail();
        if (!Boolean.TRUE.equals(principal.getEmailVerified())) throw failure("GOOGLE_EMAIL_UNVERIFIED");
        if (subject == null || subject.isBlank() || subject.length() > 255
                || email == null || email.isBlank() || email.length() > 180) throw failure("GOOGLE_LOGIN_FAILED");
        User user = users.findByGoogleSubject(subject).orElseGet(() -> {
            if (users.existsByEmailIgnoreCase(email)) throw failure("ACCOUNT_EMAIL_CONFLICT");
            User created = new User();
            created.setGoogleSubject(subject);
            created.setEmail(email.strip());
            String name = principal.getFullName();
            if (name == null || name.isBlank()) name = "Produtor";
            created.setName(name.substring(0, Math.min(name.length(), 120)));
            // Not a usable local credential, and never returned, stored in plaintext or logged.
            created.setPasswordHash(encoder.encode(UUID.randomUUID().toString() + UUID.randomUUID()));
            return users.saveAndFlush(created);
        });
        return new AuthService.AuthResponse(jwt.generateToken(user), user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    private OAuth2AuthenticationException failure(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }
}
