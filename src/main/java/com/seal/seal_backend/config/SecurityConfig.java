package com.seal.seal_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Baseline security. OWNED BY: Auth/M1 (Đồng Thành Minh Trí).
 *
 * NOTE FOR THE TEAM:
 *   - During early parallel dev this permits everything so each module can demo via Swagger
 *     without a working login. @EnableMethodSecurity is ON, so you can already put
 *     @PreAuthorize("hasRole('JUDGE')") on your controllers — keep authorization at METHOD level
 *     so this file stays stable and does NOT become a merge-conflict hotspot.
 *   - When the Auth module lands the JWT filter, replace permitAll() with the real chain here
 *     (only M1 edits this file).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // TODO(M1): tighten once JWT filter exists. permitAll for now to unblock parallel dev.
                .anyRequest().permitAll()
            );
        // TODO(M1): http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
