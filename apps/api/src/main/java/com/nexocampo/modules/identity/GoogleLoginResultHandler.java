package com.nexocampo.modules.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexocampo.config.GoogleOAuthProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginResultHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    private final GoogleAccountService accounts;
    private final GoogleOAuthProperties properties;
    private final ObjectMapper mapper;
    private static final Logger log = LoggerFactory.getLogger(GoogleLoginResultHandler.class);
    private static final Set<String> SAFE_ERRORS = Set.of("ACCOUNT_EMAIL_CONFLICT", "GOOGLE_EMAIL_UNVERIFIED");

    public GoogleLoginResultHandler(GoogleAccountService accounts, GoogleOAuthProperties properties, ObjectMapper mapper) {
        this.accounts = accounts; this.properties = properties; this.mapper = mapper;
    }

    @Override public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        Map<String, Object> payload;
        try {
            var account = accounts.login((OidcUser) authentication.getPrincipal());
            payload = Map.of("type", "nexo:google-auth", "ok", true, "data", account);
        } catch (OAuth2AuthenticationException exception) {
            log.warn("Login Google recusado: {}", exception.getError().getErrorCode());
            payload = failurePayload(exception.getError().getErrorCode());
        } catch (DataIntegrityViolationException exception) {
            log.warn("Login Google recusado por conflito de dados: {}", exception.getClass().getSimpleName());
            payload = failurePayload("ACCOUNT_EMAIL_CONFLICT");
        } catch (RuntimeException exception) {
            log.error("Falha ao concluir login Google", exception);
            payload = failurePayload("GOOGLE_LOGIN_FAILED");
        }
        write(request, response, payload);
    }

    @Override public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        // Never reflect provider error text or callback query parameters to the browser.
        // The server log keeps the OAuth error code and description (no tokens) for diagnosis.
        if (exception instanceof OAuth2AuthenticationException oauth) {
            log.warn("Login Google falhou no callback: {} - {}", oauth.getError().getErrorCode(),
                    oauth.getError().getDescription());
        } else {
            log.warn("Login Google falhou no callback: {}", exception.getClass().getSimpleName());
        }
        write(request, response, failurePayload("GOOGLE_LOGIN_FAILED"));
    }

    private Map<String, Object> failurePayload(String code) {
        return Map.of("type", "nexo:google-auth", "ok", false,
                "error", SAFE_ERRORS.contains(code) ? code : "GOOGLE_LOGIN_FAILED");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, Map<String, Object> payload) throws IOException {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        String nonce = UUID.randomUUID().toString();
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Content-Security-Policy", "default-src 'none'; script-src 'nonce-" + nonce
                + "'; base-uri 'none'; frame-ancestors 'none'; form-action 'none'");
        String serialized = safeJson(payload);
        String origin = safeJson(properties.frontendOrigin());
        response.getWriter().write("<!doctype html><html lang=\"pt-BR\"><meta charset=\"utf-8\">"
                + "<title>Login Nexo Campo</title><p>Retorne à janela do Nexo Campo para continuar.</p>"
                + "<script nonce=\"" + nonce + "\">if(window.opener){window.opener.postMessage("
                + serialized + "," + origin + ");window.close();}</script></html>");
    }

    private String safeJson(Object value) throws IOException {
        return mapper.writeValueAsString(value).replace("<", "\\u003c").replace(">", "\\u003e")
                .replace("&", "\\u0026").replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }
}
