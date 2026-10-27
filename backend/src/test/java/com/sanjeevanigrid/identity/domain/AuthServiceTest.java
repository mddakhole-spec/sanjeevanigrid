package com.sanjeevanigrid.identity.domain;

import com.sanjeevanigrid.identity.infra.*;
import com.sanjeevanigrid.notification.domain.NotificationGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private RoleRepository roleRepo;
    @Mock private OtpTokenRepository otpRepo;
    @Mock private RefreshTokenRepository refreshRepo;
    @Mock private JwtService jwtService;
    @Mock private NotificationGateway notificationGateway;
    @Mock private Environment env;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        // Configure sandbox mode
        when(env.getProperty("app.otp.sandbox", "true")).thenReturn("true");
        when(env.getProperty("app.otp.sandbox-code", "123456")).thenReturn("123456");
        when(env.getProperty("app.otp.expiry-minutes", "5")).thenReturn("5");
        when(env.getProperty("app.otp.max-attempts", "5")).thenReturn("5");

        authService = new AuthService(userRepo, roleRepo, otpRepo, refreshRepo,
                jwtService, passwordEncoder, notificationGateway, env);
    }

    @Nested
    @DisplayName("OTP Authentication")
    class OtpAuth {

        @Test
        @DisplayName("requestOtp returns sandbox OTP in sandbox mode")
        void requestOtpSandbox() {
            Map<String, Object> result = authService.requestOtp("+919876543210", "Test User", "en");

            assertNotNull(result);
            assertEquals("123456", result.get("sandboxOtp"));
            assertEquals(5, result.get("expiresInMinutes"));
            verify(otpRepo).save(any(OtpToken.class));
        }

        @Test
        @DisplayName("verifyOtp creates new user with CITIZEN role on first login")
        void verifyOtpNewUser() {
            String phone = "+919876543210";
            OtpToken otp = OtpToken.create(phone, "123456", 5);

            Role citizenRole = new Role(Role.CITIZEN);

            when(otpRepo.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(phone))
                    .thenReturn(Optional.of(otp));
            when(userRepo.findByPhone(phone)).thenReturn(Optional.empty());
            when(roleRepo.findByName(Role.CITIZEN)).thenReturn(Optional.of(citizenRole));
            when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
            when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);
            when(refreshRepo.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = authService.verifyOtp(phone, "123456", "Test User", "en");

            assertNotNull(result);
            assertEquals("access-token", result.get("accessToken"));
            assertEquals("refresh-token", result.get("refreshToken"));
            verify(userRepo, atLeastOnce()).save(any(User.class));
        }

        @Test
        @DisplayName("verifyOtp throws on expired OTP")
        void verifyOtpExpired() {
            String phone = "+919876543210";
            OtpToken otp = OtpToken.create(phone, "123456", -1); // Expired

            when(otpRepo.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(phone))
                    .thenReturn(Optional.of(otp));

            assertThrows(IllegalArgumentException.class, () ->
                authService.verifyOtp(phone, "123456", "Test", "en"));
        }

        @Test
        @DisplayName("verifyOtp throws on wrong OTP code")
        void verifyOtpWrongCode() {
            String phone = "+919876543210";
            OtpToken otp = OtpToken.create(phone, "123456", 5);

            when(otpRepo.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(phone))
                    .thenReturn(Optional.of(otp));

            assertThrows(IllegalArgumentException.class, () ->
                authService.verifyOtp(phone, "999999", "Test", "en"));
        }

        @Test
        @DisplayName("verifyOtp throws when no OTP exists")
        void verifyOtpNotFound() {
            when(otpRepo.findTopByPhoneAndVerifiedFalseOrderByCreatedAtDesc(anyString()))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () ->
                authService.verifyOtp("+91000", "123456", "Test", "en"));
        }
    }

    @Nested
    @DisplayName("Email Authentication")
    class EmailAuth {

        @Test
        @DisplayName("registerWithEmail creates user with hashed password")
        void registerSuccess() {
            Role citizenRole = new Role(Role.CITIZEN);
            when(userRepo.existsByEmail("test@test.com")).thenReturn(false);
            when(roleRepo.findByName(Role.CITIZEN)).thenReturn(Optional.of(citizenRole));
            when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
            when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);
            when(refreshRepo.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = authService.registerWithEmail(
                    "test@test.com", "Password@123", "Test User", null, "en");

            assertNotNull(result);
            assertEquals("access-token", result.get("accessToken"));
            verify(notificationGateway).sendInApp(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("registerWithEmail throws on duplicate email")
        void registerDuplicateEmail() {
            when(userRepo.existsByEmail("dup@test.com")).thenReturn(true);

            assertThrows(IllegalArgumentException.class, () ->
                authService.registerWithEmail("dup@test.com", "Password@123", "Test", null, "en"));
        }

        @Test
        @DisplayName("loginWithEmail succeeds with correct credentials")
        void loginSuccess() {
            User user = User.createWithEmail("test@test.com",
                    passwordEncoder.encode("Password@123"), "Test User", "en");
            user.transitionTo(OnboardingState.VERIFIED);
            user.transitionTo(OnboardingState.ACTIVE);

            when(userRepo.findByEmail("test@test.com")).thenReturn(Optional.of(user));
            when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");
            when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
            when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);
            when(refreshRepo.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = authService.loginWithEmail("test@test.com", "Password@123");

            assertNotNull(result);
            assertEquals("access-token", result.get("accessToken"));
        }

        @Test
        @DisplayName("loginWithEmail throws on wrong password")
        void loginWrongPassword() {
            User user = User.createWithEmail("test@test.com",
                    passwordEncoder.encode("Password@123"), "Test User", "en");

            when(userRepo.findByEmail("test@test.com")).thenReturn(Optional.of(user));

            assertThrows(IllegalArgumentException.class, () ->
                authService.loginWithEmail("test@test.com", "WrongPassword"));
        }

        @Test
        @DisplayName("loginWithEmail throws for suspended account")
        void loginSuspendedAccount() {
            User user = User.createWithEmail("test@test.com",
                    passwordEncoder.encode("Password@123"), "Test User", "en");
            user.transitionTo(OnboardingState.VERIFIED);
            user.transitionTo(OnboardingState.ACTIVE);
            user.transitionTo(OnboardingState.SUSPENDED);

            when(userRepo.findByEmail("test@test.com")).thenReturn(Optional.of(user));

            assertThrows(IllegalStateException.class, () ->
                authService.loginWithEmail("test@test.com", "Password@123"));
        }
    }

    @Nested
    @DisplayName("Token Refresh")
    class TokenRefresh {

        @Test
        @DisplayName("refreshAccessToken rotates tokens")
        void refreshSuccess() {
            UUID userId = UUID.randomUUID();
            RefreshToken rt = RefreshToken.create(userId, "old-token", 604800000L);
            User user = User.createWithEmail("test@test.com",
                    passwordEncoder.encode("pass"), "Test", "en");

            when(refreshRepo.findByToken("old-token")).thenReturn(Optional.of(rt));
            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            when(refreshRepo.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("new-access");
            when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh");
            when(jwtService.getRefreshTokenExpirationMs()).thenReturn(604800000L);

            Map<String, Object> result = authService.refreshAccessToken("old-token");

            assertNotNull(result);
            assertEquals("new-access", result.get("accessToken"));
            assertEquals("new-refresh", result.get("refreshToken"));
            assertTrue(rt.isRevoked()); // Old token should be revoked
        }
    }
}
