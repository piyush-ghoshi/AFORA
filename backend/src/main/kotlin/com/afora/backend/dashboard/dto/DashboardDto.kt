package com.afora.backend.dashboard.dto

import java.time.LocalDate
import java.time.LocalTime

// ── Student Dashboard ─────────────────────────────────────────────────────────

/**
 * Student dashboard statistics response.
 * 
 * Returned by GET /api/dashboard/student/{studentId}
 */
data class StudentDashboardStatsResponse(
    val attendancePercentage: Double,
    val presentCount: Int,
    val totalCount: Int,
    val upcomingClasses: List<TimetableSlotResponse>,
    val pendingLeaveRequests: Int
)

/**
 * Timetable slot for dashboard display.
 */
data class TimetableSlotResponse(
    val id: Long,
    val classSectionId: Long,
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val teacherId: Long,
    val teacherName: String,
    val dayOfWeek: Int,      // 1=Monday, 7=Sunday
    val startTime: String,   // "HH:mm"
    val endTime: String,     // "HH:mm"
    val roomNumber: String?
)

// ── Teacher Dashboard ─────────────────────────────────────────────────────────

/**
 * Teacher dashboard statistics response.
 * 
 * Returned by GET /api/dashboard/teacher/{teacherId}
 */
data class TeacherDashboardStatsResponse(
    val todaySchedule: List<LectureSessionResponse>,
    val pendingLeaveRequests: Int,
    val pendingQueries: Int
)

/**
 * Lecture session for dashboard display.
 */
data class LectureSessionResponse(
    val id: Long,
    val subjectId: Long,
    val classSectionId: Long,
    val teacherId: Long,
    val semesterId: Long,
    val date: LocalDate,
    val startTime: String,   // "HH:mm:ss"
    val endTime: String,     // "HH:mm:ss"
    val roomNumber: String?,
    val status: String,
    val attendanceMethod: String?,
    val totalStudents: Int?,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val startedAt: String?,
    val completedAt: String?,
    val notes: String?
)
