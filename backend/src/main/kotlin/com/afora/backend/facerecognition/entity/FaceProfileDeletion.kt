package com.afora.backend.facerecognition.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * Append-only record of every Face_Profile purge (R4.4). Never updated,
 * never deleted. The only durable trace that a student's biometric data
 * existed, once face_consents and face_embeddings rows for that student are
 * gone.
 *
 * updatable = false on every column except the generated id mirrors the
 * sibling attendance-marking AttendanceAuditEntry's own append-only shape.
 */
@Entity
@Table(name = "face_profile_deletions")
data class FaceProfileDeletion(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "student_id", nullable = false, updatable = false)
    val studentId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 30)
    val reason: PurgeReason = PurgeReason.STUDENT_REQUEST,

    @Column(name = "deleted_at", nullable = false, updatable = false)
    val deletedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * The triggering cause of a Face_Profile purge (R4.4), matching the
 * `face_profile_deletions.reason` CHECK constraint's three allowed values
 * exactly. Declared here, alongside the entity that references it, since
 * Face_Profile_Service (which also uses this type per design.md) does not
 * exist yet.
 */
enum class PurgeReason {
    STUDENT_REQUEST,
    ADMIN_REQUEST,
    ENROLLMENT_ENDED
}
