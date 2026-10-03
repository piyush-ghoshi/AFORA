package com.afora.backend.attendance.entity

import com.afora.backend.academic.entity.AttendanceMethod
import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * One student's attendance for one lecture session.
 *
 * Phase A8.3: Unique on (session, student) so a student cannot be marked twice.
 */
@Entity
@Table(
    name = "attendance_records",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["lecture_session_id", "student_id"])
    ]
)
data class AttendanceRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_session_id", nullable = false)
    val lectureSession: LectureSession? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    val student: Student? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: AttendanceStatus = AttendanceStatus.ABSENT,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val method: AttendanceMethod? = null,

    /** Face-recognition confidence when [method] is CAMERA. */
    val confidenceScore: Double? = null,

    @Column(nullable = false)
    val markedAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marked_by_teacher_id")
    val markedByTeacher: Teacher? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Attendance outcome for a single session.
 *
 * PRESENT and LATE both count as attended for percentage purposes.
 * ON_LEAVE and EXCUSED are neutral: excluded from both numerator and
 * denominator so approved absence does not damage a student's percentage.
 */
enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE,
    ON_LEAVE,
    EXCUSED;

    val countsAsAttended: Boolean
        get() = this == PRESENT || this == LATE

    val isNeutral: Boolean
        get() = this == ON_LEAVE || this == EXCUSED
}