package com.nexocampo.modules.ingestion;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Transcrição fixa para desenvolver e testar sem chave de API. */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "stub", matchIfMissing = true)
public class StubAudioTranscriber implements AudioTranscriber {

    static final String TRANSCRIPT = "Hoje apliquei 2 litros de glifosato no talhão 1, gastei 800 reais.";

    @Override
    public String transcribe(IncomingMedia audio) {
        return TRANSCRIPT;
    }
}
