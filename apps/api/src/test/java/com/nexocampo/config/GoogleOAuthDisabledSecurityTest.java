package com.nexocampo.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexocampo.modules.identity.GoogleAccountService;
import com.nexocampo.modules.identity.GoogleLoginController;
import com.nexocampo.modules.identity.GoogleLoginResultHandler;
import com.nexocampo.modules.identity.JwtService;
import com.nexocampo.modules.identity.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Login Google desligado (padrão): nada vai ao Google e o login por senha continua igual. */
@WebMvcTest(GoogleLoginController.class)
@Import({SecurityConfig.class, GoogleOAuthSecurityConfig.class, GoogleLoginResultHandler.class})
@TestPropertySource(properties = "app.oauth.google.enabled=false")
class GoogleOAuthDisabledSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean GoogleAccountService accounts;
    @MockitoBean JwtService jwt;
    @MockitoBean UserRepository users;

    @Test
    void configurationIsPublicAndSaysDisabled() throws Exception {
        mvc.perform(get("/api/autenticacao/google/configuracao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.authorizationPath").value("/oauth2/authorization/google"));
    }

    @Test
    void loginStartReturns503WithoutTouchingGoogle() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("GOOGLE_LOGIN_DISABLED"));
    }
}
