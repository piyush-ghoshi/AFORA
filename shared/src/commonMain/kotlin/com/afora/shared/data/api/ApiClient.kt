package com.afora.shared.data.api

import com.afora.shared.platform.Logger
import com.afora.shared.util.AppError
import com.afora.shared.util.Result
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * API Client for backend communication using Ktor.
 * Handles authentication, serialization, error handling.
 */
class ApiClient(
    private val baseUrl: String = "http://localhost:8080",  // Configurable
    private val tokenProvider: suspend () -> String? = { null }
) {
    
    companion object {
        const val TAG = "ApiClient"
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }
    
    val httpClient = HttpClient {
        // Content negotiation (JSON)
        install(ContentNegotiation) {
            json(json)
        }
        
        // Logging
        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    Logger.debug(TAG, message)
                }
            }
            level = LogLevel.INFO
        }
        
        // Authentication
        install(Auth) {
            bearer {
                loadTokens {
                    val token = tokenProvider()
                    token?.let {
                        BearerTokens(accessToken = it, refreshToken = "")
                    }
                }
            }
        }
        
        // Default timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        
        // Default headers
        defaultRequest {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Accept, ContentType.Application.Json)
        }
    }
    
    /**
     * Build full URL from endpoint.
     */
    fun buildUrl(endpoint: String): String {
        return if (endpoint.startsWith("http")) {
            endpoint
        } else {
            "$baseUrl/api/v1$endpoint"
        }
    }
    
    /**
     * Safe API call wrapper with error handling.
     */
    suspend inline fun <reified T> safeApiCall(
        crossinline call: suspend () -> HttpResponse
    ): Result<T> {
        return try {
            val response = call()
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> {
                    Result.Success(response.body<T>())
                }
                HttpStatusCode.Unauthorized -> {
                    Result.Error(AppError.UnauthorizedError("Session expired. Please login again."))
                }
                HttpStatusCode.NotFound -> {
                    Result.Error(AppError.NotFoundError("Resource not found"))
                }
                else -> {
                    val errorBody = try {
                        response.bodyAsText()
                    } catch (e: Exception) {
                        "Unknown error"
                    }
                    Result.Error(
                        AppError.ApiError(
                            code = response.status.value,
                            message = errorBody
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(TAG, "API call failed", e)
            Result.Error(
                when (e) {
                    is HttpRequestTimeoutException -> {
                        AppError.NetworkError("Request timeout. Please check your connection.")
                    }
                    is io.ktor.client.network.sockets.ConnectTimeoutException -> {
                        AppError.NetworkError("Connection timeout. Please check your connection.")
                    }
                    else -> {
                        AppError.NetworkError(e.message ?: "Network error occurred")
                    }
                }
            )
        }
    }
    
    /**
     * GET request.
     */
    suspend inline fun <reified T> get(
        endpoint: String,
        params: Map<String, Any?> = emptyMap()
    ): Result<T> {
        return safeApiCall {
            httpClient.get(buildUrl(endpoint)) {
                params.forEach { (key, value) ->
                    value?.let { parameter(key, it.toString()) }
                }
            }
        }
    }
    
    /**
     * POST request.
     */
    suspend inline fun <reified T, reified R> post(
        endpoint: String,
        body: R
    ): Result<T> {
        return safeApiCall {
            httpClient.post(buildUrl(endpoint)) {
                setBody(body)
            }
        }
    }
    
    /**
     * PUT request.
     */
    suspend inline fun <reified T, reified R> put(
        endpoint: String,
        body: R
    ): Result<T> {
        return safeApiCall {
            httpClient.put(buildUrl(endpoint)) {
                setBody(body)
            }
        }
    }
    
    /**
     * PATCH request.
     */
    suspend inline fun <reified T, reified R> patch(
        endpoint: String,
        body: R
    ): Result<T> {
        return safeApiCall {
            httpClient.patch(buildUrl(endpoint)) {
                setBody(body)
            }
        }
    }
    
    /**
     * DELETE request.
     */
    suspend inline fun <reified T> delete(
        endpoint: String
    ): Result<T> {
        return safeApiCall {
            httpClient.delete(buildUrl(endpoint))
        }
    }
    
    /**
     * Close the client.
     */
    fun close() {
        httpClient.close()
    }
}
