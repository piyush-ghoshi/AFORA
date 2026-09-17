package com.academia.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Teacher entity.
 */
@Serializable
data class Teacher(
    val id: Long,
    val userId: Long,
    val employeeId: String,
    val firstName: String,
    val middleName: String? = null,
    val lastName: String,
    val designation: String? = null,
    val departmentId: Long? = null,
    val phone: String? = null,
    val isActive: Boolean = true
) {
    val fullName: String
        get() = if (middleName != null) {
            "$firstName $middleName $lastName"
        } else {
            "$firstName $lastName"
        }
}
