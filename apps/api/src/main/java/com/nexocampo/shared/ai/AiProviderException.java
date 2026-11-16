package com.nexocampo.shared.ai;

/** Falha ao falar com o provedor de IA ou ao interpretar a resposta dele. */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
