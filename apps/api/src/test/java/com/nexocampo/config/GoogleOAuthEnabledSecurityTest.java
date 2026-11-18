package com.nexocampo.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexocampo.modules.identity.GoogleAccountService;
import com.nexocampo.modules.identity.GoogleLoginController;
import com.nexocampo.modules.identity.GoogleLoginResultHandler;
import com.nexocampo.modules.identity.JwtService;
import com.nexocampo.modules.identity.UserRepository;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

/** Login Google ligado com credenciais de teste: confere o pedido enviado ao Google e o isolamento da sessão. */
@WebMvcTest(GoogleLoginController.class)
@Import({SecurityConfig.class, GoogleOAuthSecurityConfig.class, GoogleLoginResultHandler.class})
@TestPropertySource(properties = {
        "app.oauth.google.enabled=true",
        "app.oauth.google.client-id=test-client.apps.googleusercontent.com",
        "app.oauth.google.client-secret=test-secret",
        "app.oauth.google.redirect-uri=http://localhost:8080/login/oauth2/code/google",
        "app.oauth.google.frontend-origin=http://localhost:5173"})
class GoogleOAuthEnabledSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean GoogleAccountService accounts;
    @MockitoBean JwtService jwt;
    @MockitoBean UserRepository users;

    @Test
    void redirectsToGoogleWithOidcStateNonceAndPkce() throws Exception {
        MvcResult result = mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        URI location = URI.create(result.getResponse().getRedirectedUrl());
        var raw = UriComponentsBuilder.fromUri(location).build().getQueryParams();
        java.util.function.Function<String, String> query = key ->
                raw.getFirst(key) == null ? null : URLDecoder.decode(raw.getFirst(key), StandardCharsets.UTF_8);

        assertThat(location.getHost()).isEqualTo("accounts.google.com");
        assertThat(query.apply("client_id")).isEqualTo("test-client.apps.googleusercontent.com");
        assertThat(query.apply("response_type")).isEqualTo("code");
        assertThat(query.apply("scope")).contains("openid").contains("profile").contains("email");
        assertThat(query.apply("redirect_uri")).contains("localhost:8080").contains("login").contains("google");
        assertThat(query.apply("state")).isNotBlank();
        assertThat(query.apply("nonce")).isNotBlank();
        assertThat(query.apply("code_challenge")).isNotBlank();
        assertThat(query.apply("code_challenge_method")).isEqualTo("S256");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("test-secret");
        assertThat(location.toString()).doesNotContain("test-secret");
    }

    @Test
    void configurationSaysEnabledWithoutExposingSecrets() throws Exception {
        String body = mvc.perform(get("/api/autenticacao/google/configuracao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("test-secret").doesNotContain("test-client");
    }

    @Test
    void handshakeSessionIsNotAnApiCredential() throws Exception {
        MvcResult start = mvc.perform(get("/oauth2/authorization/google")).andReturn();
        MockHttpSession session = (MockHttpSession) start.getRequest().getSession(false);
        assertThat(session).as("o pedido de autorização fica na sessão do handshake").isNotNull();

        mvc.perform(get("/api/fazendas").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void forgedCallbackWithoutAuthorizationRequestFailsClosed() throws Exception {
        String html = mvc.perform(get("/login/oauth2/code/google").param("code", "forged").param("state", "forged"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("GOOGLE_LOGIN_FAILED").doesNotContain("\"ok\":true").doesNotContain("forged");
    }
}
