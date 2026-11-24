package com.nexocampo.modules.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class UserContactServiceTest {

    @Test
    void updatesPhoneAndSmsPreferenceForCurrentUser() {
        PhoneNumberService phoneNumberService = mock(PhoneNumberService.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserContactService service = new UserContactService(phoneNumberService, userRepository);
        User user = new User();
        user.setEmail("produtor@example.com");
        when(phoneNumberService.normalizeForSms("(11) 98765-4321", true)).thenReturn("+5511987654321");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserContactService.ContactResponse response = service.update(
                user, new UserContactService.ContactRequest("(11) 98765-4321", true));

        assertThat(response.telefone()).isEqualTo("+5511987654321");
        assertThat(response.notificacoesSmsAtivas()).isTrue();
        assertThat(user.getPhoneNumber()).isEqualTo("+5511987654321");
        assertThat(user.isSmsNotificationsEnabled()).isTrue();
        verify(userRepository).save(user);
    }
}
