package com.afora.backend.academic.repository

import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.academic.entity.SessionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * Repository for LectureSession.
 * Phase A8.2: Dated session queries backing the teacher dashboard.
 */
@Repository
interface LectureSessionRepository : JpaRepository<LectureSession, Long> {

    /**
     * Every session a teacher runs on a given date, earliest first.
     * Fetch-joins the section graph so the dashboard can render subject names.
     */
    @Query(
        """
        SELECT ls FROM LectureSession ls
        JOIN FETCH ls.classSection cs
        JOIN FETCH cs.subject
        WHERE cs.teacher.id = :teacherId
          AND ls.sessionDate = :date
        ORDER BY ls.startTime ASC
        """
    )
    fun findByTeacherAndDate(
        @Param("teacherId") teacherId: Long,
        @Param("date") date: LocalDate
    ): List<LectureSession>

    /** Sessions for one class section within a date range. */
    fun findByClassSectionIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
        classSectionId: Long,
        from: LocalDate,
        to: LocalDate
    ): List<LectureSession>

    /** Sessions for a section in a given status. */
    fun findByClassSectionIdAndStatus(
        classSectionId: Long,
        status: SessionStatus
    ): List<LectureSession>

    /**
     * Count sessions already held for the sections a student is enrolled in.
     * Only FINALIZED sessions count toward an attendance denominator, so a
     * student is not penalised for classes that have not happened yet.
     */
    @Query(
        """
        SELECT COUNT(ls) FROM LectureSession ls
        WHERE ls.status = com.afora.backend.academic.entity.SessionStatus.FINALIZED
          AND (:semesterId IS NULL OR ls.semesterId = :semesterId)
          AND ls.classSection.id IN (
              SELECT e.classSection.id FROM Enrollment e
              WHERE e.student.id = :studentId
                AND e.status = com.afora.backend.academic.entity.EnrollmentStatus.ACTIVE
          )
        """
    )
    fun countHeldSessionsForStudent(
        @Param("studentId") studentId: Long,
        @Param("semesterId") semesterId: java.util.UUID?
    ): Long
}