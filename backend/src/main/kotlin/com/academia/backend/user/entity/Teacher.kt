package com.academia.backend.user.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

/**
 * Teacher entity with additional teacher-specific information.
 * 
 * One-to-one relationship with User entity.
 */
@Entity
@Table(
    name = "teachers",
    indexes = [
        Index(name = "idx_teachers_user_id", columnList = "user_id", unique = true),
        Index(name = "idx_teachers_employee_id", columnList = "employee_id", unique = true),
        Index(name = "idx_teachers_department", columnList = "department")
    ]
)
class Teacher(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User,
    
    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    var employeeId: String,
    
    @Column(nullable = false, length = 100)
    var department: String,
    
    @Column(length = 100)
    var designation: String? = null, // e.g., "Professor", "Assistant Professor"
    
    @Column(length = 200)
    var specialization: String? = null,
    
    @Column(name = "phone_number", length = 20)
    var phoneNumber: String? = null,
    
    @Column(name = "office_location", length = 100)
    var officeLocation: String? = null,
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
) {
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Teacher) return false
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int = id?.hashCode() ?: 0
    
    override fun toString(): String {
        return "Teacher(id=$id, employeeId='$employeeId', department='$department')"
    }
}
