package com.afora.shared.data.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.afora.shared.util.Result

/**
 * Regression tests for the stale-token bug.
 *
 * Ktor's bearer plugin loaded the token once and reused it, so after the first
 * sign-in every request went out as that first account -- registering or logging in
 * as someone else acted as the original user, and nothing refreshed once the token
 * expired. [ApiClient] now asks its token provider for each request.
 */
class ApiClientAuthTest {

    @Serializable
    private data class Probe(val success: Boolean = true)

    /** An ApiClient whose "network" records the Authorization header of every request. */
    private fun client(seen: MutableList<String?>, token: () -> String?): ApiClient {
        val engine = MockEngine { request ->
            seen += request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"success":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        return ApiClient(baseUrl = "http://test", tokenProvider = { token() }, engine = engine)
    }

    @Test
    fun `every request carries the token that is current at that moment`() = runTest {
        val seen = mutableListOf<String?>()
        var current: String? = "token-for-first-account"
        val api = client(seen) { current }

        api.get<Probe>("/api/users/me")
        current = "token-for-second-account"      // the user signed in as someone else
        api.get<Probe>("/api/users/me")
        current = "refreshed-token"                // Firebase refreshed an expired token
        api.get<Probe>("/api/users/me")

        assertEquals(
            listOf<String?>("Bearer token-for-first-account",
                "Bearer token-for-second-account",
                "Bearer refreshed-token"
            ),
            seen
        )
    }

    @Test
    fun `a signed-out client sends no Authorization header`() = runTest {
        val seen = mutableListOf<String?>()
        var current: String? = "token"
        val api = client(seen) { current }

        api.get<Probe>("/api/users/me")
        current = null                              // signed out
        api.get<Probe>("/api/users/me")

        assertEquals("Bearer token", seen[0])
        assertNull(seen[1])
    }

    @Test
    fun `a blank token is treated as no token`() = runTest {
        val seen = mutableListOf<String?>()
        val api = client(seen) { "   " }

        api.get<Probe>("/api/users/me")

        assertNull(seen.single())
    }

    @Test
    fun `post requests are authenticated per request too`() = runTest {
        val seen = mutableListOf<String?>()
        var current = "a"
        val api = client(seen) { current }

        api.post<Probe, Probe>("/api/auth/sync", Probe())
        current = "b"
        api.post<Probe, Probe>("/api/auth/sync", Probe())

        assertEquals(listOf<String?>("Bearer a", "Bearer b"), seen)
    }

    @Test
    fun `the token provider is consulted once per request`() = runTest {
        val seen = mutableListOf<String?>()
        var calls = 0
        val api = client(seen) { calls++; "t" }

        api.get<Probe>("/x")
        api.get<Probe>("/y")

        assertEquals(2, calls)
        assertEquals(2, seen.size)
    }

    @Test
    fun `a successful call still returns the decoded body`() = runTest {
        val api = client(mutableListOf()) { "t" }

        val result = api.get<Probe>("/x")

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.success)
    }
}