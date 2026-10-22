package com.afora.backend.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Web configuration for CORS and other HTTP settings.
 *
 * Note: Spring does not allow allowedOrigins("*") together with allowCredentials(true).
 * We use allowedOriginPatterns instead, which supports wildcard AND credentials.
 */
@Configuration
class WebConfig(
    @Value("\${cors.allowed-origins}") private val allowedOrigins: String,
    @Value("\${cors.allowed-methods}") private val allowedMethods: String,
    @Value("\${cors.allowed-headers}") private val allowedHeaders: String,
    @Value("\${cors.allow-credentials}") private val allowCredentials: Boolean,
    @Value("\${cors.max-age}") private val maxAge: Long
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        val origins = allowedOrigins.split(",").map { it.trim() }.toTypedArray()
        registry.addMapping("/**")
            // allowedOriginPatterns supports "*" even when allowCredentials=true
            .allowedOriginPatterns(*origins)
            .allowedMethods(*allowedMethods.split(",").toTypedArray())
            .allowedHeaders(*allowedHeaders.split(",").toTypedArray())
            .allowCredentials(allowCredentials)
            .maxAge(maxAge)
    }
}
