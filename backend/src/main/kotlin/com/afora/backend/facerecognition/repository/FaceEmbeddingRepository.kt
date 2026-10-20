package com.afora.backend.facerecognition.repository

import com.afora.backend.facerecognition.entity.FaceEmbedding
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for FaceEmbedding.
 */
@Repository
interface FaceEmbeddingRepository : JpaRepository<FaceEmbedding, Long> {

    fun findByStudentId(studentId: Long): List<FaceEmbedding>

    fun findByStudentIdIn(studentIds: Collection<Long>): List<FaceEmbedding>

    /** R2.4: a registration replaces the prior set via delete-then-insert. */
    fun deleteByStudentId(studentId: Long)

    /** R5.1/R5.3: whether a student holds at least one Face_Embedding. */
    fun existsByStudentId(studentId: Long): Boolean
}
