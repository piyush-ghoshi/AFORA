package com.afora.backend.attendance.entity

import com.afora.backend.academic.entity.ClassSection
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A student's leave application awaiting teacher review.
 *
 * Phase A8.3: A null [classSection] means the request spans all of the
 * student's classes.
 */
@Entity
@Table(name = "leave_requests")
data class LeaveRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    val student: Student? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id")
    val classSection: ClassSection? = null,

    @Column(nullable = false)
    val fromDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    val toDate: LocalDate = LocalDate.now(),

    @Column(nullable = false, columnDefinition = "TEXT")
    val reason: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: LeaveStatus = LeaveStatus.PENDING,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_teacher_id")
    val reviewedByTeacher: Teacher? = null,

    val reviewedAt: LocalDateTime? = null,

    @Column(columnDefinition = "TEXT")
    val reviewNotes: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/** Leave application lifecycle. */
enum class LeaveStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}