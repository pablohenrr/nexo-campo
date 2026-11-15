package com.nexocampo.modules.farm;

import com.nexocampo.modules.identity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlotService {

    private final PlotRepository plotRepository;
    private final FarmRepository farmRepository;

    public PlotService(PlotRepository plotRepository, FarmRepository farmRepository) {
        this.plotRepository = plotRepository;
        this.farmRepository = farmRepository;
    }

    @Transactional
    public PlotResponse create(UUID farmId, PlotRequest request, User owner) {
        Farm farm = farmRepository.findByIdAndOwnerId(farmId, owner.getId())
                .orElseThrow(() -> new IllegalArgumentException("Fazenda não encontrada"));
        Plot plot = new Plot();
        plot.setFarm(farm);
        plot.setName(request.name());
        plot.setCrop(request.crop());
        plot.setAreaHectares(request.areaHectares());
        plot.setStatus(request.status() == null ? PlotStatus.ATIVO : request.status());
        plot.setBoundary(parsePolygon(request.boundaryWkt()));
        return toResponse(plotRepository.save(plot));
    }

    @Transactional(readOnly = true)
    public List<PlotResponse> findByFarm(UUID farmId, User owner) {
        return plotRepository.findAllByFarmIdAndFarmOwnerIdOrderByName(farmId, owner.getId())
                .stream().map(this::toResponse).toList();
    }

    private Polygon parsePolygon(String boundaryWkt) {
        try {
            Geometry geometry = new WKTReader().read(boundaryWkt);
            if (!(geometry instanceof Polygon polygon) || !geometry.isValid()) {
                throw new IllegalArgumentException("boundaryWkt deve ser um polígono válido");
            }
            polygon.setSRID(4326);
            return polygon;
        } catch (Exception exception) {
            throw new IllegalArgumentException("boundaryWkt deve estar em WKT e conter um polígono válido", exception);
        }
    }

    private PlotResponse toResponse(Plot plot) {
        return new PlotResponse(plot.getId(), plot.getFarm().getId(), plot.getName(), plot.getCrop(),
                plot.getAreaHectares(), plot.getStatus(), plot.getBoundary().toText());
    }

    public record PlotRequest(
            @NotBlank @Size(max = 160) String name,
            @Size(max = 120) String crop,
            BigDecimal areaHectares,
            PlotStatus status,
            @NotBlank String boundaryWkt) {}

    public record PlotResponse(UUID id, UUID farmId, String name, String crop, BigDecimal areaHectares,
                               PlotStatus status, String boundaryWkt) {}
}
