package com.nexocampo.modules.weather;

import static com.nexocampo.modules.weather.WeatherAlertDtos.AlertaClimaticoItem;
import static com.nexocampo.modules.weather.WeatherAlertDtos.AlertasClimaticosResponse;
import static com.nexocampo.modules.weather.WeatherAlertDtos.FazendaDoAlerta;
import static com.nexocampo.modules.weather.WeatherAlertDtos.PreferenciaAlertaRequest;
import static com.nexocampo.modules.weather.WeatherAlertDtos.PreferenciaAlertaResponse;

import com.nexocampo.modules.farm.Farm;
import com.nexocampo.modules.farm.FarmRepository;
import com.nexocampo.modules.identity.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeatherAlertService {

    private static final Logger log = LoggerFactory.getLogger(WeatherAlertService.class);

    private final WeatherEventRepository events;
    private final WeatherEventFarmRepository links;
    private final WeatherAlertPreferenceRepository preferences;
    private final WeatherAlertReadRepository reads;
    private final FarmRepository farms;
    private final Clock clock;

    public WeatherAlertService(
            WeatherEventRepository events,
            WeatherEventFarmRepository links,
            WeatherAlertPreferenceRepository preferences,
            WeatherAlertReadRepository reads,
            FarmRepository farms,
            Clock clock) {
        this.events = events;
        this.links = links;
        this.preferences = preferences;
        this.reads = reads;
        this.farms = farms;
        this.clock = clock;
    }

    @Transactional
    public void apply(Farm farm, List<IncomingAlert> official, List<IncomingAlert> forecast) {
        if (farm.getLatitude() == null || farm.getLongitude() == null) {
            return;
        }
        EffectivePreference preference = effective(farm.getId());
        OffsetDateTime now = OffsetDateTime.now(clock);
        for (IncomingAlert alert : official) {
            if (!accepts(preference, alert) || alert.fim() == null || !alert.fim().isAfter(now)) {
                continue;
            }
            if (!WeatherAlertRule.reaches(
                    farm.getLatitude().doubleValue(), farm.getLongitude().doubleValue(), alert.geometry(), preference.raioKm())) {
                continue;
            }
            attach(farm, preference, alert, now);
        }
        for (IncomingAlert alert : forecast) {
            if (!accepts(preference, alert)) {
                continue;
            }
            attach(farm, preference, alert, now);
        }
    }

    @Transactional(readOnly = true)
    public AlertasClimaticosResponse list(User user) {
        Map<UUID, List<WeatherEventFarm>> grouped = new LinkedHashMap<>();
        for (WeatherEventFarm link : links.findByOwner(user.getId())) {
            grouped.computeIfAbsent(link.getEvent().getId(), id -> new ArrayList<>()).add(link);
        }
        Set<UUID> readIds = reads.findEventIdsByUserId(user.getId());
        List<AlertaClimaticoItem> items = new ArrayList<>();
        int unread = 0;
        for (List<WeatherEventFarm> group : grouped.values()) {
            WeatherEvent event = group.get(0).getEvent();
            boolean read = readIds.contains(event.getId());
            if (!read) {
                unread++;
            }
            List<FazendaDoAlerta> farmsInEvent = group.stream()
                    .map(link -> new FazendaDoAlerta(link.getFarm().getId(), link.getFarm().getName()))
                    .distinct()
                    .toList();
            String frase = WeatherAlertPhrase.of(
                    CategoriaAlertaClimatico.valueOf(event.getCategoria()), placeOf(farmsInEvent));
            items.add(new AlertaClimaticoItem(
                    event.getId(),
                    event.getCategoria(),
                    event.getFonte(),
                    frase,
                    event.getTexto(),
                    event.getInicio(),
                    event.getFim(),
                    read,
                    farmsInEvent));
        }
        return new AlertasClimaticosResponse(unread, items);
    }

    @Transactional
    public void markRead(User user, UUID eventId) {
        if (!links.visibleToOwner(eventId, user.getId())) {
            throw new IllegalArgumentException("Alerta não encontrado");
        }
        if (reads.existsByUser_IdAndEvent_Id(user.getId(), eventId)) {
            return;
        }
        WeatherEvent event = events.findById(eventId).orElseThrow(() -> new IllegalArgumentException("Alerta não encontrado"));
        WeatherAlertRead read = new WeatherAlertRead();
        read.setUser(user);
        read.setEvent(event);
        read.setLidaEm(OffsetDateTime.now(clock));
        reads.save(read);
    }

    @Transactional(readOnly = true)
    public PreferenciaAlertaResponse preference(User user, UUID farmId) {
        requireFarm(user, farmId);
        return toResponse(farmId, effective(farmId));
    }

    @Transactional
    public PreferenciaAlertaResponse update(User user, UUID farmId, PreferenciaAlertaRequest request) {
        Farm farm = requireFarm(user, farmId);
        if (request.raioKm() < 1 || request.raioKm() > WeatherAlertRule.MAX_RADIUS_KM) {
            throw new IllegalArgumentException("O raio fica entre 1 e 80 km");
        }
        WeatherAlertPreference preference = preferences.findByFarm_Id(farmId).orElseGet(WeatherAlertPreference::new);
        preference.setFarm(farm);
        preference.setRaioKm(request.raioKm());
        preference.setCategorias(join(request.categorias()));
        preference.setFrequencia(request.frequencia().name());
        preferences.save(preference);
        return toResponse(farmId, new EffectivePreference(request.raioKm(), copy(request.categorias()), request.frequencia()));
    }

    private void attach(Farm farm, EffectivePreference preference, IncomingAlert alert, OffsetDateTime now) {
        OffsetDateTime last = links.findLatest(farm.getId(), alert.categoria().name()).orElse(null);
        if (!WeatherAlertRule.allows(preference.frequencia(), last, now, clock.getZone())) {
            return;
        }
        WeatherEvent event = events
                .findByFonteAndIdExternoAndCategoria(alert.fonte(), alert.idExterno(), alert.categoria().name())
                .orElseGet(() -> events.saveAndFlush(newEvent(alert, farm, now)));
        if (links.existsByEvent_IdAndFarm_Id(event.getId(), farm.getId())) {
            return;
        }
        WeatherEventFarm link = new WeatherEventFarm();
        link.setEvent(event);
        link.setFarm(farm);
        links.saveAndFlush(link);
        event.setFrase(WeatherAlertPhrase.of(alert.categoria(), place(event, farm)));
        events.save(event);
        log.info("alerta_climatico_vinculado fazendaId={} categoria={} fonte={}", farm.getId(), alert.categoria(), alert.fonte());
    }

    private WeatherEvent newEvent(IncomingAlert alert, Farm farm, OffsetDateTime now) {
        WeatherEvent event = new WeatherEvent();
        event.setFonte(alert.fonte());
        event.setIdExterno(alert.idExterno());
        event.setCategoria(alert.categoria().name());
        event.setInicio(alert.inicio());
        event.setFim(alert.fim());
        event.setTexto(alert.texto() == null ? "" : trim(alert.texto()));
        event.setFrase(WeatherAlertPhrase.of(alert.categoria(), farm.getName()));
        String geometry = alert.geometry().serialized();
        event.setGeometria(geometry == null ? "" : geometry);
        if (alert.geometry().latitude() != null && alert.geometry().longitude() != null) {
            event.setLatitude(scaled(alert.geometry().latitude()));
            event.setLongitude(scaled(alert.geometry().longitude()));
        }
        event.setCriadoEm(now);
        return event;
    }

    private static String placeOf(List<FazendaDoAlerta> farms) {
        List<String> names = farms.stream()
                .map(FazendaDoAlerta::nome)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (names.isEmpty()) {
            return "sua fazenda";
        }
        if (names.size() <= 3) {
            return String.join(", ", names);
        }
        return names.get(0) + ", " + names.get(1) + " e outras";
    }

    private String place(WeatherEvent event, Farm current) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (current.getName() != null) {
            names.add(current.getName());
        }
        for (WeatherEventFarm link : links.findByEventId(event.getId())) {
            if (link.getFarm().getName() != null) {
                names.add(link.getFarm().getName());
            }
        }
        List<String> ordered = names.stream().sorted().toList();
        if (ordered.isEmpty()) {
            return "sua fazenda";
        }
        if (ordered.size() <= 3) {
            return String.join(", ", ordered);
        }
        return ordered.get(0) + ", " + ordered.get(1) + " e outras";
    }

    private Farm requireFarm(User user, UUID farmId) {
        return farms.findByIdAndOwnerId(farmId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Fazenda não encontrada"));
    }

    private EffectivePreference effective(UUID farmId) {
        return preferences.findByFarm_Id(farmId).map(this::fromEntity).orElseGet(EffectivePreference::defaults);
    }

    private EffectivePreference fromEntity(WeatherAlertPreference preference) {
        return new EffectivePreference(preference.getRaioKm(), parse(preference.getCategorias()), FrequenciaAlertaClimatico.valueOf(preference.getFrequencia()));
    }

    private static boolean accepts(EffectivePreference preference, IncomingAlert alert) {
        return alert != null && alert.categoria() != null && preference.categorias().contains(alert.categoria());
    }

    private static Set<CategoriaAlertaClimatico> parse(String raw) {
        EnumSet<CategoriaAlertaClimatico> categories = EnumSet.noneOf(CategoriaAlertaClimatico.class);
        if (raw == null) {
            return EnumSet.copyOf(WeatherAlertRule.DEFAULT_CATEGORIES);
        }
        if (raw.isBlank()) {
            return categories;
        }
        for (String part : raw.split(",")) {
            if (!part.isBlank()) {
                categories.add(CategoriaAlertaClimatico.valueOf(part.trim()));
            }
        }
        return categories;
    }

    private static Set<CategoriaAlertaClimatico> copy(Set<CategoriaAlertaClimatico> categories) {
        if (categories == null || categories.isEmpty()) {
            return EnumSet.noneOf(CategoriaAlertaClimatico.class);
        }
        return EnumSet.copyOf(categories);
    }

    private static String join(Set<CategoriaAlertaClimatico> categories) {
        return categories.stream().map(Enum::name).sorted().reduce((left, right) -> left + "," + right).orElse("");
    }

    private static PreferenciaAlertaResponse toResponse(UUID farmId, EffectivePreference preference) {
        List<String> categories = preference.categorias().stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .map(Enum::name)
                .toList();
        return new PreferenciaAlertaResponse(farmId, preference.raioKm(), categories, preference.frequencia().name());
    }

    private static BigDecimal scaled(double value) {
        return BigDecimal.valueOf(value).setScale(7, RoundingMode.HALF_UP);
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    private record EffectivePreference(int raioKm, Set<CategoriaAlertaClimatico> categorias, FrequenciaAlertaClimatico frequencia) {
        private static EffectivePreference defaults() {
            return new EffectivePreference(
                    WeatherAlertRule.DEFAULT_RADIUS_KM,
                    EnumSet.copyOf(WeatherAlertRule.DEFAULT_CATEGORIES),
                    FrequenciaAlertaClimatico.POR_EVENTO);
        }
    }
}
