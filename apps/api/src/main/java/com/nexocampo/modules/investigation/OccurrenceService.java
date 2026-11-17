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
        if (request.evidenciaId() != null) {
            requireEvidenceFromPlot(request.evidenciaId(), plotId);
        }
        Occurrence occurrence = new Occurrence();
        occurrence.setPlot(plot);
        occurrence.setReportedBy(reporter);
        occurrence.setCategory(request.categoria());
        occurrence.setSeverity(request.severidade());
        occurrence.setDescription(request.descricao());
        occurrence.setObservedAt(request.dataObservacao());
        occurrence.setEvidenceId(request.evidenciaId());
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
            @NotNull OccurrenceCategory categoria,
            @NotNull OccurrenceSeverity severidade,
            @NotBlank @Size(max = 1000) String descricao,
            @NotNull LocalDate dataObservacao,
            UUID evidenciaId) {}

    public record OccurrenceResponse(UUID id, UUID talhaoId, OccurrenceCategory categoria,
                                     OccurrenceSeverity severidade, OccurrenceStatus situacao, String descricao,
                                     LocalDate dataObservacao, UUID evidenciaId, UUID responsavelId,
                                     OffsetDateTime criadaEm) {}
}
