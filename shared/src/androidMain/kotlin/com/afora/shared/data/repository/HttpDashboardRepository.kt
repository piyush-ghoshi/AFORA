package com.afora.shared.data.repository

import com.afora.shared.data.api.ApiClient
import com.afora.shared.data.api.response.ApiResponse
import com.afora.shared.data.api.response.StudentDashboardStatsApiResponse
import com.afora.shared.data.api.response.TeacherDashboardStatsApiResponse
import com.afora.shared.data.mapper.toDomain
import com.afora.shared.util.AppError
import com.afora.shared.util.Result

/**
 * HTTP implementation of DashboardRepository.
 * 
 * Endpoints:
 *  GET /api/dashboard/student/{studentId}?semesterId=X
 *  GET /api/dashboard/teacher/{teacherId}?date=YYYY-MM-DD
 */
class HttpDashboardRepository(
    private val apiClient: ApiClient
) : DashboardRepository {

    override suspend fun getStudentDashboard(
        studentId: Long,
        semesterId: Long?
    ): Result<StudentDashboardStats> {
        val params = semesterId?.let { mapOf("semesterId" to it) } ?: emptyMap()
        
        return when (val result: Result<ApiResponse<StudentDashboardStatsApiResponse>> = 
            apiClient.get(
                endpoint = "/api/dashboard/student/$studentId",
                params = params
            )
        ) {
            is Result.Success -> {
                result.data.data?.let {
                    Result.Success(it.toDomain())
                } ?: Result.Error(AppError.NotFoundError("No data returned from server"))
            }
            is Result.Error -> Result.Error(result.error)
        }
    }

    override suspend fun getTeacherDashboard(
        teacherId: Long,
        date: String?
    ): Result<TeacherDashboardStats> {
        val params = date?.let { mapOf("date" to it) } ?: emptyMap()
        
        return when (val result: Result<ApiResponse<TeacherDashboardStatsApiResponse>> = 
            apiClient.get(
                endpoint = "/api/dashboard/teacher/$teacherId",
                params = params
            )
        ) {
            is Result.Success -> {
                result.data.data?.let {
                    Result.Success(it.toDomain())
                } ?: Result.Error(AppError.NotFoundError("No data returned from server"))
            }
            is Result.Error -> Result.Error(result.error)
        }
    }
}
