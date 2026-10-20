package com.afora.backend.common.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI (Swagger) configuration for Afora Backend.
 * Generates interactive API documentation accessible via `/api/swagger-ui.html`.
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        val securitySchemeName = "BearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("Afora Academic & Attendance Management API")
                    .version("1.0.0")
                    .description(
                        "Interactive API dashboard for managing academic operations: assigning teachers, " +
                        "class sections, weekly timetable slots, student enrollments, and lecture sessions."
                    )
                    .contact(
                        Contact()
                            .name("Afora Development Team")
                    )
            )
            .addSecurityItem(SecurityRequirement().addList(securitySchemeName))
            .components(
                Components().addSecuritySchemes(
                    securitySchemeName,
                    SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .`in`(SecurityScheme.In.HEADER)
                        .description("Optional: Paste Firebase ID token here for secured endpoints")
                )
            )
    }
}
