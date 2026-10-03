package com.afora.backend.academic.repository

import com.afora.backend.academic.entity.Subject
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * Repository for Subject entity.
 * Phase A8.1: Core academic repository.
 */
@Repository
interface SubjectRepository : JpaRepository<Subject, Long> {
    
    /**
     * Find subject by code.
     */
    fun findByCode(code: String): Subject?
    
    /**
     * Find all subjects by department.
     */
    fun findByDepartment(department: String): List<Subject>
    
    /**
     * Find all active subjects.
     */
    fun findByIsActiveTrue(): List<Subject>
    
    /**
     * Find active subjects by department.
     */
    fun findByDepartmentAndIsActiveTrue(department: String): List<Subject>
}
