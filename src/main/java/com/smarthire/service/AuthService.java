package com.smarthire.service;

import com.smarthire.dto.AuthResponse;
import com.smarthire.dto.LoginRequest;
import com.smarthire.dto.RegisterRequest;
import com.smarthire.dto.UserResponse;
import com.smarthire.entity.PasswordResetToken;
import com.smarthire.entity.User;
import com.smarthire.enums.Role;
import com.smarthire.repository.UserRepository;
import com.smarthire.repository.PasswordResetTokenRepository;
import com.smarthire.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokens;
    private final JavaMailSender mailSender;
    private final String frontendUrl;
    private final Duration passwordResetExpiry;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       PasswordResetTokenRepository passwordResetTokens, JavaMailSender mailSender,
                       @Value("${app.frontend-url}") String frontendUrl,
                       @Value("${app.password-reset-expiration-minutes:30}") long passwordResetExpirationMinutes) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokens = passwordResetTokens;
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
        this.passwordResetExpiry = Duration.ofMinutes(passwordResetExpirationMinutes);
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists for this email.");
        }

        User user = userRepository.save(new User(
                request.name().trim(), email, passwordEncoder.encode(request.password()), request.role()));
        return responseFor(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        return responseFor(user);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(user -> {
            passwordResetTokens.deleteByUserId(user.getId());
            String rawToken = newResetToken();
            passwordResetTokens.save(new PasswordResetToken(user, hashToken(rawToken), Instant.now().plus(passwordResetExpiry)));
            sendPasswordResetEmail(user, rawToken);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokens.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "This password-reset link is invalid or has expired."));
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This password-reset link is invalid or has expired.");
        }
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.markUsed();
        passwordResetTokens.deleteByUserIdAndIdNot(user.getId(), resetToken.getId());
    }

    public UserResponse me(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found."));
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    public AuthResponse googleLogin(String email, String name, Role requestedRole) {
        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmail(normalized).orElseGet(() -> userRepository.save(new User(
                name == null || name.isBlank() ? normalized.substring(0, normalized.indexOf('@')) : name.trim(),
                normalized, passwordEncoder.encode(UUID.randomUUID().toString()), requestedRole)));
        return responseFor(user);
    }

    private AuthResponse responseFor(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer", user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String newResetToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void sendPasswordResetEmail(User user, String token) {
        String resetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/reset-password")
                .queryParam("token", token)
                .build()
                .toUriString();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Reset your SmartHire password");
        message.setText("Hello " + user.getName() + ",\n\n"
                + "We received a request to reset your SmartHire password. Use this link within "
                + passwordResetExpiry.toMinutes() + " minutes:\n" + resetUrl
                + "\n\nIf you did not request this, you can safely ignore this email.");
        mailSender.send(message);
    }
}
