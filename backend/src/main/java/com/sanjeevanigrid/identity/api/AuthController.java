package com.sanjeevanigrid.identity.api;

import com.sanjeevanigrid.identity.domain.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ==================== PHONE + OTP ====================

    public record OtpRequest(
        @NotBlank String phone,
        String fullName,
        String lang
    ) {}

    public record OtpVerifyRequest(
        @NotBlank String phone,
        @NotBlank @Size(min = 6, max = 6) String code,
        String fullName,
        String lang
    ) {}

    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, Object>> requestOtp(@Valid @RequestBody OtpRequest request) {
        return ResponseEntity.ok(authService.requestOtp(request.phone(), request.fullName(), request.lang()));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(
                request.phone(), request.code(), request.fullName(), request.lang()));
    }

    // ==================== EMAIL + PASSWORD ====================

    public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String fullName,
        String role,
        String lang
    ) {}

    public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
    ) {}

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.registerWithEmail(
                request.email(), request.password(), request.fullName(), request.role(), request.lang()));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginWithEmail(request.email(), request.password()));
    }

    // ==================== TOKEN REFRESH ====================

    public record RefreshRequest(@NotBlank String refreshToken) {}

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refreshAccessToken(request.refreshToken()));
    }
}
