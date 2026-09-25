package com.afora.backend.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.time.format.DateTimeFormatter

/**
 * Jackson configuration to ensure Java 8 date/time types are serialized
 * as ISO-8601 strings instead of numeric arrays.
 *
 * Without this, LocalDate serializes as [2026, 9, 17] — which breaks
 * the Android Kotlin Serialization deserialization expecting a string.
 */
@Configuration
class JacksonConfig {

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val javaTimeModule = JavaTimeModule().apply {
            addSerializer(
                java.time.LocalDate::class.java,
                LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE)      // "2026-09-17"
            )
            addSerializer(
                java.time.LocalDateTime::class.java,
                LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
            addSerializer(
                java.time.LocalTime::class.java,
                LocalTimeSerializer(DateTimeFormatter.ISO_LOCAL_TIME)
            )
        }

        return ObjectMapper()
            .registerKotlinModule()
            .registerModule(javaTimeModule)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }
}
