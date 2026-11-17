package com.nexocampo.modules.ingestion;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.springframework.web.multipart.MultipartFile;

/**
 * Adapta uma mídia já em memória (ex.: baixada do WhatsApp) para o {@code EvidenceService}, que hoje
 * só aceita {@link MultipartFile}. Assim reaproveitamos toda a validação de assinatura e o armazenamento
 * de evidências sem duplicar código. Se o módulo de evidências ganhar uma entrada que não dependa de
 * HTTP, esta classe pode sair.
 */
final class InMemoryMultipartFile implements MultipartFile {

    private final IncomingMedia media;

    InMemoryMultipartFile(IncomingMedia media) {
        this.media = media;
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return media.fileName();
    }

    @Override
    public String getContentType() {
        return media.contentType();
    }

    @Override
    public boolean isEmpty() {
        return media.content().length == 0;
    }

    @Override
    public long getSize() {
        return media.content().length;
    }

    @Override
    public byte[] getBytes() {
        return media.content().clone();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(media.content());
    }

    @Override
    public void transferTo(File destination) throws IOException {
        Files.write(destination.toPath(), media.content());
    }
}
