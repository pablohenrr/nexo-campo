package com.nexocampo.modules.identity;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.springframework.stereotype.Service;

@Service
public class PhoneNumberService {

    /** O telefone identifica o produtor no WhatsApp, então só pode estar em uma conta. */
    public static final String PHONE_IN_USE = "Este telefone já está cadastrado em outra conta";

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public String normalizeForSms(String phoneNumber, boolean smsNotificationsEnabled) {
        String normalized = normalize(phoneNumber);
        if (smsNotificationsEnabled && normalized == null) {
            throw new IllegalArgumentException("Informe um telefone válido para ativar notificações por SMS");
        }
        if (smsNotificationsEnabled && !isMobileNumber(normalized)) {
            throw new IllegalArgumentException("Informe um número de celular válido para receber SMS");
        }
        return normalized;
    }

    private String normalize(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }
        try {
            Phonenumber.PhoneNumber parsed = phoneNumberUtil.parse(phoneNumber.strip(), "BR");
            if (!phoneNumberUtil.isValidNumber(parsed)) {
                throw invalidPhoneNumber();
            }
            return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException exception) {
            throw invalidPhoneNumber();
        }
    }

    private boolean isMobileNumber(String normalizedPhoneNumber) {
        try {
            Phonenumber.PhoneNumber parsed = phoneNumberUtil.parse(normalizedPhoneNumber, "BR");
            return phoneNumberUtil.getNumberType(parsed) == PhoneNumberUtil.PhoneNumberType.MOBILE;
        } catch (NumberParseException exception) {
            return false;
        }
    }

    private IllegalArgumentException invalidPhoneNumber() {
        return new IllegalArgumentException("Informe um telefone válido com DDD");
    }
}
