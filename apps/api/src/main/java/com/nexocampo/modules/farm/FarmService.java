package com.nexocampo.modules.farm;

import com.nexocampo.modules.identity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmService {

    private final FarmRepository farmRepository;

    public FarmService(FarmRepository farmRepository) {
        this.farmRepository = farmRepository;
    }

    @Transactional
    public FarmResponse create(@Valid FarmRequest request, User owner) {
        Farm farm = new Farm();
        apply(farm, request);
        farm.setOwner(owner);
        return toResponse(farmRepository.save(farm));
    }

    @Transactional
    public FarmResponse update(UUID farmId, @Valid FarmRequest request, User owner) {
        Farm farm = farmRepository.findByIdAndOwnerId(farmId, owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("Fazenda não encontrada"));
        apply(farm, request);
        return toResponse(farmRepository.save(farm));
    }

    @Transactional(readOnly = true)
    public List<FarmResponse> findMine(User owner) {
        return farmRepository.findAllByOwnerIdOrderByName(owner.getId()).stream().map(this::toResponse).toList();
    }

    private void apply(Farm farm, FarmRequest request) {
        // Valida o contorno antes de tocar na entidade: entrada inválida não altera nada.
        FarmBoundary boundary = FarmBoundary.fromPins(request.contorno() == null ? null
                : request.contorno().stream()
                        .map(c -> c == null ? null : new FarmBoundary.Pin(c.latitude(), c.longitude()))
                        .toList());
        FarmBoundary.Pin center = boundary.center();

        farm.setName(request.nome());
        farm.setDescription(request.descricao());
        farm.setMunicipality(request.municipio());
        farm.setState(request.estado());
        farm.setBoundary(boundary.polygon());
        // Os alertas climáticos usam estas duas colunas como centro da fazenda.
        farm.setLatitude(center.latitude());
        farm.setLongitude(center.longitude());
        farm.setTotalAreaHectares(boundary.areaHectares());
    }

    private FarmResponse toResponse(Farm farm) {
        FarmBoundary boundary = FarmBoundary.of(farm.getBoundary());
        List<Coordenada> contorno = boundary == null ? null
                : boundary.pins().stream().map(p -> new Coordenada(p.latitude(), p.longitude())).toList();
        return new FarmResponse(farm.getId(), farm.getName(), farm.getDescription(), farm.getMunicipality(),
                farm.getState(), farm.getTotalAreaHectares(), farm.getLatitude(), farm.getLongitude(), contorno);
    }

    public record Coordenada(@NotNull BigDecimal latitude, @NotNull BigDecimal longitude) {}

    public record FarmRequest(
            @NotBlank @Size(max = 160) String nome,
            @Size(max = 500) String descricao,
            @Size(max = 120) String municipio,
            @Size(min = 2, max = 2) String estado,
            @NotNull @Size(min = FarmBoundary.MIN_PINS, max = FarmBoundary.MAX_PINS)
            List<@NotNull @Valid Coordenada> contorno) {}

    /** contorno é null em fazendas antigas, cadastradas só com latitude/longitude. */
    public record FarmResponse(UUID id, String nome, String descricao, String municipio, String estado,
                               BigDecimal areaTotalHectares, BigDecimal latitude, BigDecimal longitude,
                               List<Coordenada> contorno) {}
}
