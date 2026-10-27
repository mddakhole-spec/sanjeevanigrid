package com.sanjeevanigrid.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, length = 15)
    private String phone;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "preferred_lang", nullable = false, length = 5)
    private String preferredLang = "en";

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider = AuthProvider.PHONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_state", nullable = false, length = 20)
    private OnboardingState onboardingState = OnboardingState.REGISTERED;

    @Column(name = "profile_complete", nullable = false)
    private boolean profileComplete = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected User() {}

    // Factory method for phone-based registration
    public static User createWithPhone(String phone, String fullName, String lang) {
        User u = new User();
        u.phone = phone;
        u.fullName = fullName;
        u.preferredLang = lang;
        u.authProvider = AuthProvider.PHONE;
        u.onboardingState = OnboardingState.REGISTERED;
        return u;
    }

    // Factory method for email-based registration
    public static User createWithEmail(String email, String passwordHash, String fullName, String lang) {
        User u = new User();
        u.email = email;
        u.passwordHash = passwordHash;
        u.fullName = fullName;
        u.preferredLang = lang;
        u.authProvider = AuthProvider.EMAIL;
        u.onboardingState = OnboardingState.REGISTERED;
        return u;
    }

    /**
     * Transition the onboarding state. Delegates validation to the state enum.
     */
    public void transitionTo(OnboardingState target) {
        this.onboardingState = this.onboardingState.transitionTo(target);
        this.updatedAt = Instant.now();
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(r -> r.getName().equals(roleName));
    }

    // Getters and setters
    public UUID getId() { return id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPreferredLang() { return preferredLang; }
    public void setPreferredLang(String preferredLang) { this.preferredLang = preferredLang; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public OnboardingState getOnboardingState() { return onboardingState; }
    public boolean isProfileComplete() { return profileComplete; }
    public void setProfileComplete(boolean profileComplete) { this.profileComplete = profileComplete; }
    public Set<Role> getRoles() { return roles; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
