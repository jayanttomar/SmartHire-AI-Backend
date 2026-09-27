package com.smarthire.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Preserves the role chosen before the Google OAuth redirect. */
@Component
public class GoogleRoleSelectionFilter extends OncePerRequestFilter {
    private static final AntPathRequestMatcher GOOGLE_AUTH = new AntPathRequestMatcher("/oauth2/authorization/google");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) { return !GOOGLE_AUTH.matches(request); }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String role = request.getParameter("role");
        request.getSession(true).setAttribute("googleSignupRole", "RECRUITER".equals(role) ? "RECRUITER" : "CANDIDATE");
        chain.doFilter(request, response);
    }
}
