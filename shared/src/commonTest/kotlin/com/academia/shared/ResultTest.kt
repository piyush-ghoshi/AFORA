package com.academia.shared

import com.academia.shared.util.AppError
import com.academia.shared.util.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Foundation test for Result wrapper.
 * Phase A1: Smoke test to verify KMP test infrastructure works.
 */
class ResultTest {

    @Test
    fun `Result Success should contain value`() {
        val result = Result.Success("test value")
        
        assertTrue(result is Result.Success)
        assertEquals("test value", result.data)
    }

    @Test
    fun `Result Error should contain error`() {
        val error = AppError.NetworkError("Connection failed")
        val result = Result.Error<String>(error)
        
        assertTrue(result is Result.Error)
        assertEquals(error, result.error)
    }

    @Test
    fun `map should transform success value`() {
        val result: Result<Int> = Result.Success(5)
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped is Result.Success)
        assertEquals(10, (mapped as Result.Success).data)
    }

    @Test
    fun `map should preserve error`() {
        val error = AppError.NetworkError("Test error")
        val result: Result<Int> = Result.Error(error)
        val mapped = result.map { it * 2 }
        
        assertTrue(mapped is Result.Error)
        assertEquals(error, (mapped as Result.Error).error)
    }
}
