package com.afora.backend.user.controller

import com.afora.backend.common.dto.ApiResponse
import com.afora.backend.common.exception.UnauthorizedException
import com.afora.backend.user.dto.UpdateProfileRequest
import com.afora.backend.user.dto.UserResponse
import com.afora.backend.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * User profile controller.
 *
 * All endpoints require a valid Firebase ID token (Authorization: Bearer <token>).
 * Spring Security extracts the Firebase UID from the token and sets it as the principal.
 */
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService
) {

    /**
     * GET /api/users/me
     *
     * Returns the authenticated user's full profile including role-specific data
     * (StudentProfileResponse or TeacherProfileResponse as applicable).
     */
    @GetMapping("/me")
    fun getMe(
        @AuthenticationPrincipal firebaseUid: String?
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val uid = firebaseUid ?: throw UnauthorizedException("Authentication required")
        val user = userService.getUserByFirebaseUid(uid)
        return ResponseEntity.ok(ApiResponse.success(user))
    }

    /**
     * PUT /api/users/me
     *
     * Update the authenticated user's display name and/or profile picture URL.
     * Role and email cannot be changed here.
     */
    @PutMapping("/me")
    fun updateMe(
        @AuthenticationPrincipal firebaseUid: String?,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<ApiResponse<UserResponse>> {
        val uid = firebaseUid ?: throw UnauthorizedException("Authentication required")
        val user = userService.updateProfile(uid, request)
        return ResponseEntity.ok(ApiResponse.success(user))
    }
}
