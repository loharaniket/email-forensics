package com.emailforensics.backend.config;

import com.emailforensics.backend.user.Role;
import com.emailforensics.backend.user.User;
import com.emailforensics.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String analystUsername;
    private final String analystEmail;
    private final String analystPassword;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    @Autowired
    public DataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Autowired(required = false) Environment environment,
            @Value("${app.seed.analyst-username:analyst}") String analystUsername,
            @Value("${app.seed.analyst-email:analyst@emailforensics.local}") String analystEmail,
            @Value("${app.seed.analyst-password:analyst123}") String analystPassword,
            @Value("${app.seed.admin-username:admin}") String adminUsername,
            @Value("${app.seed.admin-email:admin@emailforensics.local}") String adminEmail,
            @Value("${app.seed.admin-password:admin123}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.analystUsername = analystUsername;
        this.analystEmail = analystEmail;
        this.analystPassword = analystPassword;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (environment != null && environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return;
        }

        seedUser(analystUsername, analystEmail, analystPassword, Role.ANALYST);
        seedUser(adminUsername, adminEmail, adminPassword, Role.ADMIN);
    }

    private void seedUser(String username, String email, String password, Role role) {
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            User user = existing.get();
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(role);
            userRepository.save(user);
        } else {
            User user = new User(
                    UUID.randomUUID(),
                    username,
                    email,
                    passwordEncoder.encode(password),
                    role,
                    true,
                    Instant.now()
            );
            userRepository.save(user);
        }
    }
}
