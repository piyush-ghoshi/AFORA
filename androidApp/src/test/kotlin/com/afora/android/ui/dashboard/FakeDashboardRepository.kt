package com.afora.android.ui.dashboard

import com.afora.shared.data.repository.DashboardRepository
import com.afora.shared.data.repository.StudentDashboardStats
import com.afora.shared.data.repository.TeacherDashboardStats
import com.afora.shared.domain.model.LectureSession
import com.afora.shared.domain.model.SessionStatus
import com.afora.shared.domain.model.TimetableSlot
import com.afora.shared.util.Result

/**
 * Fake DashboardRepository for unit tests.
 */
class FakeDashboardRepository : DashboardRepository {

    var studentDashboardResult: Result<StudentDashboardStats> = Result.Success(
        StudentDashboardStats(
            attendancePercentage = 85.5,
            presentCount = 45,
            totalCount = 52,
            upcomingClasses = listOf(
                TimetableSlot(
                    id = 1,
                    classSectionId = 10,
                    subjectId = 101,
                    subjectCode = "CS101",
                    subjectName = "Data Structures",
                    teacherId = 20,
                    teacherName = "Dr. Smith",
                    dayOfWeek = 1, // Monday
                    startTime = "09:00",
                    endTime = "10:30",
                    roomNumber = "A-101"
                )
            ),
            pendingLeaveRequests = 2
        )
    )

    var teacherDashboardResult: Result<TeacherDashboardStats> = Result.Success(
        TeacherDashboardStats(
            todaySchedule = listOf(
                LectureSession(
                    id = 1,
                    subjectId = 101,
                    classSectionId = 10,
                    teacherId = 20,
                    semesterId = 5,
                    date = "2026-09-17",
                    startTime = "09:00:00",
                    endTime = "10:30:00",
                    roomNumber = "A-101",
                    status = SessionStatus.SCHEDULED
                )
            ),
            pendingLeaveRequests = 5,
            pendingQueries = 3
        )
    )

    override suspend fun getStudentDashboard(
        studentId: Long,
        semesterId: Long?
    ): Result<StudentDashboardStats> = studentDashboardResult

    override suspend fun getTeacherDashboard(
        teacherId: Long,
        date: String?
    ): Result<TeacherDashboardStats> = teacherDashboardResult
}
