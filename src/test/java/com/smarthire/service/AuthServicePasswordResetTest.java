package com.smarthire.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smarthire.entity.PasswordResetToken;
import com.smarthire.entity.User;
import com.smarthire.enums.Role;
import com.smarthire.repository.PasswordResetTokenRepository;
import com.smarthire.repository.UserRepository;
import com.smarthire.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServicePasswordResetTest {
    @Test
    void sendsASingleUseResetLinkToTheRegisteredEmail() {
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        PasswordResetTokenRepository tokens = org.mockito.Mockito.mock(PasswordResetTokenRepository.class);
        JavaMailSender mail = org.mockito.Mockito.mock(JavaMailSender.class);
        User user = new User("Ayesha", "ayesha@example.com", "old-hash", Role.CANDIDATE);
        when(users.findByEmail("ayesha@example.com")).thenReturn(Optional.of(user));

        service(users, tokens, mail).requestPasswordReset(" AYESHA@EXAMPLE.COM ");

        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(tokens).deleteByUserId(null);
        verify(tokens).save(any(PasswordResetToken.class));
        verify(mail).send(email.capture());
        assertEquals("ayesha@example.com", email.getValue().getTo()[0]);
        assertTrue(email.getValue().getText().contains("http://frontend.example/reset-password?token="));
    }

    @Test
    void changesPasswordAndInvalidatesOtherResetLinks() {
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        PasswordResetTokenRepository tokens = org.mockito.Mockito.mock(PasswordResetTokenRepository.class);
        JavaMailSender mail = org.mockito.Mockito.mock(JavaMailSender.class);
        PasswordEncoder encoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        User user = new User("Ayesha", "ayesha@example.com", "old-hash", Role.CANDIDATE);
        PasswordResetToken token = new PasswordResetToken(user, hash("valid-token"), Instant.now().plusSeconds(60));
        when(tokens.findByTokenHash(hash("valid-token"))).thenReturn(Optional.of(token));
        when(encoder.encode("new-password")).thenReturn("new-hash");

        new AuthService(users, encoder, org.mockito.Mockito.mock(JwtService.class), tokens, mail,
                "http://frontend.example", 30).resetPassword("valid-token", "new-password");

        assertEquals("new-hash", user.getPasswordHash());
        assertTrue(token.isUsed());
        verify(tokens).deleteByUserIdAndIdNot(eq(null), eq(null));
    }

    private AuthService service(UserRepository users, PasswordResetTokenRepository tokens, JavaMailSender mail) {
        return new AuthService(users, org.mockito.Mockito.mock(PasswordEncoder.class),
                org.mockito.Mockito.mock(JwtService.class), tokens, mail, "http://frontend.example", 30);
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
