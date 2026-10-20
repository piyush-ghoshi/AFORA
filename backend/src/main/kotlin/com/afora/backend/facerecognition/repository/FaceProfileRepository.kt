package com.afora.backend.facerecognition.repository

import com.afora.backend.facerecognition.entity.FaceProfile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for FaceProfile.
 * studentId is the primary key, so existsById/findById/deleteById are the
 * inherited O(1) operations backing R2.4/R2.5/R2.7.
 */
@Repository
interface FaceProfileRepository : JpaRepository<FaceProfile, Long>
