package com.afora.shared.data.repository

import com.afora.shared.data.api.ApiClient
import com.afora.shared.data.api.response.ApiResponse
import com.afora.shared.data.api.response.TimetableSlotApiResponse
import com.afora.shared.data.mapper.toDomain
import com.afora.shared.domain.model.LectureSession
import com.afora.shared.domain.model.TimetableSlot
import com.afora.shared.util.Result

/**
 * HTTP implementation of TimetableRepository.
 *
 * Endpoints:
 *  GET /timetable/teacher/{teacherId}?date=YYYY-MM-DD
 *  GET /timetable/teacher/{teacherId}/today
 *  GET /timetable/student/{studentId}?semesterId=
 *  GET /timetable/class/{classSectionId}?semesterId=
 */
class HttpTimetableRepository(
    private val apiClient: ApiClient
) : TimetableRepository {

    override suspend fun getTeacherSchedule(
        teacherId: Long,
        date: String
    ): Result<List<LectureSession>> {
        // Lecture session list endpoint — Phase A4 implementation
        // Returns empty list until lecture_sessions table and API are wired up
        return Result.Success(emptyList())
    }

    override suspend fun getTeacherTodaySchedule(teacherId: Long): Result<List<LectureSession>> {
        // Lecture session list endpoint — Phase A4 implementation
        return Result.Success(emptyList())
    }

    override suspend fun getStudentTimetable(
        studentId: Long,
        semesterId: Long
    ): Result<List<TimetableSlot>> {
        val result: Result<ApiResponse<List<TimetableSlotApiResponse>>> = apiClient.get(
            endpoint = "/timetable/student/$studentId",
            params = mapOf("semesterId" to semesterId)
        )
        return result.mapSlots()
    }

    override suspend fun getClassTimetable(
        classSectionId: Long,
        semesterId: Long
    ): Result<List<TimetableSlot>> {
        val result: Result<ApiResponse<List<TimetableSlotApiResponse>>> = apiClient.get(
            endpoint = "/timetable/class/$classSectionId",
            params = mapOf("semesterId" to semesterId)
        )
        return result.mapSlots()
    }

    private fun Result<ApiResponse<List<TimetableSlotApiResponse>>>.mapSlots(): Result<List<TimetableSlot>> =
        when (this) {
            is Result.Success -> Result.Success(data.data?.map { it.toDomain() } ?: emptyList())
            is Result.Error -> this
        }
}
