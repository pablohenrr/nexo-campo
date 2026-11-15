package com.nexocampo.modules.farm;

import com.nexocampo.modules.identity.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fazendas/{farmId}/talhoes")
public class PlotController {

    private final PlotService plotService;

    public PlotController(PlotService plotService) {
        this.plotService = plotService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlotService.PlotResponse create(
            @PathVariable UUID farmId,
            @Valid @RequestBody PlotService.PlotRequest request,
            @AuthenticationPrincipal User user) {
        return plotService.create(farmId, request, user);
    }

    @GetMapping
    public List<PlotService.PlotResponse> findByFarm(
            @PathVariable UUID farmId,
            @AuthenticationPrincipal User user) {
        return plotService.findByFarm(farmId, user);
    }
}
