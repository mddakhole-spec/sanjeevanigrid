package com.sanjeevanigrid.common.config;

import com.sanjeevanigrid.identity.domain.*;
import com.sanjeevanigrid.identity.infra.RoleRepository;
import com.sanjeevanigrid.identity.infra.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo accounts for every role on startup.
 * Only runs in default/dev profile.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepo, RoleRepository roleRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) {
            log.info("Database already seeded, skipping.");
            return;
        }

        log.info("Seeding demo accounts...");

        seedUser("admin@sanjeevanigrid.in", "Admin User", Role.PLATFORM_ADMIN);
        seedUser("doctor@sanjeevanigrid.in", "Dr. Priya Sharma", Role.DOCTOR);
        seedUser("hospital@sanjeevanigrid.in", "Hospital Admin", Role.HOSPITAL_ADMIN);
        seedUser("citizen@sanjeevanigrid.in", "Ramesh Patil", Role.CITIZEN);
        seedUser("donor@sanjeevanigrid.in", "Anita Deshmukh", Role.DONOR);
        seedUser("campworker@sanjeevanigrid.in", "Suresh Jadhav", Role.CAMP_WORKER);
        seedUser("organizer@sanjeevanigrid.in", "Meera Kulkarni", Role.CAMP_ORGANIZER);

        log.info("Demo accounts seeded. Password for all: Password@123");
    }

    private void seedUser(String email, String fullName, String roleName) {
        if (userRepo.existsByEmail(email)) return;

        Role role = roleRepo.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        User user = User.createWithEmail(email, passwordEncoder.encode("Password@123"), fullName, "en");
        user.addRole(role);
        user.transitionTo(OnboardingState.VERIFIED);
        user.transitionTo(OnboardingState.ACTIVE);
        userRepo.save(user);

        log.info("  Seeded: {} [{}] -> {}", email, roleName, user.getId());
    }
}
