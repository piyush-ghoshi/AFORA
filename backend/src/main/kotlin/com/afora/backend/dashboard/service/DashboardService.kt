package com.afora.backend.dashboard.service

import com.afora.backend.academic.entity.LectureSession
import com.afora.backend.academic.entity.TimetableSlot
import com.afora.backend.academic.repository.LectureSessionRepository
import com.afora.backend.academic.repository.SemesterRepository
import com.afora.backend.academic.repository.TimetableSlotRepository
import com.afora.backend.attendance.entity.LeaveStatus
import com.afora.backend.attendance.repository.AttendanceRecordRepository
import com.afora.backend.attendance.repository.LeaveRequestRepository
import com.afora.backend.dashboard.dto.LectureSessionResponse
import com.afora.backend.dashboard.dto.StudentDashboardStatsResponse
import com.afora.backend.dashboard.dto.TeacherDashboardStatsResponse
import com.afora.backend.dashboard.dto.TimetableSlotResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.round

/**
 * Aggregates dashboard data from the academic and attendance repositories.
 *
 * Phase A8.4: Replaces the Phase A6 mock data with real queries.
 */
@Service
@Transactional(readOnly = true)
class DashboardService(
    private val timetableSlotRepository: TimetableSlotRepository,
    private val lectureSessionRepository: LectureSessionRepository,
    private val attendanceRecordRepository: AttendanceRecordRepository,
    private val leaveRequestRepository: LeaveRequestRepository,
    private val semesterRepository: SemesterRepository
) {

    companion object {
        private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val HHMMSS: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

        /** Cap on how many upcoming classes the dashboard returns. */
        private const val MAX_UPCOMING = 10
    }

    /**
     * Student dashboard: attendance rate, this week's remaining classes and
     * the student's own outstanding leave requests.
     *
     * @param semesterId scope for the attendance figures; falls back to the
     *   semester flagged current, and to all-time if none is flagged.
     */
    fun getStudentDashboard(studentId: Long, semesterId: UUID?): StudentDashboardStatsResponse {
        val scope = semesterId ?: currentSemesterId()

        val attended = attendanceRecordRepository.countAttended(studentId, scope)
        val countable = attendanceRecordRepository.countCountable(studentId, scope)

        return StudentDashboardStatsResponse(
            attendancePercentage = percentage(attended, countable),
            presentCount = attended.toInt(),
            totalCount = countable.toInt(),
            upcomingClasses = upcomingClassesFor(studentId),
            pendingLeaveRequests = leaveRequestRepository
                .countByStudentIdAndStatus(studentId, LeaveStatus.PENDING)
        )
    }

    /**
     * Teacher dashboard: the given day's sessions plus review workload.
     *
     * @param date defaults to today.
     */
    fun getTeacherDashboard(teacherId: Long, date: LocalDate?): TeacherDashboardStatsResponse {
        val target = date ?: LocalDate.now()

        return TeacherDashboardStatsResponse(
            todaySchedule = lectureSessionRepository
                .findByTeacherAndDate(teacherId, target)
                .map { it.toResponse() },
            pendingLeaveRequests = leaveRequestRepository.countPendingForTeacher(teacherId),
            // Attendance queries are not modelled yet; reported as zero rather
            // than fabricated. Wire up when the queries table lands.
            pendingQueries = 0
        )
    }

    // -- internals ----------------------------------------------------------

    private fun currentSemesterId(): UUID? =
        semesterRepository.findFirstByIsCurrentTrue()?.id

    /**
     * Attendance percentage rounded to 2 decimal places.
     * Returns 0.0 when nothing countable has been held yet, avoiding
     * divide-by-zero for students at the very start of a semester.
     */
    private fun percentage(attended: Long, countable: Long): Double =
        if (countable <= 0L) 0.0
        else round(attended.toDouble() / countable * 10000) / 100

    /**
     * The student's weekly timetable rotated so today comes first, which is
     * what "upcoming" means for a recurring schedule.
     */
    private fun upcomingClassesFor(studentId: Long): List<TimetableSlotResponse> {
        val today = LocalDate.now().dayOfWeek.value
        return timetableSlotRepository.findActiveForStudent(studentId)
            .sortedWith(
                compareBy({ (it.dayOfWeek - today + 7) % 7 }, { it.startTime })
            )
            .take(MAX_UPCOMING)
            .map { it.toResponse() }
    }

    private fun TimetableSlot.toResponse(): TimetableSlotResponse {
        val section = requireNotNull(classSection) { "timetable slot $id has no class section" }
        val subject = requireNotNull(section.subject) { "class section ${section.id} has no subject" }
        val teacher = requireNotNull(section.teacher) { "class section ${section.id} has no teacher" }
        return TimetableSlotResponse(
            id = id,
            classSectionId = section.id,
            subjectId = subject.id,
            subjectCode = subject.code,
            subjectName = subject.name,
            teacherId = teacher.id ?: 0L,
            teacherName = teacher.user.getFullName(),
            dayOfWeek = dayOfWeek,
            startTime = startTime.format(HHMM),
            endTime = endTime.format(HHMM),
            roomNumber = roomNumber ?: section.roomNumber
        )
    }

    private fun LectureSession.toResponse(): LectureSessionResponse {
        val section = requireNotNull(classSection) { "lecture session $id has no class section" }
        val subject = requireNotNull(section.subject) { "class section ${section.id} has no subject" }
        val teacher = requireNotNull(section.teacher) { "class section ${section.id} has no teacher" }
        return LectureSessionResponse(
            id = id,
            subjectId = subject.id,
            classSectionId = section.id,
            teacherId = teacher.id ?: 0L,
            semesterId = semesterId.toString(),
            date = sessionDate,
            startTime = startTime.format(HHMMSS),
            endTime = endTime.format(HHMMSS),
            roomNumber = roomNumber ?: section.roomNumber,
            status = status.name,
            attendanceMethod = attendanceMethod?.name,
            totalStudents = totalStudents,
            presentCount = presentCount,
            absentCount = absentCount,
            onLeaveCount = onLeaveCount,
            startedAt = startedAt?.toString(),
            completedAt = completedAt?.toString(),
            notes = notes
        )
    }
}