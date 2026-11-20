package com.nexocampo.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.oauth.google")
public record GoogleOAuthProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("") String clientId,
        @DefaultValue("") String clientSecret,
        @DefaultValue("http://localhost:8080/login/oauth2/code/google") String redirectUri,
        @DefaultValue("http://localhost:5173") String frontendOrigin) {
    public GoogleOAuthProperties {
        validateUrl(frontendOrigin, true);
        validateUrl(redirectUri, false);
        if (enabled && (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank())) {
            throw new IllegalArgumentException("Configure GOOGLE_CLIENT_ID e GOOGLE_CLIENT_SECRET para habilitar Google OAuth");
        }
    }

    private static void validateUrl(String value, boolean originOnly) {
        URI uri;
        try { uri = URI.create(value); } catch (Exception exception) {
            throw new IllegalArgumentException("URL inválida na configuração Google OAuth");
        }
        boolean local = "localhost".equals(uri.getHost()) || "127.0.0.1".equals(uri.getHost()) || "[::1]".equals(uri.getHost());
        if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !("https".equals(uri.getScheme()) || (local && "http".equals(uri.getScheme())))
                || (originOnly && uri.getPath() != null && !uri.getPath().isEmpty())) {
            throw new IllegalArgumentException("Google OAuth exige origem exata e HTTPS, exceto localhost");
        }
        if (!originOnly && !"/login/oauth2/code/google".equals(uri.getPath())) {
            throw new IllegalArgumentException("Callback Google deve terminar em /login/oauth2/code/google");
        }
    }
}
