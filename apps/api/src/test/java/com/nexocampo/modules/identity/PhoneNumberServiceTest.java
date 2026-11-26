package com.nexocampo.modules.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhoneNumberServiceTest {

    private final PhoneNumberService phoneNumberService = new PhoneNumberService();

    @Test
    void normalizesBrazilianMobileNumberToE164() {
        assertThat(phoneNumberService.normalizeForSms("(11) 98765-4321", false))
                .isEqualTo("+5511987654321");
    }

    @Test
    void allowsPhoneToBeOmittedWhenSmsIsDisabled() {
        assertThat(phoneNumberService.normalizeForSms(" ", false)).isNull();
    }

    @Test
    void rejectsInvalidPhoneNumber() {
        assertThatThrownBy(() -> phoneNumberService.normalizeForSms("123", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe um telefone válido com DDD");
    }

    @Test
    void requiresMobilePhoneWhenSmsIsEnabled() {
        assertThatThrownBy(() -> phoneNumberService.normalizeForSms(null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe um telefone válido para ativar notificações por SMS");

        assertThatThrownBy(() -> phoneNumberService.normalizeForSms("(11) 3333-4444", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Informe um número de celular válido para receber SMS");
    }
}
