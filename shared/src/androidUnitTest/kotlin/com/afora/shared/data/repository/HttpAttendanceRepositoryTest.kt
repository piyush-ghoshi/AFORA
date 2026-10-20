package com.afora.shared.data.repository

import com.afora.shared.data.api.ApiClient
import com.afora.shared.data.api.ConfirmAttendanceRequest
import com.afora.shared.data.api.AmendAttendanceRequest
import com.afora.shared.data.api.SubmittedAttendanceEntry
import com.afora.shared.data.api.AmendmentEntry
import com.afora.shared.domain.model.AttendanceStatus
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import com.afora.shared.util.AppError
import com.afora.shared.util.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `MockEngine`-based tests for [HttpAttendanceRepository]
 * (`.kiro/specs/attendance-marking/tasks.md` task 12.2), following the
 * [com.afora.shared.data.api.ApiClientAuthTest] harness pattern: a
 * [MockEngine] records the request it receives and hands back a canned
 * response, and an [ApiClient] is built with that engine so no real network
 * call is made.
 *
 * One test per [AttendanceRepository] member asserts the exact
 * `/api/attendance/sessions/...` path and HTTP method the task calls for
 * (Binding Convention 6). A final test asserts that a malformed 200 body is
 * handled by [HttpAttendanceRepository] exactly the way the existing shared
 * `com.afora.shared.data.api.ApiClient.safeApiCall` already handles it for
 * every other repository (e.g. `HttpFaceProfileRepository`) -- no new branch
 * is being verified here, only that this repository goes through the same
 * shared code path.
 *
 * **Validates: Requirements 1.3, 2.1, 4.2**
 */
class HttpAttendanceRepositoryTest {

    /** Captures the single request the repository call makes, then replies with [responseBody]. */
    private fun repository(
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        captured: MutableList<io.ktor.client.request.HttpRequestData> = mutableListOf()
    ): Pair<HttpAttendanceRepository, MutableList<io.ktor.client.request.HttpRequestData>> {
        val engine = MockEngine { request ->
            captured += request
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val apiClient = ApiClient(baseUrl = "http://test", engine = engine)
        return HttpAttendanceRepository(apiClient) to captured
    }

    // ── getSessionRoster(lectureSessionId) ───────────────────────────────────

    @Test
    fun `getSessionRoster GETs to the exact roster path`() = runTest {
        val (repo, requests) = repository(
            responseBody = """{"success":true,"data":{"sessionId":42,"classSectionId":1,"subjectId":1,"subjectCode":"CS101","subjectName":"Introduction to Computer Science","sessionDate":"2026-02-15","startTime":"09:00","endTime":"10:30","sessionStatus":"ACTIVE","presentCount":0,"absentCount":0,"onLeaveCount":0,"amendmentWindowOpen":false,"entries":[]}}"""
        )

        val result = repo.getSessionRoster(lectureSessionId = 42L)

        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/api/attendance/sessions/42/roster", request.url.encodedPath)
        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data.sessionId)
    }

    // ── activateSession(lectureSessionId) ────────────────────────────────────

    @Test
    fun `activateSession POSTs to the exact activate path`() = runTest {
        val (repo, requests) = repository(
            responseBody = """{"success":true,"data":{"sessionId":42,"sessionStatus":"ACTIVE","sessionDate":"2026-02-15","startTime":"09:00","endTime":"10:30","presentCount":0,"absentCount":0,"onLeaveCount":0}}"""
        )

        val result = repo.activateSession(lectureSessionId = 42L)

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/attendance/sessions/42/activate", request.url.encodedPath)
        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data.sessionId)
    }

    // ── cancelSession(lectureSessionId) ──────────────────────────────────────

    @Test
    fun `cancelSession POSTs to the exact cancel path`() = runTest {
        val (repo, requests) = repository(
            responseBody = """{"success":true,"data":{"sessionId":42,"sessionStatus":"CANCELLED","sessionDate":"2026-02-15","startTime":"09:00","endTime":"10:30","presentCount":0,"absentCount":0,"onLeaveCount":0}}"""
        )

        val result = repo.cancelSession(lectureSessionId = 42L)

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/attendance/sessions/42/cancel", request.url.encodedPath)
        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data.sessionId)
    }

    // ── confirmManualAttendance(lectureSessionId, request) ───────────────────

    @Test
    fun `confirmManualAttendance POSTs to the exact confirm path`() = runTest {
        val (repo, requests) = repository(
            responseBody = """{"success":true,"data":{"sessionId":42,"submissionId":"550e8400-e29b-41d4-a716-446655440000","sessionStatus":"FINALIZED","totalMarked":2,"presentCount":1,"absentCount":1,"onLeaveCount":0,"confirmedAt":"2026-02-15T10:35:00","defaultedStudentIds":[],"replayed":false}}"""
        )

        val confirmRequest = ConfirmAttendanceRequest(
            submissionId = "550e8400-e29b-41d4-a716-446655440000",
            entries = listOf(
                SubmittedAttendanceEntry(
                    studentId = 1L,
                    status = AttendanceStatus.PRESENT,
                    notes = null
                ),
                SubmittedAttendanceEntry(
                    studentId = 2L,
                    status = AttendanceStatus.ABSENT,
                    notes = null
                )
            ),
            markUnspecifiedAsAbsent = false
        )
        val result = repo.confirmManualAttendance(
            lectureSessionId = 42L,
            request = confirmRequest
        )

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/attendance/sessions/42/confirm", request.url.encodedPath)
        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data.sessionId)
    }

    // ── amendAttendance(lectureSessionId, request) ───────────────────────────

    @Test
    fun `amendAttendance POSTs to the exact amend path`() = runTest {
        val (repo, requests) = repository(
            responseBody = """{"success":true,"data":{"sessionId":42,"sessionStatus":"FINALIZED","amendedCount":1,"unchangedCount":0,"presentCount":1,"absentCount":0,"onLeaveCount":0,"auditEntryIds":[1],"amendedAt":"2026-02-15T11:00:00"}}"""
        )

        val amendRequest = AmendAttendanceRequest(
            reason = "Correcting status",
            entries = listOf(
                AmendmentEntry(
                    studentId = 1L,
                    status = AttendanceStatus.LATE
                )
            )
        )
        val result = repo.amendAttendance(
            lectureSessionId = 42L,
            request = amendRequest
        )

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/attendance/sessions/42/amend", request.url.encodedPath)
        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data.sessionId)
    }

    // ── getAuditHistory(lectureSessionId) ────────────────────────────────────

    @Test
    fun `getAuditHistory GETs to the exact audit-history path`() = runTest {
        val (repo, requests) = repository(
            responseBody = """{"success":true,"data":{"sessionId":42,"entries":[]}}"""
        )

        val result = repo.getAuditHistory(lectureSessionId = 42L)

        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/api/attendance/sessions/42/audit-history", request.url.encodedPath)
        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data.sessionId)
    }

    // ── malformed 200 body: shared ApiClient deserialization-failure branch ──

    /**
     * A malformed 200 body is decoded by Ktor's content negotiation as an
     * `io.ktor.serialization.JsonConvertException`, which is NOT a subtype of
     * the `kotlinx.serialization.SerializationException` that
     * [com.afora.shared.data.api.ApiClient.safeApiCall]'s inner `catch`
     * block names -- so the failure falls through to the outer `catch (e:
     * Exception)` branch and surfaces as [AppError.NetworkError] carrying the
     * decoder's message, for every repository built on [ApiClient] alike.
     * This is the existing, shared behavior this task verifies also applies
     * to [HttpAttendanceRepository]; no new branch is introduced here.
     */
    @Test
    fun `a malformed 200 body is surfaced as the same AppError the shared ApiClient already produces`() = runTest {
        val (repo, _) = repository(responseBody = "not valid json")

        val result = repo.getSessionRoster(lectureSessionId = 42L)

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.NetworkError)
        assertTrue(
            error.message.contains("Unexpected JSON token"),
            "expected the shared ApiClient's decoding-failure message, got: ${error.message}"
        )
    }
}
