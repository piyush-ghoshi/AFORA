package com.academia.backend.user.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

/**
 * Student entity with additional student-specific information.
 * 
 * One-to-one relationship with User entity.
 */
@Entity
@Table(
    name = "students",
    indexes = [
        Index(name = "idx_students_user_id", columnList = "user_id", unique = true),
        Index(name = "idx_students_roll_number", columnList = "roll_number", unique = true),
        Index(name = "idx_students_batch", columnList = "batch")
    ]
)
class Student(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User,
    
    @Column(name = "roll_number", nullable = false, unique = true, length = 50)
    var rollNumber: String,
    
    @Column(nullable = false, length = 100)
    var department: String,
    
    @Column(nullable = false, length = 20)
    var batch: String, // e.g., "2024", "2023"
    
    @Column(length = 50)
    var semester: String? = null, // e.g., "1", "2", "3", etc.
    
    @Column(name = "phone_number", length = 20)
    var phoneNumber: String? = null,
    
    @Column(length = 500)
    var address: String? = null,
    
    @Column(name = "date_of_birth")
    var dateOfBirth: LocalDateTime? = null,
    
    @Column(length = 20)
    var gender: String? = null,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
) {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Student) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "Student(id=$id, rollNumber='$rollNumber', department='$department', batch='$batch')"
    }
}
