package com.afora.shared.data.repository

import com.afora.shared.domain.model.LectureSession
import com.afora.shared.domain.model.TimetableSlot
import com.afora.shared.util.Result

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
