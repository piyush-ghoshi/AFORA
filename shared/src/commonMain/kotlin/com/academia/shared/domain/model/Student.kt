package com.academia.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Student entity.
 */
@Serializable
data class Student(
    val id: Long,
    val userId: Long,
    val rollNumber: String,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String,
    val email: String,
    val phone: String? = null,
    val dateOfBirth: String,  // ISO 8601 date string (YYYY-MM-DD)
    val enrollmentDate: String,  // ISO 8601 date string
    val isActive: Boolean = true
) {
    val fullName: String
        get() = if (middleName != null) {
            "$firstName $middleName $lastName"
        } else {
            "$firstName $lastName"
        }
}
