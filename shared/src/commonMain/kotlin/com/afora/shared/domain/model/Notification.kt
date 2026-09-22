package com.afora.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Notification entity.
 */
@Serializable
data class Notification(
    val id: Long,
    val userId: Long,
    val title: String,
    val message: String,
    val notificationType: NotificationType,
    val relatedEntityType: String? = null,  // e.g., "lecture_session", "leave_request"
    val relatedEntityId: Long? = null,
    val isRead: Boolean = false,
    val readAt: String? = null,  // ISO 8601 datetime
    val sentAt: String  // ISO 8601 datetime
)

/**
 * Notification type.
 */
@Serializable
enum class NotificationType {
    ATTENDANCE_MARKED,
    LEAVE_APPROVED,
    LEAVE_REJECTED,
    QUERY_APPROVED,
    QUERY_REJECTED,
    LOW_ATTENDANCE,
    LECTURE_SCHEDULED,
    LECTURE_CANCELLED,
    SYSTEM
}
