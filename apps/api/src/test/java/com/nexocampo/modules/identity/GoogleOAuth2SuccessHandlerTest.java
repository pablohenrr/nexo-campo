package com.nexocampo.modules.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class GoogleOAuth2SuccessHandlerTest {

    @Test
    void createsGoogleUserAndRedirectsWithJwt() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        JwtService jwtService = mock(JwtService.class);
        GoogleOAuth2SuccessHandler handler =
                new GoogleOAuth2SuccessHandler(repository, jwtService, "http://localhost:5173");

        OidcUser googleUser = mock(OidcUser.class);
        when(googleUser.getAttribute("email_verified")).thenReturn(true);
        when(googleUser.getEmail()).thenReturn("produtor@example.com");
        when(googleUser.getSubject()).thenReturn("google-subject-123");
        when(googleUser.getFullName()).thenReturn("Produtor Teste");
        when(repository.findByEmailIgnoreCase("produtor@example.com")).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-teste");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(googleUser);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/oauth/callback#token=jwt-teste");
        verify(repository).save(any(User.class));
    }
}
