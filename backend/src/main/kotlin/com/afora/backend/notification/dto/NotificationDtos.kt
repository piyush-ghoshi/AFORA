package com.afora.backend.notification.dto

data class RegisterFcmTokenRequest(
    val token: String,
    val deviceType: String = "ANDROID"
)

data class RegisterFcmTokenResponse(
    val success: Boolean,
    val message: String
)

data class PushNotificationPayload(
    val title: String,
    val body: String,
    val channelId: String = "channel_general",
    val data: Map<String, String> = emptyMap()
)
