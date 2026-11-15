package com.nexocampo.modules.evidence;

import com.nexocampo.modules.farm.Plot;
import com.nexocampo.modules.farm.PlotRepository;
import com.nexocampo.modules.identity.User;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EvidenceService {

    private static final java.util.Map<EvidenceType, Set<String>> ALLOWED_CONTENT_TYPES = java.util.Map.of(
            EvidenceType.FOTO, Set.of("image/jpeg", "image/png", "image/webp"),
            EvidenceType.VIDEO, Set.of("video/mp4", "video/quicktime"),
            EvidenceType.AUDIO, Set.of("audio/mpeg", "audio/wav", "audio/ogg", "audio/mp4"),
            EvidenceType.DOCUMENTO, Set.of(
                    "application/pdf",
                    "text/plain",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

    private final EvidenceRepository evidenceRepository;
    private final PlotRepository plotRepository;
    private final FileStorage fileStorage;

    public EvidenceService(
            EvidenceRepository evidenceRepository,
            PlotRepository plotRepository,
            FileStorage fileStorage) {
        this.evidenceRepository = evidenceRepository;
        this.plotRepository = plotRepository;
        this.fileStorage = fileStorage;
    }

    @Transactional
    public EvidenceResponse upload(UUID plotId, EvidenceType type, MultipartFile file, User user) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo não pode estar vazio");
        }
        Plot plot = plotRepository.findByIdAndFarmOwnerId(plotId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Talhão não encontrado"));
        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String storageKey = null;
        try {
            String contentType = validateUpload(type, file);
            Evidence evidence = new Evidence();
            evidence.setPlot(plot);
            evidence.setUploadedBy(user);
            evidence.setType(type);
            evidence.setOriginalName(originalName);
            evidence.setContentType(contentType);
            evidence.setSizeBytes(file.getSize());
            storageKey = fileStorage.save(file, contentType);
            evidence.setStorageKey(storageKey);
            return toResponse(evidenceRepository.save(evidence));
        } catch (IOException exception) {
            deleteAfterFailure(storageKey, exception);
            throw new IllegalStateException("Não foi possível armazenar o arquivo", exception);
        } catch (RuntimeException exception) {
            deleteAfterFailure(storageKey, exception);
            throw exception;
        }
    }

    private String validateUpload(EvidenceType type, MultipartFile file) throws IOException {
        String detectedContentType = detectContentType(file, type);
        if (!allowedContentTypes(type).contains(detectedContentType)) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido para esta evidência");
        }
        return detectedContentType;
    }

    private String detectContentType(MultipartFile file, EvidenceType type) throws IOException {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(16);
            if (startsWith(header, new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF})) {
                return "image/jpeg";
            }
            if (startsWith(header, new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
                return "image/png";
            }
            if (startsWith(header, new byte[] {0x52, 0x49, 0x46, 0x46})
                    && containsAt(header, 8, new byte[] {0x57, 0x45, 0x42, 0x50})) {
                return "image/webp";
            }
            if (containsAt(header, 4, new byte[] {0x66, 0x74, 0x79, 0x70})) {
                return type == EvidenceType.AUDIO ? "audio/mp4" : "video/mp4";
            }
            if (startsWith(header, new byte[] {0x49, 0x44, 0x33})) {
                return "audio/mpeg";
            }
            if (startsWith(header, new byte[] {0x52, 0x49, 0x46, 0x46})
                    && containsAt(header, 8, new byte[] {0x57, 0x41, 0x56, 0x45})) {
                return "audio/wav";
            }
            if (startsWith(header, new byte[] {0x4F, 0x67, 0x67, 0x53})) {
                return "audio/ogg";
            }
            if (startsWith(header, new byte[] {0x25, 0x50, 0x44, 0x46})) {
                return "application/pdf";
            }
            throw new IllegalArgumentException("Assinatura do arquivo não reconhecida");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        return containsAt(value, 0, prefix);
    }

    private boolean containsAt(byte[] value, int offset, byte[] expected) {
        if (value.length < offset + expected.length) {
            return false;
        }
        for (int index = 0; index < expected.length; index++) {
            if (value[offset + index] != expected[index]) {
                return false;
            }
        }
        return true;
    }

    private Set<String> allowedContentTypes(EvidenceType type) {
        return ALLOWED_CONTENT_TYPES.getOrDefault(type, Set.of());
    }

    private String sanitizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "arquivo";
        }
        String sanitized = originalName
                .replaceAll("[\\r\\n\\x00]", "_")
                .replaceAll("[^a-zA-Z0-9._ -]", "_");
        return sanitized.substring(0, Math.min(sanitized.length(), 255));
    }

    private void deleteAfterFailure(String storageKey, Exception failure) {
        if (storageKey == null) {
            return;
        }
        try {
            fileStorage.delete(storageKey);
        } catch (IOException cleanupFailure) {
            failure.addSuppressed(cleanupFailure);
        }
    }

    @Transactional(readOnly = true)
    public List<EvidenceResponse> findByPlot(UUID plotId, User owner) {
        plotRepository.findByIdAndFarmOwnerId(plotId, owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("Talhão não encontrado"));
        return evidenceRepository.findAllByPlotIdOrderByCreatedAtDesc(plotId)
                .stream().map(this::toResponse).toList();
    }

    private EvidenceResponse toResponse(Evidence evidence) {
        return new EvidenceResponse(evidence.getId(), evidence.getPlot().getId(), evidence.getType(),
                evidence.getOriginalName(), evidence.getContentType(), evidence.getSizeBytes(),
                evidence.getStorageKey(), evidence.getCreatedAt());
    }

    public record EvidenceResponse(
            UUID id, UUID plotId, EvidenceType type, String originalName,
            String contentType, long sizeBytes, String storageKey,
            java.time.OffsetDateTime createdAt) {}
}
