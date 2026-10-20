package com.afora.backend.notification.repository

import com.afora.backend.notification.entity.DeviceToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DeviceTokenRepository : JpaRepository<DeviceToken, Long> {
    fun findByUserId(userId: Long): List<DeviceToken>
    fun findByUserIdAndFcmToken(userId: Long, fcmToken: String): Optional<DeviceToken>
    fun findByFcmToken(fcmToken: String): Optional<DeviceToken>
    fun deleteByFcmToken(fcmToken: String)
}
