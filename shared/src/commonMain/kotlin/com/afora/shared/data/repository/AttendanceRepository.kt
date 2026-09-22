package com.afora.shared.data.repository

import com.afora.shared.domain.model.*
import com.afora.shared.util.Result

/**
 * Attendance repository interface.
 * Handles lecture sessions and attendance records.
 */
interface AttendanceRepository {
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
     * Submit attendance for confirmation (teacher review → backend validation → storage).
     */
    suspend fun confirmAttendance(
        lectureSessionId: Long,
        attendanceCandidates: List<AttendanceCandidate>,
        submissionId: String  // Client-generated UUID for idempotency
    ): Result<AttendanceConfirmation>
    
    /**
     * Get attendance records for a lecture session.
     */
    suspend fun getAttendanceByLectureSession(
        lectureSessionId: Long
    ): Result<List<AttendanceRecord>>
    
    /**
     * Get student's attendance records for a subject.
     */
    suspend fun getStudentAttendance(
        studentId: Long,
        subjectId: Long,
        semesterId: Long
    ): Result<List<AttendanceRecord>>
    
    /**
     * Get student's overall attendance statistics.
     */
    suspend fun getStudentAttendanceStats(
        studentId: Long,
        semesterId: Long
    ): Result<AttendanceStats>
    
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
 * Attendance confirmation result.
 */
data class AttendanceConfirmation(
    val lectureSessionId: Long,
    val totalMarked: Int,
    val presentCount: Int,
    val absentCount: Int,
    val onLeaveCount: Int,
    val recordsCreated: List<AttendanceRecord>,
    val confirmedAt: String  // ISO 8601 datetime
)

/**
 * Student attendance statistics.
 */
data class AttendanceStats(
    val studentId: Long,
    val semesterId: Long,
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
