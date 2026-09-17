package com.academia.shared.data.repository

import com.academia.shared.domain.model.LectureSession
import com.academia.shared.util.Result

/**
 * Timetable repository interface.
 */
interface TimetableRepository {
    /**
     * Get teacher's schedule for a specific date.
     */
    suspend fun getTeacherSchedule(
        teacherId: Long,
        date: String  // ISO 8601 date (YYYY-MM-DD)
    ): Result<List<LectureSession>>
    
    /**
     * Get teacher's schedule for current day.
     */
    suspend fun getTeacherTodaySchedule(teacherId: Long): Result<List<LectureSession>>
    
    /**
     * Get student's timetable.
     */
    suspend fun getStudentTimetable(
        studentId: Long,
        semesterId: Long
    ): Result<List<TimetableSlot>>
    
    /**
     * Get class timetable.
     */
    suspend fun getClassTimetable(
        classSectionId: Long,
        semesterId: Long
    ): Result<List<TimetableSlot>>
}

/**
 * Timetable slot representation.
 */
data class TimetableSlot(
    val id: Long,
    val classSectionId: Long,
    val subjectId: Long,
    val subjectCode: String,
    val subjectName: String,
    val teacherId: Long,
    val teacherName: String,
    val dayOfWeek: Int,  // 1=Monday, 7=Sunday
    val startTime: String,  // ISO 8601 time
    val endTime: String,  // ISO 8601 time
    val roomNumber: String?
)
