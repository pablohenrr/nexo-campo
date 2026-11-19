package com.nexocampo.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexocampo.modules.identity.User;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WhatsAppLinkServiceTest {

    private WhatsAppLinkRepository repository;
    private WhatsAppLinkService service;
    private User producer;

    @BeforeEach
    void setUp() {
        repository = mock(WhatsAppLinkRepository.class);
        service = new WhatsAppLinkService(repository);
        producer = user();
        when(repository.findByUserId(producer.getId())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void storesTheNumberInWhatsAppFormat() {
        when(repository.findByPhoneIn(any())).thenReturn(List.of());

        WhatsAppLinkService.WhatsAppLinkResponse response =
                service.link(producer, new WhatsAppLinkService.WhatsAppLinkRequest("(11) 91234-5678"));

        assertThat(response.telefone()).isEqualTo("5511912345678");
    }

    @Test
    void refusesANumberLinkedToSomeoneElse() {
        WhatsAppLink otherProducersLink = link(user(), "551112345678");
        when(repository.findByPhoneIn(Set.of("5511912345678", "551112345678"))).thenReturn(List.of(otherProducersLink));

        assertThatThrownBy(() -> service.link(producer, new WhatsAppLinkService.WhatsAppLinkRequest("11 91234-5678")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Este número já está vinculado a outra conta");
        verify(repository, never()).save(any());
    }

    @Test
    void findsTheProducerEvenWhenWhatsAppDropsTheNinthDigit() {
        WhatsAppLink producersLink = link(producer, "5511912345678");
        when(repository.findByPhoneIn(Set.of("551112345678", "5511912345678"))).thenReturn(List.of(producersLink));

        assertThat(service.findUserByPhone("551112345678")).contains(producer);
    }

    private static User user() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        return user;
    }

    private static WhatsAppLink link(User user, String phone) {
        WhatsAppLink link = new WhatsAppLink();
        link.setUser(user);
        link.setPhone(phone);
        return link;
    }
}
