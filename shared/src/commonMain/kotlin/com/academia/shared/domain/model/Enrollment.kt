package com.academia.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Student enrollment in a subject for a semester.
 */
@Serializable
data class Enrollment(
    val id: Long,
    val studentId: Long,
    val classSectionId: Long,
    val subjectId: Long,
    val semesterId: Long,
    val enrollmentDate: String,  // ISO 8601 date
    val status: EnrollmentStatus = EnrollmentStatus.ACTIVE
)

/**
 * Enrollment status.
 */
@Serializable
enum class EnrollmentStatus {
    ACTIVE,
    DROPPED,
    COMPLETED
}
