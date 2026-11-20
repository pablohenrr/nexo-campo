package com.nexocampo.config;

import com.nexocampo.modules.identity.GoogleLoginResultHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class GoogleOAuthSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain googleSecurityChain(HttpSecurity http, GoogleOAuthProperties properties,
            GoogleLoginResultHandler handler) throws Exception {
        http.securityMatcher("/oauth2/authorization/google", "/login/oauth2/code/google")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        if (!properties.enabled()) {
            http.addFilterBefore(new OncePerRequestFilter() {
                @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                        jakarta.servlet.FilterChain chain) throws java.io.IOException {
                    response.setStatus(503);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-store");
                    response.getWriter().write("{\"error\":\"GOOGLE_LOGIN_DISABLED\"}");
                }
            }, UsernamePasswordAuthenticationFilter.class);
            return http.build();
        }
        ClientRegistrationRepository registrations = new InMemoryClientRegistrationRepository(
                CommonOAuth2Provider.GOOGLE.getBuilder("google")
                        .clientId(properties.clientId()).clientSecret(properties.clientSecret())
                        .redirectUri(properties.redirectUri()).scope("openid", "profile", "email").build());
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        http.oauth2Login(oauth -> oauth.clientRegistrationRepository(registrations)
                .authorizedClientRepository(new EphemeralAuthorizedClients())
                .authorizationEndpoint(endpoint -> endpoint.authorizationRequestResolver(resolver))
                .successHandler(handler).failureHandler(handler));
        return http.build();
    }

    /** Google access/refresh tokens are not needed after identity verification. */
    private static class EphemeralAuthorizedClients implements OAuth2AuthorizedClientRepository {
        @Override public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String id, Authentication user, HttpServletRequest request) { return null; }
        @Override public void saveAuthorizedClient(OAuth2AuthorizedClient client, Authentication user, HttpServletRequest request, HttpServletResponse response) {}
        @Override public void removeAuthorizedClient(String id, Authentication user, HttpServletRequest request, HttpServletResponse response) {}
    }
}
