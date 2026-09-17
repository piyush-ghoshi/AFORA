package com.academia.backend

import com.academia.backend.common.controller.HealthController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Foundation test for HealthController.
 * Phase A1: Smoke test to verify Spring Boot context loads and health endpoint works.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthControllerTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `health endpoint should return UP status`() {
        val response = restTemplate.getForEntity("/health", Map::class.java)
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(true, response.body!!["success"])
    }

    @Test
    fun `HealthController should be instantiated`() {
        val controller = HealthController()
        val response = controller.health()
        
        assertNotNull(response)
        assertEquals(true, response.success)
        assertNotNull(response.data)
    }
}
