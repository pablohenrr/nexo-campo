package com.nexocampo.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhoneNumbersTest {

    @Test
    void normalizesWhatTheProducerTypes() {
        assertThat(PhoneNumbers.normalize("(11) 91234-5678")).isEqualTo("5511912345678");
        assertThat(PhoneNumbers.normalize("+55 11 91234-5678")).isEqualTo("5511912345678");
        assertThat(PhoneNumbers.normalize("11 3456-7890")).isEqualTo("551134567890");
    }

    @Test
    void rejectsNumbersWithoutAreaCode() {
        assertThatThrownBy(() -> PhoneNumbers.normalize("91234-5678")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PhoneNumbers.normalize(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void matchesBrazilianMobilesWithAndWithoutTheNinthDigit() {
        assertThat(PhoneNumbers.variants("5511912345678")).containsExactly("5511912345678", "551112345678");
        assertThat(PhoneNumbers.variants("551192345678")).containsExactly("551192345678", "5511992345678");
        assertThat(PhoneNumbers.variants("551112345678")).containsExactly("551112345678", "5511912345678");
        assertThat(PhoneNumbers.variants("551134567890")).containsExactly("551134567890");
        assertThat(PhoneNumbers.variants("14155552671")).containsExactly("14155552671");
    }
}
