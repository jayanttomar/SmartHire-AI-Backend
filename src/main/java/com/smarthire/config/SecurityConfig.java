package com.smarthire.config;

import com.smarthire.security.JwtAuthenticationFilter;
import com.smarthire.security.GoogleAuthenticationSuccessHandler;
import com.smarthire.security.GoogleRoleSelectionFilter;
import java.util.List;
import java.util.ArrayList;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final GoogleRoleSelectionFilter googleRoleSelectionFilter;
    private final String frontendUrl;
    private final GoogleAuthenticationSuccessHandler googleSuccessHandler;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrations;
    private final boolean allowLocalOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          GoogleRoleSelectionFilter googleRoleSelectionFilter,
                          @Value("${app.frontend-url}") String frontendUrl,
                          @Value("${app.cors.allow-local-origins:true}") boolean allowLocalOrigins,
                          @Lazy GoogleAuthenticationSuccessHandler googleSuccessHandler,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrations) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.googleRoleSelectionFilter = googleRoleSelectionFilter;
        this.frontendUrl = frontendUrl;
        this.allowLocalOrigins = allowLocalOrigins;
        this.googleSuccessHandler = googleSuccessHandler;
        this.clientRegistrations = clientRegistrations;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // API callers must receive an HTTP error, never an OAuth HTML redirect.
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED),
                        new AntPathRequestMatcher("/api/**")))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/oauth/exchange").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/jobs/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/jobs", "/api/jobs/*").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        if (clientRegistrations.getIfAvailable() != null) {
            http.addFilterBefore(googleRoleSelectionFilter, OAuth2AuthorizationRequestRedirectFilter.class);
            http.oauth2Login(oauth -> oauth.successHandler(googleSuccessHandler)
                    .failureHandler((request, response, exception) -> {
                        if (request.getSession(false) != null) request.getSession(false).invalidate();
                        response.sendRedirect(frontendUrl.replaceAll("/+$", "") + "/?oauthError=google_sign_in_failed");
                    }));
        }
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Vite moves to the next local port when its default port is occupied.
        // Permit the configured frontend URL plus local development ports.
        List<String> allowedOrigins = new ArrayList<>(List.of(frontendUrl));
        if (allowLocalOrigins) {
            allowedOrigins.add("http://localhost:*");
            allowedOrigins.add("http://127.0.0.1:*");
        }
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
