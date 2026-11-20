package com.nexocampo.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GoogleOAuthPropertiesTest {
    private static final String CALLBACK = "http://localhost:8080/login/oauth2/code/google";

    @Test
    void disabledByDefaultNeedsNoCredentials() {
        assertThatCode(() -> new GoogleOAuthProperties(false, "", "", CALLBACK, "http://localhost:5173"))
                .doesNotThrowAnyException();
    }

    @Test
    void enablingRequiresClientIdAndSecret() {
        assertThatThrownBy(() -> new GoogleOAuthProperties(true, "id", "", CALLBACK, "http://localhost:5173"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GoogleOAuthProperties(true, "", "secret", CALLBACK, "http://localhost:5173"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new GoogleOAuthProperties(true, "id", "secret", CALLBACK, "http://localhost:5173"))
                .doesNotThrowAnyException();
    }

    @Test
    void frontendOriginMustBeAnExactOrigin() {
        for (String origin : new String[] {"http://localhost:5173/", "http://localhost:5173/app",
                "http://example.com", "https://user@example.com", "*", "https://example.com?x=1"}) {
            assertThatThrownBy(() -> new GoogleOAuthProperties(false, "", "", CALLBACK, origin))
                    .as(origin).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatCode(() -> new GoogleOAuthProperties(false, "", "", "https://api.example.com/login/oauth2/code/google",
                "https://app.example.com")).doesNotThrowAnyException();
    }

    @Test
    void callbackMustBeTheSpringGoogleEndpoint() {
        assertThatThrownBy(() -> new GoogleOAuthProperties(false, "", "", "http://localhost:8080/callback", "http://localhost:5173"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GoogleOAuthProperties(false, "", "", "http://api.example.com/login/oauth2/code/google",
                "http://localhost:5173")).isInstanceOf(IllegalArgumentException.class);
    }
}
