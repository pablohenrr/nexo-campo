package com.nexocampo.modules.farm;

import com.nexocampo.modules.identity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
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
        plot.setName(request.nome());
        plot.setCrop(request.cultura());
        plot.setAreaHectares(request.areaHectares());
        plot.setStatus(request.situacao() == null ? PlotStatus.ATIVO : request.situacao());
        plot.setBoundary(parsePolygon(request.limiteWkt()));
        return toResponse(plotRepository.save(plot));
    }

    @Transactional(readOnly = true)
    public List<PlotResponse> findByFarm(UUID farmId, User owner) {
        return plotRepository.findAllByFarmIdAndFarmOwnerIdOrderByName(farmId, owner.getId())
                .stream().map(this::toResponse).toList();
    }

    private Polygon parsePolygon(String limiteWkt) {
        try {
            Geometry geometry = new WKTReader().read(limiteWkt);
            if (!(geometry instanceof Polygon polygon)) {
                throw new IllegalArgumentException("limiteWkt deve ser um polígono válido");
            }
            if (polygon.isEmpty() || polygon.getNumPoints() == 0) {
                throw new IllegalArgumentException("limiteWkt não pode ser vazio");
            }
            if (!polygon.isValid()) {
                throw new IllegalArgumentException("limiteWkt deve ser um polígono válido");
            }
            validarCoordenadas(polygon);
            polygon.setSRID(4326);
            return polygon;
        } catch (Exception exception) {
            if (exception instanceof IllegalArgumentException argumento) {
                throw argumento;
            }
            throw new IllegalArgumentException("limiteWkt deve estar em WKT e conter um polígono válido", exception);
        }
    }

    private void validarCoordenadas(Polygon polygon) {
        for (Coordinate coordinate : polygon.getCoordinates()) {
            if (!Double.isFinite(coordinate.getX())
                    || !Double.isFinite(coordinate.getY())
                    || coordinate.getX() < -180
                    || coordinate.getX() > 180
                    || coordinate.getY() < -90
                    || coordinate.getY() > 90) {
                throw new IllegalArgumentException(
                        "limiteWkt contém coordenadas fora dos limites de longitude/latitude");
            }
        }
    }

    private PlotResponse toResponse(Plot plot) {
        return new PlotResponse(plot.getId(), plot.getFarm().getId(), plot.getName(), plot.getCrop(),
                plot.getAreaHectares(), plot.getStatus(), plot.getBoundary().toText());
    }

    public record PlotRequest(
            @NotBlank @Size(max = 160) String nome,
            @Size(max = 120) String cultura,
            BigDecimal areaHectares,
            PlotStatus situacao,
            @NotBlank String limiteWkt) {}

    public record PlotResponse(UUID id, UUID fazendaId, String nome, String cultura, BigDecimal areaHectares,
                               PlotStatus situacao, String limiteWkt) {}
}
