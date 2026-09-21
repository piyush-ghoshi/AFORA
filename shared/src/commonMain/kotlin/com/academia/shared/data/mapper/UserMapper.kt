package com.academia.shared.data.mapper

import com.academia.shared.data.api.response.*
import com.academia.shared.domain.model.Student
import com.academia.shared.domain.model.Teacher
import com.academia.shared.domain.model.TimetableSlot
import com.academia.shared.domain.model.User

/**
 * Maps API response DTOs to domain models.
 */

fun UserApiResponse.toDomain(): User = User(
    id = this.id,
    firebaseUid = this.firebaseUid,
    email = this.email,
    firstName = this.firstName,
    lastName = this.lastName,
    role = this.role,
    profilePictureUrl = this.profilePictureUrl,
    isActive = this.active,
    lastLoginAt = this.lastLoginAt
)

fun StudentApiResponse.toDomain(): Student = Student(
    id = this.id,
    userId = this.userId,
    rollNumber = this.rollNumber,
    firstName = this.firstName,
    lastName = this.lastName,
    email = this.email,
    phone = this.phone,
    dateOfBirth = "",        // not returned by list endpoint
    enrollmentDate = "",     // not returned by list endpoint
    isActive = this.isActive
)

fun TeacherApiResponse.toDomain(): Teacher = Teacher(
    id = this.id,
    userId = this.userId,
    employeeId = this.employeeId,
    firstName = this.firstName,
    lastName = this.lastName,
    designation = this.designation,
    phone = this.phone,
    isActive = this.isActive
)

fun TimetableSlotApiResponse.toDomain(): TimetableSlot = TimetableSlot(
    id = this.id,
    classSectionId = this.classSectionId,
    subjectId = this.subjectId,
    subjectCode = this.subjectCode,
    subjectName = this.subjectName,
    teacherId = this.teacherId,
    teacherName = this.teacherName,
    dayOfWeek = this.dayOfWeek,
    startTime = this.startTime,
    endTime = this.endTime,
    roomNumber = this.roomNumber
)
