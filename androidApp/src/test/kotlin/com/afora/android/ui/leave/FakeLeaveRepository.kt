package com.afora.android.ui.leave

import com.afora.shared.data.api.LeaveClassOption
import com.afora.shared.data.api.LeaveRequestResponse
import com.afora.shared.data.repository.LeaveRepository
import com.afora.shared.domain.model.LeaveStatus
import com.afora.shared.util.AppError
import com.afora.shared.util.Result

/**
 * Fake LeaveRepository for unit tests, following the [FakeDashboardRepository]
 * and [com.afora.android.ui.attendance.FakeAttendanceRepository] precedent.
 */
class FakeLeaveRepository : LeaveRepository {

    private fun sampleRequest(id: Long = 1, status: LeaveStatus = LeaveStatus.PENDING) = LeaveRequestResponse(
        id = id,
        studentId = 201,
        studentName = "Alice Johnson",
        rollNumber = "2024CS001",
        classSectionId = 10,
        classSectionName = "CS101 A",
        fromDate = "2026-09-21",
        toDate = "2026-09-21",
        reason = "Medical appointment",
        status = status,
        reviewedByTeacherId = null,
        reviewedByTeacherName = null,
        reviewedAt = null,
        reviewNotes = null,
        createdAt = "2026-09-15T10:00:00",
        updatedAt = "2026-09-15T10:00:00"
    )

    var classOptionsResult: Result<List<LeaveClassOption>> = Result.Success(emptyList())
    var myRequestsResult: Result<List<LeaveRequestResponse>> = Result.Success(listOf(sampleRequest()))
    var pendingForTeacherResult: Result<List<LeaveRequestResponse>> = Result.Success(listOf(sampleRequest()))
    var submitResult: Result<LeaveRequestResponse>? = null
    var approveResult: Result<LeaveRequestResponse>? = null
    var rejectResult: Result<LeaveRequestResponse>? = null
    var cancelResult: Result<LeaveRequestResponse>? = null

    var lastSubmitArgs: List<Any?>? = null
    var lastApproveId: Long? = null
    var lastRejectArgs: Pair<Long, String>? = null
    var lastCancelId: Long? = null

    override suspend fun submitLeaveRequest(
        fromDate: String,
        toDate: String,
        reason: String,
        classSectionId: Long?
    ): Result<LeaveRequestResponse> {
        lastSubmitArgs = listOf(fromDate, toDate, reason, classSectionId)
        return submitResult ?: Result.Success(sampleRequest(id = 99))
    }

    override suspend fun getMyClassOptions(): Result<List<LeaveClassOption>> = classOptionsResult

    override suspend fun getMyLeaveRequests(status: LeaveStatus?): Result<List<LeaveRequestResponse>> =
        myRequestsResult

    override suspend fun getPendingLeaveRequests(teacherId: Long): Result<List<LeaveRequestResponse>> =
        pendingForTeacherResult

    override suspend fun approveLeaveRequest(
        leaveRequestId: Long,
        notes: String?
    ): Result<LeaveRequestResponse> {
        lastApproveId = leaveRequestId
        return approveResult ?: Result.Success(sampleRequest(id = leaveRequestId, status = LeaveStatus.APPROVED))
    }

    override suspend fun rejectLeaveRequest(
        leaveRequestId: Long,
        notes: String
    ): Result<LeaveRequestResponse> {
        lastRejectArgs = leaveRequestId to notes
        return rejectResult ?: Result.Success(sampleRequest(id = leaveRequestId, status = LeaveStatus.REJECTED))
    }

    override suspend fun cancelLeaveRequest(leaveRequestId: Long): Result<LeaveRequestResponse> {
        lastCancelId = leaveRequestId
        return cancelResult ?: Result.Success(sampleRequest(id = leaveRequestId, status = LeaveStatus.CANCELLED))
    }
}