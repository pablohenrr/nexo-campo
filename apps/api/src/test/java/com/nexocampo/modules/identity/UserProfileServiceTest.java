package com.nexocampo.modules.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserProfileServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private final UserProfileService service =
            new UserProfileService(userRepository, new PhoneNumberService(), passwordEncoder);

    @Test
    void showsTheProfileOfTheLoggedInUser() {
        User user = localUser("senha-antiga-123");
        user.setPhoneNumber("+5511987654321");
        user.setSmsNotificationsEnabled(true);

        UserProfileService.ProfileResponse profile = service.get(user);

        assertThat(profile.nome()).isEqualTo("Maria da Silva");
        assertThat(profile.email()).isEqualTo("maria@example.com");
        assertThat(profile.perfil()).isEqualTo(Role.PRODUTOR);
        assertThat(profile.telefone()).isEqualTo("+5511987654321");
        assertThat(profile.notificacoesSmsAtivas()).isTrue();
        assertThat(profile.contaGoogle()).isFalse();
    }

    @Test
    void updatesNamePhoneAndSmsPreference() {
        User user = localUser("senha-antiga-123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileService.ProfileResponse profile = service.update(
                user, new UserProfileService.ProfileRequest("  Maria Souza ", "(11) 98765-4321", true));

        assertThat(profile.nome()).isEqualTo("Maria Souza");
        assertThat(profile.telefone()).isEqualTo("+5511987654321");
        assertThat(profile.notificacoesSmsAtivas()).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void removesThePhoneWhenItIsLeftBlank() {
        User user = localUser("senha-antiga-123");
        user.setPhoneNumber("+5511987654321");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileService.ProfileResponse profile =
                service.update(user, new UserProfileService.ProfileRequest("Maria", "  ", false));

        assertThat(profile.telefone()).isNull();
        verify(userRepository, never()).existsByPhoneNumberAndIdNot(any(), any());
    }

    @Test
    void rejectsAPhoneThatBelongsToAnotherAccount() {
        User user = localUser("senha-antiga-123");
        when(userRepository.existsByPhoneNumberAndIdNot("+5511987654321", user.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.update(
                        user, new UserProfileService.ProfileRequest("Maria", "11 98765-4321", false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PhoneNumberService.PHONE_IN_USE);
        verify(userRepository, never()).save(any());
    }

    @Test
    void changesThePasswordWhenTheCurrentOneMatches() {
        User user = localUser("senha-antiga-123");

        service.changePassword(user, new UserProfileService.PasswordChangeRequest("senha-antiga-123", "senha-nova-456"));

        assertThat(passwordEncoder.matches("senha-nova-456", user.getPasswordHash())).isTrue();
        verify(userRepository).save(user);
    }

    @Test
    void refusesToChangeThePasswordWithAWrongCurrentPassword() {
        User user = localUser("senha-antiga-123");

        assertThatThrownBy(() -> service.changePassword(
                        user, new UserProfileService.PasswordChangeRequest("chute-errado", "senha-nova-456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A senha atual não confere");
        assertThat(passwordEncoder.matches("senha-antiga-123", user.getPasswordHash())).isTrue();
    }

    @Test
    void refusesToReuseTheCurrentPassword() {
        User user = localUser("senha-antiga-123");

        assertThatThrownBy(() -> service.changePassword(
                        user, new UserProfileService.PasswordChangeRequest("senha-antiga-123", "senha-antiga-123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A nova senha precisa ser diferente da atual");
    }

    @Test
    void googleAccountsHaveNoPasswordToChange() {
        User user = localUser("senha-aleatoria-que-ninguem-sabe");
        user.setGoogleSubject("google-123");

        assertThat(service.get(user).contaGoogle()).isTrue();
        assertThatThrownBy(() -> service.changePassword(
                        user, new UserProfileService.PasswordChangeRequest("qualquer", "senha-nova-456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entra pelo Google");
        verify(userRepository, never()).save(any());
    }

    private User localUser(String password) {
        User user = new User();
        user.setName("Maria da Silva");
        user.setEmail("maria@example.com");
        user.setPasswordHash(passwordEncoder.encode(password));
        return user;
    }
}
