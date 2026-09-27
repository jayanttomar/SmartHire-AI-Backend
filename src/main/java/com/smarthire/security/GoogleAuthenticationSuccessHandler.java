package com.smarthire.security;

import com.smarthire.dto.AuthResponse;
import com.smarthire.service.AuthService;
import com.smarthire.service.OAuthLoginCodeService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.smarthire.enums.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService auth; private final OAuthLoginCodeService codes; private final String frontendUrl;
    public GoogleAuthenticationSuccessHandler(AuthService auth, OAuthLoginCodeService codes, @Value("${app.frontend-url}") String frontendUrl) { this.auth=auth; this.codes=codes; this.frontendUrl=frontendUrl; }
    @Override public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User googleUser=(OAuth2User)authentication.getPrincipal();
        String email=googleUser.getAttribute("email"); String name=googleUser.getAttribute("name");
        if(email==null || email.isBlank() || !Boolean.TRUE.equals(googleUser.getAttribute("email_verified"))){
            if(request.getSession(false)!=null) request.getSession(false).invalidate();
            response.sendRedirect(frontendUrl.replaceAll("/+$", "")+"/?oauthError=unverified_email");
            return;
        }
        Object savedRole=request.getSession(false)==null?null:request.getSession(false).getAttribute("googleSignupRole");
        Role requestedRole="RECRUITER".equals(savedRole)?Role.RECRUITER:Role.CANDIDATE;
        AuthResponse result=auth.googleLogin(email, name, requestedRole);
        if(request.getSession(false)!=null) request.getSession(false).invalidate();
        String target=frontendUrl.replaceAll("/+$", "")+"/?oauthCode="+URLEncoder.encode(codes.create(result),StandardCharsets.UTF_8);
        response.sendRedirect(target);
    }
}
