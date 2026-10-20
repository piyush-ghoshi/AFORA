package com.afora.shared.data.repository

import com.afora.shared.data.api.AmendAttendanceRequest
import com.afora.shared.data.api.AmendmentResultResponse
import com.afora.shared.data.api.AttendanceConfirmationResponse
import com.afora.shared.data.api.AuditHistoryResponse
import com.afora.shared.data.api.ConfirmAttendanceRequest
import com.afora.shared.data.api.RosterResponse
import com.afora.shared.data.api.SessionSummary
import com.afora.shared.data.api.StudentAttendanceResponse
import com.afora.shared.domain.model.*
import com.afora.shared.util.Result

/**
 * Attendance repository interface.
 * Handles lecture sessions and attendance records.
 */
interface AttendanceRepository {

    // --- Manual_Marking_Flow (R11.13) ---

    /**
     * Get the roster for a lecture session: the ACTIVE enrollment set with
     * each student's stored status, suggested status and leave flag.
     */
    suspend fun getSessionRoster(lectureSessionId: Long): Result<RosterResponse>

    /**
     * Activate a lecture session, opening it for manual marking.
     */
    suspend fun activateSession(lectureSessionId: Long): Result<SessionSummary>

    /**
     * Cancel a lecture session.
     */
    suspend fun cancelSession(lectureSessionId: Long): Result<SessionSummary>

    /**
     * Confirm manual attendance for a session's roster. Idempotent on
     * `request.submissionId` (R6).
     */
    suspend fun confirmManualAttendance(
        lectureSessionId: Long,
        request: ConfirmAttendanceRequest
    ): Result<AttendanceConfirmationResponse>

    /**
     * Amend one or more attendance records of a FINALIZED session, writing
     * an audit trail (R9).
     */
    suspend fun amendAttendance(
        lectureSessionId: Long,
        request: AmendAttendanceRequest
    ): Result<AmendmentResultResponse>

    /**
     * Get the audit history for a lecture session, ordered by change
     * timestamp ascending.
     */
    suspend fun getAuditHistory(lectureSessionId: Long): Result<AuditHistoryResponse>

    // --- corrected signatures (R11.1, R11.2) ---

    /**
     * Get student's attendance records for a subject.
     */
    suspend fun getStudentAttendance(
        studentId: Long,
        subjectId: Long,
        semesterId: String  // semester UUID as text (R11.1)
    ): Result<List<AttendanceRecord>>

    /**
     * Get student's overall attendance statistics.
     */
    suspend fun getStudentAttendanceStats(
        studentId: Long,
        semesterId: String  // semester UUID as text (R11.2)
    ): Result<AttendanceStats>

    /**
     * Get detailed attendance response for a student, grouped by subject and sessions.
     */
    suspend fun getStudentAttendanceDetail(
        studentId: Long,
        semesterId: String? = null
    ): Result<StudentAttendanceResponse>

    // --- camera-flow members, left as-is, unimplemented (R11.12) ---

    /**
     * Start a new lecture session.
     * @return Lecture session with enrolled students and face embeddings
     */
    suspend fun startLectureSession(lectureId: Long): Result<LectureSessionWithStudents>

    /**
     * Get enrolled students for a lecture with their face embeddings.
     */
    suspend fun getEnrolledStudentsWithEmbeddings(
        lectureSessionId: Long
    ): Result<List<StudentWithEmbedding>>

    /**
     * Get attendance records for a lecture session.
     */
    suspend fun getAttendanceByLectureSession(
        lectureSessionId: Long
    ): Result<List<AttendanceRecord>>

    /**
     * Get lecture session by ID.
     */
    suspend fun getLectureSession(lectureSessionId: Long): Result<LectureSession>

    /**
     * Cancel lecture session.
     */
    suspend fun cancelLectureSession(lectureSessionId: Long): Result<Unit>
}

/**
 * Lecture session with enrolled students.
 */
data class LectureSessionWithStudents(
    val session: LectureSession,
    val students: List<Student>,
    val subject: com.afora.shared.domain.model.Subject,
    val classSection: ClassSection
)

/**
 * Student with face embeddings for recognition.
 */
data class StudentWithEmbedding(
    val student: Student,
    val embeddings: List<FloatArray>  // Multiple embeddings per student
)

/**
 * Student attendance statistics.
 */
data class AttendanceStats(
    val studentId: Long,
    val semesterId: String,  // semester UUID as text (R11.3)
    val totalLectures: Int,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val lateCount: Int,
    val attendancePercentage: Double,
    val subjectWiseStats: List<SubjectAttendanceStats>
)

/**
 * Subject-wise attendance statistics.
 */
data class SubjectAttendanceStats(
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val totalLectures: Int,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val attendancePercentage: Double
)
