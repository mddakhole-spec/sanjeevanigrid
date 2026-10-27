package com.sanjeevanigrid.identity.api;

import com.sanjeevanigrid.identity.domain.Role;
import com.sanjeevanigrid.identity.domain.User;
import com.sanjeevanigrid.identity.infra.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserRepository userRepo;

    public ProfileController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getProfile(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "phone", user.getPhone() != null ? user.getPhone() : "",
                "email", user.getEmail() != null ? user.getEmail() : "",
                "preferredLang", user.getPreferredLang(),
                "roles", user.getRoles().stream().map(Role::getName).toList(),
                "onboardingState", user.getOnboardingState().name(),
                "profileComplete", user.isProfileComplete()
        ));
    }

    public record UpdateProfileRequest(String fullName, String preferredLang) {}

    @PatchMapping
    public ResponseEntity<Map<String, String>> updateProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.preferredLang() != null) user.setPreferredLang(request.preferredLang());
        userRepo.save(user);

        return ResponseEntity.ok(Map.of("message", "Profile updated"));
    }
}
