package com.afora.backend.attendance.service

import com.afora.backend.academic.repository.SemesterRepository
import com.afora.backend.attendance.dto.SessionAttendanceEntry
import com.afora.backend.attendance.dto.StudentAttendanceResponse
import com.afora.backend.attendance.dto.SubjectAttendanceSummary
import com.afora.backend.attendance.entity.AttendanceRecord
import com.afora.backend.attendance.entity.AttendanceStatus
import com.afora.backend.attendance.repository.AttendanceRecordRepository
import com.afora.backend.common.exception.NotFoundException
import com.afora.backend.user.repository.StudentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.math.round

/**
 * Aggregates student attendance data for the student detail screen (Phase 4).
 *
 * Returns overall + per-subject + per-session attendance, all scoped to a
 * semester (defaulting to the current one).
 */
@Service
@Transactional(readOnly = true)
class StudentAttendanceService(
    private val studentRepository: StudentRepository,
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val semesterRepository: SemesterRepository
) {

    fun getStudentAttendance(studentId: Long, semesterId: UUID?): StudentAttendanceResponse {
        // Verify student exists
        studentRepository.findById(studentId)
            .orElseThrow { NotFoundException("Student $studentId not found") }

        val scope = semesterId ?: semesterRepository.findFirstByIsCurrentTrue()?.id

        val records = attendanceRecordRepository.findByStudentAndSemester(studentId, scope)

        // Group by subject
        val bySubject = records.groupBy { record ->
            val section = requireNotNull(record.lectureSession?.classSection)
            requireNotNull(section.subject)
        }

        val subjects = bySubject.map { (subject, subjectRecords) ->
            val countable = subjectRecords.filter { !it.status.isNeutral }
            val present  = countable.count { it.status.countsAsAttended }
            val absent   = countable.count { it.status == AttendanceStatus.ABSENT }
            val onLeave  = subjectRecords.count { it.status.isNeutral }
            val pct      = if (countable.isEmpty()) 0.0
                           else round(present.toDouble() / countable.size * 10000) / 100

            SubjectAttendanceSummary(
                subjectId           = subject.id,
                subjectCode         = subject.code,
                subjectName         = subject.name,
                totalLectures       = countable.size,
                presentCount        = present,
                absentCount         = absent,
                onLeaveCount        = onLeave,
                attendancePercentage = pct,
                sessions            = subjectRecords.map { it.toSessionEntry() }
            )
        }.sortedBy { it.subjectCode }

        // Overall totals across all subjects
        val allCountable = records.filter { !it.status.isNeutral }
        val totalPresent = allCountable.count { it.status.countsAsAttended }
        val totalAbsent  = allCountable.count { it.status == AttendanceStatus.ABSENT }
        val totalOnLeave = records.count { it.status.isNeutral }
        val overallPct   = if (allCountable.isEmpty()) 0.0
                           else round(totalPresent.toDouble() / allCountable.size * 10000) / 100

        return StudentAttendanceResponse(
            studentId           = studentId,
            semesterId          = scope?.toString() ?: "all",
            overallPercentage   = overallPct,
            totalLectures       = allCountable.size,
            presentCount        = totalPresent,
            absentCount         = totalAbsent,
            onLeaveCount        = totalOnLeave,
            subjects            = subjects
        )
    }

    private fun AttendanceRecord.toSessionEntry(): SessionAttendanceEntry {
        val session = requireNotNull(lectureSession)
        val corrected = attendanceRecordRepository.isCorrected(this.id)
        return SessionAttendanceEntry(
            sessionId   = session.id,
            sessionDate = session.sessionDate,
            startTime   = session.startTime,
            endTime     = session.endTime,
            status      = status,
            isCorrected = corrected,
            notes       = notes
        )
    }
}
