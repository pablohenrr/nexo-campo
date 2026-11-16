package com.nexocampo.modules.ingestion;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.springframework.web.multipart.MultipartFile;

/** Arquivo recebido junto com a mensagem (áudio ou foto), já em memória. */
public record IncomingMedia(byte[] content, String contentType, String fileName) {

    /** Limite do Whisper e folga para fotos de celular. */
    public static final long MAX_BYTES = 25L * 1024 * 1024;

    public IncomingMedia {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("O arquivo não pode estar vazio");
        }
        if (content.length > MAX_BYTES) {
            throw new IllegalArgumentException("Arquivo muito grande para análise (máximo de 25 MB)");
        }
    }

    /** Converte um upload HTTP; devolve {@code null} quando nenhum arquivo foi enviado. */
    public static IncomingMedia fromUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Arquivo muito grande para análise (máximo de 25 MB)");
        }
        try {
            return new IncomingMedia(file.getBytes(), file.getContentType(), file.getOriginalFilename());
        } catch (IOException exception) {
            throw new UncheckedIOException("Não foi possível ler o arquivo enviado", exception);
        }
    }
}
