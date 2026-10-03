package com.afora.backend.academic.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/**
 * A concrete, dated occurrence of a class.
 *
 * Phase A8.2: This is the unit attendance is recorded against. Usually
 * generated from a [TimetableSlot], but may be created ad hoc (slot is null).
 */
@Entity
@Table(
    name = "lecture_sessions",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["class_section_id", "session_date", "start_time"])
    ]
)
data class LectureSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    val classSection: ClassSection? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timetable_slot_id")
    val timetableSlot: TimetableSlot? = null,

    @Column(name = "semester_id", nullable = false)
    val semesterId: UUID = UUID.randomUUID(),

    /** Named sessionDate rather than date to avoid reserved-word friction. */
    @Column(nullable = false)
    val sessionDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    val startTime: LocalTime = LocalTime.MIDNIGHT,

    @Column(nullable = false)
    val endTime: LocalTime = LocalTime.MIDNIGHT,

    @Column(length = 50)
    val roomNumber: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: SessionStatus = SessionStatus.SCHEDULED,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    val attendanceMethod: AttendanceMethod? = null,

    val totalStudents: Int? = null,

    @Column(nullable = false)
    val presentCount: Int = 0,

    @Column(nullable = false)
    val absentCount: Int = 0,

    @Column(nullable = false)
    val onLeaveCount: Int = 0,

    val startedAt: LocalDateTime? = null,

    val completedAt: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    val notes: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Lecture session lifecycle.
 * Mirrors the shared KMP SessionStatus enum so the values serialise 1:1.
 */
enum class SessionStatus {
    SCHEDULED,
    CREATED,
    ACTIVE,
    SCANNING,
    REVIEW,
    FINALIZING,
    FINALIZED,
    CANCELLED,
    FAILED
}

/**
 * How attendance was captured.
 * Mirrors the shared KMP AttendanceMethod enum.
 */
enum class AttendanceMethod {
    CAMERA,
    MANUAL,
    HYBRID
}