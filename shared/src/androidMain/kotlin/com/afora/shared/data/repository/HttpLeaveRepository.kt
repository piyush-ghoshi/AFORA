package com.afora.shared.data.repository

import com.afora.shared.data.api.ApiClient
import com.afora.shared.data.api.LeaveClassOption
import com.afora.shared.data.api.LeaveRequestResponse
import com.afora.shared.data.api.ReviewLeaveRequest
import com.afora.shared.data.api.SubmitLeaveRequest
import com.afora.shared.data.api.response.ApiResponse
import com.afora.shared.domain.model.LeaveStatus
import com.afora.shared.util.AppError
import com.afora.shared.util.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * HTTP implementation of [LeaveRepository] for the Leave Requests feature.
 *
 * Follows the [HttpAttendanceRepository] pattern exactly: constructor-injected
 * [ApiClient], unwrap `result.data.data`, return [Result] carrying [AppError]
 * on failure, humanise the backend's `error.message` for display.
 */
class HttpLeaveRepository(
    private val apiClient: ApiClient
) : LeaveRepository {

    override suspend fun submitLeaveRequest(
        fromDate: String,
        toDate: String,
        reason: String,
        classSectionId: Long?
    ): Result<LeaveRequestResponse> {
        val body = SubmitLeaveRequest(
            fromDate = fromDate,
            toDate = toDate,
            reason = reason,
            classSectionId = classSectionId
        )
        return when (
            val result: Result<ApiResponse<LeaveRequestResponse>> =
                apiClient.post(endpoint = "/api/leave/requests", body = body)
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No leave request returned from submission"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun getMyClassOptions(): Result<List<LeaveClassOption>> {
        return when (
            val result: Result<ApiResponse<List<LeaveClassOption>>> =
                apiClient.get(endpoint = "/api/leave/classes")
        ) {
            is Result.Success -> Result.Success(result.data.data ?: emptyList())
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun getMyLeaveRequests(status: LeaveStatus?): Result<List<LeaveRequestResponse>> {
        val params = status?.let { mapOf("status" to it.name) } ?: emptyMap()
        return when (
            val result: Result<ApiResponse<List<LeaveRequestResponse>>> =
                apiClient.get(endpoint = "/api/leave/requests/me", params = params)
        ) {
            is Result.Success -> Result.Success(result.data.data ?: emptyList())
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun getPendingLeaveRequests(teacherId: Long): Result<List<LeaveRequestResponse>> {
        return when (
            val result: Result<ApiResponse<List<LeaveRequestResponse>>> =
                apiClient.get(endpoint = "/api/leave/requests/pending/$teacherId")
        ) {
            is Result.Success -> Result.Success(result.data.data ?: emptyList())
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun approveLeaveRequest(
        leaveRequestId: Long,
        notes: String?
    ): Result<LeaveRequestResponse> {
        return when (
            val result: Result<ApiResponse<LeaveRequestResponse>> =
                apiClient.post(
                    endpoint = "/api/leave/requests/$leaveRequestId/approve",
                    body = ReviewLeaveRequest(notes = notes)
                )
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No leave request returned from approval"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun rejectLeaveRequest(
        leaveRequestId: Long,
        notes: String
    ): Result<LeaveRequestResponse> {
        return when (
            val result: Result<ApiResponse<LeaveRequestResponse>> =
                apiClient.post(
                    endpoint = "/api/leave/requests/$leaveRequestId/reject",
                    body = ReviewLeaveRequest(notes = notes)
                )
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No leave request returned from rejection"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun cancelLeaveRequest(leaveRequestId: Long): Result<LeaveRequestResponse> {
        return when (
            val result: Result<ApiResponse<LeaveRequestResponse>> =
                apiClient.post(
                    endpoint = "/api/leave/requests/$leaveRequestId/cancel",
                    body = Unit
                )
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No leave request returned from cancellation"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    // --- tolerant error decoding (identical to HttpAttendanceRepository) ---

    private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class BackendErrorEnvelope(val error: BackendErrorDetail? = null)

    @Serializable
    private data class BackendErrorDetail(val message: String, val code: String? = null)

    private fun humanise(error: AppError): AppError {
        if (error !is AppError.ApiError) return error
        val decoded = try {
            errorJson.decodeFromString<BackendErrorEnvelope>(error.message)
        } catch (e: Exception) {
            null
        }
        val message = decoded?.error?.message ?: error.message
        return AppError.ApiError(code = error.code, message = message)
    }
}