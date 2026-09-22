package com.afora.shared.data.api.response

import kotlinx.serialization.Serializable

/**
 * Standard API response wrapper.
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: ErrorResponse? = null
)

/**
 * Error response structure.
 */
@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: List<FieldError>? = null,
    val timestamp: String? = null,
    val path: String? = null
)

/**
 * Field-level error (validation errors).
 */
@Serializable
data class FieldError(
    val field: String,
    val message: String
)

/**
 * Paginated response.
 */
@Serializable
data class PagedResponse<T>(
    val success: Boolean,
    val data: List<T>,
    val pagination: PaginationInfo,
    val message: String? = null
)

/**
 * Pagination metadata.
 */
@Serializable
data class PaginationInfo(
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)
