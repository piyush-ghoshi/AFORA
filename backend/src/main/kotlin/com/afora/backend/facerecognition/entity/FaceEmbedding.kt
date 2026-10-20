package com.afora.backend.facerecognition.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * One numeric Face_Embedding vector -- never a face image (R3.2).
 *
 * studentId is a scalar FK column, not @ManyToOne, because this entity is
 * written once and read as a flat projection, never navigated -- the same
 * reasoning the sibling attendance-marking entities (e.g. AttendanceSubmission)
 * already use for their own scalar FK columns.
 *
 * Append-only: updatable = false on every column except the generated id,
 * since a registration replaces rows (delete + insert) rather than updating
 * them in place (R2.4).
 *
 * embeddingVector is a comma-delimited list of doubles, parsed and
 * serialized only by Face_Profile_Service -- never compared in the database
 * or in any other backend process (R3.1, R3.3).
 */
@Entity
@Table(name = "face_embeddings")
data class FaceEmbedding(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "student_id", nullable = false, updatable = false)
    val studentId: Long = 0,

    @Column(name = "embedding_vector", nullable = false, updatable = false, columnDefinition = "TEXT")
    val embeddingVector: String = "",

    @Column(nullable = false, updatable = false)
    val dimensions: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
