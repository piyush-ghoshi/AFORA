package com.academia.backend.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Web configuration for CORS and other HTTP settings.
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
        registry.addMapping("/**")
            .allowedOrigins(*allowedOrigins.split(",").toTypedArray())
            .allowedMethods(*allowedMethods.split(",").toTypedArray())
            .allowedHeaders(*allowedHeaders.split(",").toTypedArray())
            .allowCredentials(allowCredentials)
            .maxAge(maxAge)
    }
}
