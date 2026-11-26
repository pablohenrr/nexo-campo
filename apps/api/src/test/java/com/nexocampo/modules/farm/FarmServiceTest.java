package com.nexocampo.modules.farm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexocampo.modules.identity.User;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FarmServiceTest {

    private FarmRepository farmRepository;
    private FarmService service;
    private User owner;

    @BeforeEach
    void setUp() {
        farmRepository = mock(FarmRepository.class);
        when(farmRepository.save(any(Farm.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new FarmService(farmRepository);
        owner = mock(User.class);
        when(owner.getId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void createSavesTheCenterOfTheOutlineInLatitudeAndLongitude() {
        FarmService.FarmResponse response = service.create(request("Fazenda Boa Vista", square()), owner);

        assertThat(response.latitude()).isEqualByComparingTo("-23.3050000");
        assertThat(response.longitude()).isEqualByComparingTo("-51.1650000");
        assertThat(response.areaTotalHectares()).isNotNull();
        assertThat(response.contorno()).hasSize(4);
        assertThat(response.contorno().getFirst().latitude()).isEqualByComparingTo("-23.300");
    }

    @Test
    void updateReplacesOutlineAndRecalculatesCenter() {
        UUID farmId = UUID.randomUUID();
        Farm existing = new Farm();
        existing.setLatitude(new BigDecimal("-10"));
        existing.setLongitude(new BigDecimal("-40"));
        when(farmRepository.findByIdAndOwnerId(farmId, owner.getId())).thenReturn(Optional.of(existing));

        FarmService.FarmResponse response = service.update(farmId, request("Fazenda Nova", square()), owner);

        assertThat(response.nome()).isEqualTo("Fazenda Nova");
        assertThat(response.latitude()).isEqualByComparingTo("-23.3050000");
        assertThat(existing.getBoundary()).isNotNull();
    }

    @Test
    void updateOfAnotherOwnersFarmIsRejected() {
        UUID farmId = UUID.randomUUID();
        when(farmRepository.findByIdAndOwnerId(farmId, owner.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(farmId, request("X", square()), owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Fazenda não encontrada");
        verify(farmRepository, never()).save(any());
    }

    @Test
    void invalidOutlineDoesNotChangeTheFarm() {
        UUID farmId = UUID.randomUUID();
        Farm existing = new Farm();
        existing.setName("Original");
        when(farmRepository.findByIdAndOwnerId(farmId, owner.getId())).thenReturn(Optional.of(existing));
        List<FarmService.Coordenada> bowTie = List.of(square().get(0), square().get(2), square().get(1), square().get(3));

        assertThatThrownBy(() -> service.update(farmId, request("Renomeada", bowTie), owner))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(existing.getName()).isEqualTo("Original");
        verify(farmRepository, never()).save(any());
    }

    @Test
    void legacyFarmWithoutOutlineIsListedWithNullContorno() {
        Farm legacy = new Farm();
        legacy.setName("Antiga");
        legacy.setLatitude(new BigDecimal("-23.31"));
        legacy.setLongitude(new BigDecimal("-51.16"));
        when(farmRepository.findAllByOwnerIdOrderByName(owner.getId())).thenReturn(List.of(legacy));

        FarmService.FarmResponse response = service.findMine(owner).getFirst();

        assertThat(response.contorno()).isNull();
        assertThat(response.latitude()).isEqualByComparingTo("-23.31");
    }

    private static FarmService.FarmRequest request(String nome, List<FarmService.Coordenada> contorno) {
        return new FarmService.FarmRequest(nome, null, "Londrina", "PR", contorno);
    }

    private static List<FarmService.Coordenada> square() {
        return List.of(
                coord("-23.300", "-51.170"),
                coord("-23.300", "-51.160"),
                coord("-23.310", "-51.160"),
                coord("-23.310", "-51.170"));
    }

    private static FarmService.Coordenada coord(String latitude, String longitude) {
        return new FarmService.Coordenada(new BigDecimal(latitude), new BigDecimal(longitude));
    }
}
