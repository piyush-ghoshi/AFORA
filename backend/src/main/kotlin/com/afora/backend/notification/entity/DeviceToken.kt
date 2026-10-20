package com.afora.backend.notification.entity

import com.afora.backend.user.entity.User
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "device_tokens",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_device_tokens_user_token", columnNames = ["user_id", "fcm_token"])
    ],
    indexes = [
        Index(name = "idx_device_tokens_user_id", columnList = "user_id"),
        Index(name = "idx_device_tokens_token", columnList = "fcm_token")
    ]
)
class DeviceToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "fcm_token", nullable = false, length = 512)
    var fcmToken: String,

    @Column(name = "device_type", nullable = false, length = 50)
    var deviceType: String = "ANDROID",

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
)
