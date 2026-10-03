package com.afora.backend.academic.repository

import com.afora.backend.academic.entity.Enrollment
import com.afora.backend.academic.entity.EnrollmentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for Enrollment entity.
 * Phase A8.1: Student-class enrollment repository.
 */
@Repository
interface EnrollmentRepository : JpaRepository<Enrollment, Long> {

    /** All enrollments for a student. */
    fun findByStudentId(studentId: Long): List<Enrollment>

    /** Enrollments for a student filtered by status. */
    fun findByStudentIdAndStatus(studentId: Long, status: EnrollmentStatus): List<Enrollment>

    /** All enrollments in a class section. */
    fun findByClassSectionId(classSectionId: Long): List<Enrollment>

    /** Enrollments in a class section filtered by status. */
    fun findByClassSectionIdAndStatus(
        classSectionId: Long,
        status: EnrollmentStatus
    ): List<Enrollment>

    /** Count students in a class section with a given status. */
    fun countByClassSectionIdAndStatus(classSectionId: Long, status: EnrollmentStatus): Int

    /** Whether a student is enrolled in a class section. */
    fun existsByStudentIdAndClassSectionId(studentId: Long, classSectionId: Long): Boolean

    /** Lookup a single enrollment by student and class section. */
    fun findByStudentIdAndClassSectionId(studentId: Long, classSectionId: Long): Enrollment?

    /**
     * Count a student's active enrollments.
     * Uses a fully-qualified enum literal for unambiguous bootstrap validation.
     */
    @Query(
        """
        SELECT COUNT(e) FROM Enrollment e
        WHERE e.student.id = :studentId
          AND e.status = com.afora.backend.academic.entity.EnrollmentStatus.ACTIVE
        """
    )
    fun countActiveEnrollmentsByStudentId(@Param("studentId") studentId: Long): Long
}
