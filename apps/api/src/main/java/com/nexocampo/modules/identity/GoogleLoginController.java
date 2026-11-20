package com.nexocampo.modules.identity;

import com.nexocampo.config.GoogleOAuthProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoogleLoginController {
    private final GoogleOAuthProperties properties;
    public GoogleLoginController(GoogleOAuthProperties properties) { this.properties = properties; }

    @GetMapping("/api/autenticacao/google/configuracao")
    public Configuration configuration() {
        return new Configuration(properties.enabled(), "/oauth2/authorization/google");
    }
    public record Configuration(boolean enabled, String authorizationPath) {}
}
