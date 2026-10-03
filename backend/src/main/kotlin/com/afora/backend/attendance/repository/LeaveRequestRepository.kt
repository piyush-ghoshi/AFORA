package com.afora.backend.attendance.repository

import com.afora.backend.attendance.entity.LeaveRequest
import com.afora.backend.attendance.entity.LeaveStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Repository for LeaveRequest.
 * Phase A8.3: Pending-count queries for both dashboards.
 */
@Repository
interface LeaveRequestRepository : JpaRepository<LeaveRequest, Long> {

    fun findByStudentId(studentId: Long): List<LeaveRequest>

    fun findByStudentIdAndStatus(studentId: Long, status: LeaveStatus): List<LeaveRequest>

    /** How many of this student's own requests are still awaiting review. */
    fun countByStudentIdAndStatus(studentId: Long, status: LeaveStatus): Int

    /**
     * Requests awaiting this teacher's review: anything PENDING against a class
     * section they teach. Requests with no section are not attributable to a
     * single teacher and are excluded.
     */
    @Query(
        """
        SELECT COUNT(lr) FROM LeaveRequest lr
        WHERE lr.status = com.afora.backend.attendance.entity.LeaveStatus.PENDING
          AND lr.classSection.teacher.id = :teacherId
        """
    )
    fun countPendingForTeacher(@Param("teacherId") teacherId: Long): Int
}