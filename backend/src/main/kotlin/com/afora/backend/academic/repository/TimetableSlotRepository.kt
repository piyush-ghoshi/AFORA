package com.afora.backend.academic.repository

import com.afora.backend.academic.entity.TimetableSlot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for TimetableSlot.
 * Phase A8.2: Weekly schedule queries.
 */
@Repository
interface TimetableSlotRepository : JpaRepository<TimetableSlot, Long> {

    /** Active slots for one class section, earliest first. */
    fun findByClassSectionIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(
        classSectionId: Long
    ): List<TimetableSlot>

    /**
     * Active slots across every class section a student is actively enrolled in,
     * ordered by weekday then start time. Fetch-joins subject and teacher so the
     * dashboard can render subject/teacher names without N+1 queries.
     */
    @Query(
        """
        SELECT ts FROM TimetableSlot ts
        JOIN FETCH ts.classSection cs
        JOIN FETCH cs.subject
        JOIN FETCH cs.teacher t
        JOIN FETCH t.user
        WHERE ts.isActive = true
          AND cs.isActive = true
          AND cs.id IN (
              SELECT e.classSection.id FROM Enrollment e
              WHERE e.student.id = :studentId
                AND e.status = com.afora.backend.academic.entity.EnrollmentStatus.ACTIVE
          )
        ORDER BY ts.dayOfWeek ASC, ts.startTime ASC
        """
    )
    fun findActiveForStudent(@Param("studentId") studentId: Long): List<TimetableSlot>

    /** Active slots for all sections taught by a teacher. */
    @Query(
        """
        SELECT ts FROM TimetableSlot ts
        JOIN FETCH ts.classSection cs
        JOIN FETCH cs.subject
        WHERE ts.isActive = true
          AND cs.isActive = true
          AND cs.teacher.id = :teacherId
        ORDER BY ts.dayOfWeek ASC, ts.startTime ASC
        """
    )
    fun findActiveForTeacher(@Param("teacherId") teacherId: Long): List<TimetableSlot>
}