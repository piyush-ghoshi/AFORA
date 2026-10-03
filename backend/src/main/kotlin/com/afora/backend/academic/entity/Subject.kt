package com.afora.backend.academic.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Subject entity representing academic subjects/courses.
 *
 * Phase A8.1: Core academic structure for attendance system.
 * All properties have defaults so Kotlin generates the no-arg constructor
 * that Hibernate requires (also covered by the kotlin-jpa plugin).
 */
@Entity
@Table(name = "subjects")
data class Subject(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 20)
    val code: String = "",

    @Column(nullable = false, length = 200)
    val name: String = "",

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(nullable = false)
    val credits: Int = 3,

    @Column(nullable = false, length = 100)
    val department: String = "",

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
