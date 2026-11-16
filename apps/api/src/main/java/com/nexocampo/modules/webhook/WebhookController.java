package com.nexocampo.modules.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookService service;

    public WebhookController(WebhookService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void receive(
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestBody String payload) {
        service.receive(payload, signature);
    }
}
