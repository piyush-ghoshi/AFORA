package com.afora.shared.data.repository

import com.afora.shared.domain.model.Teacher
import com.afora.shared.util.Result

/**
 * Teacher repository interface.
 */
interface TeacherRepository {
    /**
     * Get teacher by ID.
     */
    suspend fun getTeacherById(teacherId: Long): Result<Teacher>
    
    /**
     * Get teacher by user ID.
     */
    suspend fun getTeacherByUserId(userId: Long): Result<Teacher>
    
    /**
     * Get teachers assigned to a subject.
     */
    suspend fun getTeachersBySubject(subjectId: Long): Result<List<Teacher>>
    
    /**
     * Get teachers in a department.
     */
    suspend fun getTeachersByDepartment(departmentId: Long): Result<List<Teacher>>
}
