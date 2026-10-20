package com.afora.shared.data.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Round-trip serialization tests for the six Face_Recognition shared wire
 * types in [FaceProfileWire] (`.kiro/specs/face-recognition/design.md` →
 * "Wire Contract"), mirroring the task's requirement that each type survive
 * `Json.encodeToString`/`decodeFromString` with every field value preserved,
 * and that the two collection fields with declared defaults
 * (`RegisterFaceProfileRequest.embeddings`, `EnrolledEmbeddingSetResponse.entries`)
 * decode correctly to their default when omitted from the JSON.
 *
 * **Validates: Requirements 3.4, 13.3**
 */
class FaceProfileWireSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ── FaceConsentResponse ──────────────────────────────────────────────────

    @Test
    fun `FaceConsentResponse round-trips through encode and decode`() {
        val original = FaceConsentResponse(
            studentId = 42L,
            consentedAt = "2026-03-17T10:15:30"
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<FaceConsentResponse>(encoded)

        assertEquals(original, decoded)
        assertEquals(42L, decoded.studentId)
        assertEquals("2026-03-17T10:15:30", decoded.consentedAt)
    }

    // ── RegisterFaceProfileRequest ───────────────────────────────────────────

    @Test
    fun `RegisterFaceProfileRequest round-trips with populated embeddings`() {
        val original = RegisterFaceProfileRequest(
            embeddings = listOf(listOf(0.1, -0.2, 0.3), listOf(0.4, 0.5, -0.6))
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<RegisterFaceProfileRequest>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `RegisterFaceProfileRequest defaults embeddings to empty list when omitted`() {
        val decoded = json.decodeFromString<RegisterFaceProfileRequest>("{}")

        assertEquals(emptyList(), decoded.embeddings)
    }

    // ── FaceProfileResponse ───────────────────────────────────────────────────

    @Test
    fun `FaceProfileResponse round-trips through encode and decode`() {
        val original = FaceProfileResponse(
            studentId = 7L,
            embeddingCount = 5,
            dimensions = 128,
            registeredAt = "2026-01-01T00:00:00"
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<FaceProfileResponse>(encoded)

        assertEquals(original, decoded)
    }

    // ── FaceProfileDeletionResponse ──────────────────────────────────────────

    @Test
    fun `FaceProfileDeletionResponse round-trips through encode and decode`() {
        val original = FaceProfileDeletionResponse(
            studentId = 99L,
            reason = "STUDENT_REQUEST",
            deletedAt = "2026-02-02T12:00:00"
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<FaceProfileDeletionResponse>(encoded)

        assertEquals(original, decoded)
    }

    // ── EnrolledEmbeddingSetResponse / EnrolledEmbeddingEntry ────────────────

    @Test
    fun `EnrolledEmbeddingSetResponse round-trips with populated entries`() {
        val original = EnrolledEmbeddingSetResponse(
            classSectionId = 3L,
            entries = listOf(
                EnrolledEmbeddingEntry(
                    studentId = 1L,
                    rollNumber = "R001",
                    fullName = "Asha Rao",
                    embeddings = listOf(listOf(0.11, 0.22))
                ),
                EnrolledEmbeddingEntry(
                    studentId = 2L,
                    rollNumber = "R002",
                    fullName = "Vikram Shah",
                    embeddings = listOf(listOf(0.33, 0.44), listOf(0.55, 0.66))
                )
            ),
            highConfidenceThreshold = 0.80,
            mediumConfidenceThreshold = 0.60,
            unmatchedFloorThreshold = 0.60
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<EnrolledEmbeddingSetResponse>(encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `EnrolledEmbeddingSetResponse defaults entries to empty list when omitted`() {
        val decoded = json.decodeFromString<EnrolledEmbeddingSetResponse>(
            """{"classSectionId":5,"highConfidenceThreshold":0.80,"mediumConfidenceThreshold":0.60,"unmatchedFloorThreshold":0.60}"""
        )

        assertEquals(emptyList(), decoded.entries)
        assertEquals(5L, decoded.classSectionId)
    }

    @Test
    fun `EnrolledEmbeddingEntry round-trips with empty embeddings default`() {
        val original = EnrolledEmbeddingEntry(
            studentId = 10L,
            rollNumber = "R010",
            fullName = "Priya Mehta"
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<EnrolledEmbeddingEntry>(encoded)

        assertEquals(original, decoded)
        assertEquals(emptyList(), decoded.embeddings)
    }

    @Test
    fun `EnrolledEmbeddingEntry defaults embeddings to empty list when omitted`() {
        val decoded = json.decodeFromString<EnrolledEmbeddingEntry>(
            """{"studentId":11,"rollNumber":"R011","fullName":"Dev Patel"}"""
        )

        assertEquals(emptyList(), decoded.embeddings)
    }
}
