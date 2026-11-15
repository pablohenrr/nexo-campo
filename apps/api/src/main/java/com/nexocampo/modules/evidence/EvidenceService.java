package com.nexocampo.modules.evidence;

import com.nexocampo.modules.farm.Plot;
import com.nexocampo.modules.farm.PlotRepository;
import com.nexocampo.modules.identity.User;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EvidenceService {

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
        String storageKey = null;
        try {
            Evidence evidence = new Evidence();
            evidence.setPlot(plot);
            evidence.setUploadedBy(user);
            evidence.setType(type);
            evidence.setOriginalName(file.getOriginalFilename() == null ? "arquivo" : file.getOriginalFilename());
            evidence.setContentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            evidence.setSizeBytes(file.getSize());
            storageKey = fileStorage.save(file);
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
