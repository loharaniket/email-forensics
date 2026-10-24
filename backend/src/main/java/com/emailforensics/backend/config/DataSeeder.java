package com.emailforensics.backend.config;

import com.emailforensics.backend.user.Role;
import com.emailforensics.backend.user.User;
import com.emailforensics.backend.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String analystUsername;
    private final String analystEmail;
    private final String analystPassword;

    public DataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.analyst-username:analyst}") String analystUsername,
            @Value("${app.seed.analyst-email:analyst@emailforensics.local}") String analystEmail,
            @Value("${app.seed.analyst-password:Analyst@123}") String analystPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.analystUsername = analystUsername;
        this.analystEmail = analystEmail;
        this.analystPassword = analystPassword;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername(analystUsername)) {
            User analyst = new User(
                    UUID.randomUUID(),
                    analystUsername,
                    analystEmail,
                    passwordEncoder.encode(analystPassword),
                    Role.ANALYST,
                    true,
                    Instant.now()
            );
            userRepository.save(analyst);
        }
    }
}
