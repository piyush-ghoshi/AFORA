package com.afora.backend.academic.repository

import com.afora.backend.academic.entity.ClassSection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for ClassSection entity.
 * Phase A8.1: Core academic repository.
 */
@Repository
interface ClassSectionRepository : JpaRepository<ClassSection, Long> {

    /** All class sections taught by a teacher. */
    fun findByTeacherId(teacherId: Long): List<ClassSection>

    /** Active class sections taught by a teacher. */
    fun findByTeacherIdAndIsActiveTrue(teacherId: Long): List<ClassSection>

    /** All class sections for a subject. */
    fun findBySubjectId(subjectId: Long): List<ClassSection>

    /** All class sections in a semester. */
    fun findBySemesterId(semesterId: UUID): List<ClassSection>

    /** Active class sections for a batch. */
    fun findByBatchAndIsActiveTrue(batch: String): List<ClassSection>

    /**
     * Active class sections a student is enrolled in.
     *
     * Navigates Enrollment -> classSection rather than using a JOIN ... ON
     * entity join, and uses a fully-qualified enum literal so the query is
     * unambiguous during Hibernate bootstrap validation.
     */
    @Query(
        """
        SELECT DISTINCT e.classSection FROM Enrollment e
        WHERE e.student.id = :studentId
          AND e.status = com.afora.backend.academic.entity.EnrollmentStatus.ACTIVE
          AND e.classSection.isActive = true
        """
    )
    fun findActiveByStudentId(@Param("studentId") studentId: Long): List<ClassSection>
}
