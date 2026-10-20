package com.afora.backend.auth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import kotlin.test.assertEquals

/**
 * Boots the whole application with security on and drives the real sign-up and
 * profile endpoints. These are the checks the unit tests could not make:
 *
 *  - The server already serves everything under the `/api` context path, so no
 *    controller may add `/api` itself. A doubled prefix made `/api/users/me` and
 *    `/api/auth/sync` unreachable while the app's calls returned 404.
 *  - `auth/sync` must require a verified token and act only on that token's UID.
 *  - A missing token must be a 401 so the app can tell an expired session apart.
 *
 * Paths here are the in-app paths (what the context path is stripped to).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthRoutingIntegrationTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var handlerMapping: RequestMappingHandlerMapping

    private fun signedInAs(uid: String) =
        authentication(UsernamePasswordAuthenticationToken(uid, null, emptyList()))

    private fun syncBody(uid: String, email: String = "alex@univ.edu", role: String = "STUDENT", idNumber: String? = "CS-2023-048"): String {
        val id = if (idNumber == null) "null" else "\"$idNumber\""
        return """{"firebaseUid":"$uid","email":"$email","firstName":"Alex","lastName":"Morgan","role":"$role","idNumber":$id}"""
    }

    @Test
    fun `no controller mapping repeats the api context path`() {
        val doubled = handlerMapping.handlerMethods.keys
            .flatMap { it.pathPatternsCondition?.patternValues ?: emptySet() }
            .filter { it == "/api" || it.startsWith("/api/") }
        assertEquals(emptyList<String>(), doubled, "The context path already provides /api")
    }

    @Test
    fun `health is public`() {
        mockMvc.perform(get("/health")).andExpect(status().isOk)
    }

    @Test
    fun `profile requires a token and answers 401 without one`() {
        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized)
    }

    @Test
    fun `sync requires a token and answers 401 without one`() {
        mockMvc.perform(
            post("/auth/sync").contentType(MediaType.APPLICATION_JSON).content(syncBody("uid-1"))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `sync refuses a body whose uid differs from the verified token`() {
        mockMvc.perform(
            post("/auth/sync")
                .with(signedInAs("uid-attacker"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(syncBody("uid-victim"))
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `a student signs up then loads and edits their profile`() {
        mockMvc.perform(
            post("/auth/sync")
                .with(signedInAs("uid-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(syncBody("uid-1"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.isNewUser").value(true))
            .andExpect(jsonPath("$.data.user.role").value("STUDENT"))
            .andExpect(jsonPath("$.data.user.student.rollNumber").value("CS-2023-048"))

        mockMvc.perform(get("/users/me").with(signedInAs("uid-1")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.email").value("alex@univ.edu"))
            .andExpect(jsonPath("$.data.student.id").isNumber)

        mockMvc.perform(
            put("/users/me")
                .with(signedInAs("uid-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Alexander","lastName":"Morgan"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.firstName").value("Alexander"))

        mockMvc.perform(get("/users/me").with(signedInAs("uid-1")))
            .andExpect(jsonPath("$.data.firstName").value("Alexander"))
    }

    @Test
    fun `a teacher signs up with an employee id`() {
        mockMvc.perform(
            post("/auth/sync")
                .with(signedInAs("uid-t"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(syncBody("uid-t", email = "dana@univ.edu", role = "TEACHER", idNumber = "FAC-104"))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.user.teacher.employeeId").value("FAC-104"))
    }

    @Test
    fun `signing up without a roll number is a 400 that says why`() {
        mockMvc.perform(
            post("/auth/sync")
                .with(signedInAs("uid-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(syncBody("uid-1", idNumber = null))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("roll number")))
    }

    @Test
    fun `a roll number already taken is a 409`() {
        mockMvc.perform(
            post("/auth/sync").with(signedInAs("uid-1")).contentType(MediaType.APPLICATION_JSON).content(syncBody("uid-1"))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/auth/sync")
                .with(signedInAs("uid-2"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(syncBody("uid-2", email = "other@univ.edu"))
        ).andExpect(status().isConflict)
    }

    @Test
    fun `signing in again returns the same account and is not new`() {
        mockMvc.perform(
            post("/auth/sync").with(signedInAs("uid-1")).contentType(MediaType.APPLICATION_JSON).content(syncBody("uid-1"))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/auth/sync")
                .with(signedInAs("uid-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(syncBody("uid-1", idNumber = null))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.isNewUser").value(false))
            .andExpect(jsonPath("$.data.user.student.rollNumber").value("CS-2023-048"))
    }
}