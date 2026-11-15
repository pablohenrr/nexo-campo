package com.nexocampo.modules.evidence;

import com.nexocampo.modules.identity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/talhoes/{plotId}/evidencias")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenceService.EvidenceResponse upload(
            @PathVariable UUID plotId,
            @RequestParam EvidenceType type,
            @RequestPart MultipartFile file,
            @AuthenticationPrincipal User user) {
        return evidenceService.upload(plotId, type, file, user);
    }

    @GetMapping
    public List<EvidenceService.EvidenceResponse> findByPlot(
            @PathVariable UUID plotId,
            @AuthenticationPrincipal User user) {
        return evidenceService.findByPlot(plotId, user);
    }
}
