package com.nexocampo.modules.ingestion;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Assinatura que a Meta envia em cada webhook: {@code X-Hub-Signature-256: sha256=<hex>}, o
 * HMAC-SHA256 do corpo cru calculado com a chave secreta do app.
 */
final class WhatsAppSignature {

    static final String HEADER = "X-Hub-Signature-256";
    private static final String PREFIX = "sha256=";
    private static final String ALGORITHM = "HmacSHA256";

    private WhatsAppSignature() {
    }

    static boolean isValid(String appSecret, byte[] body, String header) {
        if (appSecret == null || appSecret.isBlank() || header == null || !header.startsWith(PREFIX)) {
            return false;
        }
        byte[] expected = sign(appSecret, body).getBytes(StandardCharsets.US_ASCII);
        byte[] received = header.substring(PREFIX.length()).toLowerCase().getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, received);
    }

    /** Hex minúsculo, sem o prefixo {@code sha256=}. */
    static String sign(String appSecret, byte[] body) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("HMAC-SHA256 indisponível", exception);
        }
    }

    static boolean sameToken(String expected, String received) {
        if (expected == null || expected.isBlank() || received == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8));
    }
}
