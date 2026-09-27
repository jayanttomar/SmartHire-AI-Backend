package com.smarthire.config;

import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Prevents an accidentally insecure deployment when the prod profile is selected. */
@Component
@Profile("prod")
public class ProductionConfigurationValidator implements ApplicationRunner {
    private final String jwtSecret;
    private final String frontendUrl;
    private final String databasePassword;
    private final String mailHost;
    private final String mailUsername;
    private final String mailPassword;
    private final String googleClientId;
    private final String googleClientSecret;

    public ProductionConfigurationValidator(
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.frontend-url}") String frontendUrl,
            @Value("${spring.datasource.password}") String databasePassword,
            @Value("${spring.mail.host}") String mailHost,
            @Value("${spring.mail.username}") String mailUsername,
            @Value("${spring.mail.password}") String mailPassword,
            @Value("${app.google.client-id:}") String googleClientId,
            @Value("${app.google.client-secret:}") String googleClientSecret) {
        this.jwtSecret = jwtSecret;
        this.frontendUrl = frontendUrl;
        this.databasePassword = databasePassword;
        this.mailHost = mailHost;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        this.googleClientId = googleClientId;
        this.googleClientSecret = googleClientSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> errors = new java.util.ArrayList<>();
        if (jwtSecret == null || jwtSecret.length() < 32 || jwtSecret.startsWith("change-this")) {
            errors.add("JWT_SECRET must be a unique secret of at least 32 characters");
        }
        if (!isHttpsUrl(frontendUrl)) {
            errors.add("FRONTEND_URL must be an https URL");
        }
        if (isBlankOrPlaceholder(databasePassword)) {
            errors.add("DB_PASSWORD must be configured");
        }
        if (isBlankOrPlaceholder(mailHost) || isBlankOrPlaceholder(mailUsername) || isBlankOrPlaceholder(mailPassword)) {
            errors.add("MAIL_HOST, MAIL_USERNAME, and MAIL_PASSWORD must be configured for password reset");
        }
        if (googleClientId.isBlank() != googleClientSecret.isBlank()) {
            errors.add("GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET must be configured together");
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Production configuration is unsafe: " + String.join("; ", errors));
        }
    }

    private boolean isHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isBlankOrPlaceholder(String value) {
        return value == null || value.isBlank() || value.startsWith("your_") || value.startsWith("replace_");
    }
}
