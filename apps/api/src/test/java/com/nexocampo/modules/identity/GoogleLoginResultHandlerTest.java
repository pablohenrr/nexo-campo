package com.nexocampo.modules.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexocampo.config.GoogleOAuthProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class GoogleLoginResultHandlerTest {
    private final GoogleAccountService accounts = mock(GoogleAccountService.class);
    private final GoogleOAuthProperties properties = new GoogleOAuthProperties(true, "id", "secret",
            "http://localhost:8080/login/oauth2/code/google", "http://localhost:5173");
    private final GoogleLoginResultHandler handler = new GoogleLoginResultHandler(accounts, properties, new ObjectMapper());

    private MockHttpServletResponse succeed(String nome) throws Exception {
        when(accounts.login(any())).thenReturn(new AuthService.AuthResponse(
                "jwt-local", UUID.randomUUID(), nome, "p@example.com", Role.PRODUTOR));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, new TestingAuthenticationToken(mock(OidcUser.class), null));
        assertThat(session.isInvalid()).as("sessão do handshake encerrada").isTrue();
        return response;
    }

    @Test
    void successPostsTheLocalSessionOnlyToTheConfiguredFrontend() throws Exception {
        MockHttpServletResponse response = succeed("Produtora");
        String html = response.getContentAsString();

        assertThat(html).contains("window.opener.postMessage(").contains("\"http://localhost:5173\")")
                .contains("\"type\":\"nexo:google-auth\"").contains("\"ok\":true").contains("\"token\":\"jwt-local\"")
                .doesNotContain("\"*\"");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getHeader("Referrer-Policy")).isEqualTo("no-referrer");
        assertThat(response.getHeader("Content-Security-Policy")).contains("default-src 'none'").contains("script-src 'nonce-");
    }

    @Test
    void escapesNamesThatCouldBreakOutOfTheScript() throws Exception {
        String html = succeed("</script><script>alert(1)</script>").getContentAsString();

        assertThat(html).doesNotContain("</script><script>alert(1)").contains("\\u003c/script\\u003e");
    }

    @Test
    void accountConflictIsReportedWithoutToken() throws Exception {
        when(accounts.login(any())).thenThrow(new OAuth2AuthenticationException(new OAuth2Error("ACCOUNT_EMAIL_CONFLICT")));
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                new TestingAuthenticationToken(mock(OidcUser.class), null));

        assertThat(response.getContentAsString()).contains("\"ok\":false").contains("ACCOUNT_EMAIL_CONFLICT")
                .doesNotContain("token");
    }

    @Test
    void providerFailureNeverReflectsItsMessage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new BadCredentialsException("<img src=x onerror=alert(1)> invalid_grant"));

        assertThat(response.getContentAsString()).contains("GOOGLE_LOGIN_FAILED")
                .doesNotContain("invalid_grant").doesNotContain("onerror");
    }
}
