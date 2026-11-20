package com.nexocampo.modules.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

class GoogleAccountServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final GoogleAccountService service = new GoogleAccountService(users, new BCryptPasswordEncoder(4), jwt);

    @BeforeEach
    void tokens() {
        when(jwt.generateToken(any(User.class))).thenReturn("jwt-local");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });
    }

    private static OidcUser google(String subject, String email, Boolean verified, String name) {
        OidcUser principal = mock(OidcUser.class);
        when(principal.getSubject()).thenReturn(subject);
        when(principal.getEmail()).thenReturn(email);
        when(principal.getEmailVerified()).thenReturn(verified);
        when(principal.getFullName()).thenReturn(name);
        return principal;
    }

    private static String errorCode(Throwable error) {
        return ((OAuth2AuthenticationException) error).getError().getErrorCode();
    }

    @Test
    void returningGoogleUserIsFoundBySubjectNotByEmail() {
        User existing = new User();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        existing.setName("Produtora");
        existing.setEmail("produtora@example.com");
        existing.setGoogleSubject("sub-123");
        when(users.findByGoogleSubject("sub-123")).thenReturn(Optional.of(existing));

        // E-mail trocado no Google: a identidade continua sendo o "sub".
        var response = service.login(google("sub-123", "novo@example.com", true, "Outro Nome"));

        assertThat(response.token()).isEqualTo("jwt-local");
        assertThat(response.usuarioId()).isEqualTo(existing.getId());
        assertThat(response.email()).isEqualTo("produtora@example.com");
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void createsProducerWithGoogleSubjectAndUnusableLocalPassword() {
        when(users.findByGoogleSubject("sub-new")).thenReturn(Optional.empty());
        when(users.existsByEmailIgnoreCase("Nova@Example.com")).thenReturn(false);

        var response = service.login(google("sub-new", "Nova@Example.com", true, "Nova Produtora"));

        ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(created.capture());
        assertThat(created.getValue().getGoogleSubject()).isEqualTo("sub-new");
        assertThat(created.getValue().getEmail()).isEqualTo("nova@example.com");
        assertThat(created.getValue().getRole()).isEqualTo(Role.PRODUTOR);
        assertThat(created.getValue().getPasswordHash()).startsWith("$2");
        assertThat(response.nome()).isEqualTo("Nova Produtora");
        assertThat(response.perfil()).isEqualTo(Role.PRODUTOR);
    }

    @Test
    void neverLinksAnExistingLocalAccountByEmail() {
        when(users.findByGoogleSubject("sub-attacker")).thenReturn(Optional.empty());
        when(users.existsByEmailIgnoreCase("demo@nexocampo.local")).thenReturn(true);

        assertThatThrownBy(() -> service.login(google("sub-attacker", "demo@nexocampo.local", true, "X")))
                .satisfies(error -> assertThat(errorCode(error)).isEqualTo("ACCOUNT_EMAIL_CONFLICT"));
        verify(users, never()).saveAndFlush(any());
        verify(jwt, never()).generateToken(any());
    }

    @Test
    void rejectsUnverifiedOrMissingEmail() {
        assertThatThrownBy(() -> service.login(google("sub", "x@example.com", false, "X")))
                .satisfies(error -> assertThat(errorCode(error)).isEqualTo("GOOGLE_EMAIL_UNVERIFIED"));
        assertThatThrownBy(() -> service.login(google("sub", "x@example.com", null, "X")))
                .satisfies(error -> assertThat(errorCode(error)).isEqualTo("GOOGLE_EMAIL_UNVERIFIED"));
        assertThatThrownBy(() -> service.login(google("sub", " ", true, "X")))
                .satisfies(error -> assertThat(errorCode(error)).isEqualTo("GOOGLE_LOGIN_FAILED"));
        verify(jwt, never()).generateToken(any());
    }

    @Test
    void longOrMissingNameIsSafeForTheColumn() {
        when(users.findByGoogleSubject(any())).thenReturn(Optional.empty());
        when(users.existsByEmailIgnoreCase(any())).thenReturn(false);

        assertThat(service.login(google("sub-a", "a@example.com", true, null)).nome()).isEqualTo("Produtor");
        assertThat(service.login(google("sub-b", "b@example.com", true, "N".repeat(300))).nome()).hasSize(120);
    }
}
