package com.smarthire.security;

import com.smarthire.dto.AuthResponse;
import com.smarthire.enums.Role;
import com.smarthire.service.AuthService;
import com.smarthire.service.OAuthLoginCodeService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GoogleAuthenticationSuccessHandlerTest {
    @Test void verifiedGoogleAccountGetsSelectedRoleAndOneTimeCode() throws Exception {
        AuthService auth = mock(AuthService.class);
        OAuthLoginCodeService codes = new OAuthLoginCodeService();
        AuthResponse expected = new AuthResponse("jwt", "Bearer", 1L, "Test", "test@example.test", Role.RECRUITER);
        when(auth.googleLogin("test@example.test", "Test", Role.RECRUITER)).thenReturn(expected);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("googleSignupRole", "RECRUITER");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new GoogleAuthenticationSuccessHandler(auth, codes, "http://localhost:5173/")
                .onAuthenticationSuccess(request, response, authentication(true));
        String url = response.getRedirectedUrl();
        assertTrue(url.startsWith("http://localhost:5173/?oauthCode="));
        assertFalse(url.contains("jwt"));
        assertEquals(expected, codes.consume(url.substring(url.indexOf('=') + 1)));
        assertNull(request.getSession(false));
    }

    @Test void unverifiedGoogleEmailCannotLogIntoExistingAccount() throws Exception {
        AuthService auth = mock(AuthService.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new GoogleAuthenticationSuccessHandler(auth, new OAuthLoginCodeService(), "http://localhost:5173")
                .onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication(false));
        assertEquals("http://localhost:5173/?oauthError=unverified_email", response.getRedirectedUrl());
        verifyNoInteractions(auth);
    }

    private Authentication authentication(boolean verified) {
        OAuth2User user = mock(OAuth2User.class);
        when(user.getAttribute("email")).thenReturn("test@example.test");
        when(user.getAttribute("name")).thenReturn("Test");
        when(user.getAttribute("email_verified")).thenReturn(verified);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }
}
