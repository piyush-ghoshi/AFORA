package com.afora.backend.facerecognition.repository

import com.afora.backend.facerecognition.entity.FaceConsent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for FaceConsent.
 * studentId is the primary key, so existsById/findById/deleteById are the
 * inherited O(1) operations for R1.1/R1.4.
 */
@Repository
interface FaceConsentRepository : JpaRepository<FaceConsent, Long>
