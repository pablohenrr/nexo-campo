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
    public String save(MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename() == null ? "arquivo" : file.getOriginalFilename();
        String extension = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf('.')).replaceAll("[^a-zA-Z0-9.]", "")
                : "";
        String key = UUID.randomUUID() + extension;
        file.transferTo(root.resolve(key));
        return key;
    }
}
