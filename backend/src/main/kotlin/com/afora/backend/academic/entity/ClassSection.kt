package com.afora.backend.academic.entity

import com.afora.backend.user.entity.Teacher
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * ClassSection entity representing a specific section of a subject.
 * 
 * Example: "CS101 Section A" for batch 2024, taught by Teacher X.
 * Phase A8.1: Core academic structure.
 */
@Entity
@Table(
    name = "class_sections",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["subject_id", "semester_id", "section"])
    ]
)
data class ClassSection(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(nullable = false, length = 100)
    val name: String = "",
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    val subject: Subject? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    val teacher: Teacher? = null,
    
    @Column(name = "semester_id", nullable = false)
    val semesterId: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, length = 20)
    val batch: String = "",
    
    @Column(nullable = false, length = 10)
    val section: String = "",
    
    @Column(length = 50)
    val roomNumber: String? = null,
    
    @Column(nullable = false)
    val maxStudents: Int = 60,
    
    @Column(nullable = false)
    val isActive: Boolean = true,
    
    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
