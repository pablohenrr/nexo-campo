package com.nexocampo.modules.activity;

import com.nexocampo.modules.farm.Plot;
import com.nexocampo.modules.farm.PlotRepository;
import com.nexocampo.modules.identity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final PlotRepository plotRepository;

    public ActivityService(ActivityRepository activityRepository, PlotRepository plotRepository) {
        this.activityRepository = activityRepository;
        this.plotRepository = plotRepository;
    }

    @Transactional
    public ActivityResponse create(UUID plotId, ActivityRequest request, User responsible) {
        Plot plot = plotRepository.findByIdAndFarmOwnerId(plotId, responsible.getId())
                .orElseThrow(() -> new IllegalArgumentException("Talhão não encontrado"));
        Activity activity = new Activity();
        activity.setPlot(plot);
        activity.setResponsible(responsible);
        activity.setType(request.tipo());
        activity.setDescription(request.descricao());
        activity.setActivityDate(request.dataAtividade());
        activity.setCost(request.custo());
        activity.setInputs(request.insumos());
        return toResponse(activityRepository.save(activity));
    }

    @Transactional(readOnly = true)
    public List<ActivityResponse> findByPlot(UUID plotId, User owner) {
        plotRepository.findByIdAndFarmOwnerId(plotId, owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("Talhão não encontrado"));
        return activityRepository.findAllByPlotIdOrderByActivityDateDesc(plotId)
                .stream().map(this::toResponse).toList();
    }

    private ActivityResponse toResponse(Activity activity) {
        return new ActivityResponse(activity.getId(), activity.getPlot().getId(), activity.getType(),
                activity.getDescription(), activity.getActivityDate(), activity.getCost(), activity.getInputs(),
                activity.getResponsible().getId());
    }

    public record ActivityRequest(
            @NotBlank @Size(max = 80) String tipo,
            @Size(max = 1000) String descricao,
            @NotNull LocalDate dataAtividade,
            BigDecimal custo,
            @Size(max = 500) String insumos) {}

    public record ActivityResponse(UUID id, UUID talhaoId, String tipo, String descricao, LocalDate dataAtividade,
                                   BigDecimal custo, String insumos, UUID responsavelId) {}
}
