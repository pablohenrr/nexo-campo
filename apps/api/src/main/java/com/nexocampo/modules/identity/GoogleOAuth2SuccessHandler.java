package com.nexocampo.modules.identity;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final String frontendUrl;

    public GoogleOAuth2SuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        if (!(authentication.getPrincipal() instanceof OidcUser googleUser)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_google_user"),
                    "Resposta OAuth do Google inválida");
        }

        Object emailVerified = googleUser.getAttribute("email_verified");
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_verified"),
                    "O e-mail do Google não foi verificado");
        }

        String email = googleUser.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_missing"),
                    "O Google não informou um e-mail");
        }

        String subject = googleUser.getSubject();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> criarUsuario(googleUser, email));

        if (user.getProvider() == null) {
            user.setProvider("GOOGLE");
            user.setProviderSubject(subject);
        } else if (!"GOOGLE".equals(user.getProvider()) || !subject.equals(user.getProviderSubject())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("account_link_conflict"),
                    "Esta conta já está vinculada a outro provedor");
        }

        userRepository.save(user);
        String token = jwtService.generateToken(user);
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/oauth/callback")
                .fragment("token=" + token)
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    private User criarUsuario(OidcUser googleUser, String email) {
        User user = new User();
        user.setName(nomeGoogle(googleUser, email));
        user.setEmail(email);
        user.setProvider("GOOGLE");
        user.setProviderSubject(googleUser.getSubject());
        return user;
    }

    private String nomeGoogle(OidcUser googleUser, String email) {
        String name = googleUser.getFullName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return email.substring(0, email.indexOf('@'));
    }

    public String frontendUrl(String path) {
        return UriComponentsBuilder
                .fromUriString(frontendUrl)
                .path("/oauth/callback")
                .queryParam("erro", "oauth_google_falhou")
                .build()
                .encode()
                .toUriString();
    }
}
