package com.afora.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Academic semester entity.
 */
@Serializable
data class Semester(
    val id: Long,
    val academicYearId: Long,
    val semesterNumber: Int,  // 1 or 2
    val name: String,  // e.g., "Fall 2024", "Spring 2025"
    val startDate: String,  // ISO 8601 date
    val endDate: String,  // ISO 8601 date
    val isCurrent: Boolean = false
)

/**
 * Academic year entity.
 */
@Serializable
data class AcademicYear(
    val id: Long,
    val year: String,  // e.g., "2024-2025"
    val startDate: String,  // ISO 8601 date
    val endDate: String,  // ISO 8601 date
    val isCurrent: Boolean = false
)
