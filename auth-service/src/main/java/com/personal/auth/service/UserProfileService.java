package com.personal.auth.service;

import com.personal.auth.entity.User;
import com.personal.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User createOrUpdateFromKeycloak(String subject, String username, String email) {
        if (!StringUtils.hasText(subject)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token subject is required");
        }
        if (!StringUtils.hasText(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keycloak token must contain an email");
        }

        User profile = userRepository.findByKeycloakSubject(subject)
                .orElseGet(() -> newProfile(subject, username));

        userRepository.findByEmail(email)
                .filter(existingProfile -> !existingProfile.getId().equals(profile.getId()))
                .ifPresent(existingProfile -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "A profile already uses this email; link the Keycloak identity explicitly");
                });

        profile.setUsername(StringUtils.hasText(username) ? username : subject);
        profile.setEmail(email);
        return userRepository.save(profile);
    }

    private User newProfile(String subject, String username) {
        return User.builder()
                .keycloakSubject(subject)
                .username(StringUtils.hasText(username) ? username : subject)
                .role("USER")
                .build();
    }
}
