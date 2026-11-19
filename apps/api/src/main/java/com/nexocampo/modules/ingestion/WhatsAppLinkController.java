package com.nexocampo.modules.ingestion;

import com.nexocampo.modules.identity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** O produtor cadastra o próprio WhatsApp para que as mensagens enviadas ao bot caiam na conta dele. */
@RestController
@RequestMapping("/api/ingestao/whatsapp/vinculo")
public class WhatsAppLinkController {

    private final WhatsAppLinkService linkService;

    public WhatsAppLinkController(WhatsAppLinkService linkService) {
        this.linkService = linkService;
    }

    @PutMapping
    public WhatsAppLinkService.WhatsAppLinkResponse link(
            @Valid @RequestBody WhatsAppLinkService.WhatsAppLinkRequest request,
            @AuthenticationPrincipal User user) {
        return linkService.link(user, request);
    }

    @GetMapping
    public ResponseEntity<WhatsAppLinkService.WhatsAppLinkResponse> find(@AuthenticationPrincipal User user) {
        return linkService.find(user).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal User user) {
        linkService.unlink(user);
        return ResponseEntity.noContent().build();
    }
}
