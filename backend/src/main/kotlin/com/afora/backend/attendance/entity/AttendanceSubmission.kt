package com.afora.backend.attendance.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * A persisted confirmation submission, keyed on the client-generated
 * submission identifier so a duplicate insert fails at the database
 * level (R6.12) and a matching retry can be replayed verbatim (R6.5).
 *
 * Append-only: every column is updatable = false.
 * Foreign keys are scalar Long columns rather than @ManyToOne because this
 * entity is written once and read as a flat projection, never navigated.
 */
@Entity
@Table(name = "attendance_submissions")
data class AttendanceSubmission(
    @Id
    @Column(name = "submission_id", nullable = false, updatable = false)
    val submissionId: UUID = UUID.randomUUID(),

    @Column(name = "lecture_session_id", nullable = false, updatable = false)
    val lectureSessionId: Long = 0,

    @Column(name = "acting_user_id", nullable = false, updatable = false)
    val actingUserId: Long = 0,

    @Column(name = "request_fingerprint", nullable = false, updatable = false, length = 64)
    val requestFingerprint: String = "",

    @Column(name = "confirmation_json", nullable = false, updatable = false, columnDefinition = "TEXT")
    val confirmationJson: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)