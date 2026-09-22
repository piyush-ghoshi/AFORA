package com.afora.backend.user.repository

import com.afora.backend.user.entity.Student
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * JPA repository for Student entity.
 */
@Repository
interface StudentRepository : JpaRepository<Student, Long> {

    fun findByUserId(userId: Long): Optional<Student>

    fun findByRollNumber(rollNumber: String): Optional<Student>

    fun existsByRollNumber(rollNumber: String): Boolean

    fun findAllByDepartment(department: String): List<Student>

    fun findAllByBatch(batch: String): List<Student>
}
