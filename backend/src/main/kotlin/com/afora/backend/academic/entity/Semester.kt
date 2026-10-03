package com.afora.backend.academic.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.util.UUID

/**
 * Academic semester. The table is created by migration V1.
 *
 * Phase A8.4: Mapped read-only so the dashboard can resolve "the current
 * semester" when a caller does not specify one. Only the columns the
 * dashboard needs are declared; ddl-auto=validate tolerates extra DB columns.
 */
@Entity
@Table(name = "semesters")
data class Semester(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "academic_year_id", nullable = false)
    val academicYearId: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 100)
    val name: String = "",

    @Column(nullable = false)
    val semesterNumber: Int = 1,

    @Column(nullable = false)
    val startDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    val endDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    val isCurrent: Boolean = false
)