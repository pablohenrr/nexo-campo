package com.nexocampo.shared.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        OffsetDateTime dataHora,
        int status,
        String erro,
        String mensagem,
        String caminho,
        List<String> detalhes) {
}
