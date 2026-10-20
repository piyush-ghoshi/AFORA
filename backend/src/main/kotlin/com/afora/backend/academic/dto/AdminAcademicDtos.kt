package com.afora.backend.academic.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

@Schema(description = "Request to create a new class section and assign a teacher")
data class CreateClassSectionRequest(
    @field:NotBlank
    @Schema(description = "Display name of class section", example = "CS101 Section B")
    val name: String,

    @field:NotNull
    @Schema(description = "Subject ID (e.g. 1 for CS101)", example = "1")
    val subjectId: Long,

    @field:NotNull
    @Schema(description = "Teacher ID to assign as instructor", example = "4")
    val teacherId: Long,

    @Schema(description = "Semester UUID. If omitted, uses current active semester", example = "null")
    val semesterId: UUID? = null,

    @field:NotBlank
    @Schema(description = "Batch year", example = "2024")
    val batch: String,

    @field:NotBlank
    @Schema(description = "Section identifier", example = "B")
    val section: String,

    @Schema(description = "Default room number", example = "A-102")
    val roomNumber: String? = null,

    @field:Min(1)
    @Schema(description = "Maximum capacity", example = "60")
    val maxStudents: Int = 60
)

@Schema(description = "Details of a class section including assigned teacher and subject")
data class ClassSectionDetailResponse(
    val id: Long,
    val name: String,
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val teacherId: Long,
    val teacherName: String,
    val teacherEmployeeId: String,
    val semesterId: String,
    val batch: String,
    val section: String,
    val roomNumber: String?,
    val maxStudents: Int,
    val enrolledCount: Int,
    val isActive: Boolean
)

@Schema(description = "Request to add a weekly recurring timetable slot")
data class CreateTimetableSlotRequest(
    @field:NotNull
    @Schema(description = "Class Section ID", example = "1")
    val classSectionId: Long,

    @field:Min(1)
    @field:Max(7)
    @Schema(description = "ISO Day of Week: 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday", example = "1")
    val dayOfWeek: Int,

    @field:NotBlank
    @Schema(description = "Start time in HH:mm format", example = "09:00")
    val startTime: String,

    @field:NotBlank
    @Schema(description = "End time in HH:mm format", example = "10:30")
    val endTime: String,

    @Schema(description = "Room number. If omitted, inherits class section room", example = "A-101")
    val roomNumber: String? = null
)

@Schema(description = "Details of a timetable slot")
data class TimetableSlotDetailResponse(
    val id: Long,
    val classSectionId: Long,
    val classSectionName: String,
    val subjectCode: String,
    val subjectName: String,
    val teacherName: String,
    val dayOfWeek: Int,
    val dayOfWeekName: String,
    val startTime: String,
    val endTime: String,
    val roomNumber: String?
)

@Schema(description = "Request to batch enroll students into a class section")
data class BatchEnrollRequest(
    @field:NotNull
    @Schema(description = "Class Section ID to enroll students into", example = "1")
    val classSectionId: Long,

    @Schema(description = "List of student roll numbers to enroll", example = "[\"2024CS001\", \"2024CS002\"]")
    val rollNumbers: List<String> = emptyList()
)

@Schema(description = "Result of batch enrollment")
data class BatchEnrollResponse(
    val classSectionId: Long,
    val classSectionName: String,
    val enrolledCount: Int,
    val alreadyEnrolledCount: Int,
    val notFoundRollNumbers: List<String>
)

@Schema(description = "Request to generate dated lecture sessions from timetable slots")
data class GenerateSessionsRequest(
    @Schema(description = "Number of calendar days ahead to generate (e.g. 14 or 30)", example = "30")
    val daysAhead: Int = 30,

    @Schema(description = "Optional: Specific class section ID. If null, generates for all active sections", example = "null")
    val classSectionId: Long? = null
)

@Schema(description = "Result of session generation")
data class GenerateSessionsResponse(
    val generatedCount: Int,
    val message: String
)

@Schema(description = "Summary of a teacher for selection dropdowns")
data class TeacherSummaryResponse(
    val id: Long,
    val employeeId: String,
    val fullName: String,
    val email: String,
    val department: String,
    val designation: String?
)

@Schema(description = "Summary of a subject for selection dropdowns")
data class SubjectSummaryResponse(
    val id: Long,
    val code: String,
    val name: String,
    val credits: Int,
    val department: String
)

@Schema(description = "Summary of a student for selection dropdowns and face management")
data class StudentSummaryResponse(
    val id: Long,
    val rollNumber: String,
    val fullName: String,
    val department: String,
    val batch: String,
    val hasFaceProfile: Boolean = false,
    val faceDimensions: Int? = null,
    val embeddingCount: Int = 0
)
