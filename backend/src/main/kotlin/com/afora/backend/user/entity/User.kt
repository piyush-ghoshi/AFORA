package com.afora.backend.user.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

/**
 * User entity representing all system users.
 * 
 * Phase A2: Integrated with Firebase Authentication.
 * - firebaseUid: Maps to Firebase user UID
 * - email: Synced from Firebase
 * - role: Determines access level (STUDENT, TEACHER, ADMIN)
 */
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_firebase_uid", columnList = "firebase_uid", unique = true),
        Index(name = "idx_users_email", columnList = "email", unique = true),
        Index(name = "idx_users_role", columnList = "role")
    ]
)
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    
    /**
     * Firebase User UID - primary authentication identifier.
     * This links the local user to their Firebase account.
     */
    @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
    var firebaseUid: String,
    
    @Column(nullable = false, unique = true, length = 255)
    var email: String,
    
    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String,
    
    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole,
    
    @Column(name = "profile_picture_url", length = 500)
    var profilePictureUrl: String? = null,
    
    @Column(nullable = false)
    var active: Boolean = true,
    
    /**
     * Track last login for security and analytics.
     */
    @Column(name = "last_login_at")
    var lastLoginAt: LocalDateTime? = null,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
) {
    
    /**
     * Get full name.
     */
    fun getFullName(): String = "$firstName $lastName"
    
    /**
     * Check if user has specific role.
     */
    fun hasRole(role: UserRole): Boolean = this.role == role
    
    /**
     * Check if user is admin.
     */
    fun isAdmin(): Boolean = role == UserRole.ADMIN
    
    /**
     * Check if user is teacher.
     */
    fun isTeacher(): Boolean = role == UserRole.TEACHER
    
    /**
     * Check if user is student.
     */
    fun isStudent(): Boolean = role == UserRole.STUDENT
    
    /**
     * Update last login timestamp.
     */
    fun updateLastLogin() {
        lastLoginAt = LocalDateTime.now()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "User(id=$id, firebaseUid='$firebaseUid', email='$email', role=$role)"
    }
}

/**
 * User roles for role-based access control.
 */
enum class UserRole {
    STUDENT,    // Can view own attendance, submit queries, apply for leave
    TEACHER,    // Can mark attendance, approve leave, view class data
    ADMIN       // Full system access, user management, analytics
}
