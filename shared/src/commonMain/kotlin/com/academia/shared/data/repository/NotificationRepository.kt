package com.academia.shared.data.repository

import com.academia.shared.domain.model.Notification
import com.academia.shared.domain.model.NotificationType
import com.academia.shared.util.Result

/**
 * Notification repository interface.
 */
interface NotificationRepository {
    /**
     * Get user's notifications.
     */
    suspend fun getNotifications(
        userId: Long,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Notification>>
    
    /**
     * Get unread notifications count.
     */
    suspend fun getUnreadCount(userId: Long): Result<Int>
    
    /**
     * Mark notification as read.
     */
    suspend fun markAsRead(notificationId: Long): Result<Unit>
    
    /**
     * Mark all notifications as read.
     */
    suspend fun markAllAsRead(userId: Long): Result<Unit>
    
    /**
     * Delete notification.
     */
    suspend fun deleteNotification(notificationId: Long): Result<Unit>
    
    /**
     * Get notifications by type.
     */
    suspend fun getNotificationsByType(
        userId: Long,
        type: NotificationType,
        limit: Int = 50
    ): Result<List<Notification>>
}
