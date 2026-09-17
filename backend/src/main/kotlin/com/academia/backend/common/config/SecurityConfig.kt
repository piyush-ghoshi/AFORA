package com.academia.backend.common.config

import com.academia.backend.common.security.FirebaseAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * Security configuration with Firebase Authentication.
 * 
 * Phase A2: Integrated Firebase ID token validation.
 * 
 * Security model:
 * - Stateless (no server sessions)
 * - Firebase handles authentication
 * - Backend validates Firebase tokens
 * - Role-based authorization via @PreAuthorize
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig(
    private val firebaseAuthenticationFilter: FirebaseAuthenticationFilter
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Disable CSRF (stateless API with token auth)
            .csrf { it.disable() }
            
            // Stateless session (no cookies)
            .sessionManagement { 
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) 
            }
            
            // Add Firebase authentication filter before Spring Security's default filter
            .addFilterBefore(
                firebaseAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java
            )
            
            // Configure authorization rules
            .authorizeHttpRequests { auth ->
                auth
                    // Public endpoints (no authentication required)
                    .requestMatchers(
                        "/health",
                        "/api/health",
                        "/actuator/**",
                        "/error"
                    ).permitAll()
                    
                    // Auth endpoints (handled by Firebase, but backend needs to process)
                    .requestMatchers(
                        "/api/auth/**"
                    ).permitAll()
                    
                    // All other endpoints require authentication
                    .anyRequest().authenticated()
            }
        
        return http.build()
    }

    /**
     * Password encoder for local user passwords (if needed).
     * Firebase handles auth, but may be useful for admin passwords, etc.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}

