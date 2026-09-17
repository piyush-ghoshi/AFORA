package com.academia.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Leave request entity.
 */
@Serializable
data class LeaveRequest(
    val id: Long,
    val studentId: Long,
    val startDate: String,  // ISO 8601 date
    val endDate: String,  // ISO 8601 date
    val reason: String,
    val leaveType: LeaveType,
    val attachmentUrl: String? = null,
    val status: LeaveStatus = LeaveStatus.PENDING,
    val approvedByUserId: Long? = null,
    val approvalDate: String? = null,  // ISO 8601 datetime
    val approvalComments: String? = null,
    val requestedAt: String  // ISO 8601 datetime
)

/**
 * Leave request type.
 */
@Serializable
enum class LeaveType {
    MEDICAL,
    PERSONAL,
    EMERGENCY,
    OTHER
}

/**
 * Leave request status.
 */
@Serializable
enum class LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}
