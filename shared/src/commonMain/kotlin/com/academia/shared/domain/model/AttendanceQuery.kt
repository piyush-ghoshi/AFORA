package com.academia.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Attendance query/dispute entity.
 */
@Serializable
data class AttendanceQuery(
    val id: Long,
    val attendanceRecordId: Long,
    val studentId: Long,
    val currentStatus: AttendanceStatus,
    val requestedStatus: AttendanceStatus,
    val reason: String,
    val evidenceUrl: String? = null,
    val status: QueryStatus = QueryStatus.PENDING,
    val reviewedByUserId: Long? = null,
    val reviewDate: String? = null,  // ISO 8601 datetime
    val reviewComments: String? = null,
    val raisedAt: String  // ISO 8601 datetime
)

/**
 * Query status.
 */
@Serializable
enum class QueryStatus {
    PENDING,
    APPROVED,
    REJECTED
}
