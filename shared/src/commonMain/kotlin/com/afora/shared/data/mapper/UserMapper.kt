package com.afora.shared.data.mapper

import com.afora.shared.data.api.response.*
import com.afora.shared.data.repository.StudentDashboardStats
import com.afora.shared.data.repository.TeacherDashboardStats
import com.afora.shared.domain.model.*

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

fun LectureSessionApiResponse.toDomain(): LectureSession = LectureSession(
    id = this.id,
    subjectId = this.subjectId,
    classSectionId = this.classSectionId,
    teacherId = this.teacherId,
    semesterId = this.semesterId,
    date = this.date,
    startTime = this.startTime,
    endTime = this.endTime,
    roomNumber = this.roomNumber,
    status = SessionStatus.valueOf(this.status),
    attendanceMethod = this.attendanceMethod?.let { AttendanceMethod.valueOf(it) },
    totalStudents = this.totalStudents,
    presentCount = this.presentCount,
    absentCount = this.absentCount,
    onLeaveCount = this.onLeaveCount,
    startedAt = this.startedAt,
    completedAt = this.completedAt,
    notes = this.notes
)

fun StudentDashboardStatsApiResponse.toDomain(): StudentDashboardStats = StudentDashboardStats(
    attendancePercentage = this.attendancePercentage,
    presentCount = this.presentCount,
    totalCount = this.totalCount,
    upcomingClasses = this.upcomingClasses.map { it.toDomain() },
    pendingLeaveRequests = this.pendingLeaveRequests
)

fun TeacherDashboardStatsApiResponse.toDomain(): TeacherDashboardStats = TeacherDashboardStats(
    todaySchedule = this.todaySchedule.map { it.toDomain() },
    pendingLeaveRequests = this.pendingLeaveRequests,
    pendingQueries = this.pendingQueries
)
