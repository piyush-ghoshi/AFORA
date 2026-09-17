package com.academia.shared.data.repository

import com.academia.shared.domain.model.Student
import com.academia.shared.util.Result

/**
 * Student repository interface.
 */
interface StudentRepository {
    /**
     * Get student by ID.
     */
    suspend fun getStudentById(studentId: Long): Result<Student>
    
    /**
     * Get student by roll number.
     */
    suspend fun getStudentByRollNumber(rollNumber: String): Result<Student>
    
    /**
     * Get students enrolled in a class section.
     */
    suspend fun getStudentsByClassSection(classSectionId: Long): Result<List<Student>>
    
    /**
     * Get students enrolled in a subject for a specific class.
     */
    suspend fun getEnrolledStudents(
        classSectionId: Long,
        subjectId: Long,
        semesterId: Long
    ): Result<List<Student>>
    
    /**
     * Search students by name or roll number.
     */
    suspend fun searchStudents(query: String, limit: Int = 20): Result<List<Student>>
}
