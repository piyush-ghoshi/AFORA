package com.afora.android.ui.attendance.camera

import com.afora.shared.domain.model.AttendanceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FaceDeduplicatorTest {

    private lateinit var deduplicator: FaceDeduplicator

    @Before
    fun setup() {
        deduplicator = FaceDeduplicator()
    }

    @Test
    fun `categorizeConfidence correctly classifies thresholds`() {
        assertEquals(ConfidenceCategory.HIGH, FaceDeduplicator.categorizeConfidence(0.85))
        assertEquals(ConfidenceCategory.HIGH, FaceDeduplicator.categorizeConfidence(0.80))
        assertEquals(ConfidenceCategory.MEDIUM, FaceDeduplicator.categorizeConfidence(0.75))
        assertEquals(ConfidenceCategory.MEDIUM, FaceDeduplicator.categorizeConfidence(0.65))
        assertEquals(ConfidenceCategory.LOW, FaceDeduplicator.categorizeConfidence(0.55))
        assertEquals(ConfidenceCategory.LOW, FaceDeduplicator.categorizeConfidence(0.50))
        assertEquals(ConfidenceCategory.UNKNOWN, FaceDeduplicator.categorizeConfidence(0.40))
    }

    @Test
    fun `registerDetection adds new student candidate`() {
        val candidate = DetectedFaceCandidate(
            studentId = 101,
            rollNumber = "CS001",
            fullName = "John Doe",
            confidence = 0.88,
            category = ConfidenceCategory.HIGH,
            trackingId = 1,
            boundingBox = CameraBoundingBox(0.1f, 0.1f, 0.3f, 0.3f),
            attendanceStatus = AttendanceStatus.PRESENT
        )

        val result = deduplicator.registerDetection(candidate)
        assertEquals(101L, result.studentId)
        assertEquals(1, result.detectionCount)
        assertEquals(0.88, result.confidence, 0.001)

        val all = deduplicator.getAllCandidates()
        assertEquals(1, all.size)
        assertNotNull(all[101L])
    }

    @Test
    fun `registerDetection across multiple frames preserves highest confidence and increments count`() {
        val frame1 = DetectedFaceCandidate(
            studentId = 101,
            rollNumber = "CS001",
            fullName = "John Doe",
            confidence = 0.72,
            category = ConfidenceCategory.MEDIUM,
            trackingId = 1,
            boundingBox = CameraBoundingBox(0.1f, 0.1f, 0.3f, 0.3f)
        )
        deduplicator.registerDetection(frame1)

        val frame2 = DetectedFaceCandidate(
            studentId = 101,
            rollNumber = "CS001",
            fullName = "John Doe",
            confidence = 0.94,
            category = ConfidenceCategory.HIGH,
            trackingId = 1,
            boundingBox = CameraBoundingBox(0.12f, 0.11f, 0.32f, 0.31f)
        )
        val updated = deduplicator.registerDetection(frame2)

        assertEquals(2, updated.detectionCount)
        assertEquals(0.94, updated.confidence, 0.001)
        assertEquals(ConfidenceCategory.HIGH, updated.category)

        val frame3 = DetectedFaceCandidate(
            studentId = 101,
            rollNumber = "CS001",
            fullName = "John Doe",
            confidence = 0.80, // Lower than 0.94
            category = ConfidenceCategory.HIGH,
            trackingId = 1
        )
        val afterLower = deduplicator.registerDetection(frame3)

        assertEquals(3, afterLower.detectionCount)
        assertEquals(0.94, afterLower.confidence, 0.001) // Kept the peak confidence
    }

    @Test
    fun `trackingId mapping associates faces correctly`() {
        val candidate = DetectedFaceCandidate(
            studentId = 205,
            rollNumber = "CS002",
            fullName = "Sarah Connor",
            confidence = 0.89,
            category = ConfidenceCategory.HIGH,
            trackingId = 42
        )
        deduplicator.registerDetection(candidate)

        assertEquals(205L, deduplicator.getStudentIdForTrackingId(42))
        assertNull(deduplicator.getStudentIdForTrackingId(999))
    }

    @Test
    fun `calculateIoU computes accurate overlap ratio`() {
        val box1 = CameraBoundingBox(0f, 0f, 10f, 10f)
        val box2 = CameraBoundingBox(0f, 0f, 10f, 10f)
        assertEquals(1.0f, FaceDeduplicator.calculateIoU(box1, box2), 0.001f)

        val box3 = CameraBoundingBox(10f, 10f, 20f, 20f)
        assertEquals(0.0f, FaceDeduplicator.calculateIoU(box1, box3), 0.001f)

        val box4 = CameraBoundingBox(5f, 0f, 15f, 10f)
        // Intersection: (10 - 5) * (10 - 0) = 50
        // Union: 100 + 100 - 50 = 150 -> 50/150 = 0.333f
        assertEquals(0.333f, FaceDeduplicator.calculateIoU(box1, box4), 0.01f)
    }

    @Test
    fun `FrameSampler throttles frames according to targetFps`() {
        val sampler = FrameSampler(targetFps = 2) // 500ms interval
        val t0 = 1000L
        assertEquals(true, sampler.shouldProcess(t0))
        assertEquals(false, sampler.shouldProcess(t0 + 200L)) // 200ms < 500ms
        assertEquals(false, sampler.shouldProcess(t0 + 499L)) // 499ms < 500ms
        assertEquals(true, sampler.shouldProcess(t0 + 500L))  // 500ms >= 500ms
    }
}
