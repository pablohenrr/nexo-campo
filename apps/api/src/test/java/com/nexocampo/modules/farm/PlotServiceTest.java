package com.nexocampo.modules.farm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexocampo.modules.identity.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlotServiceTest {

    private PlotRepository plotRepository;
    private FarmRepository farmRepository;
    private PlotService service;
    private User owner;
    private UUID farmId;

    @BeforeEach
    void setUp() {
        plotRepository = mock(PlotRepository.class);
        farmRepository = mock(FarmRepository.class);
        service = new PlotService(plotRepository, farmRepository);
        owner = mock(User.class);
        farmId = UUID.randomUUID();
        when(owner.getId()).thenReturn(UUID.randomUUID());
        when(farmRepository.findByIdAndOwnerId(farmId, owner.getId()))
                .thenReturn(Optional.of(new Farm()));
    }

    @Test
    void rejectsEmptyPolygon() {
        PlotService.PlotRequest request =
                new PlotService.PlotRequest("Talhão", "Soja", null, PlotStatus.ATIVO, "POLYGON EMPTY");

        assertThatThrownBy(() -> service.create(farmId, request, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limiteWkt não pode ser vazio");
    }

    @Test
    void rejectsCoordinatesOutsideGeographicBounds() {
        PlotService.PlotRequest request = new PlotService.PlotRequest(
                "Talhão",
                "Soja",
                null,
                PlotStatus.ATIVO,
                "POLYGON((181 0, 181 1, 182 1, 181 0))");

        assertThatThrownBy(() -> service.create(farmId, request, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fora dos limites");
    }
}
