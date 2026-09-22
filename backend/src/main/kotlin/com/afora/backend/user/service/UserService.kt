package com.afora.backend.user.service

import com.afora.backend.common.exception.ConflictException
import com.afora.backend.common.exception.NotFoundException
import com.afora.backend.user.dto.*
import com.afora.backend.user.entity.User
import com.afora.backend.user.entity.UserRole
import com.afora.backend.user.repository.StudentRepository
import com.afora.backend.user.repository.TeacherRepository
import com.afora.backend.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * User management service.
 *
 * Handles:
 * - Firebase user sync (create local User on first login/register)
 * - Profile retrieval and updates
 */
@Service
@Transactional
class UserService(
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val teacherRepository: TeacherRepository
) {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * Sync a Firebase-authenticated user to the local database.
     *
     * Called after Firebase registration from the Android app.
     * If the user already exists (re-login), we update last_login_at and return the profile.
     *
     * @return pair of (response, isNewUser)
     */
    fun syncUser(request: SyncUserRequest): AuthSyncResponse {
        val existing = userRepository.findByFirebaseUid(request.firebaseUid)

        return if (existing.isPresent) {
            // Already exists — update last login and return
            val user = existing.get().also { it.updateLastLogin() }
            userRepository.save(user)
            logger.debug("Returning existing user: ${user.email}")

            val student = if (user.role == UserRole.STUDENT)
                studentRepository.findByUserId(user.id!!).orElse(null)
            else null
            val teacher = if (user.role == UserRole.TEACHER)
                teacherRepository.findByUserId(user.id!!).orElse(null)
            else null

            AuthSyncResponse(
                user = UserResponse.from(user, student, teacher),
                isNewUser = false
            )
        } else {
            // New user — validate role and persist
            if (userRepository.existsByEmail(request.email)) {
                throw ConflictException("Email already registered: ${request.email}")
            }

            val role = try {
                UserRole.valueOf(request.role.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid role '${request.role}'. Must be STUDENT or TEACHER.")
            }
            if (role == UserRole.ADMIN) {
                throw IllegalArgumentException("Admin accounts cannot be created via self-registration.")
            }

            val user = userRepository.save(
                User(
                    firebaseUid = request.firebaseUid,
                    email = request.email,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    role = role
                ).also { it.updateLastLogin() }
            )

            logger.info("Created new user: ${user.email} (${user.role})")
            AuthSyncResponse(
                user = UserResponse.from(user),
                isNewUser = true
            )
        }
    }

    /**
     * Get full user profile by Firebase UID.
     * Includes student/teacher sub-profile if applicable.
     */
    @Transactional(readOnly = true)
    fun getUserByFirebaseUid(firebaseUid: String): UserResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            .orElseThrow { NotFoundException("User not found for UID: $firebaseUid") }

        val student = if (user.role == UserRole.STUDENT)
            studentRepository.findByUserId(user.id!!).orElse(null)
        else null
        val teacher = if (user.role == UserRole.TEACHER)
            teacherRepository.findByUserId(user.id!!).orElse(null)
        else null

        return UserResponse.from(user, student, teacher)
    }

    /**
     * Update user's display name and/or profile picture.
     */
    fun updateProfile(firebaseUid: String, request: UpdateProfileRequest): UserResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            .orElseThrow { NotFoundException("User not found for UID: $firebaseUid") }

        user.firstName = request.firstName
        user.lastName = request.lastName
        request.profilePictureUrl?.let { user.profilePictureUrl = it }
        userRepository.save(user)

        val student = if (user.role == UserRole.STUDENT)
            studentRepository.findByUserId(user.id!!).orElse(null)
        else null
        val teacher = if (user.role == UserRole.TEACHER)
            teacherRepository.findByUserId(user.id!!).orElse(null)
        else null

        return UserResponse.from(user, student, teacher)
    }
}
