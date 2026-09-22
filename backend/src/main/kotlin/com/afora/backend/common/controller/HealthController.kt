package com.afora.backend.common.controller

import com.afora.backend.common.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Health check controller for Phase A1 foundation verification.
 */
@RestController
@RequestMapping("/health")
class HealthController {

    @GetMapping
    fun health(): ApiResponse<HealthStatus> {
        return ApiResponse.success(
            HealthStatus(
                status = "UP",
                service = "afora-backend",
                version = "1.0.0-A1",
                timestamp = Instant.now()
            )
        )
    }
}

data class HealthStatus(
    val status: String,
    val service: String,
    val version: String,
    val timestamp: Instant
)
