package com.sanjeevanigrid.identity.domain;

import com.sanjeevanigrid.identity.infra.*;
import com.sanjeevanigrid.notification.domain.NotificationGateway;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final OtpTokenRepository otpRepo;
    private final RefreshTokenRepository refreshRepo;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final NotificationGateway notificationGateway;

    private final boolean sandboxOtp;
    private final String sandboxCode;
    private final int otpExpiryMinutes;
    private final int otpMaxAttempts;

    public AuthService(
            UserRepository userRepo,
            RoleRepository roleRepo,
            OtpTokenRepository otpRepo,
            RefreshTokenRepository refreshRepo,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            NotificationGateway notificationGateway,
            org.springframework.core.env.Environment env) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.otpRepo = otpRepo;
        this.refreshRepo = refreshRepo;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.notificationGateway = notificationGateway;
        this.sandboxOtp = Boolean.parseBoolean(env.getProperty("app.otp.sandbox", "true"));
        this.sandboxCode = env.getProperty("app.otp.sandbox-code", "123456");
        this.otpExpiryMinutes = Integer.parseInt(env.getProperty("app.otp.expiry-minutes", "5"));
        this.otpMaxAttempts = Integer.parseInt(env.getProperty("app.otp.max-attempts", "5"));
    }

    // ==================== PHONE + OTP AUTH ====================

    /**
     * Request OTP for phone-based login/registration.
     * In sandbox mode, always uses the fixed sandbox code.
     */
    @Transactional
    public Map<String, Object> requestOtp(String phone, String fullName, String lang) {
        String code = sandboxOtp ? sandboxCode : generateOtp();
        OtpToken otp = OtpToken.create(phone, code, otpExpiryMinutes);
        otpRepo.save(otp);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "OTP sent to " + phone);
        result.put("expiresInMinutes", otpExpiryMinutes);

        // In sandbox mode, show OTP on screen for demo purposes
        if (sandboxOtp) {
            result.put("sandboxOtp", code);
        }

        return result;
    }

    /**
     * Verify OTP and complete phone-based login/registration.
     */
    @Transactional
    public Map<String, Object> verifyOtp(String phone, String code, String fullName, String lang) {
        OtpToken otp = otpRepo.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(phone)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found. Please request a new one."));

        if (otp.isExpired()) {
            throw new IllegalArgumentException("OTP expired. Please request a new one.");
        }
        if (otp.getAttempts() >= otpMaxAttempts) {
            throw new IllegalArgumentException("Too many attempts. Please request a new OTP.");
        }

        otp.incrementAttempts();

        if (!otp.matches(code)) {
            otpRepo.save(otp);
            int remaining = otpMaxAttempts - otp.getAttempts();
            throw new IllegalArgumentException("Invalid OTP. " + remaining + " attempts remaining.");
        }

        otp.markVerified();
        otpRepo.save(otp);

        // Find or create user
        User user = userRepo.findByPhone(phone).orElseGet(() -> {
            User newUser = User.createWithPhone(phone, fullName != null ? fullName : "User", lang != null ? lang : "en");
            Role citizenRole = roleRepo.findByName(Role.CITIZEN)
                    .orElseThrow(() -> new RuntimeException("CITIZEN role not found"));
            newUser.addRole(citizenRole);
            return userRepo.save(newUser);
        });

        // Auto-verify on OTP verification
        if (user.getOnboardingState() == OnboardingState.REGISTERED) {
            user.transitionTo(OnboardingState.VERIFIED);
            // CITIZEN role auto-activates
            if (user.hasRole(Role.CITIZEN) && user.getRoles().size() == 1) {
                user.transitionTo(OnboardingState.ACTIVE);
            }
            userRepo.save(user);
        }

        return generateTokenResponse(user);
    }

    // ==================== EMAIL + PASSWORD AUTH ====================

    @Transactional
    public Map<String, Object> registerWithEmail(String email, String password, String fullName, String role, String lang) {
        if (userRepo.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered.");
        }

        User user = User.createWithEmail(email, passwordEncoder.encode(password), fullName, lang != null ? lang : "en");

        // Default role assignment
        String roleName = role != null ? role : Role.CITIZEN;
        Role userRole = roleRepo.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role: " + roleName));
        user.addRole(userRole);

        // Email users start as VERIFIED (email itself is verification)
        user.transitionTo(OnboardingState.VERIFIED);

        // Auto-activate citizens, require approval for other roles
        if (Role.CITIZEN.equals(roleName)) {
            user.transitionTo(OnboardingState.ACTIVE);
        }

        userRepo.save(user);

        // Send welcome notification
        notificationGateway.sendInApp(user.getId(), "Welcome to SanjeevaniGrid!",
                "Your account has been created. Complete your profile to get started.");

        return generateTokenResponse(user);
    }

    @Transactional
    public Map<String, Object> loginWithEmail(String email, String password) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials.");
        }

        if (user.getOnboardingState() == OnboardingState.SUSPENDED) {
            throw new IllegalStateException("Account suspended. Contact support.");
        }

        return generateTokenResponse(user);
    }

    // ==================== TOKEN REFRESH ====================

    @Transactional
    public Map<String, Object> refreshAccessToken(String refreshTokenStr) {
        RefreshToken rt = refreshRepo.findByToken(refreshTokenStr)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token."));

        if (!rt.isUsable()) {
            throw new IllegalArgumentException("Refresh token expired or revoked.");
        }

        User user = userRepo.findById(rt.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // Rotate: revoke old, issue new
        rt.revoke();
        refreshRepo.save(rt);

        return generateTokenResponse(user);
    }

    // ==================== HELPERS ====================

    private Map<String, Object> generateTokenResponse(User user) {
        List<String> roleNames = user.getRoles().stream().map(Role::getName).toList();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleNames);
        String refreshTokenStr = jwtService.generateRefreshToken(user.getId());

        RefreshToken refreshToken = RefreshToken.create(user.getId(), refreshTokenStr, jwtService.getRefreshTokenExpirationMs());
        refreshRepo.save(refreshToken);

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshTokenStr);
        response.put("userId", user.getId());
        response.put("fullName", user.getFullName());
        response.put("roles", roleNames);
        response.put("onboardingState", user.getOnboardingState().name());
        response.put("preferredLang", user.getPreferredLang());
        return response;
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}
