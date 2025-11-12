package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // Allow iframe (for PDF preview in modal)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))

            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth
                    // public endpoints
                    .requestMatchers(
                        "/api/v1/admin/login",
                        "/api/v1/admin/register",
                        "/api/v1/admin/forgot-password",

                        // file access (public)
                        "/api/v1/employees/*/photo",
                        "/api/v1/employees/*/resume/view",
                        "/api/v1/employees/*/resume/download",

                        // 2FA verify must be public because it uses preAuthToken in body
                        "/api/v1/2fa/verify",
                        // optional debug/test endpoint (remove in production)
                        "/api/v1/2fa/generate-test",
                        
                        
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs.yaml",
                        "/swagger-resources/**",
                        "/webjars/**"
                    ).permitAll()

                    // Endpoints that require a logged-in user
                    .requestMatchers("/api/v1/2fa/generate", "/api/v1/2fa/confirm").authenticated()

                    // Role-based APIs
                    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .requestMatchers("/api/v1/employees/**").hasAnyRole("ADMIN", "HR")

                    .anyRequest().authenticated()
            )

            // JWT is stateless
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // Register JWT filter before authentication
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
