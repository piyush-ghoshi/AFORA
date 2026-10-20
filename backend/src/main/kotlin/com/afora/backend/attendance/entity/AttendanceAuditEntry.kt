package com.afora.backend.attendance.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * One recorded attendance amendment (R9.6).
 *
 * Append-only and never purged (R9.15); updatable = false on every column is
 * the JPA-level expression of that, so Hibernate will not emit an UPDATE even
 * if a caller tries.
 *
 * lectureSessionId and studentId are denormalised so the audit-history query
 * (R9.16) and the isCorrected derivation (R9.14) need no entity join.
 *
 * The chk_audit_status_changed database constraint guarantees
 * previousStatus != newStatus, which is R9.13 enforced below the service.
 */
@Entity
@Table(name = "attendance_audit_entries")
data class AttendanceAuditEntry(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "attendance_record_id", nullable = false, updatable = false)
    val attendanceRecordId: Long = 0,

    @Column(name = "lecture_session_id", nullable = false, updatable = false)
    val lectureSessionId: Long = 0,

    @Column(name = "student_id", nullable = false, updatable = false)
    val studentId: Long = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, updatable = false, length = 20)
    val previousStatus: AttendanceStatus = AttendanceStatus.ABSENT,

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, updatable = false, length = 20)
    val newStatus: AttendanceStatus = AttendanceStatus.PRESENT,

    @Column(name = "changed_by_user_id", nullable = false, updatable = false)
    val changedByUserId: Long = 0,

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    val reason: String = "",

    @Column(name = "changed_at", nullable = false, updatable = false)
    val changedAt: LocalDateTime = LocalDateTime.now()
)