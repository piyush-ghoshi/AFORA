package com.academia.shared.data.repository

import com.academia.shared.data.api.ApiClient
import com.academia.shared.data.api.response.ApiResponse
import com.academia.shared.data.api.response.StudentApiResponse
import com.academia.shared.data.mapper.toDomain
import com.academia.shared.domain.model.Student
import com.academia.shared.util.AppError
import com.academia.shared.util.Result

/**
 * HTTP implementation of StudentRepository.
 *
 * Endpoints (Phase A3 foundation — returns empty list when endpoints not yet live):
 *  GET /students/{id}
 *  GET /students/roll/{rollNumber}
 *  GET /students?classSectionId=&subjectId=&semesterId=
 *  GET /students/search?q=
 */
class HttpStudentRepository(
    private val apiClient: ApiClient
) : StudentRepository {

    override suspend fun getStudentById(studentId: Long): Result<Student> {
        val result: Result<ApiResponse<StudentApiResponse>> = apiClient.get(
            endpoint = "/students/$studentId"
        )
        return result.mapData { it?.toDomain() }
    }

    override suspend fun getStudentByRollNumber(rollNumber: String): Result<Student> {
        val result: Result<ApiResponse<StudentApiResponse>> = apiClient.get(
            endpoint = "/students/roll/$rollNumber"
        )
        return result.mapData { it?.toDomain() }
    }

    override suspend fun getStudentsByClassSection(classSectionId: Long): Result<List<Student>> {
        val result: Result<ApiResponse<List<StudentApiResponse>>> = apiClient.get(
            endpoint = "/students",
            params = mapOf("classSectionId" to classSectionId)
        )
        return result.mapList()
    }

    override suspend fun getEnrolledStudents(
        classSectionId: Long,
        subjectId: Long,
        semesterId: Long
    ): Result<List<Student>> {
        val result: Result<ApiResponse<List<StudentApiResponse>>> = apiClient.get(
            endpoint = "/students",
            params = mapOf(
                "classSectionId" to classSectionId,
                "subjectId" to subjectId,
                "semesterId" to semesterId
            )
        )
        return result.mapList()
    }

    override suspend fun searchStudents(query: String, limit: Int): Result<List<Student>> {
        val result: Result<ApiResponse<List<StudentApiResponse>>> = apiClient.get(
            endpoint = "/students/search",
            params = mapOf("q" to query, "limit" to limit)
        )
        return result.mapList()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun <T> Result<ApiResponse<T>>.mapData(
        transform: (T?) -> Student?
    ): Result<Student> = when (this) {
        is Result.Success -> {
            val item = transform(data.data)
            if (item != null) Result.Success(item)
            else Result.Error(AppError.NotFoundError("Student not found"))
        }
        is Result.Error -> this
    }

    private fun Result<ApiResponse<List<StudentApiResponse>>>.mapList(): Result<List<Student>> =
        when (this) {
            is Result.Success -> Result.Success(data.data?.map { it.toDomain() } ?: emptyList())
            is Result.Error -> this
        }
}
