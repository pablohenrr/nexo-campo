package com.nexocampo.modules.investigation;

import com.nexocampo.modules.identity.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/talhoes/{plotId}/ocorrencias")
public class OccurrenceController {

    private final OccurrenceService occurrenceService;

    public OccurrenceController(OccurrenceService occurrenceService) {
        this.occurrenceService = occurrenceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OccurrenceService.OccurrenceResponse register(
            @PathVariable UUID plotId,
            @Valid @RequestBody OccurrenceService.OccurrenceRequest request,
            @AuthenticationPrincipal User user) {
        return occurrenceService.register(plotId, request, user);
    }

    @GetMapping
    public List<OccurrenceService.OccurrenceResponse> findByPlot(
            @PathVariable UUID plotId,
            @AuthenticationPrincipal User user) {
        return occurrenceService.findByPlot(plotId, user);
    }
}
