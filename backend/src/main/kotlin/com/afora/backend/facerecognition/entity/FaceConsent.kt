package com.afora.backend.facerecognition.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * One row per student: the single most recent Face_Consent (R1.3).
 *
 * The primary key is studentId itself, not a generated surrogate id, so
 * existence of a row is an O(1) lookup for R1.1/R1.4 with no extra index.
 * Deleted whenever face_embeddings rows are purged under Requirement 4
 * (R4.3), so re-registration after deletion requires consenting again
 * (R1.1, R1.5).
 */
@Entity
@Table(name = "face_consents")
data class FaceConsent(
    @Id
    @Column(name = "student_id")
    val studentId: Long = 0,

    @Column(name = "consented_at", nullable = false)
    val consentedAt: LocalDateTime = LocalDateTime.now()
)
