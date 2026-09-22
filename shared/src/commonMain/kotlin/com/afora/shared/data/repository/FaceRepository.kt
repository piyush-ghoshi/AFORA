package com.afora.shared.data.repository

import com.afora.shared.domain.model.FaceEmbedding
import com.afora.shared.domain.model.FaceProfile
import com.afora.shared.util.Result

/**
 * Face recognition data repository interface.
 */
interface FaceRepository {
    /**
     * Get face embeddings for a list of students (used during attendance scanning).
     */
    suspend fun getFaceEmbeddings(studentIds: List<Long>): Result<Map<Long, List<FloatArray>>>
    
    /**
     * Get student's face profile.
     */
    suspend fun getFaceProfile(studentId: Long): Result<FaceProfile>
    
    /**
     * Register/enroll face data for a student.
     * @param images Multiple face images for better recognition
     */
    suspend fun registerFace(
        studentId: Long,
        images: List<ByteArray>
    ): Result<FaceProfile>
    
    /**
     * Re-enroll face data (update existing profile).
     */
    suspend fun updateFaceProfile(
        studentId: Long,
        images: List<ByteArray>
    ): Result<FaceProfile>
    
    /**
     * Delete face profile.
     */
    suspend fun deleteFaceProfile(studentId: Long): Result<Unit>
    
    /**
     * Check if student has active face profile.
     */
    suspend fun hasFaceProfile(studentId: Long): Result<Boolean>
}
