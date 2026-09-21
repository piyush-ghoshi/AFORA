package com.academia.backend.auth.controller

import com.academia.backend.common.dto.ApiResponse
import com.academia.backend.user.dto.AuthSyncResponse
import com.academia.backend.user.dto.SyncUserRequest
import com.academia.backend.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Authentication controller.
 *
 * Auth in AFORA is handled by Firebase on the client side.
 * These endpoints handle the backend side of auth:
 *  - POST /api/auth/sync  — called after Firebase registration/login to persist/return local user
 */
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userService: UserService
) {

    /**
     * Sync a Firebase-authenticated user to the local database.
     *
     * Called by the Android app after:
     *  1. Successful Firebase registration (new user)
     *  2. First login from a new device (creates local record if missing)
     *
     * The client must include a valid Firebase ID token in the Authorization header.
     * The body contains the profile data to persist (name, role).
     *
     * Returns the full UserResponse and a flag indicating if this was a new user.
     */
    @PostMapping("/sync")
    fun syncUser(
        @Valid @RequestBody request: SyncUserRequest
    ): ResponseEntity<ApiResponse<AuthSyncResponse>> {
        val response = userService.syncUser(request)
        val status = if (response.isNewUser) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(ApiResponse.success(response))
    }
}
