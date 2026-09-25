package com.afora.backend.dashboard.service

import com.afora.backend.dashboard.dto.LectureSessionResponse
import com.afora.backend.dashboard.dto.StudentDashboardStatsResponse
import com.afora.backend.dashboard.dto.TeacherDashboardStatsResponse
import com.afora.backend.dashboard.dto.TimetableSlotResponse
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Dashboard service for aggregating data from multiple sources.
 * 
 * Phase A6: Mock implementation with dummy data.
 * Future phases will integrate with:
 * - AttendanceRepository (for attendance stats)
 * - TimetableRepository (for schedule data)
 * - LeaveRepository (for pending leave requests)
 * - QueryRepository (for pending queries)
 */
@Service
class DashboardService {

    /**
     * Get student dashboard statistics.
     * 
     * @param studentId Student ID
     * @param semesterId Optional semester ID (defaults to current semester)
     * @return Dashboard statistics including attendance, timetable, and leave requests
     */
    fun getStudentDashboard(studentId: Long, semesterId: Long?): StudentDashboardStatsResponse {
        // Phase A6: Mock data
        // TODO: Phase A7 - Integrate with real repositories
        
        return StudentDashboardStatsResponse(
            attendancePercentage = 85.5,
            presentCount = 45,
            totalCount = 52,
            upcomingClasses = getUpcomingClassesMock(studentId),
            pendingLeaveRequests = 2
        )
    }

    /**
     * Get teacher dashboard statistics.
     * 
     * @param teacherId Teacher ID
     * @param date Optional date for schedule (defaults to today)
     * @return Dashboard statistics including today's schedule and pending counts
     */
    fun getTeacherDashboard(teacherId: Long, date: LocalDate?): TeacherDashboardStatsResponse {
        // Phase A6: Mock data
        // TODO: Phase A7 - Integrate with real repositories
        
        val targetDate = date ?: LocalDate.now()
        
        return TeacherDashboardStatsResponse(
            todaySchedule = getTodayScheduleMock(teacherId, targetDate),
            pendingLeaveRequests = 5,
            pendingQueries = 3
        )
    }

    // ── Mock Data Helpers ─────────────────────────────────────────────────────

    private fun getUpcomingClassesMock(studentId: Long): List<TimetableSlotResponse> {
        val today = LocalDate.now()
        val currentDayOfWeek = today.dayOfWeek.value // 1=Monday, 7=Sunday
        
        return listOf(
            TimetableSlotResponse(
                id = 1,
                classSectionId = 10,
                subjectId = 101,
                subjectCode = "CS101",
                subjectName = "Data Structures",
                teacherId = 20,
                teacherName = "Dr. Smith",
                dayOfWeek = currentDayOfWeek,
                startTime = "09:00",
                endTime = "10:30",
                roomNumber = "A-101"
            ),
            TimetableSlotResponse(
                id = 2,
                classSectionId = 10,
                subjectId = 102,
                subjectCode = "CS102",
                subjectName = "Algorithms",
                teacherId = 21,
                teacherName = "Prof. Johnson",
                dayOfWeek = currentDayOfWeek,
                startTime = "11:00",
                endTime = "12:30",
                roomNumber = "B-205"
            ),
            TimetableSlotResponse(
                id = 3,
                classSectionId = 10,
                subjectId = 103,
                subjectCode = "CS103",
                subjectName = "Database Systems",
                teacherId = 22,
                teacherName = "Dr. Williams",
                dayOfWeek = (currentDayOfWeek % 7) + 1, // Next day
                startTime = "14:00",
                endTime = "15:30",
                roomNumber = "C-301"
            )
        )
    }

    private fun getTodayScheduleMock(teacherId: Long, date: LocalDate): List<LectureSessionResponse> {
        return listOf(
            LectureSessionResponse(
                id = 1,
                subjectId = 101,
                classSectionId = 10,
                teacherId = teacherId,
                semesterId = 5,
                date = date,
                startTime = "09:00:00",
                endTime = "10:30:00",
                roomNumber = "A-101",
                status = "SCHEDULED",
                attendanceMethod = null,
                totalStudents = 60,
                presentCount = 0,
                absentCount = 0,
                onLeaveCount = 0,
                startedAt = null,
                completedAt = null,
                notes = null
            ),
            LectureSessionResponse(
                id = 2,
                subjectId = 102,
                classSectionId = 11,
                teacherId = teacherId,
                semesterId = 5,
                date = date,
                startTime = "11:00:00",
                endTime = "12:30:00",
                roomNumber = "B-205",
                status = "SCHEDULED",
                attendanceMethod = null,
                totalStudents = 55,
                presentCount = 0,
                absentCount = 0,
                onLeaveCount = 0,
                startedAt = null,
                completedAt = null,
                notes = null
            ),
            LectureSessionResponse(
                id = 3,
                subjectId = 103,
                classSectionId = 12,
                teacherId = teacherId,
                semesterId = 5,
                date = date,
                startTime = "14:00:00",
                endTime = "15:30:00",
                roomNumber = "C-301",
                status = "SCHEDULED",
                attendanceMethod = null,
                totalStudents = 58,
                presentCount = 0,
                absentCount = 0,
                onLeaveCount = 0,
                startedAt = null,
                completedAt = null,
                notes = null
            )
        )
    }
}
