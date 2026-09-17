package com.academia.shared.platform

/**
 * Platform-agnostic face detection interface.
 * Android: ML Kit Face Detection
 * iOS: Vision framework (future)
 * 
 * IMPORTANT: This is ONLY for face detection (finding faces in frames).
 * Recognition (identifying who) is handled by FaceRecognizer.
 */
expect class FaceDetector {
    suspend fun detectFaces(image: PlatformImage): List<DetectedFace>
    fun isAvailable(): Boolean
}

/**
 * A detected face in a frame with bounding box and optional landmarks.
 */
data class DetectedFace(
    val boundingBox: BoundingBox,
    val landmarks: List<FaceLandmark>?,
    val confidence: Double,
    val trackingId: Int?  // For cross-frame tracking
)

/**
 * Facial landmark (eye, nose, mouth positions).
 */
data class FaceLandmark(
    val type: LandmarkType,
    val x: Int,
    val y: Int
)

/**
 * Common facial landmark types.
 */
enum class LandmarkType {
    LEFT_EYE,
    RIGHT_EYE,
    NOSE_BASE,
    MOUTH_LEFT,
    MOUTH_RIGHT
}
