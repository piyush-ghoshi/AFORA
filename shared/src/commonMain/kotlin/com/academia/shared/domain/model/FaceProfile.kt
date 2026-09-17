package com.academia.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Face profile entity for face recognition.
 */
@Serializable
data class FaceProfile(
    val id: Long,
    val studentId: Long,
    val enrollmentDate: String,  // ISO 8601 date
    val totalImages: Int = 0,
    val status: FaceProfileStatus = FaceProfileStatus.PENDING,
    val lastUpdatedAt: String  // ISO 8601 datetime
)

/**
 * Face profile status.
 */
@Serializable
enum class FaceProfileStatus {
    PENDING,   // Not yet enrolled
    ACTIVE,    // Enrolled and active
    INACTIVE,  // Temporarily disabled
    EXPIRED    // Needs re-enrollment
}

/**
 * Face embedding entity (vector representation).
 */
@Serializable
data class FaceEmbedding(
    val id: Long,
    val faceProfileId: Long,
    val embedding: List<Float>,  // 512-dimensional vector (model-dependent)
    val modelName: String,
    val modelVersion: String,
    val embeddingDate: String  // ISO 8601 datetime
)
