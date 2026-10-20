package com.afora.backend.facerecognition.service

import com.afora.backend.academic.repository.EnrollmentRepository
import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.facerecognition.dto.EnrolledEmbeddingEntryDto
import com.afora.backend.facerecognition.dto.EnrolledEmbeddingSetResponseDto
import com.afora.backend.facerecognition.dto.FaceConsentResponseDto
import com.afora.backend.facerecognition.dto.FaceProfileDeletionResponseDto
import com.afora.backend.facerecognition.dto.FaceProfileResponseDto
import com.afora.backend.facerecognition.entity.FaceConsent
import com.afora.backend.facerecognition.entity.FaceEmbedding
import com.afora.backend.facerecognition.entity.FaceProfile
import com.afora.backend.facerecognition.entity.FaceProfileDeletion
import com.afora.backend.facerecognition.entity.PurgeReason
import com.afora.backend.facerecognition.repository.FaceConsentRepository
import com.afora.backend.facerecognition.repository.FaceEmbeddingRepository
import com.afora.backend.facerecognition.repository.FaceProfileDeletionRepository
import com.afora.backend.facerecognition.repository.FaceProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime

/**
 * Owns Face_Consent, Face_Profile and Face_Embedding persistence, distribution and
 * purge (`.kiro/specs/face-recognition/design.md` -> "Components and Interfaces" ->
 * `Face_Profile_Service`).
 *
 * Every timestamp here is derived from the single injected [clock] bean
 * (`common/config/TimeConfig.kt`), reused unchanged -- no second `Clock` bean is
 * introduced, matching Settled Decision A's stated reasoning.
 */
@Service
class FaceProfileService(
    private val faceConsentRepository: FaceConsentRepository,
    private val faceProfileRepository: FaceProfileRepository,
    private val faceEmbeddingRepository: FaceEmbeddingRepository,
    private val faceProfileDeletionRepository: FaceProfileDeletionRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val clock: Clock
) {

    /**
     * R1.3: upserts `face_consents` for [studentId] with the current timestamp from
     * [clock]. `FaceConsent.studentId` is the primary key, so [save][FaceConsentRepository]
     * performs an insert on first consent and an update (new `consentedAt`) on any
     * re-consent, with no separate existence check needed.
     */
    @Transactional
    fun recordConsent(studentId: Long): FaceConsentResponseDto {
        val consentedAt = LocalDateTime.now(clock)
        faceConsentRepository.save(FaceConsent(studentId = studentId, consentedAt = consentedAt))
        return FaceConsentResponseDto(studentId = studentId, consentedAt = consentedAt)
    }

    /**
     * R2.1-R2.8: registers (or replaces) [studentId]'s Face_Profile from [embeddings].
     *
     * Validation order, each traceable to design.md's "`register` flow":
     * 1. No recorded Face_Consent -> [ForbiddenException] (R1.2, R1.4).
     * 2. Zero vectors submitted -> [BadRequestException] (R2.6).
     * 3. Vectors within this one request disagree on dimensionality -> [BadRequestException].
     *    `face_profiles.dimensions` is a single integer, so a Face_Profile cannot carry
     *    two dimensionalities even within one registration request.
     * 4. A dimensionality mismatch against the stored profile's `dimensions` ->
     *    [BadRequestException] (R2.7).
     *
     * On success: the student's existing `face_embeddings` rows are deleted and the
     * newly submitted vectors inserted in their place (R2.4), and `face_profiles` is
     * upserted, preserving the original `registeredAt` on replacement and updating only
     * `updatedAt` (R2.5). The response carries counts only -- the vectors themselves are
     * never returned (R3.4).
     */
    @Transactional
    fun register(studentId: Long, embeddings: List<List<Double>>): FaceProfileResponseDto {
        if (!faceConsentRepository.existsById(studentId)) {
            throw ForbiddenException("Student $studentId holds no recorded Face_Consent")
        }
        if (embeddings.isEmpty()) {
            throw BadRequestException("Face_Profile registration requires at least one Face_Embedding vector")
        }

        val submittedDimensions = embeddings.map { it.size }.distinct()
        if (submittedDimensions.size > 1) {
            throw BadRequestException(
                "Submitted Face_Embedding vectors disagree on dimensionality: $submittedDimensions"
            )
        }
        val dimensions = submittedDimensions.single()

        val existingProfile = faceProfileRepository.findById(studentId).orElse(null)
        if (existingProfile != null && existingProfile.dimensions != dimensions) {
            throw BadRequestException(
                "Submitted dimensionality $dimensions differs from the dimensionality " +
                    "${existingProfile.dimensions} already stored for student $studentId"
            )
        }

        faceEmbeddingRepository.deleteByStudentId(studentId)
        val createdAt = LocalDateTime.now(clock)
        val newRows = embeddings.map { vector ->
            FaceEmbedding(
                studentId = studentId,
                embeddingVector = vector.joinToString(","),
                dimensions = dimensions,
                createdAt = createdAt
            )
        }
        faceEmbeddingRepository.saveAll(newRows)

        val registeredAt = existingProfile?.registeredAt ?: createdAt
        faceProfileRepository.save(
            FaceProfile(
                studentId = studentId,
                dimensions = dimensions,
                registeredAt = registeredAt,
                updatedAt = createdAt
            )
        )

        return FaceProfileResponseDto(
            studentId = studentId,
            embeddingCount = embeddings.size,
            dimensions = dimensions,
            registeredAt = registeredAt
        )
    }

    /**
     * R4.2, R4.3, R4.4, R4.6, R4.7: purges every trace of [studentId]'s biometric data
     * for the given [reason] -- `face_embeddings` deleted, `face_consents` deleted
     * (R4.3), `face_profiles` deleted, and exactly one `face_profile_deletions` row
     * inserted recording the timestamp and triggering reason (R4.4).
     *
     * All four writes happen in this single `@Transactional` method so that a failure
     * partway through rolls every one of them back -- partial deletion cannot occur.
     * Called from the controller for a student's self-delete request (R4.2) and an
     * admin's named-student delete request (R4.7), and from
     * [purgeIfLastActiveEnrollmentEnded] below (R4.1).
     */
    @Transactional
    fun deleteProfile(studentId: Long, reason: PurgeReason): FaceProfileDeletionResponseDto {
        faceEmbeddingRepository.deleteByStudentId(studentId)
        faceConsentRepository.deleteById(studentId)
        faceProfileRepository.deleteById(studentId)

        val deletedAt = LocalDateTime.now(clock)
        faceProfileDeletionRepository.save(
            FaceProfileDeletion(studentId = studentId, reason = reason, deletedAt = deletedAt)
        )

        return FaceProfileDeletionResponseDto(
            studentId = studentId,
            reason = reason.name,
            deletedAt = deletedAt
        )
    }

    /**
     * R5.1-R5.9: assembles the Enrolled_Embedding_Set for [classSectionId].
     *
     * Reuses [EnrollmentRepository.findActiveRosterByClassSectionId] unchanged -- the
     * exact query the sibling attendance-marking feature already added -- rather than
     * duplicating its ACTIVE-only, fetch-joined roster logic (R5.1, R5.2). A student
     * holding zero `face_embeddings` rows is dropped from the result (R5.3), and a
     * Class_Section with no qualifying student returns `entries = []`, never null
     * (R5.8). The three [FaceMatchingThresholds] values are always included, giving the
     * device and the backend's own [FaceMatchReviewGate] one shared source of truth.
     */
    @Transactional(readOnly = true)
    fun enrolledEmbeddingSet(classSectionId: Long): EnrolledEmbeddingSetResponseDto {
        val activeEnrollments = enrollmentRepository.findActiveRosterByClassSectionId(classSectionId)
        val activeStudentIds = activeEnrollments.mapNotNull { it.student?.id }

        val embeddingsByStudentId = if (activeStudentIds.isEmpty()) {
            emptyMap()
        } else {
            faceEmbeddingRepository.findByStudentIdIn(activeStudentIds).groupBy { it.studentId }
        }

        val entries = activeEnrollments.mapNotNull { enrollment ->
            val student = enrollment.student ?: return@mapNotNull null
            val studentId = student.id ?: return@mapNotNull null
            val embeddingRows = embeddingsByStudentId[studentId]
            if (embeddingRows.isNullOrEmpty()) return@mapNotNull null // R5.3

            EnrolledEmbeddingEntryDto(
                studentId = studentId,
                rollNumber = student.rollNumber,
                fullName = student.user.getFullName(),
                embeddings = embeddingRows.map { row -> parseEmbeddingVector(row.embeddingVector) }
            )
        }

        return EnrolledEmbeddingSetResponseDto(
            classSectionId = classSectionId,
            entries = entries,
            highConfidenceThreshold = FaceMatchingThresholds.HIGH_CONFIDENCE,
            mediumConfidenceThreshold = FaceMatchingThresholds.MEDIUM_CONFIDENCE,
            unmatchedFloorThreshold = FaceMatchingThresholds.UNMATCHED_FLOOR
        )
    }

    /**
     * See design.md Settled Decision C in full.
     *
     * R4.1 requires the purge to run "within the same database transaction" as a
     * Student's Enrollment status change resulting in zero remaining ACTIVE Enrollment
     * rows for that student. As of this writing, **no code anywhere in this codebase
     * changes `Enrollment.status`** -- `EnrollmentRepository` is read-only in every
     * production path today, so this method has zero call sites in the current
     * codebase.
     *
     * **Binding contract for future work:** any future feature that writes a non-ACTIVE
     * `Enrollment.status` (e.g. a drop/withdraw/complete flow) MUST call this method
     * inline, with the affected student's id, before its own transaction commits --
     * NOT via an event listener, NOT asynchronously. Because this is a direct method
     * call rather than an event, a future PR that mutates enrollment status without
     * calling this is a visibly missing line in that PR's diff, not a silently
     * unobserved event that never fired. See Settled Decision C for the full reasoning
     * behind rejecting a `@TransactionalEventListener`-based alternative.
     *
     * Behavior: a no-op when at least one ACTIVE enrollment remains for [studentId]
     * (R4.5); otherwise delegates to [deleteProfile] with [PurgeReason.ENROLLMENT_ENDED].
     */
    @Transactional
    fun purgeIfLastActiveEnrollmentEnded(studentId: Long) {
        val remainingActive = enrollmentRepository.countActiveEnrollmentsByStudentId(studentId)
        if (remainingActive == 0L) {
            deleteProfile(studentId, PurgeReason.ENROLLMENT_ENDED)
        }
    }

    /** Parses `face_embeddings.embedding_vector`'s comma-delimited TEXT back into doubles. */
    private fun parseEmbeddingVector(embeddingVector: String): List<Double> =
        embeddingVector.split(",").map { it.toDouble() }
}
