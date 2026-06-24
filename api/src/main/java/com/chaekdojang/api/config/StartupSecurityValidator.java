package com.chaekdojang.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class StartupSecurityValidator implements ApplicationRunner {

    private static final String DEV_JWT_SECRET = "chaekdojang-dev-secret-key-must-be-32chars-ok!";

    private final Environment environment;
    private final String jwtSecret;

    public StartupSecurityValidator(
            Environment environment,
            @Value("${jwt.secret:}") String jwtSecret
    ) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean productionLike = Arrays.stream(environment.getActiveProfiles())
                .noneMatch(profile -> profile.equals("local") || profile.equals("test"));

        if (!productionLike) {
            return;
        }

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured in staging/production.");
        }

        if (DEV_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("Default development JWT secret cannot be used in staging/production.");
        }

        if (jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HS256.");
        }
    }
}
