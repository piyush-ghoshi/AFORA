package com.afora.android.ui.attendance.camera

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/**
 * Cross-frame deduplicator per docs/face-recognition.md section 6.
 *
 * Keeps track of unique students detected across camera rotation frames,
 * retaining the highest confidence score and aggregating multi-frame evidence.
 */
class FaceDeduplicator {
    private val confirmedIdentities = mutableMapOf<Long, DetectedFaceCandidate>()
    private val trackingIdMap = mutableMapOf<Int, Long>()

    @Synchronized
    fun registerDetection(
        candidate: DetectedFaceCandidate
    ): DetectedFaceCandidate {
        val studentId = candidate.studentId
        candidate.trackingId?.let { trackingIdMap[it] = studentId }

        val existing = confirmedIdentities[studentId]
        val updated = if (existing == null) {
            candidate
        } else {
            existing.copy(
                confidence = max(existing.confidence, candidate.confidence),
                category = if (candidate.confidence > existing.confidence) candidate.category else existing.category,
                detectionCount = existing.detectionCount + 1,
                lastSeenTimestamp = System.currentTimeMillis(),
                boundingBox = candidate.boundingBox ?: existing.boundingBox,
                trackingId = candidate.trackingId ?: existing.trackingId
            )
        }
        confirmedIdentities[studentId] = updated
        return updated
    }

    @Synchronized
    fun getStudentIdForTrackingId(trackingId: Int): Long? = trackingIdMap[trackingId]

    @Synchronized
    fun getAllCandidates(): Map<Long, DetectedFaceCandidate> = confirmedIdentities.toMap()

    @Synchronized
    fun clear() {
        confirmedIdentities.clear()
        trackingIdMap.clear()
    }

    companion object {
        fun categorizeConfidence(confidence: Double): ConfidenceCategory = when {
            confidence >= 0.80 -> ConfidenceCategory.HIGH
            confidence >= 0.65 -> ConfidenceCategory.MEDIUM
            confidence >= 0.50 -> ConfidenceCategory.LOW
            else -> ConfidenceCategory.UNKNOWN
        }

        fun calculateIoU(box1: CameraBoundingBox, box2: CameraBoundingBox): Float {
            val left = max(box1.left, box2.left)
            val top = max(box1.top, box2.top)
            val right = min(box1.right, box2.right)
            val bottom = min(box1.bottom, box2.bottom)

            val intersection = max(0f, right - left) * max(0f, bottom - top)
            val area1 = (box1.right - box1.left) * (box1.bottom - box1.top)
            val area2 = (box2.right - box2.left) * (box2.bottom - box2.top)
            val union = area1 + area2 - intersection

            return if (union <= 0f) 0f else intersection / union
        }
    }
}

/**
 * Throttles frame analysis rate (docs/face-recognition.md section 7.1)
 * to prevent thermal throttling, high battery drain, and UI latency.
 */
class FrameSampler(private val targetFps: Int = 3) {
    private var lastProcessedTimestamp = 0L
    private val minIntervalMs = 1000L / targetFps

    fun shouldProcess(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        if (currentTimeMs - lastProcessedTimestamp >= minIntervalMs) {
            lastProcessedTimestamp = currentTimeMs
            return true
        }
        return false
    }
}
