package com.nexocampo.modules.investigation;

import com.nexocampo.modules.evidence.EvidenceRepository;
import com.nexocampo.modules.farm.Plot;
import com.nexocampo.modules.farm.PlotRepository;
import com.nexocampo.modules.identity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OccurrenceService {

    private final OccurrenceRepository occurrenceRepository;
    private final PlotRepository plotRepository;
    private final EvidenceRepository evidenceRepository;

    public OccurrenceService(
            OccurrenceRepository occurrenceRepository,
            PlotRepository plotRepository,
            EvidenceRepository evidenceRepository) {
        this.occurrenceRepository = occurrenceRepository;
        this.plotRepository = plotRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional
    public OccurrenceResponse register(UUID plotId, OccurrenceRequest request, User reporter) {
        Plot plot = plotRepository.findByIdAndFarmOwnerId(plotId, reporter.getId())
                .orElseThrow(() -> new IllegalArgumentException("Talhão não encontrado"));
        if (request.evidenceId() != null) {
            requireEvidenceFromPlot(request.evidenceId(), plotId);
        }
        Occurrence occurrence = new Occurrence();
        occurrence.setPlot(plot);
        occurrence.setReportedBy(reporter);
        occurrence.setCategory(request.category());
        occurrence.setSeverity(request.severity());
        occurrence.setDescription(request.description());
        occurrence.setObservedAt(request.observedAt());
        occurrence.setEvidenceId(request.evidenceId());
        return toResponse(occurrenceRepository.save(occurrence));
    }

    @Transactional(readOnly = true)
    public List<OccurrenceResponse> findByPlot(UUID plotId, User owner) {
        plotRepository.findByIdAndFarmOwnerId(plotId, owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("Talhão não encontrado"));
        return occurrenceRepository.findAllByPlotIdOrderByObservedAtDescCreatedAtDesc(plotId)
                .stream().map(this::toResponse).toList();
    }

    private void requireEvidenceFromPlot(UUID evidenceId, UUID plotId) {
        evidenceRepository.findById(evidenceId)
                .filter(evidence -> evidence.getPlot().getId().equals(plotId))
                .orElseThrow(() -> new IllegalArgumentException("Evidência não encontrada neste talhão"));
    }

    private OccurrenceResponse toResponse(Occurrence occurrence) {
        return new OccurrenceResponse(occurrence.getId(), occurrence.getPlot().getId(), occurrence.getCategory(),
                occurrence.getSeverity(), occurrence.getStatus(), occurrence.getDescription(),
                occurrence.getObservedAt(), occurrence.getEvidenceId(), occurrence.getReportedBy().getId(),
                occurrence.getCreatedAt());
    }

    public record OccurrenceRequest(
            @NotNull OccurrenceCategory category,
            @NotNull OccurrenceSeverity severity,
            @NotBlank @Size(max = 1000) String description,
            @NotNull LocalDate observedAt,
            UUID evidenceId) {}

    public record OccurrenceResponse(UUID id, UUID plotId, OccurrenceCategory category, OccurrenceSeverity severity,
                                     OccurrenceStatus status, String description, LocalDate observedAt,
                                     UUID evidenceId, UUID reportedById, OffsetDateTime createdAt) {}
}
