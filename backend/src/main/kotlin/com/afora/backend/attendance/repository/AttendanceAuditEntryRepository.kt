package com.afora.backend.attendance.repository

import com.afora.backend.attendance.entity.AttendanceAuditEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for AttendanceAuditEntry.
 */
@Repository
interface AttendanceAuditEntryRepository : JpaRepository<AttendanceAuditEntry, Long> {

    /** R9.16: every entry for a session, change timestamp ascending. */
    fun findByLectureSessionIdOrderByChangedAtAsc(lectureSessionId: Long): List<AttendanceAuditEntry>

    /**
     * R9.14: the records of this session carrying at least one status-changing
     * audit entry. The chk_audit_status_changed constraint guarantees every
     * stored entry changed the status, so a plain projection suffices.
     */
    @Query(
        """
        SELECT DISTINCT ae.attendanceRecordId FROM AttendanceAuditEntry ae
        WHERE ae.lectureSessionId = :sessionId
        """
    )
    fun findStatusChangedRecordIds(@Param("sessionId") sessionId: Long): List<Long>

    fun countByAttendanceRecordId(attendanceRecordId: Long): Int
}