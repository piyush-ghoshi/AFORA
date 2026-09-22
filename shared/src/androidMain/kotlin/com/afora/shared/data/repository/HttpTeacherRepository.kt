package com.afora.shared.data.repository

import com.afora.shared.data.api.ApiClient
import com.afora.shared.data.api.response.ApiResponse
import com.afora.shared.data.api.response.TeacherApiResponse
import com.afora.shared.data.mapper.toDomain
import com.afora.shared.domain.model.Teacher
import com.afora.shared.util.AppError
import com.afora.shared.util.Result

/**
 * HTTP implementation of TeacherRepository.
 *
 * Endpoints:
 *  GET /teachers/{id}
 *  GET /teachers/user/{userId}
 *  GET /teachers?subjectId=
 *  GET /teachers?departmentId=
 */
class HttpTeacherRepository(
    private val apiClient: ApiClient
) : TeacherRepository {

    override suspend fun getTeacherById(teacherId: Long): Result<Teacher> {
        val result: Result<ApiResponse<TeacherApiResponse>> = apiClient.get(
            endpoint = "/teachers/$teacherId"
        )
        return when (result) {
            is Result.Success -> {
                val teacher = result.data.data?.toDomain()
                if (teacher != null) Result.Success(teacher)
                else Result.Error(AppError.NotFoundError("Teacher not found"))
            }
            is Result.Error -> result
        }
    }

    override suspend fun getTeacherByUserId(userId: Long): Result<Teacher> {
        val result: Result<ApiResponse<TeacherApiResponse>> = apiClient.get(
            endpoint = "/teachers/user/$userId"
        )
        return when (result) {
            is Result.Success -> {
                val teacher = result.data.data?.toDomain()
                if (teacher != null) Result.Success(teacher)
                else Result.Error(AppError.NotFoundError("Teacher not found"))
            }
            is Result.Error -> result
        }
    }

    override suspend fun getTeachersBySubject(subjectId: Long): Result<List<Teacher>> {
        val result: Result<ApiResponse<List<TeacherApiResponse>>> = apiClient.get(
            endpoint = "/teachers",
            params = mapOf("subjectId" to subjectId)
        )
        return result.mapList()
    }

    override suspend fun getTeachersByDepartment(departmentId: Long): Result<List<Teacher>> {
        val result: Result<ApiResponse<List<TeacherApiResponse>>> = apiClient.get(
            endpoint = "/teachers",
            params = mapOf("departmentId" to departmentId)
        )
        return result.mapList()
    }

    private fun Result<ApiResponse<List<TeacherApiResponse>>>.mapList(): Result<List<Teacher>> =
        when (this) {
            is Result.Success -> Result.Success(data.data?.map { it.toDomain() } ?: emptyList())
            is Result.Error -> this
        }
}
