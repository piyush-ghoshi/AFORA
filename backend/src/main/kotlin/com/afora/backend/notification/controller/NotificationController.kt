package com.afora.backend.notification.controller

import com.afora.backend.common.dto.ApiResponse
import com.afora.backend.notification.dto.RegisterFcmTokenRequest
import com.afora.backend.notification.dto.RegisterFcmTokenResponse
import com.afora.backend.notification.service.NotificationPushService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notifications")
class NotificationController(
    private val notificationPushService: NotificationPushService
) {

    @PostMapping("/fcm-token")
    fun registerToken(
        @AuthenticationPrincipal firebaseUid: String?,
        @RequestBody request: RegisterFcmTokenRequest
    ): ResponseEntity<ApiResponse<RegisterFcmTokenResponse>> {
        if (firebaseUid == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication required"))
        }

        val success = notificationPushService.registerToken(
            firebaseUid = firebaseUid,
            token = request.token,
            deviceType = request.deviceType
        )

        return if (success) {
            ResponseEntity.ok(
                ApiResponse.success(
                    RegisterFcmTokenResponse(
                        success = true,
                        message = "FCM token registered successfully"
                    )
                )
            )
        } else {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Failed to register FCM token"))
        }
    }
}
