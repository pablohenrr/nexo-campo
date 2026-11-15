package com.nexocampo.modules.activity;

import com.nexocampo.modules.identity.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/talhoes/{plotId}/atividades")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityService.ActivityResponse create(
            @PathVariable UUID plotId,
            @Valid @RequestBody ActivityService.ActivityRequest request,
            @AuthenticationPrincipal User user) {
        return activityService.create(plotId, request, user);
    }

    @GetMapping
    public List<ActivityService.ActivityResponse> findByPlot(@PathVariable UUID plotId) {
        return activityService.findByPlot(plotId);
    }
}
