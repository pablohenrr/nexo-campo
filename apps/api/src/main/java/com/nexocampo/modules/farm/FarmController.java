package com.nexocampo.modules.farm;

import com.nexocampo.modules.identity.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fazendas")
public class FarmController {

    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FarmService.FarmResponse create(
            @Valid @RequestBody FarmService.FarmRequest request,
            @AuthenticationPrincipal User user) {
        return farmService.create(request, user);
    }

    @GetMapping
    public List<FarmService.FarmResponse> findMine(@AuthenticationPrincipal User user) {
        return farmService.findMine(user);
    }
}
