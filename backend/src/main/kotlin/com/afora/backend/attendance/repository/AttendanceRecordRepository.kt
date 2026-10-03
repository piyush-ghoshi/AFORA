package com.afora.backend.attendance.repository

import com.afora.backend.attendance.entity.AttendanceRecord
import com.afora.backend.attendance.entity.AttendanceStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for AttendanceRecord.
 * Phase A8.3: Aggregates backing the student dashboard percentage.
 */
@Repository
interface AttendanceRecordRepository : JpaRepository<AttendanceRecord, Long> {

    fun findByStudentId(studentId: Long): List<AttendanceRecord>

    fun findByLectureSessionId(lectureSessionId: Long): List<AttendanceRecord>

    fun countByLectureSessionIdAndStatus(lectureSessionId: Long, status: AttendanceStatus): Int

    /**
     * Attended (PRESENT or LATE) count for a student, optionally scoped to a semester.
     * Passing a null semesterId counts across all semesters.
     */
    @Query(
        """
        SELECT COUNT(ar) FROM AttendanceRecord ar
        WHERE ar.student.id = :studentId
          AND ar.status IN (
              com.afora.backend.attendance.entity.AttendanceStatus.PRESENT,
              com.afora.backend.attendance.entity.AttendanceStatus.LATE
          )
          AND (:semesterId IS NULL OR ar.lectureSession.semesterId = :semesterId)
        """
    )
    fun countAttended(
        @Param("studentId") studentId: Long,
        @Param("semesterId") semesterId: UUID?
    ): Long

    /**
     * Sessions that count toward the denominator: every record except the
     * neutral ones (ON_LEAVE, EXCUSED), so approved absence is not punished.
     */
    @Query(
        """
        SELECT COUNT(ar) FROM AttendanceRecord ar
        WHERE ar.student.id = :studentId
          AND ar.status NOT IN (
              com.afora.backend.attendance.entity.AttendanceStatus.ON_LEAVE,
              com.afora.backend.attendance.entity.AttendanceStatus.EXCUSED
          )
          AND (:semesterId IS NULL OR ar.lectureSession.semesterId = :semesterId)
        """
    )
    fun countCountable(
        @Param("studentId") studentId: Long,
        @Param("semesterId") semesterId: UUID?
    ): Long
}