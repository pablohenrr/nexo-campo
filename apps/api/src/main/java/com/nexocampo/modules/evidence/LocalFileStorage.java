package com.nexocampo.modules.evidence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.root}") String root) throws IOException {
        this.root = Path.of(root).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    @Override
    public String save(MultipartFile file, String contentType) throws IOException {
        String extension = extensionFor(contentType);
        String key = UUID.randomUUID() + extension;
        Path target = resolveKey(key);
        try {
            file.transferTo(target);
        } catch (IOException exception) {
            Files.deleteIfExists(target);
            throw exception;
        }
        return key;
    }

    @Override
    public byte[] read(String storageKey) throws IOException {
        return Files.readAllBytes(resolveKey(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolveKey(storageKey));
    }

    private Path resolveKey(String storageKey) {
        Path target = root.resolve(storageKey).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Chave de armazenamento inválida");
        }
        return target;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/quicktime" -> ".mov";
            case "audio/mpeg" -> ".mp3";
            case "audio/wav" -> ".wav";
            case "audio/ogg" -> ".ogg";
            case "audio/mp4" -> ".m4a";
            case "application/pdf" -> ".pdf";
            default -> throw new IllegalArgumentException("Tipo de arquivo não permitido");
        };
    }
}
