package com.academia.shared.data.repository

import com.academia.shared.domain.model.LeaveRequest
import com.academia.shared.domain.model.LeaveStatus
import com.academia.shared.util.Result

/**
 * Leave management repository interface.
 */
interface LeaveRepository {
    /**
     * Submit a new leave request.
     */
    suspend fun submitLeaveRequest(
        studentId: Long,
        startDate: String,
        endDate: String,
        reason: String,
        leaveType: com.academia.shared.domain.model.LeaveType,
        subjectIds: List<Long>? = null,
        attachmentData: ByteArray? = null
    ): Result<LeaveRequest>
    
    /**
     * Get leave request by ID.
     */
    suspend fun getLeaveRequest(leaveRequestId: Long): Result<LeaveRequest>
    
    /**
     * Get student's leave requests.
     */
    suspend fun getStudentLeaveRequests(
        studentId: Long,
        status: LeaveStatus? = null
    ): Result<List<LeaveRequest>>
    
    /**
     * Get pending leave requests for teacher approval.
     */
    suspend fun getPendingLeaveRequests(
        teacherId: Long,
        subjectId: Long? = null
    ): Result<List<LeaveRequest>>
    
    /**
     * Approve leave request.
     */
    suspend fun approveLeaveRequest(
        leaveRequestId: Long,
        comments: String? = null
    ): Result<LeaveRequest>
    
    /**
     * Reject leave request.
     */
    suspend fun rejectLeaveRequest(
        leaveRequestId: Long,
        comments: String
    ): Result<LeaveRequest>
    
    /**
     * Cancel leave request (by student).
     */
    suspend fun cancelLeaveRequest(leaveRequestId: Long): Result<LeaveRequest>
}
