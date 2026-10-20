package com.afora.shared.data.repository

import com.afora.shared.data.api.AmendAttendanceRequest
import com.afora.shared.data.api.AmendmentResultResponse
import com.afora.shared.data.api.ApiClient
import com.afora.shared.data.api.AttendanceConfirmationResponse
import com.afora.shared.data.api.AuditHistoryResponse
import com.afora.shared.data.api.ConfirmAttendanceRequest
import com.afora.shared.data.api.RosterResponse
import com.afora.shared.data.api.SessionSummary
import com.afora.shared.data.api.StudentAttendanceResponse
import com.afora.shared.data.api.response.ApiResponse
import com.afora.shared.domain.model.AttendanceRecord
import com.afora.shared.domain.model.ClassSection
import com.afora.shared.domain.model.LectureSession
import com.afora.shared.domain.model.Student
import com.afora.shared.util.AppError
import com.afora.shared.util.Result
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * HTTP implementation of [AttendanceRepository] for the Manual_Marking_Flow.
 *
 * Follows the [HttpDashboardRepository] pattern: constructor-injected
 * [ApiClient], unwrap `result.data.data`, return [com.afora.shared.util.Result]
 * carrying [AppError] on failure.
 *
 * Endpoint strings are full paths beginning `/api/` because
 * [ApiClient.buildUrl] prepends only the base URL.
 *
 * The camera-flow members, plus the two pre-existing `getStudentAttendance`/
 * `getStudentAttendanceStats` signatures that have no backend endpoint yet,
 * return [AppError.UnknownError] naming the member rather than `TODO()` or a
 * thrown `NotImplementedError` -- a thrown error inside a coroutine would
 * surface as an app crash rather than a handled error (R11.12).
 */
class HttpAttendanceRepository(
    private val apiClient: ApiClient
) : AttendanceRepository {

    private fun unimplemented(memberName: String): AppError.UnknownError =
        AppError.UnknownError("$memberName is not implemented in the manual marking flow")

    // --- Manual_Marking_Flow (R11.13) ---

    override suspend fun getSessionRoster(lectureSessionId: Long): Result<RosterResponse> {
        return when (
            val result: Result<ApiResponse<RosterResponse>> =
                apiClient.get(endpoint = "/api/attendance/sessions/$lectureSessionId/roster")
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No roster returned for session $lectureSessionId"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun activateSession(lectureSessionId: Long): Result<SessionSummary> {
        return when (
            val result: Result<ApiResponse<SessionSummary>> =
                apiClient.post(
                    endpoint = "/api/attendance/sessions/$lectureSessionId/activate",
                    body = Unit
                )
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No session summary returned for session $lectureSessionId"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun cancelSession(lectureSessionId: Long): Result<SessionSummary> {
        return when (
            val result: Result<ApiResponse<SessionSummary>> =
                apiClient.post(
                    endpoint = "/api/attendance/sessions/$lectureSessionId/cancel",
                    body = Unit
                )
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No session summary returned for session $lectureSessionId"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun confirmManualAttendance(
        lectureSessionId: Long,
        request: ConfirmAttendanceRequest
    ): Result<AttendanceConfirmationResponse> {
        return when (
            val result: Result<ApiResponse<AttendanceConfirmationResponse>> =
                apiClient.post(
                    endpoint = "/api/attendance/sessions/$lectureSessionId/confirm",
                    body = request
                )
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No confirmation returned for session $lectureSessionId"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun amendAttendance(
        lectureSessionId: Long,
        request: AmendAttendanceRequest
    ): Result<AmendmentResultResponse> {
        return when (
            val result: Result<ApiResponse<AmendmentResultResponse>> =
                apiClient.post(
                    endpoint = "/api/attendance/sessions/$lectureSessionId/amend",
                    body = request
                )
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No amendment result returned for session $lectureSessionId"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun getAuditHistory(lectureSessionId: Long): Result<AuditHistoryResponse> {
        return when (
            val result: Result<ApiResponse<AuditHistoryResponse>> =
                apiClient.get(endpoint = "/api/attendance/sessions/$lectureSessionId/audit-history")
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No audit history returned for session $lectureSessionId"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    // --- Pre-existing signatures with no backend endpoint yet (R11.1, R11.2) ---

    override suspend fun getStudentAttendance(
        studentId: Long,
        subjectId: Long,
        semesterId: String
    ): Result<List<AttendanceRecord>> = Result.Error(unimplemented("getStudentAttendance"))

    /**
     * GET /api/attendance/student/{studentId} — subject-wise + session-wise
     * attendance detail for the student detail screen (Phase 4).
     */
    override suspend fun getStudentAttendanceDetail(
        studentId: Long,
        semesterId: String?
    ): Result<StudentAttendanceResponse> {
        val endpoint = if (semesterId != null) {
            "/api/attendance/student/$studentId?semesterId=$semesterId"
        } else {
            "/api/attendance/student/$studentId"
        }
        return when (
            val result: Result<ApiResponse<StudentAttendanceResponse>> = apiClient.get(endpoint)
        ) {
            is Result.Success -> result.data.data?.let { Result.Success(it) }
                ?: Result.Error(AppError.NotFoundError("No attendance data returned for student $studentId"))
            is Result.Error -> Result.Error(humanise(result.error))
        }
    }

    override suspend fun getStudentAttendanceStats(
        studentId: Long,
        semesterId: String
    ): Result<AttendanceStats> = Result.Error(unimplemented("getStudentAttendanceStats"))

    // --- camera-flow members, left unimplemented (R11.12) ---

    override suspend fun startLectureSession(lectureId: Long): Result<LectureSessionWithStudents> =
        Result.Error(unimplemented("startLectureSession"))

    override suspend fun getEnrolledStudentsWithEmbeddings(
        lectureSessionId: Long
    ): Result<List<StudentWithEmbedding>> =
        Result.Error(unimplemented("getEnrolledStudentsWithEmbeddings"))

    override suspend fun getAttendanceByLectureSession(
        lectureSessionId: Long
    ): Result<List<AttendanceRecord>> =
        Result.Error(unimplemented("getAttendanceByLectureSession"))

    override suspend fun getLectureSession(lectureSessionId: Long): Result<LectureSession> =
        Result.Error(unimplemented("getLectureSession"))

    override suspend fun cancelLectureSession(lectureSessionId: Long): Result<Unit> =
        Result.Error(unimplemented("cancelLectureSession"))

    // --- tolerant error decoding ---

    private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    private data class BackendErrorEnvelope(val error: BackendErrorDetail? = null)

    @Serializable
    private data class BackendErrorDetail(val message: String, val code: String? = null)

    /**
     * Replaces an [AppError.ApiError]'s raw-JSON message with the backend's
     * human `error.message`, so the Compose banner shows prose rather than
     * raw JSON. `ignoreUnknownKeys` skips the backend's `details` field
     * whatever shape it has (an object today, an array in the shared
     * `ErrorResponse` -- that mismatch is sidestepped here without touching
     * the shared `ErrorResponse` type). Falls back to the raw body, and
     * passes every other [AppError] variant through unchanged.
     */
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