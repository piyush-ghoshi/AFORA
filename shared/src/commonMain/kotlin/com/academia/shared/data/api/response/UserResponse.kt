package com.academia.shared.data.api.response

import kotlinx.serialization.Serializable

/**
 * User response DTO — matches backend UserResponse shape.
 */
@Serializable
data class UserApiResponse(
    val id: Long,
    val firebaseUid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val role: String,
    val profilePictureUrl: String? = null,
    val active: Boolean = true,
    val lastLoginAt: String? = null,
    val student: StudentProfileApiResponse? = null,
    val teacher: TeacherProfileApiResponse? = null
)

@Serializable
data class StudentProfileApiResponse(
    val id: Long,
    val rollNumber: String,
    val department: String,
    val batch: String,
    val semester: String? = null,
    val phoneNumber: String? = null,
    val gender: String? = null
)

@Serializable
data class TeacherProfileApiResponse(
    val id: Long,
    val employeeId: String,
    val department: String,
    val designation: String? = null,
    val specialization: String? = null,
    val phoneNumber: String? = null,
    val officeLocation: String? = null
)

/**
 * Auth sync response — wraps UserApiResponse with isNewUser flag.
 */
@Serializable
data class AuthSyncApiResponse(
    val user: UserApiResponse,
    val isNewUser: Boolean
)

/**
 * Timetable slot DTO from backend.
 */
@Serializable
data class TimetableSlotApiResponse(
    val id: Long,
    val classSectionId: Long,
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val teacherId: Long,
    val teacherName: String,
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String,
    val roomNumber: String? = null
)

/**
 * Student API response (for list/detail endpoints).
 */
@Serializable
data class StudentApiResponse(
    val id: Long,
    val userId: Long,
    val rollNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String? = null,
    val department: String,
    val batch: String,
    val semester: String? = null,
    val isActive: Boolean = true
)

/**
 * Teacher API response (for list/detail endpoints).
 */
@Serializable
data class TeacherApiResponse(
    val id: Long,
    val userId: Long,
    val employeeId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val department: String,
    val designation: String? = null,
    val specialization: String? = null,
    val phone: String? = null,
    val isActive: Boolean = true
)
