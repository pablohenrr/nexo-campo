package com.nexocampo.modules.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class AuthServicePhoneRegistrationTest {

    @Test
    void savesNormalizedPhoneAndSmsPreferenceDuringRegistration() {
        UserRepository userRepository = mock(UserRepository.class);
        var passwordEncoder = mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        PhoneNumberService phoneNumberService = mock(PhoneNumberService.class);
        AuthService service = new AuthService(userRepository, passwordEncoder, jwtService, phoneNumberService);
        when(userRepository.existsByEmailIgnoreCase("produtor@example.com")).thenReturn(false);
        when(passwordEncoder.encode("senha-segura-123")).thenReturn("hash");
        when(phoneNumberService.normalizeForSms("(11) 98765-4321", true)).thenReturn("+5511987654321");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt");

        service.register(new AuthService.RegisterRequest(
                "Produtor", "produtor@example.com", "senha-segura-123", "(11) 98765-4321", true));

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("+5511987654321");
        assertThat(captor.getValue().isSmsNotificationsEnabled()).isTrue();
    }

    @Test
    void rejectsAPhoneAlreadyRegisteredInAnotherAccount() {
        UserRepository userRepository = mock(UserRepository.class);
        PhoneNumberService phoneNumberService = new PhoneNumberService();
        AuthService service = new AuthService(
                userRepository,
                mock(org.springframework.security.crypto.password.PasswordEncoder.class),
                mock(JwtService.class),
                phoneNumberService);
        when(userRepository.existsByPhoneNumber("+5511987654321")).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.register(new AuthService.RegisterRequest(
                        "Produtor", "outro@example.com", "senha-segura-123", "(11) 98765-4321", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PhoneNumberService.PHONE_IN_USE);
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(any());
    }
}
