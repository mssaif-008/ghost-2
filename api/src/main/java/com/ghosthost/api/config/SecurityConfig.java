package com.ghosthost.api.config;

import com.ghosthost.api.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * WHAT THIS DOES:
 * - Disables CSRF (we're a stateless API, not a form-based app)
 * - Makes sessions stateless (every request must have a JWT)
 * - Allows unauthenticated access to /auth/** endpoints
 * - Requires authentication for everything else
 * - Adds our JwtAuthFilter before Spring's default auth filter
 *
 * COMMON MISTAKE: If you forget to add the JwtAuthFilter,
 * all authenticated endpoints will return 403 Forbidden.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            // Disable CSRF — not needed for stateless REST APIs
            .csrf(csrf -> csrf.disable())

            // Stateless sessions — no server-side session
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // URL-based authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no token needed)
                .requestMatchers("/auth/**").permitAll()
                // Health check
                .requestMatchers("/health").permitAll()
                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // Add our JWT filter before the default auth filter
            .addFilterBefore(jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.Arrays.asList("*"));
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * BCrypt password encoder — used for hashing passwords.
     * BCrypt is intentionally slow to prevent brute-force attacks.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
