package com.academia.backend.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * JWT configuration properties.
 * Phase A1: Configuration structure only.
 * Phase A2: Will be used for actual JWT token generation/validation.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
data class JwtConfig(
    var secret: String = "",
    var expiration: Long = 86400000, // 24 hours
    var refreshExpiration: Long = 604800000 // 7 days
)
