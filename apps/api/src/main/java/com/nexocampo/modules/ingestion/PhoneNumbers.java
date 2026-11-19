package com.nexocampo.modules.ingestion;

import java.util.LinkedHashSet;
import java.util.Set;

/** Telefones no formato do WhatsApp: só dígitos, com código do país (ex.: {@code 5511912345678}). */
final class PhoneNumbers {

    private static final String BRAZIL = "55";

    private PhoneNumbers() {
    }

    /** Aceita o número como o produtor digita ("(11) 91234-5678"); sem código do país, assume Brasil. */
    static String normalize(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.length() == 10 || digits.length() == 11) {
            digits = BRAZIL + digits;
        }
        if (digits.length() < 12 || digits.length() > 15) {
            throw new IllegalArgumentException("Informe o telefone com DDD, por exemplo (11) 91234-5678");
        }
        return digits;
    }

    /**
     * Formas equivalentes do mesmo número. No Brasil, o WhatsApp de contas antigas identifica o celular
     * sem o nono dígito ({@code 55 11 1234-5678}), então a busca precisa considerar as duas formas.
     */
    static Set<String> variants(String digits) {
        Set<String> variants = new LinkedHashSet<>();
        variants.add(digits);
        if (digits.startsWith(BRAZIL)) {
            if (digits.length() == 13 && digits.charAt(4) == '9') {
                variants.add(digits.substring(0, 4) + digits.substring(5));
            } else if (digits.length() == 12 && !isLandline(digits.charAt(4))) {
                variants.add(digits.substring(0, 4) + "9" + digits.substring(4));
            }
        }
        return variants;
    }

    /** No Brasil, telefone fixo começa com 2 a 5 depois do DDD; o resto, com 8 dígitos, é celular sem o 9. */
    private static boolean isLandline(char firstDigit) {
        return firstDigit >= '2' && firstDigit <= '5';
    }
}
