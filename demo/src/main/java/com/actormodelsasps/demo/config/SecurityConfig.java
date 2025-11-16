package com.actormodelsasps.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the chat application
 * 
 * This configuration:
 * 1. Disables CSRF for simplicity (in production, implement proper CSRF protection)
 * 2. Allows public access to authentication endpoints
 * 3. Allows WebSocket connections
 * 4. Provides password encoding
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    /**
     * Configure HTTP security
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST API and WebSocket (in production, implement properly)
            .csrf(csrf -> csrf.disable())
            
            // Configure URL access permissions
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()  // Allow public access to auth endpoints
                .requestMatchers("/ws/**").permitAll()        // Allow WebSocket connections
                .requestMatchers("/h2-console/**").permitAll() // Allow H2 console access (development)
                .anyRequest().authenticated()                 // All other requests need authentication
            )
            
            // Disable frame options for H2 console (development only)
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.disable())
            )
            
            // Disable form login (we're using REST endpoints)
            .formLogin(form -> form.disable())
            
            // Disable HTTP Basic authentication
            .httpBasic(basic -> basic.disable());
        
        return http.build();
    }
    
    /**
     * Password encoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}