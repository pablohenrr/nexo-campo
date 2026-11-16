package com.nexocampo.modules.farm;

import com.nexocampo.modules.identity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    @Transactional(readOnly = true)
    public List<FarmResponse> findMine(User owner) {
        return farmRepository.findAllByOwnerIdOrderByName(owner.getId()).stream().map(this::toResponse).toList();
    }

    private void apply(Farm farm, FarmRequest request) {
        farm.setName(request.nome());
        farm.setDescription(request.descricao());
        farm.setMunicipality(request.municipio());
        farm.setState(request.estado());
        farm.setTotalAreaHectares(request.areaTotalHectares());
        farm.setLatitude(request.latitude());
        farm.setLongitude(request.longitude());
    }

    private FarmResponse toResponse(Farm farm) {
        return new FarmResponse(farm.getId(), farm.getName(), farm.getDescription(), farm.getMunicipality(),
                farm.getState(), farm.getTotalAreaHectares(), farm.getLatitude(), farm.getLongitude());
    }

    public record FarmRequest(
            @NotBlank @Size(max = 160) String nome,
            @Size(max = 500) String descricao,
            @Size(max = 120) String municipio,
            @Size(min = 2, max = 2) String estado,
            BigDecimal areaTotalHectares,
            BigDecimal latitude,
            BigDecimal longitude) {}

    public record FarmResponse(UUID id, String nome, String descricao, String municipio, String estado,
                               BigDecimal areaTotalHectares, BigDecimal latitude, BigDecimal longitude) {}
}
