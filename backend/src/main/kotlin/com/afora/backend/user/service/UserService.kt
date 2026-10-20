package com.afora.backend.user.service

import com.afora.backend.common.exception.BadRequestException
import com.afora.backend.common.exception.ConflictException
import com.afora.backend.common.exception.NotFoundException
import com.afora.backend.user.dto.*
import com.afora.backend.user.entity.Student
import com.afora.backend.user.entity.Teacher
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
 * - Firebase user sync (create the local User and its Student/Teacher profile on sign-up)
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

    companion object {
        /**
         * Placeholder stored until an administrator assigns the real value. The
         * `students` and `teachers` tables require these columns, and sign-up only
         * collects the roll number or employee id.
         */
        const val UNASSIGNED = "Unassigned"

        const val NO_PROFILE_MESSAGE =
            "No AFORA profile exists for this account yet. " +
                "Register with your roll number (students) or employee ID (teachers) to create one."
    }

    /**
     * Sync a Firebase-authenticated user to the local database.
     *
     * The caller has already been authenticated; the controller guarantees
     * [SyncUserRequest.firebaseUid] is the verified token's UID.
     *
     * - Existing user: update last login and return the profile. A user created
     *   before profiles existed gets one when an id number is supplied.
     * - New user: requires a roll number or employee id, and creates the user
     *   and its Student/Teacher row in one transaction.
     */
    fun syncUser(request: SyncUserRequest): AuthSyncResponse {
        val existing = userRepository.findByFirebaseUid(request.firebaseUid)
        val idNumber = request.idNumber?.trim().orEmpty()

        if (existing.isPresent) {
            val user = existing.get().also { it.updateLastLogin() }
            userRepository.save(user)
            logger.debug("Returning existing user: ${user.email}")

            var student = profileOfStudent(user)
            var teacher = profileOfTeacher(user)

            if (student == null && teacher == null && idNumber.isNotEmpty()) {
                when (user.role) {
                    UserRole.STUDENT -> student = createStudent(user, idNumber)
                    UserRole.TEACHER -> teacher = createTeacher(user, idNumber)
                    UserRole.ADMIN -> Unit
                }
            }

            return AuthSyncResponse(
                user = UserResponse.from(user, student, teacher),
                isNewUser = false
            )
        }

        // New user -- validate everything before writing anything.
        val role = try {
            UserRole.valueOf(request.role.uppercase())
        } catch (e: IllegalArgumentException) {
            throw BadRequestException("Invalid role '${request.role}'. Must be STUDENT or TEACHER.")
        }
        if (role == UserRole.ADMIN) {
            throw BadRequestException("Admin accounts cannot be created via self-registration.")
        }
        if (idNumber.isEmpty()) {
            throw BadRequestException(NO_PROFILE_MESSAGE)
        }
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("Email already registered: ${request.email}")
        }
        when (role) {
            UserRole.STUDENT -> if (studentRepository.existsByRollNumber(idNumber)) {
                throw ConflictException("Roll number $idNumber is already registered")
            }
            UserRole.TEACHER -> if (teacherRepository.existsByEmployeeId(idNumber)) {
                throw ConflictException("Employee ID $idNumber is already registered")
            }
            UserRole.ADMIN -> Unit
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

        val student = if (role == UserRole.STUDENT) createStudent(user, idNumber) else null
        val teacher = if (role == UserRole.TEACHER) createTeacher(user, idNumber) else null

        logger.info("Created new user: ${user.email} (${user.role})")
        return AuthSyncResponse(
            user = UserResponse.from(user, student, teacher),
            isNewUser = true
        )
    }

    /**
     * Get full user profile by Firebase UID.
     * Includes student/teacher sub-profile if applicable.
     */
    @Transactional(readOnly = true)
    fun getUserByFirebaseUid(firebaseUid: String): UserResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            .orElseThrow { NotFoundException("User not found for UID: $firebaseUid") }

        return UserResponse.from(user, profileOfStudent(user), profileOfTeacher(user))
    }

    /**
     * Update user's display name and/or profile picture.
     */
    fun updateProfile(firebaseUid: String, request: UpdateProfileRequest): UserResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            .orElseThrow { NotFoundException("User not found for UID: $firebaseUid") }

        user.firstName = request.firstName.trim()
        user.lastName = request.lastName.trim()
        request.profilePictureUrl?.let { user.profilePictureUrl = it }
        userRepository.save(user)

        return UserResponse.from(user, profileOfStudent(user), profileOfTeacher(user))
    }

    // -- internals ----------------------------------------------------------

    private fun profileOfStudent(user: User): Student? =
        if (user.role == UserRole.STUDENT) studentRepository.findByUserId(user.id!!).orElse(null) else null

    private fun profileOfTeacher(user: User): Teacher? =
        if (user.role == UserRole.TEACHER) teacherRepository.findByUserId(user.id!!).orElse(null) else null

    private fun createStudent(user: User, rollNumber: String): Student {
        if (studentRepository.existsByRollNumber(rollNumber)) {
            throw ConflictException("Roll number $rollNumber is already registered")
        }
        return studentRepository.save(
            Student(user = user, rollNumber = rollNumber, department = UNASSIGNED, batch = UNASSIGNED)
        )
    }

    private fun createTeacher(user: User, employeeId: String): Teacher {
        if (teacherRepository.existsByEmployeeId(employeeId)) {
            throw ConflictException("Employee ID $employeeId is already registered")
        }
        return teacherRepository.save(
            Teacher(user = user, employeeId = employeeId, department = UNASSIGNED)
        )
    }
}