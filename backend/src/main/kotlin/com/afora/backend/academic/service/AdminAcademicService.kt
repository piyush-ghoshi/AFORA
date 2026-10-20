package com.afora.backend.academic.service

import com.afora.backend.academic.dto.*
import com.afora.backend.academic.entity.*
import com.afora.backend.academic.repository.*
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.TeacherRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@Service
class AdminAcademicService(
    private val classSectionRepository: ClassSectionRepository,
    private val timetableSlotRepository: TimetableSlotRepository,
    private val enrollmentRepository: EnrollmentRepository,
    private val lectureSessionRepository: LectureSessionRepository,
    private val subjectRepository: SubjectRepository,
    private val teacherRepository: TeacherRepository,
    private val studentRepository: StudentRepository,
    private val semesterRepository: SemesterRepository,
    private val faceProfileRepository: com.afora.backend.facerecognition.repository.FaceProfileRepository,
    private val faceEmbeddingRepository: com.afora.backend.facerecognition.repository.FaceEmbeddingRepository,
    private val faceProfileService: com.afora.backend.facerecognition.service.FaceProfileService
) {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // -- Teachers, Subjects, Students lookups ---------------------------------

    @Transactional(readOnly = true)
    fun listTeachers(): List<TeacherSummaryResponse> {
        return teacherRepository.findAll().map { t ->
            TeacherSummaryResponse(
                id = t.id ?: 0L,
                employeeId = t.employeeId,
                fullName = t.user.getFullName(),
                email = t.user.email,
                department = t.department,
                designation = t.designation
            )
        }
    }

    @Transactional(readOnly = true)
    fun listSubjects(): List<SubjectSummaryResponse> {
        return subjectRepository.findAll().map { s ->
            SubjectSummaryResponse(
                id = s.id,
                code = s.code,
                name = s.name,
                credits = s.credits,
                department = s.department
            )
        }
    }

    @Transactional(readOnly = true)
    fun listStudents(batch: String?, department: String?): List<StudentSummaryResponse> {
        val list = when {
            batch != null -> studentRepository.findAllByBatch(batch)
            department != null -> studentRepository.findAllByDepartment(department)
            else -> studentRepository.findAll()
        }
        val profiles = faceProfileRepository.findAll().associateBy { it.studentId }
        val embeddingCounts = faceEmbeddingRepository.findAll().groupBy { it.studentId }.mapValues { it.value.size }

        return list.map { st ->
            val sid = st.id ?: 0L
            val profile = profiles[sid]
            StudentSummaryResponse(
                id = sid,
                rollNumber = st.rollNumber,
                fullName = st.user.getFullName(),
                department = st.department,
                batch = st.batch,
                hasFaceProfile = profile != null,
                faceDimensions = profile?.dimensions,
                embeddingCount = embeddingCounts[sid] ?: 0
            )
        }
    }

    @Transactional
    fun registerSampleFace(studentId: Long): StudentSummaryResponse {
        faceProfileService.recordConsent(studentId)
        val random = java.util.Random(studentId)
        val rawVector = (0 until 192).map { (random.nextDouble() * 2.0 - 1.0) }
        val norm = Math.sqrt(rawVector.sumOf { it * it })
        val normalizedVector = rawVector.map { it / norm }
        faceProfileService.register(studentId, listOf(normalizedVector))

        val student = studentRepository.findById(studentId)
            .orElseThrow { IllegalArgumentException("Student $studentId not found") }
        val profile = faceProfileRepository.findById(studentId).orElse(null)
        return StudentSummaryResponse(
            id = studentId,
            rollNumber = student.rollNumber,
            fullName = student.user.getFullName(),
            department = student.department,
            batch = student.batch,
            hasFaceProfile = true,
            faceDimensions = profile?.dimensions ?: 192,
            embeddingCount = 1
        )
    }

    @Transactional
    fun deleteFaceProfile(studentId: Long): Boolean {
        return try {
            faceProfileService.deleteProfile(studentId, com.afora.backend.facerecognition.entity.PurgeReason.ADMIN_REQUEST)
            true
        } catch (e: Exception) {
            false
        }
    }

    // -- Class Sections -------------------------------------------------------

    @Transactional
    fun createClassSection(req: CreateClassSectionRequest): ClassSectionDetailResponse {
        val subject = subjectRepository.findById(req.subjectId)
            .orElseThrow { IllegalArgumentException("Subject with ID ${req.subjectId} not found") }
        val teacher = teacherRepository.findById(req.teacherId)
            .orElseThrow { IllegalArgumentException("Teacher with ID ${req.teacherId} not found") }
        
        val semesterId = req.semesterId ?: semesterRepository.findFirstByIsCurrentTrue()?.id
            ?: UUID.randomUUID()

        val section = classSectionRepository.save(
            ClassSection(
                name = req.name,
                subject = subject,
                teacher = teacher,
                semesterId = semesterId,
                batch = req.batch,
                section = req.section,
                roomNumber = req.roomNumber,
                maxStudents = req.maxStudents,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        return toClassSectionDetail(section)
    }

    @Transactional(readOnly = true)
    fun listClassSections(): List<ClassSectionDetailResponse> {
        return classSectionRepository.findAll().map { toClassSectionDetail(it) }
    }

    private fun toClassSectionDetail(cs: ClassSection): ClassSectionDetailResponse {
        val enrolledCount = enrollmentRepository.countByClassSectionIdAndStatus(cs.id, EnrollmentStatus.ACTIVE)
        return ClassSectionDetailResponse(
            id = cs.id,
            name = cs.name,
            subjectId = cs.subject?.id ?: 0L,
            subjectCode = cs.subject?.code ?: "",
            subjectName = cs.subject?.name ?: "",
            teacherId = cs.teacher?.id ?: 0L,
            teacherName = cs.teacher?.user?.getFullName() ?: "",
            teacherEmployeeId = cs.teacher?.employeeId ?: "",
            semesterId = cs.semesterId.toString(),
            batch = cs.batch,
            section = cs.section,
            roomNumber = cs.roomNumber,
            maxStudents = cs.maxStudents,
            enrolledCount = enrolledCount,
            isActive = cs.isActive
        )
    }

    // -- Timetable Slots ------------------------------------------------------

    @Transactional
    fun createTimetableSlot(req: CreateTimetableSlotRequest): TimetableSlotDetailResponse {
        val classSection = classSectionRepository.findById(req.classSectionId)
            .orElseThrow { IllegalArgumentException("ClassSection with ID ${req.classSectionId} not found") }

        val start = LocalTime.parse(req.startTime, timeFormatter)
        val end = LocalTime.parse(req.endTime, timeFormatter)
        require(end.isAfter(start)) { "End time must be after start time" }

        val slot = timetableSlotRepository.save(
            TimetableSlot(
                classSection = classSection,
                dayOfWeek = req.dayOfWeek,
                startTime = start,
                endTime = end,
                roomNumber = req.roomNumber ?: classSection.roomNumber,
                isActive = true,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        return toTimetableSlotDetail(slot)
    }

    @Transactional(readOnly = true)
    fun listTimetableSlots(classSectionId: Long?): List<TimetableSlotDetailResponse> {
        val slots = if (classSectionId != null) {
            timetableSlotRepository.findByClassSectionIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(classSectionId)
        } else {
            timetableSlotRepository.findAll()
        }
        return slots.map { toTimetableSlotDetail(it) }
    }

    private fun toTimetableSlotDetail(slot: TimetableSlot): TimetableSlotDetailResponse {
        val dayName = try {
            DayOfWeek.of(slot.dayOfWeek).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        } catch (_: Exception) {
            "Day ${slot.dayOfWeek}"
        }
        return TimetableSlotDetailResponse(
            id = slot.id,
            classSectionId = slot.classSection?.id ?: 0L,
            classSectionName = slot.classSection?.name ?: "",
            subjectCode = slot.classSection?.subject?.code ?: "",
            subjectName = slot.classSection?.subject?.name ?: "",
            teacherName = slot.classSection?.teacher?.user?.getFullName() ?: "",
            dayOfWeek = slot.dayOfWeek,
            dayOfWeekName = dayName,
            startTime = slot.startTime.format(timeFormatter),
            endTime = slot.endTime.format(timeFormatter),
            roomNumber = slot.roomNumber
        )
    }

    // -- Batch Enrollment -----------------------------------------------------

    @Transactional
    fun batchEnroll(req: BatchEnrollRequest): BatchEnrollResponse {
        val section = classSectionRepository.findById(req.classSectionId)
            .orElseThrow { IllegalArgumentException("ClassSection with ID ${req.classSectionId} not found") }

        var enrolled = 0
        var alreadyEnrolled = 0
        val notFound = mutableListOf<String>()

        for (roll in req.rollNumbers) {
            val studentOpt = studentRepository.findByRollNumber(roll.trim())
            if (studentOpt.isEmpty) {
                notFound.add(roll)
                continue
            }
            val student = studentOpt.get()
            val studentId = student.id ?: continue
            if (enrollmentRepository.existsByStudentIdAndClassSectionId(studentId, section.id)) {
                alreadyEnrolled++
            } else {
                enrollmentRepository.save(
                    Enrollment(
                        student = student,
                        classSection = section,
                        enrollmentDate = LocalDateTime.now(),
                        status = EnrollmentStatus.ACTIVE
                    )
                )
                enrolled++
            }
        }

        return BatchEnrollResponse(
            classSectionId = section.id,
            classSectionName = section.name,
            enrolledCount = enrolled,
            alreadyEnrolledCount = alreadyEnrolled,
            notFoundRollNumbers = notFound
        )
    }

    // -- Session Generation ---------------------------------------------------

    @Transactional
    fun generateSessions(req: GenerateSessionsRequest): GenerateSessionsResponse {
        val slots = if (req.classSectionId != null) {
            timetableSlotRepository.findByClassSectionIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(req.classSectionId)
        } else {
            timetableSlotRepository.findAll().filter { it.isActive && it.classSection?.isActive == true }
        }

        if (slots.isEmpty()) {
            return GenerateSessionsResponse(
                generatedCount = 0,
                message = "No active timetable slots found to generate sessions from."
            )
        }

        val today = LocalDate.now()
        val days = req.daysAhead.coerceIn(1, 90)
        var count = 0

        for (offset in 0L..days) {
            val date = today.plusDays(offset)
            val dayOfWeek = date.dayOfWeek.value // 1=Mon .. 7=Sun

            for (slot in slots) {
                if (slot.dayOfWeek == dayOfWeek) {
                    val section = slot.classSection ?: continue
                    
                    // Check if a session already exists for this section, date, and startTime
                    val existing = lectureSessionRepository.findByClassSectionIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
                        section.id, date, date
                    ).any { it.startTime == slot.startTime }

                    if (!existing) {
                        val activeEnrollments = enrollmentRepository.countByClassSectionIdAndStatus(
                            section.id, EnrollmentStatus.ACTIVE
                        )
                        lectureSessionRepository.save(
                            LectureSession(
                                classSection = section,
                                timetableSlot = slot,
                                semesterId = section.semesterId,
                                sessionDate = date,
                                startTime = slot.startTime,
                                endTime = slot.endTime,
                                roomNumber = slot.roomNumber ?: section.roomNumber,
                                status = SessionStatus.SCHEDULED,
                                totalStudents = activeEnrollments
                            )
                        )
                        count++
                    }
                }
            }
        }

        return GenerateSessionsResponse(
            generatedCount = count,
            message = "Successfully created $count lecture session(s) across the next $days days."
        )
    }
}
