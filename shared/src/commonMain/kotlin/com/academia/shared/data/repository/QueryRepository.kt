package com.academia.shared.data.repository

import com.academia.shared.domain.model.AttendanceQuery
import com.academia.shared.domain.model.AttendanceStatus
import com.academia.shared.domain.model.QueryStatus
import com.academia.shared.util.Result

/**
 * Attendance query/dispute repository interface.
 */
interface QueryRepository {
    /**
     * Raise an attendance query.
     */
    suspend fun raiseAttendanceQuery(
        attendanceRecordId: Long,
        requestedStatus: AttendanceStatus,
        reason: String,
        evidenceData: ByteArray? = null
    ): Result<AttendanceQuery>
    
    /**
     * Get query by ID.
     */
    suspend fun getQuery(queryId: Long): Result<AttendanceQuery>
    
    /**
     * Get student's queries.
     */
    suspend fun getStudentQueries(
        studentId: Long,
        status: QueryStatus? = null
    ): Result<List<AttendanceQuery>>
    
    /**
     * Get pending queries for teacher review.
     */
    suspend fun getPendingQueries(
        teacherId: Long,
        subjectId: Long? = null
    ): Result<List<AttendanceQuery>>
    
    /**
     * Approve attendance query.
     */
    suspend fun approveQuery(
        queryId: Long,
        comments: String? = null
    ): Result<AttendanceQuery>
    
    /**
     * Reject attendance query.
     */
    suspend fun rejectQuery(
        queryId: Long,
        comments: String
    ): Result<AttendanceQuery>
}
