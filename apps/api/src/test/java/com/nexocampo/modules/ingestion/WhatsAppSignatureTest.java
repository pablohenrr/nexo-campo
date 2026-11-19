package com.nexocampo.modules.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WhatsAppSignatureTest {

    private static final String SECRET = "segredo-do-app";
    private static final byte[] BODY = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);

    @Test
    void acceptsTheSignatureMetaComputesOverTheRawBody() {
        // Calculado fora do Java: hmac.new(b"segredo-do-app", BODY, hashlib.sha256).hexdigest()
        String expected = "4b590d2917669029f2858051aeb8de139ef37aa8ac23e04fca1a1dbc289633f8";

        assertThat(WhatsAppSignature.sign(SECRET, BODY)).isEqualTo(expected);
        assertThat(WhatsAppSignature.isValid(SECRET, BODY, "sha256=" + expected)).isTrue();
        assertThat(WhatsAppSignature.isValid(SECRET, BODY, "sha256=" + expected.toUpperCase())).isTrue();
    }

    @Test
    void rejectsChangedBodyWrongSecretOrMissingHeader() {
        String header = "sha256=" + WhatsAppSignature.sign(SECRET, BODY);

        assertThat(WhatsAppSignature.isValid(SECRET, "{}".getBytes(StandardCharsets.UTF_8), header)).isFalse();
        assertThat(WhatsAppSignature.isValid("outro", BODY, header)).isFalse();
        assertThat(WhatsAppSignature.isValid(SECRET, BODY, null)).isFalse();
        assertThat(WhatsAppSignature.isValid(SECRET, BODY, WhatsAppSignature.sign(SECRET, BODY))).isFalse();
        assertThat(WhatsAppSignature.isValid("", BODY, header)).isFalse();
    }

    @Test
    void comparesVerifyTokens() {
        assertThat(WhatsAppSignature.sameToken("abc", "abc")).isTrue();
        assertThat(WhatsAppSignature.sameToken("abc", "abd")).isFalse();
        assertThat(WhatsAppSignature.sameToken("", "")).isFalse();
        assertThat(WhatsAppSignature.sameToken(null, null)).isFalse();
    }
}
