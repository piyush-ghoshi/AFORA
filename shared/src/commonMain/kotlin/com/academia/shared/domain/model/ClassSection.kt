package com.academia.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Class section entity (e.g., CSE-3A, CSE-3B).
 */
@Serializable
data class ClassSection(
    val id: Long,
    val batchId: Long,
    val yearOfStudy: Int,  // 1, 2, 3, 4
    val section: String,   // A, B, C
    val name: String,      // CSE-3A
    val capacity: Int? = null,
    val isActive: Boolean = true
)
