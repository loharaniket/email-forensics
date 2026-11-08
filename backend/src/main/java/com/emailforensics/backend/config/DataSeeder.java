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

    public static final UUID ANALYST_ID = UUID.fromString("3d0658fa-dead-47ea-b7d3-2d82e9918a95");
    public static final UUID ADMIN_ID = UUID.fromString("6c8eec53-5430-475f-91e1-a08faa3bae52");

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

        reseedDefaultUsers();
    }

    public void reseedDefaultUsers() {
        seedUser(ANALYST_ID, analystUsername, analystEmail, analystPassword, Role.ANALYST);
        seedUser(ADMIN_ID, adminUsername, adminEmail, adminPassword, Role.ADMIN);
    }

    private void seedUser(UUID id, String username, String email, String password, Role role) {
        Optional<User> existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            User user = existing.get();
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setRole(role);
            userRepository.save(user);
        } else {
            User user = new User(
                    id != null ? id : UUID.randomUUID(),
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
