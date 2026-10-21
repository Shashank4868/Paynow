package com.personal.auth.controller;

import com.personal.auth.entity.User;
import com.personal.auth.service.UserProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class CurrentUserController {

    private final UserProfileService userProfileService;

    public CurrentUserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public SignedInResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        User profile = userProfileService.createOrUpdateFromKeycloak(
                jwt.getSubject(),
                jwt.getClaimAsString("preferred_username"),
                jwt.getClaimAsString("email")
        );
        return new SignedInResponse(
                "User signed in",
                profile.getId().toString(),
                profile.getUsername(),
                profile.getEmail()
        );
    }

    public record SignedInResponse(String message, String profileId, String username, String email) {
    }
}
