package com.emailforensics.backend.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ProductionConfigurationValidator {

    private static final Set<String> KNOWN_DEFAULT_JWT_SECRETS = Set.of(
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
            "404e635266556a586e3272357538782f413f4428472b4b6250645367566b5970",
            "secret",
            "password",
            "jwtsecret",
            "changeme",
            "admin",
            "default"
    );

    private static final Set<String> KNOWN_DEFAULT_DB_PASSWORDS = Set.of(
            "aniket@12345",
            "password",
            "postgres",
            "admin",
            "root",
            "123456",
            "email_password",
            "changeme"
    );

    private final Environment environment;
    private final String dbPassword;
    private final String jwtSecret;

    @Autowired
    public ProductionConfigurationValidator(
            Environment environment,
            @Value("${spring.datasource.password:}") String dbPassword,
            @Value("${app.jwt.secret:}") String jwtSecret) {
        this.environment = environment;
        this.dbPassword = dbPassword;
        this.jwtSecret = jwtSecret;
    }

    @PostConstruct
    public void validateConfiguration() {
        if (environment == null || !environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return;
        }

        String effectiveDbPassword = (dbPassword != null && !dbPassword.isBlank()) ? dbPassword : environment.getProperty("DB_PASSWORD");
        if (effectiveDbPassword == null || effectiveDbPassword.isBlank()) {
            throw new IllegalStateException("Production startup rejected: DB_PASSWORD must be configured in production profile.");
        }
        if (KNOWN_DEFAULT_DB_PASSWORDS.contains(effectiveDbPassword.trim().toLowerCase()) || effectiveDbPassword.trim().equalsIgnoreCase("aniket@12345")) {
            throw new IllegalStateException("Production startup rejected: Known default or insecure database password detected.");
        }

        String effectiveJwtSecret = (jwtSecret != null && !jwtSecret.isBlank()) ? jwtSecret : environment.getProperty("JWT_SECRET");
        if (effectiveJwtSecret == null || effectiveJwtSecret.isBlank()) {
            throw new IllegalStateException("Production startup rejected: JWT_SECRET must be configured in production profile.");
        }
        if (effectiveJwtSecret.trim().length() < 32) {
            throw new IllegalStateException("Production startup rejected: JWT_SECRET must be at least 32 characters in production profile.");
        }
        if (KNOWN_DEFAULT_JWT_SECRETS.contains(effectiveJwtSecret.trim()) || KNOWN_DEFAULT_JWT_SECRETS.contains(effectiveJwtSecret.trim().toLowerCase())) {
            throw new IllegalStateException("Production startup rejected: Known default or insecure JWT secret detected.");
        }
    }
}
