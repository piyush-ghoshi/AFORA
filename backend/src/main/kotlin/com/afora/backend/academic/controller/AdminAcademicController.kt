package com.afora.backend.academic.controller

import com.afora.backend.academic.dto.*
import com.afora.backend.academic.service.AdminAcademicService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(
    name = "Academic & Timetable Administration",
    description = "Assign teachers, manage class sections, define weekly timetable slots, batch enroll students, and generate lecture sessions"
)
@RestController
@RequestMapping("/admin")
class AdminAcademicController(
    private val adminAcademicService: AdminAcademicService
) {

    // -- Lookups (Teachers, Subjects, Students) ------------------------------

    @Operation(summary = "List all teachers", description = "Get list of all teachers with IDs, names, and departments for assigning to classes")
    @GetMapping("/teachers")
    fun listTeachers(): ResponseEntity<List<TeacherSummaryResponse>> {
        return ResponseEntity.ok(adminAcademicService.listTeachers())
    }

    @Operation(summary = "List all subjects", description = "Get list of all academic subjects/courses")
    @GetMapping("/subjects")
    fun listSubjects(): ResponseEntity<List<SubjectSummaryResponse>> {
        return ResponseEntity.ok(adminAcademicService.listSubjects())
    }

    @Operation(summary = "List students", description = "Get list of students, optionally filtered by batch or department")
    @GetMapping("/students")
    fun listStudents(
        @RequestParam(required = false) batch: String?,
        @RequestParam(required = false) department: String?
    ): ResponseEntity<List<StudentSummaryResponse>> {
        return ResponseEntity.ok(adminAcademicService.listStudents(batch, department))
    }

    @Operation(summary = "Register Sample Face Profile", description = "Enrolls sample 192-d face profile with consent for testing attendance")
    @PostMapping("/students/{studentId}/face-profile")
    fun registerSampleFace(
        @PathVariable studentId: Long
    ): ResponseEntity<StudentSummaryResponse> {
        return ResponseEntity.ok(adminAcademicService.registerSampleFace(studentId))
    }

    @Operation(summary = "Purge Student Face Profile", description = "Deletes registered face embeddings and profile for a student")
    @DeleteMapping("/students/{studentId}/face-profile")
    fun deleteFaceProfile(
        @PathVariable studentId: Long
    ): ResponseEntity<Map<String, Any>> {
        val success = adminAcademicService.deleteFaceProfile(studentId)
        return ResponseEntity.ok(mapOf("success" to success, "studentId" to studentId))
    }

    // -- Class Sections -------------------------------------------------------

    @Operation(
        summary = "Create Class Section & Assign Teacher",
        description = "Binds a Subject, Section, Batch, Room, and Teacher into a single class section"
    )
    @PostMapping("/class-sections")
    fun createClassSection(
        @Valid @RequestBody req: CreateClassSectionRequest
    ): ResponseEntity<ClassSectionDetailResponse> {
        val created = adminAcademicService.createClassSection(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @Operation(summary = "List all Class Sections", description = "Retrieve all class sections along with assigned teachers, subjects, and enrollment counts")
    @GetMapping("/class-sections")
    fun listClassSections(): ResponseEntity<List<ClassSectionDetailResponse>> {
        return ResponseEntity.ok(adminAcademicService.listClassSections())
    }

    // -- Timetable Slots ------------------------------------------------------

    @Operation(
        summary = "Add Recurring Weekly Timetable Slot",
        description = "Adds a weekly recurring slot (e.g., Monday 09:00 - 10:30) for a class section. The assigned teacher is automatically inherited."
    )
    @PostMapping("/timetable-slots")
    fun createTimetableSlot(
        @Valid @RequestBody req: CreateTimetableSlotRequest
    ): ResponseEntity<TimetableSlotDetailResponse> {
        val created = adminAcademicService.createTimetableSlot(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @Operation(
        summary = "List Timetable Slots",
        description = "Retrieve all weekly timetable slots, optionally filtered by classSectionId"
    )
    @GetMapping("/timetable-slots")
    fun listTimetableSlots(
        @RequestParam(required = false) classSectionId: Long?
    ): ResponseEntity<List<TimetableSlotDetailResponse>> {
        return ResponseEntity.ok(adminAcademicService.listTimetableSlots(classSectionId))
    }

    // -- Batch Enrollment -----------------------------------------------------

    @Operation(
        summary = "Batch Enroll Students by Roll Numbers",
        description = "Enrolls multiple students into a class section using their roll numbers (e.g., ['2024CS001', '2024CS002'])"
    )
    @PostMapping("/enrollments/batch")
    fun batchEnroll(
        @Valid @RequestBody req: BatchEnrollRequest
    ): ResponseEntity<BatchEnrollResponse> {
        return ResponseEntity.ok(adminAcademicService.batchEnroll(req))
    }

    // -- Lecture Session Generation -------------------------------------------

    @Operation(
        summary = "Generate Dated Lecture Sessions",
        description = "Automatically calculates and creates dated lecture sessions from weekly timetable slots for the next N days. Once generated, they appear on teacher dashboards and allow attendance marking."
    )
    @PostMapping("/sessions/generate")
    fun generateSessions(
        @RequestBody req: GenerateSessionsRequest
    ): ResponseEntity<GenerateSessionsResponse> {
        return ResponseEntity.ok(adminAcademicService.generateSessions(req))
    }
}
