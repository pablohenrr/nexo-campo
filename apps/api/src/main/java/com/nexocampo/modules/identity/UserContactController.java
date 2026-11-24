package com.nexocampo.modules.identity;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios/me/contato")
public class UserContactController {

    private final UserContactService userContactService;

    public UserContactController(UserContactService userContactService) {
        this.userContactService = userContactService;
    }

    @GetMapping
    public UserContactService.ContactResponse get(@AuthenticationPrincipal User user) {
        return userContactService.get(user);
    }

    @PutMapping
    public UserContactService.ContactResponse update(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserContactService.ContactRequest request) {
        return userContactService.update(user, request);
    }
}
