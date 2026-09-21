package com.academia.backend.user.dto

import com.academia.backend.user.entity.Student
import com.academia.backend.user.entity.Teacher
import com.academia.backend.user.entity.User
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

// ── Response DTOs ─────────────────────────────────────────────────────────────

/**
 * User profile response (returned by /api/users/me and /api/auth/register).
 */
data class UserResponse(
    val id: Long,
    val firebaseUid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val role: String,
    val profilePictureUrl: String?,
    val active: Boolean,
    val lastLoginAt: LocalDateTime?,
    val student: StudentProfileResponse?,
    val teacher: TeacherProfileResponse?
) {
    companion object {
        fun from(user: User, student: Student? = null, teacher: Teacher? = null) = UserResponse(
            id = user.id!!,
            firebaseUid = user.firebaseUid,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            fullName = user.getFullName(),
            role = user.role.name,
            profilePictureUrl = user.profilePictureUrl,
            active = user.active,
            lastLoginAt = user.lastLoginAt,
            student = student?.let { StudentProfileResponse.from(it) },
            teacher = teacher?.let { TeacherProfileResponse.from(it) }
        )
    }
}

data class StudentProfileResponse(
    val id: Long,
    val rollNumber: String,
    val department: String,
    val batch: String,
    val semester: String?,
    val phoneNumber: String?,
    val gender: String?
) {
    companion object {
        fun from(s: Student) = StudentProfileResponse(
            id = s.id!!,
            rollNumber = s.rollNumber,
            department = s.department,
            batch = s.batch,
            semester = s.semester,
            phoneNumber = s.phoneNumber,
            gender = s.gender
        )
    }
}

data class TeacherProfileResponse(
    val id: Long,
    val employeeId: String,
    val department: String,
    val designation: String?,
    val specialization: String?,
    val phoneNumber: String?,
    val officeLocation: String?
) {
    companion object {
        fun from(t: Teacher) = TeacherProfileResponse(
            id = t.id!!,
            employeeId = t.employeeId,
            department = t.department,
            designation = t.designation,
            specialization = t.specialization,
            phoneNumber = t.phoneNumber,
            officeLocation = t.officeLocation
        )
    }
}

// ── Auth DTOs ────────────────────────────────────────────────────────────────

/**
 * Sent by Android after Firebase registration to persist user in DB.
 */
data class SyncUserRequest(
    @field:NotBlank val firebaseUid: String,
    @field:NotBlank val email: String,
    @field:NotBlank val firstName: String,
    @field:NotBlank val lastName: String,
    /** STUDENT or TEACHER */
    @field:NotBlank val role: String
)

/**
 * Response for auth sync — returns full user profile.
 */
data class AuthSyncResponse(
    val user: UserResponse,
    val isNewUser: Boolean
)

// ── Update DTOs ───────────────────────────────────────────────────────────────

data class UpdateProfileRequest(
    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val firstName: String,

    @field:NotBlank
    @field:Size(min = 1, max = 100)
    val lastName: String,

    val profilePictureUrl: String? = null
)
