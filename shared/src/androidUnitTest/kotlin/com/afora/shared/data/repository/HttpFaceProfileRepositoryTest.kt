package com.afora.shared.data.repository

import com.afora.shared.data.api.ApiClient
import com.afora.shared.data.api.RegisterFaceProfileRequest
import com.afora.shared.util.AppError
import com.afora.shared.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `MockEngine`-based tests for [HttpFaceProfileRepository]
 * (`.kiro/specs/attendance-marking/tasks.md` task 12.2), following the
 * [HttpAttendanceRepositoryTest] harness pattern: a [MockEngine] records the
 * request it receives and hands back a canned response, and an [ApiClient] is
 * built with that engine so no real network call is made.
 *
 * One test per [FaceProfileRepository] member asserts the exact
 * `/api/face-profiles/...` path and HTTP method the implementation calls for
 * (Binding Convention 6). A final test asserts that a malformed 200 body is
 * handled by [HttpFaceProfileRepository] exactly the way the existing shared
 * `com.afora.shared.data.api.ApiClient.safeApiCall` already handles it for
 * every other repository (e.g. `HttpAttendanceRepository`) -- no new branch
 * is being verified here, only that this repository goes through the same
 * shared code path.
 *
 * **Validates: Requirements 1.3, 2.1, 4.2**
 */
class HttpFaceProfileRepositoryTest {

    /** Captures the single request the repository call makes, then replies with [responseBody]. */
    private fun repository(
        responseBody: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        captured: MutableList<io.ktor.client.request.HttpRequestData> = mutableListOf()
    ): Pair<HttpFaceProfileRepository, MutableList<io.ktor.client.request.HttpRequestData>> {
        val engine = MockEngine { request ->
            captured += request
            respond(
                content = responseBody,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val apiClient = ApiClient(baseUrl = "http://test", engine = engine)
        return HttpFaceProfileRepository(apiClient) to captured
    }

    // ── recordConsent() ──────────────────────────────────────────────────────

    @Test
    fun `recordConsent POSTs to the exact consent path`() = runTest {
        val (repo, requests) = repository(
            responseBody = """{"success":true,"data":{"studentId":1,"consentedAt":"2026-02-15T10:00:00"}}"""
        )

        val result = repo.recordConsent()

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/face-profiles/consent", request.url.encodedPath)
        assertTrue(result is Result.Success)
        assertEquals(1L, (result as Result.Success).data.studentId)
    }

    // ── registerFaceProfile(embeddings) ──────────────────────────────────────

    @Test
    fun `registerFaceProfile POSTs to the exact embeddings path`() = runTest {
        val (repo, requests) = repository(
            responseBody = """{"success":true,"data":{"studentId":42,"embeddingCount":5,"dimensions":512,"registeredAt":"2026-02-15T10:05:00"}}"""
        )

        val embeddings = listOf(
            listOf(0.1, 0.2, 0.3),
            listOf(0.4, 0.5, 0.6)
        )
        val result = repo.registerFaceProfile(embeddings = embeddings)

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/face-profiles/embeddings", request.url.encodedPath)
        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data.studentId)
    }

    // ── deleteFaceProfile(studentId) ─────────────────────────────────────────

    @Test
    fun `deleteFaceProfile DELETEs to the exact student-scoped path`() = runTest {
        val (repo, requests) = repository(
            responseBody = """{"success":true,"data":{"studentId":42,"reason":"User requested deletion","deletedAt":"2026-02-15T10:10:00"}}"""
        )

        val result = repo.deleteFaceProfile(studentId = 42L)

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/face-profiles/42", request.url.encodedPath)
        assertTrue(result is Result.Success)
        assertEquals(42L, (result as Result.Success).data.studentId)
    }

    // ── getEnrolledEmbeddingSet(classSectionId) ──────────────────────────────

    @Test
    fun `getEnrolledEmbeddingSet GETs to the exact class-section-scoped enrolled-embeddings path`() = runTest {
        val (repo, requests) = repository(
            responseBody = """{"success":true,"data":{"classSectionId":10,"entries":[],"highConfidenceThreshold":0.85,"mediumConfidenceThreshold":0.70,"unmatchedFloorThreshold":0.50}}"""
        )

        val result = repo.getEnrolledEmbeddingSet(classSectionId = 10L)

        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/api/face-profiles/class-sections/10/enrolled-embeddings", request.url.encodedPath)
        assertTrue(result is Result.Success)
        assertEquals(10L, (result as Result.Success).data.classSectionId)
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
     * to [HttpFaceProfileRepository]; no new branch is introduced here.
     */
    @Test
    fun `a malformed 200 body is surfaced as the same AppError the shared ApiClient already produces`() = runTest {
        val (repo, _) = repository(responseBody = "not valid json")

        val result = repo.recordConsent()

        assertTrue(result is Result.Error)
        val error = (result as Result.Error).error
        assertTrue(error is AppError.NetworkError)
        assertTrue(
            error.message.contains("Unexpected JSON token"),
            "expected the shared ApiClient's decoding-failure message, got: ${error.message}"
        )
    }
}
