package com.academia.backend.common.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

/**
 * Standard API response wrapper.
 * Consistent response structure across all endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(
            success = true,
            data = data
        )
        
        fun <T> error(
            message: String,
            code: String? = null,
            details: Map<String, Any>? = null
        ): ApiResponse<T> = ApiResponse(
            success = false,
            error = ErrorDetail(message, code, details)
        )
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDetail(
    val message: String,
    val code: String? = null,
    val details: Map<String, Any>? = null
)

/**
 * Paginated response wrapper.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean
)
