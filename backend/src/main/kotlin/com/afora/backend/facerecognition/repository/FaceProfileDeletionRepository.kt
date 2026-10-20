package com.afora.backend.facerecognition.repository

import com.afora.backend.facerecognition.entity.FaceProfileDeletion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for FaceProfileDeletion. Append-only (R4.4): no update or
 * delete-by-criteria method is exposed beyond what JpaRepository provides.
 */
@Repository
interface FaceProfileDeletionRepository : JpaRepository<FaceProfileDeletion, Long> {

    fun findByStudentId(studentId: Long): List<FaceProfileDeletion>
}
