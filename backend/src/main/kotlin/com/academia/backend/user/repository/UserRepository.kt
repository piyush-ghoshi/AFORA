package com.academia.backend.user.repository

import com.academia.backend.user.entity.User
import com.academia.backend.user.entity.UserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * JPA repository for User entity.
 */
@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun findByFirebaseUid(firebaseUid: String): Optional<User>

    fun findByEmail(email: String): Optional<User>

    fun existsByEmail(email: String): Boolean

    fun existsByFirebaseUid(firebaseUid: String): Boolean

    fun findAllByRole(role: UserRole): List<User>

    fun findAllByActive(active: Boolean): List<User>
}
