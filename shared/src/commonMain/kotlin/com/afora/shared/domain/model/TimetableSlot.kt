package com.afora.shared.domain.model

/**
 * Timetable slot for student/teacher schedule display.
 */
data class TimetableSlot(
    val id: Long,
    val classSectionId: Long,
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val teacherId: Long,
    val teacherName: String,
    val dayOfWeek: Int,      // 1=Monday … 7=Sunday
    val startTime: String,   // "HH:mm"
    val endTime: String,     // "HH:mm"
    val roomNumber: String? = null
)
