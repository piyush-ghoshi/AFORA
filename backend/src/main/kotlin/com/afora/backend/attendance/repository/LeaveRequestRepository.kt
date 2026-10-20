package com.afora.backend.attendance.repository

import com.afora.backend.attendance.entity.LeaveRequest
import com.afora.backend.attendance.entity.LeaveStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * Repository for LeaveRequest.
 * Phase A8.3: Pending-count queries for both dashboards.
 * Leave Requests feature: adds the list-form of the teacher's review queue
 * (findPendingForTeacher), alongside the pre-existing count-only query.
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

    /**
     * The list form of [countPendingForTeacher]: every PENDING request
     * against a class section this teacher teaches, oldest first so the
     * review queue works through requests in submission order. Same
     * null-classSection exclusion as the count query, for the same reason.
     */
    @Query(
        """
        SELECT lr FROM LeaveRequest lr
        WHERE lr.status = com.afora.backend.attendance.entity.LeaveStatus.PENDING
          AND lr.classSection.teacher.id = :teacherId
        ORDER BY lr.createdAt ASC
        """
    )
    fun findPendingForTeacher(@Param("teacherId") teacherId: Long): List<LeaveRequest>

    /**
     * R4.1: APPROVED leave covering a session date for any of these students.
     *
     * Both date comparisons are inclusive, which is what "inclusively span"
     * means. A null classSection means the request spans all of the student's
     * classes, so it matches any section. The enum literal is fully qualified,
     * matching the established pattern in countPendingForTeacher.
     */
    @Query(
        """
        SELECT lr FROM LeaveRequest lr
        WHERE lr.student.id IN :studentIds
          AND lr.status = com.afora.backend.attendance.entity.LeaveStatus.APPROVED
          AND lr.fromDate <= :sessionDate
          AND lr.toDate   >= :sessionDate
          AND (lr.classSection IS NULL OR lr.classSection.id = :classSectionId)
        ORDER BY lr.id ASC
        """
    )
    fun findApprovedCovering(
        @Param("studentIds") studentIds: Collection<Long>,
        @Param("sessionDate") sessionDate: LocalDate,
        @Param("classSectionId") classSectionId: Long
    ): List<LeaveRequest>
}