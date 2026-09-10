package com.smarthire.service;

import com.smarthire.dto.AuthResponse;
import com.smarthire.dto.LoginRequest;
import com.smarthire.dto.RegisterRequest;
import com.smarthire.entity.User;
import com.smarthire.repository.UserRepository;
import com.smarthire.security.JwtService;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    private AuthResponse responseFor(User user) {
        return new AuthResponse(jwtService.generateToken(user), "Bearer", user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
