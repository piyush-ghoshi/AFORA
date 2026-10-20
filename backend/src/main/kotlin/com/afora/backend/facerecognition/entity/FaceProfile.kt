package com.afora.backend.facerecognition.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * One row per student holding a registered Face_Profile.
 *
 * The primary key is studentId itself, not a generated surrogate id, so
 * "does a profile already exist" (R2.4) and the registration timestamp
 * (R2.5) are O(1) lookups that do not scan face_embeddings. dimensions
 * caches the vector length so R2.7's cross-request dimensionality check
 * never has to parse an embedding_vector.
 */
@Entity
@Table(name = "face_profiles")
data class FaceProfile(
    @Id
    @Column(name = "student_id")
    val studentId: Long = 0,

    @Column(nullable = false)
    val dimensions: Int = 0,

    @Column(name = "registered_at", nullable = false)
    val registeredAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
