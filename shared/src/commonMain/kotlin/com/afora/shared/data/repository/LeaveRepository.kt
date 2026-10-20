package com.afora.shared.data.repository

import com.afora.shared.data.api.LeaveClassOption
import com.afora.shared.data.api.LeaveRequestResponse
import com.afora.shared.domain.model.LeaveStatus
import com.afora.shared.util.Result

/**
 * Leave Requests repository interface.
 *
 * Corrected to match the backend's actual `leave_requests` persistence
 * (`fromDate`/`toDate`, no `leaveType`, no `attachmentUrl`) -- see
 * [com.afora.shared.data.api.LeaveRequestResponse] for the wire shape and
 * `backend/.../attendance/dto/LeaveDto.kt` for the backend side.
 */
interface LeaveRepository {

    /**
     * Submit a new leave request. [classSectionId] is optional; a null value
     * spans all of the student's classes.
     */
    suspend fun submitLeaveRequest(
        fromDate: String,
        toDate: String,
        reason: String,
        classSectionId: Long? = null
    ): Result<LeaveRequestResponse>

    /** The classes the authenticated student is actively enrolled in, for the scope picker. */
    suspend fun getMyClassOptions(): Result<List<LeaveClassOption>>

    /** The authenticated student's own leave requests, optionally filtered to one status. */
    suspend fun getMyLeaveRequests(status: LeaveStatus? = null): Result<List<LeaveRequestResponse>>

    /** A teacher's PENDING review queue. */
    suspend fun getPendingLeaveRequests(teacherId: Long): Result<List<LeaveRequestResponse>>

    /** Approve a PENDING leave request. */
    suspend fun approveLeaveRequest(
        leaveRequestId: Long,
        notes: String? = null
    ): Result<LeaveRequestResponse>

    /** Reject a PENDING leave request. [notes] is required. */
    suspend fun rejectLeaveRequest(
        leaveRequestId: Long,
        notes: String
    ): Result<LeaveRequestResponse>

    /** Cancel the caller's own PENDING leave request. */
    suspend fun cancelLeaveRequest(leaveRequestId: Long): Result<LeaveRequestResponse>
}