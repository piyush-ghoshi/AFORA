package com.afora.backend.academic.repository

import com.afora.backend.academic.entity.Semester
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Repository for Semester.
 * Phase A8.4: Resolves the current semester for dashboard scoping.
 */
@Repository
interface SemesterRepository : JpaRepository<Semester, UUID> {

    /** The semester flagged as current, if any. */
    fun findFirstByIsCurrentTrue(): Semester?
}