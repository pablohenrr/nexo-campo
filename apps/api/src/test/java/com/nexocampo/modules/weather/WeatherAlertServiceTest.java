package com.nexocampo.modules.weather;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexocampo.modules.farm.Farm;
import com.nexocampo.modules.farm.FarmRepository;
import com.nexocampo.modules.identity.Role;
import com.nexocampo.modules.identity.User;
import com.nexocampo.modules.weather.WeatherAlertDtos.AlertasClimaticosResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WeatherAlertServiceTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Mock
    private WeatherEventRepository events;
    @Mock
    private WeatherEventFarmRepository links;
    @Mock
    private WeatherAlertPreferenceRepository preferences;
    @Mock
    private WeatherAlertReadRepository reads;
    @Mock
    private FarmRepository farms;

    private WeatherAlertService service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-25T15:00:00Z"), SAO_PAULO);
    private final OffsetDateTime now = OffsetDateTime.now(clock);

    @BeforeEach
    void setUp() {
        service = new WeatherAlertService(events, links, preferences, reads, farms, clock);
    }

    @Test
    void warningInParanaDoesNotAttachToFarmInGoias() {
        Farm goias = farm("Fazenda B", -16.68, -49.25);
        when(preferences.findByFarm_Id(goias.getId())).thenReturn(Optional.empty());

        service.apply(goias, List.of(paranaRain()), List.of());

        verify(links, never()).saveAndFlush(any());
    }

    @Test
    void sameExternalIdDoesNotCreateAnotherNotice() {
        Farm farm = farm("Fazenda São João", -23.31, -51.16);
        WeatherEvent existing = new WeatherEvent();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        when(preferences.findByFarm_Id(farm.getId())).thenReturn(Optional.empty());
        when(links.findLatest(farm.getId(), "CHUVA")).thenReturn(Optional.empty());
        when(events.findByFonteAndIdExternoAndCategoria("INMET", "urn:inmet:chuva-pr", "CHUVA"))
                .thenReturn(Optional.of(existing));
        when(links.existsByEvent_IdAndFarm_Id(existing.getId(), farm.getId())).thenReturn(true);

        service.apply(farm, List.of(paranaRain()), List.of());

        verify(events, never()).saveAndFlush(any());
        verify(links, never()).saveAndFlush(any());
    }

    @Test
    void oneForecastEventLinksEachFarmThatMatches() {
        Farm first = farm("Fazenda São João", -23.31, -51.16);
        Farm second = farm("Fazenda Boa Vista", -23.36, -51.20);
        AtomicReference<WeatherEvent> stored = new AtomicReference<>();
        when(preferences.findByFarm_Id(any())).thenReturn(Optional.empty());
        when(links.findLatest(any(), any())).thenReturn(Optional.empty());
        when(events.findByFonteAndIdExternoAndCategoria(any(), any(), any()))
                .thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(events.saveAndFlush(any())).thenAnswer(invocation -> {
            WeatherEvent event = invocation.getArgument(0);
            ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
            stored.set(event);
            return event;
        });
        when(links.existsByEvent_IdAndFarm_Id(any(), any())).thenReturn(false);
        when(links.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(links.findByEventId(any())).thenReturn(List.of());
        IncomingAlert forecast = new IncomingAlert(
                "OPEN_METEO",
                "open-meteo:CHUVA:2026-11-25:-23.5:-51.0",
                CategoriaAlertaClimatico.CHUVA,
                now,
                now.plusHours(24),
                AlertGeometry.point(-23.31, -51.16),
                null);

        service.apply(first, List.of(), List.of(forecast));
        service.apply(second, List.of(), List.of(forecast));

        verify(events).saveAndFlush(any());
        verify(links, org.mockito.Mockito.times(2)).saveAndFlush(any());
    }

    @Test
    void dailyPreferenceSkipsASecondCategoryOnTheSameDay() {
        Farm farm = farm("Fazenda São João", -23.31, -51.16);
        WeatherAlertPreference preference = new WeatherAlertPreference();
        preference.setRaioKm(30);
        preference.setCategorias("CHUVA,VENTO,TEMPESTADE");
        preference.setFrequencia(FrequenciaAlertaClimatico.DIARIO.name());
        when(preferences.findByFarm_Id(farm.getId())).thenReturn(Optional.of(preference));
        when(links.findLatest(farm.getId(), "CHUVA"))
                .thenReturn(Optional.of(OffsetDateTime.of(2026, 11, 25, 8, 0, 0, 0, ZoneOffset.ofHours(-3))));

        service.apply(farm, List.of(paranaRain()), List.of());

        verify(links, never()).saveAndFlush(any());
    }

    @Test
    void bellCountsEventsRatherThanFarms() {
        User owner = user();
        WeatherEvent event = new WeatherEvent();
        ReflectionTestUtils.setField(event, "id", UUID.randomUUID());
        event.setCategoria("CHUVA");
        event.setFonte("INMET");
        event.setFrase("NEXO Campo: chuva intensa prevista em Fazenda de outro dono nas próximas 24h. Evite pulverizar.");
        event.setInicio(now);
        event.setFim(now.plusHours(24));
        WeatherEventFarm first = new WeatherEventFarm();
        first.setEvent(event);
        first.setFarm(farm("Fazenda São João", -23.31, -51.16));
        WeatherEventFarm second = new WeatherEventFarm();
        second.setEvent(event);
        second.setFarm(farm("Fazenda Boa Vista", -23.36, -51.20));
        when(links.findByOwner(owner.getId())).thenReturn(List.of(first, second));
        when(reads.findEventIdsByUserId(owner.getId())).thenReturn(Set.of());

        AlertasClimaticosResponse response = service.list(owner);

        assertThat(response.naoLidos()).isEqualTo(1);
        assertThat(response.eventos()).hasSize(1);
        assertThat(response.eventos().get(0).fazendas()).hasSize(2);
        assertThat(response.eventos().get(0).frase()).contains("Fazenda São João", "Fazenda Boa Vista");
        assertThat(response.eventos().get(0).frase()).doesNotContain("Fazenda de outro dono");
    }

    @Test
    void preferenceOfAnotherOwnerIsRejected() {
        User owner = user();
        UUID farmId = UUID.randomUUID();
        when(farms.findByIdAndOwnerId(farmId, owner.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.preference(owner, farmId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Fazenda não encontrada");
    }

    private IncomingAlert paranaRain() {
        return new IncomingAlert(
                "INMET",
                "urn:inmet:chuva-pr",
                CategoriaAlertaClimatico.CHUVA,
                now.minusHours(1),
                now.plusHours(12),
                AlertGeometry.polygon(List.of(
                        new double[] {-23.0, -51.5},
                        new double[] {-23.0, -50.8},
                        new double[] {-23.6, -50.8},
                        new double[] {-23.6, -51.5})),
                "Chuva intensa");
    }

    private Farm farm(String name, double latitude, double longitude) {
        Farm farm = new Farm();
        ReflectionTestUtils.setField(farm, "id", UUID.randomUUID());
        farm.setName(name);
        farm.setLatitude(BigDecimal.valueOf(latitude));
        farm.setLongitude(BigDecimal.valueOf(longitude));
        return farm;
    }

    private User user() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setName("Ana");
        user.setEmail("ana@nexocampo.local");
        user.setPasswordHash("hash");
        user.setRole(Role.PRODUTOR);
        return user;
    }
}
