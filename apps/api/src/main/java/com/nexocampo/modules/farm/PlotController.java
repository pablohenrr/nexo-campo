package com.nexocampo.modules.farm;

import com.nexocampo.modules.identity.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fazendas/{fazendaId}/talhoes")
public class PlotController {

    private final PlotService plotService;

    public PlotController(PlotService plotService) {
        this.plotService = plotService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlotService.PlotResponse create(
            @PathVariable UUID fazendaId,
            @Valid @RequestBody PlotService.PlotRequest request,
            @AuthenticationPrincipal User user) {
        return plotService.create(fazendaId, request, user);
    }

    @GetMapping
    public List<PlotService.PlotResponse> findByFarm(
            @PathVariable UUID fazendaId,
            @AuthenticationPrincipal User user) {
        return plotService.findByFarm(fazendaId, user);
    }
}
