package com.afora.android.ui.attendance

import com.afora.shared.data.api.AmendAttendanceRequest
import com.afora.shared.data.api.AmendmentResultResponse
import com.afora.shared.data.api.AttendanceConfirmationResponse
import com.afora.shared.data.api.AuditHistoryResponse
import com.afora.shared.data.api.ConfirmAttendanceRequest
import com.afora.shared.data.api.RosterEntry
import com.afora.shared.data.api.RosterResponse
import com.afora.shared.data.api.SessionSummary
import com.afora.shared.data.api.StudentAttendanceResponse
import com.afora.shared.data.repository.AttendanceRepository
import com.afora.shared.data.repository.AttendanceStats
import com.afora.shared.data.repository.LectureSessionWithStudents
import com.afora.shared.data.repository.StudentWithEmbedding
import com.afora.shared.domain.model.AttendanceRecord
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.LectureSession
import com.afora.shared.domain.model.SessionStatus
import com.afora.shared.util.AppError
import com.afora.shared.util.Result

/**
 * Fake AttendanceRepository for unit tests, following the existing
 * [com.afora.android.ui.dashboard.FakeDashboardRepository] precedent.
 *
 * A fake is easier to drive from a generator across many iterations than a
 * mock requiring a stubbing per case, which is why the view-model tests use
 * this rather than MockK.
 */
class FakeAttendanceRepository : AttendanceRepository {

    var rosterResult: Result<RosterResponse> = Result.Success(
        RosterResponse(
            sessionId = 1,
            classSectionId = 10,
            subjectId = 101,
            subjectCode = "CS101",
            subjectName = "Data Structures",
            sessionDate = "2026-09-17",
            startTime = "09:00",
            endTime = "10:30",
            sessionStatus = SessionStatus.ACTIVE,
            attendanceMethod = null,
            totalStudents = 2,
            presentCount = 0,
            absentCount = 0,
            onLeaveCount = 0,
            amendmentWindowOpen = false,
            entries = listOf(
                RosterEntry(
                    studentId = 201,
                    rollNumber = "2024CS001",
                    fullName = "Alice Johnson",
                    storedStatus = null,
                    suggestedStatus = AttendanceStatus.PRESENT,
                    isCorrected = false
                ),
                RosterEntry(
                    studentId = 202,
                    rollNumber = "2024CS002",
                    fullName = "Bob Williams",
                    storedStatus = null,
                    suggestedStatus = AttendanceStatus.ON_LEAVE,
                    leaveRequestId = 5,
                    leaveReason = "Medical appointment",
                    isCorrected = false
                )
            )
        )
    )

    var activateResult: Result<SessionSummary>? = null
    var cancelResult: Result<SessionSummary>? = null
    var confirmResult: Result<AttendanceConfirmationResponse>? = null
    var amendResult: Result<AmendmentResultResponse>? = null
    var auditHistoryResult: Result<AuditHistoryResponse>? = null
    var studentAttendanceDetailResult: Result<StudentAttendanceResponse>? = null

    var lastConfirmRequest: ConfirmAttendanceRequest? = null
    var lastAmendRequest: AmendAttendanceRequest? = null

    override suspend fun getSessionRoster(lectureSessionId: Long): Result<RosterResponse> = rosterResult

    override suspend fun activateSession(lectureSessionId: Long): Result<SessionSummary> =
        activateResult ?: Result.Error(AppError.UnknownError("activateResult not configured"))

    override suspend fun cancelSession(lectureSessionId: Long): Result<SessionSummary> =
        cancelResult ?: Result.Error(AppError.UnknownError("cancelResult not configured"))

    override suspend fun confirmManualAttendance(
        lectureSessionId: Long,
        request: ConfirmAttendanceRequest
    ): Result<AttendanceConfirmationResponse> {
        lastConfirmRequest = request
        return confirmResult ?: Result.Error(AppError.UnknownError("confirmResult not configured"))
    }

    override suspend fun amendAttendance(
        lectureSessionId: Long,
        request: AmendAttendanceRequest
    ): Result<AmendmentResultResponse> {
        lastAmendRequest = request
        return amendResult ?: Result.Error(AppError.UnknownError("amendResult not configured"))
    }

    override suspend fun getAuditHistory(lectureSessionId: Long): Result<AuditHistoryResponse> =
        auditHistoryResult ?: Result.Error(AppError.UnknownError("auditHistoryResult not configured"))

    override suspend fun getStudentAttendance(
        studentId: Long,
        subjectId: Long,
        semesterId: String
    ): Result<List<AttendanceRecord>> = Result.Error(AppError.UnknownError("getStudentAttendance not configured"))

    override suspend fun getStudentAttendanceStats(
        studentId: Long,
        semesterId: String
    ): Result<AttendanceStats> = Result.Error(AppError.UnknownError("getStudentAttendanceStats not configured"))

    override suspend fun getStudentAttendanceDetail(
        studentId: Long,
        semesterId: String?
    ): Result<StudentAttendanceResponse> =
        studentAttendanceDetailResult ?: Result.Error(AppError.UnknownError("studentAttendanceDetailResult not configured"))

    override suspend fun startLectureSession(lectureId: Long): Result<LectureSessionWithStudents> =
        Result.Error(AppError.UnknownError("startLectureSession not configured"))

    override suspend fun getEnrolledStudentsWithEmbeddings(
        lectureSessionId: Long
    ): Result<List<StudentWithEmbedding>> =
        Result.Error(AppError.UnknownError("getEnrolledStudentsWithEmbeddings not configured"))

    override suspend fun getAttendanceByLectureSession(
        lectureSessionId: Long
    ): Result<List<AttendanceRecord>> =
        Result.Error(AppError.UnknownError("getAttendanceByLectureSession not configured"))

    override suspend fun getLectureSession(lectureSessionId: Long): Result<LectureSession> =
        Result.Error(AppError.UnknownError("getLectureSession not configured"))

    override suspend fun cancelLectureSession(lectureSessionId: Long): Result<Unit> =
        Result.Error(AppError.UnknownError("cancelLectureSession not configured"))
}