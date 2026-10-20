package com.afora.shared.data.repository

import com.afora.shared.data.api.EnrolledEmbeddingSetResponse
import com.afora.shared.data.api.FaceConsentResponse
import com.afora.shared.data.api.FaceProfileDeletionResponse
import com.afora.shared.data.api.FaceProfileResponse
import com.afora.shared.util.Result

/**
 * `commonMain` repository interface for the Face_Recognition feature
 * (`.kiro/specs/face-recognition/design.md` → "Components and Interfaces").
 *
 * Mirrors the shape of [AttendanceRepository]: every member is `suspend`
 * and returns [com.afora.shared.util.Result] carrying the wire response
 * type on success or an [com.afora.shared.util.AppError] on failure
 * (Binding Convention 8). Implemented by `HttpFaceProfileRepository` in
 * `androidMain`.
 */
interface FaceProfileRepository {

    /**
     * Record Face_Consent for the calling student before any face capture
     * or embedding generation (R1.3, R13.2).
     */
    suspend fun recordConsent(): Result<FaceConsentResponse>

    /**
     * Register a Face_Profile for the calling student from one or more
     * on-device-generated Face_Embedding vectors (R2.1, R13.3).
     */
    suspend fun registerFaceProfile(embeddings: List<List<Double>>): Result<FaceProfileResponse>

    /**
     * Delete a student's Face_Profile (self-request R4.2 or an admin-named
     * request R4.7).
     */
    suspend fun deleteFaceProfile(studentId: Long): Result<FaceProfileDeletionResponse>

    /**
     * Fetch the Enrolled_Embedding_Set for a Class_Section, distributed to
     * the requesting, authorized teacher device (R5.9).
     */
    suspend fun getEnrolledEmbeddingSet(classSectionId: Long): Result<EnrolledEmbeddingSetResponse>
}
