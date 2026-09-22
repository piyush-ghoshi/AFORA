package com.afora.shared.data.repository

import com.afora.shared.domain.model.LectureSession
import com.afora.shared.domain.model.TimetableSlot
import com.afora.shared.util.Result

/**
 * Dashboard-specific repository for fetching aggregated data.
 * 
 * Phase A5: Provides dashboard stats for students and teachers.
 */
interface DashboardRepository {

    /**
     * Get student dashboard data.
     * 
     * @param studentId Student ID
     * @param semesterId Current semester ID (optional, backend uses current if null)
     * @return StudentDashboardStats containing attendance, timetable, and leave info
     */
    suspend fun getStudentDashboard(
        studentId: Long,
        semesterId: Long? = null
    ): Result<StudentDashboardStats>

    /**
     * Get teacher dashboard data.
     * 
     * @param teacherId Teacher ID
     * @param date Date for schedule (YYYY-MM-DD), defaults to today
     * @return TeacherDashboardStats containing today's schedule and pending counts
     */
    suspend fun getTeacherDashboard(
        teacherId: Long,
        date: String? = null
    ): Result<TeacherDashboardStats>
}

/**
 * Student dashboard statistics.
 */
data class StudentDashboardStats(
    val attendancePercentage: Double,
    val presentCount: Int,
    val totalCount: Int,
    val upcomingClasses: List<TimetableSlot>,
    val pendingLeaveRequests: Int
)

/**
 * Teacher dashboard statistics.
 */
data class TeacherDashboardStats(
    val todaySchedule: List<LectureSession>,
    val pendingLeaveRequests: Int,
    val pendingQueries: Int
)
