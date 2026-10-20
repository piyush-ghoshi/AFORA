package com.afora.backend.attendance.repository

import com.afora.backend.attendance.entity.AttendanceSubmission
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for AttendanceSubmission.
 *
 * Lookup by submission identifier is the inherited findById, since the
 * submission identifier is the primary key (R6.12).
 */
@Repository
interface AttendanceSubmissionRepository : JpaRepository<AttendanceSubmission, UUID> {

    fun findByLectureSessionId(lectureSessionId: Long): List<AttendanceSubmission>
}