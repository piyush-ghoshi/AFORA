package com.afora.backend.user.repository

import com.afora.backend.user.entity.Teacher
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * JPA repository for Teacher entity.
 */
@Repository
interface TeacherRepository : JpaRepository<Teacher, Long> {

    fun findByUserId(userId: Long): Optional<Teacher>

    fun findByEmployeeId(employeeId: String): Optional<Teacher>

    fun existsByEmployeeId(employeeId: String): Boolean

    fun findAllByDepartment(department: String): List<Teacher>
}
