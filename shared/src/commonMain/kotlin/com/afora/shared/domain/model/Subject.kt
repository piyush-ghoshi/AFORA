package com.afora.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Subject/Course entity.
 */
@Serializable
data class Subject(
    val id: Long,
    val code: String,
    val name: String,
    val credits: Int,
    val subjectType: SubjectType,
    val departmentId: Long? = null,
    val isActive: Boolean = true
)

/**
 * Subject/Course type.
 */
@Serializable
enum class SubjectType {
    THEORY,
    PRACTICAL,
    PROJECT,
    ELECTIVE
}
