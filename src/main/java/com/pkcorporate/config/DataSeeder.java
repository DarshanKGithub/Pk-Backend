package com.pkcorporate.config;

import com.pkcorporate.entity.User;
import com.pkcorporate.enums.Role;
import com.pkcorporate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${admin.seed.password:Admin@PK2024!LocalDev}")
    private String adminSeedPassword;

    @Override
    public void run(String... args) {
        // Step 7: gate the seeder so prod boots stay lean. Disable on Render
        // (schema + admin already provisioned) with SEED_ENABLED=false to skip
        // even the count() probe. Defaults true so fresh local/CI DBs still get
        // an admin bootstrap.
        boolean seedEnabled = Boolean.parseBoolean(environment.getProperty("SEED_ENABLED", "true"));
        if (!seedEnabled) {
            log.info("DataSeeder skipped (SEED_ENABLED=false)");
            return;
        }
        seedUsers();
    }

    private void seedUsers() {
        // Always ensure at least one Admin user exists if the database is empty
        if (userRepository.count() == 0) {
            if (adminSeedPassword.equals("Admin@PK2024!LocalDev")) {
                log.warn("⚠️  Admin seeded with DEFAULT local-dev password. Set ADMIN_SEED_PASSWORD env var in production!");
            }
            createUserIfNotExists("Admin User", "admin@pkcorporate.com", adminSeedPassword, Role.ADMIN, null);
            log.info("Initial admin user seeded: admin@pkcorporate.com");
        }

        // Seed other demo users only in dev or demo profiles
        boolean isDevOrDemo = Arrays.asList(environment.getActiveProfiles()).contains("dev") ||
                              Arrays.asList(environment.getActiveProfiles()).contains("demo") ||
                              Arrays.asList(environment.getDefaultProfiles()).contains("dev") ||
                              Arrays.asList(environment.getDefaultProfiles()).contains("demo");
        if (isDevOrDemo) {
            createUserIfNotExists("Ravi Kumar", "agent@pkcorporate.com", "Agent@123", Role.AGENT, "AGT-001");
            createUserIfNotExists("Priya Shah", "accountant@pkcorporate.com", "Acc@123", Role.ACCOUNTANT, null);
            createUserIfNotExists("Arjun Mehta", "designer@pkcorporate.com", "Design@123", Role.DESIGNER, null);
            log.info("✅ PK Corporate ERP — Demo users seeded successfully (dev/demo profile only)");
        }
    }

    private void createUserIfNotExists(String name, String email, String password, Role role, String agentCode) {
        if (!userRepository.existsByEmail(email)) {
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(role)
                    .active(true)
                    .emailVerified(true)
                    .agentCode(agentCode)
                    .commissionRate(role == Role.AGENT ? 4.0 : null)
                    .build();
            userRepository.save(user);
            log.info("Created {} user: {}", role, email);
        }
    }
}
