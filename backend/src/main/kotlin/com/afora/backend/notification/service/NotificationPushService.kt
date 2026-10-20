package com.afora.backend.notification.service

import com.afora.backend.notification.dto.PushNotificationPayload
import com.afora.backend.notification.entity.DeviceToken
import com.afora.backend.notification.repository.DeviceTokenRepository
import com.afora.backend.user.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationPushService(
    private val deviceTokenRepository: DeviceTokenRepository,
    private val userRepository: UserRepository,
    private val firebaseMessaging: FirebaseMessaging
) {
    private val logger = LoggerFactory.getLogger(NotificationPushService::class.java)

    /**
     * Register or update a user's device FCM token.
     */
    @Transactional
    fun registerToken(firebaseUid: String, token: String, deviceType: String): Boolean {
        val user = userRepository.findByFirebaseUid(firebaseUid).orElse(null)
            ?: run {
                logger.warn("Cannot register FCM token: user with uid $firebaseUid not found")
                return false
            }

        try {
            val existing = deviceTokenRepository.findByUserIdAndFcmToken(user.id!!, token)
            if (existing.isPresent) {
                val dt = existing.get()
                dt.deviceType = deviceType
                deviceTokenRepository.save(dt)
                logger.info("Updated existing FCM token for user ${user.id}")
            } else {
                val dt = DeviceToken(
                    user = user,
                    fcmToken = token,
                    deviceType = deviceType
                )
                deviceTokenRepository.save(dt)
                logger.info("Registered new FCM token for user ${user.id}")
            }
        } catch (_: org.springframework.dao.DataIntegrityViolationException) {
            logger.info("FCM token already registered concurrently for user ${user.id}")
        }
        return true
    }

    /**
     * Send push notification to a specific user across all registered devices.
     */
    fun sendToUser(userId: Long, payload: PushNotificationPayload) {
        val tokens = deviceTokenRepository.findByUserId(userId)
        if (tokens.isEmpty()) {
            logger.info("No FCM tokens registered for user $userId, skipping push")
            return
        }

        tokens.forEach { deviceToken ->
            sendToToken(deviceToken.fcmToken, payload)
        }
    }

    /**
     * Send push notification to a specific FCM token.
     */
    fun sendToToken(token: String, payload: PushNotificationPayload): Boolean {
        try {
            val notification = Notification.builder()
                .setTitle(payload.title)
                .setBody(payload.body)
                .build()

            val messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .putData("channelId", payload.channelId)

            payload.data.forEach { (k, v) ->
                messageBuilder.putData(k, v)
            }

            val response = firebaseMessaging.send(messageBuilder.build())
            logger.info("Successfully sent FCM push notification: $response")
            return true
        } catch (e: FirebaseMessagingException) {
            logger.error("Failed to send FCM message: errorCode=${e.errorCode}, msg=${e.message}")
            if (e.messagingErrorCode != null && e.messagingErrorCode.name == "UNREGISTERED") {
                logger.info("Removing unregistered FCM token: $token")
                try {
                    deviceTokenRepository.deleteByFcmToken(token)
                } catch (ignored: Exception) { }
            }
            return false
        } catch (e: Exception) {
            logger.error("Unexpected error sending FCM message", e)
            return false
        }
    }
}
