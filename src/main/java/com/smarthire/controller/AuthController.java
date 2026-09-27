package com.smarthire.controller;

import com.smarthire.dto.AuthResponse;
import com.smarthire.dto.LoginRequest;
import com.smarthire.dto.RegisterRequest;
import com.smarthire.dto.OAuthCodeRequest;
import com.smarthire.dto.PasswordResetConfirmRequest;
import com.smarthire.dto.PasswordResetRequest;
import com.smarthire.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import com.smarthire.dto.UserResponse;
import com.smarthire.service.OAuthLoginCodeService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final OAuthLoginCodeService oauthCodes;

    public AuthController(AuthService authService, OAuthLoginCodeService oauthCodes) {
        this.authService = authService;
        this.oauthCodes = oauthCodes;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.requestPasswordReset(request.email());
        // Do not disclose whether an email address is registered.
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        authService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth/exchange")
    public AuthResponse exchangeGoogleCode(@Valid @RequestBody OAuthCodeRequest request) {
        return oauthCodes.consume(request.code());
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public UserResponse me(Principal principal) {
        return authService.me(principal.getName());
    }
}
