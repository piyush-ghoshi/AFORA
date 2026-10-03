package com.afora.backend.academic.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A recurring weekly timetable entry for a class section.
 *
 * Phase A8.2: Schedule management. One row per weekday occurrence, so a class
 * meeting Monday and Wednesday has two slots.
 */
@Entity
@Table(name = "timetable_slots")
data class TimetableSlot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_section_id", nullable = false)
    val classSection: ClassSection? = null,

    /** ISO day of week: 1 = Monday .. 7 = Sunday. */
    @Column(nullable = false)
    val dayOfWeek: Int = 1,

    @Column(nullable = false)
    val startTime: LocalTime = LocalTime.MIDNIGHT,

    @Column(nullable = false)
    val endTime: LocalTime = LocalTime.MIDNIGHT,

    @Column(length = 50)
    val roomNumber: String? = null,

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)