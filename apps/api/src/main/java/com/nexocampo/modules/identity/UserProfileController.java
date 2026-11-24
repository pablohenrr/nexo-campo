package com.nexocampo.modules.identity;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public UserProfileService.ProfileResponse get(@AuthenticationPrincipal User user) {
        return userProfileService.get(user);
    }

    @PutMapping
    public UserProfileService.ProfileResponse update(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserProfileService.ProfileRequest request) {
        return userProfileService.update(user, request);
    }

    @PutMapping("/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserProfileService.PasswordChangeRequest request) {
        userProfileService.changePassword(user, request);
    }
}
