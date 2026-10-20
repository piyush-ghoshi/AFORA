package com.afora.backend.auth.controller

import com.afora.backend.common.dto.ApiResponse
import com.afora.backend.common.exception.ForbiddenException
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.user.dto.AuthSyncResponse
import com.afora.backend.user.dto.SyncUserRequest
import com.afora.backend.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * Authentication controller.
 *
 * Auth in AFORA is handled by Firebase on the client side.
 * These endpoints handle the backend side of auth:
 *  - POST /api/auth/sync (requires a verified Firebase token; the body's UID must match it) — called after Firebase registration/login to persist/return local user
 */
@RestController
@RequestMapping("/auth")
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
        @AuthenticationPrincipal firebaseUid: String?,
        @Valid @RequestBody request: SyncUserRequest
    ): ResponseEntity<ApiResponse<AuthSyncResponse>> {
        val verifiedUid = firebaseUid ?: throw UnauthorizedException("Authentication required")
        if (request.firebaseUid != verifiedUid) {
            throw ForbiddenException("The request does not match the signed-in account")
        }
        val response = userService.syncUser(request)
        val status = if (response.isNewUser) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(status).body(ApiResponse.success(response))
    }
}
