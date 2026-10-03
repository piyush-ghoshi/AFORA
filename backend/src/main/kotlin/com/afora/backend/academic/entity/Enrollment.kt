package com.afora.backend.academic.entity

import com.afora.backend.user.entity.Student
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Enrollment entity representing student enrollment in a class section.
 * 
 * Phase A8.1: Maps students to their registered classes.
 */
@Entity
@Table(
    name = "enrollments",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["student_id", "class_section_id"])
    ]
)
data class Enrollment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    val student: Student? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    val classSection: ClassSection? = null,
    
    @Column(nullable = false)
    val enrollmentDate: LocalDateTime = LocalDateTime.now(),
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val status: EnrollmentStatus = EnrollmentStatus.ACTIVE,
    
    @Column(length = 5)
    val grade: String? = null,
    
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Enrollment status enum.
 */
enum class EnrollmentStatus {
    ACTIVE,    // Currently enrolled
    DROPPED,   // Dropped the class
    COMPLETED  // Completed the semester
}
