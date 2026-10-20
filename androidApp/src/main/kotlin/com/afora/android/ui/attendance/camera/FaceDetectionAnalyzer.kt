package com.afora.android.ui.attendance.camera

import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.afora.shared.data.api.RosterEntry
import com.afora.shared.domain.model.AttendanceStatus
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * CameraX ImageAnalysis.Analyzer integrating Google ML Kit Face Detection.
 * Scans video frames at ~3 fps, detects faces, maps coordinates, and correlates
 * with the enrolled class roster via the [OnDeviceFaceMatcher] (Requirement 6, 12).
 */
class FaceDetectionAnalyzer(
    private var enrolledStudents: List<RosterEntry> = emptyList(),
    private val matcher: OnDeviceFaceMatcher = StubFaceMatcher(),
    private var enrolledEmbeddings: List<EnrolledEmbedding> = emptyList(),
    private val onDetections: (List<FaceOverlayItem>, List<DetectedFaceCandidate>) -> Unit,
    /**
     * R10.3: invoked when the On_Device_Matcher raises an unrecoverable error during a
     * Camera_Session, so the caller (`CameraAttendanceViewModel.reportMatcherError`) can
     * navigate to Mark_Attendance_Screen. Defaulted to a no-op since the shipped
     * [StubFaceMatcher] never throws.
     */
    private val onMatcherError: (Throwable) -> Unit = {}
) : ImageAnalysis.Analyzer {

    private val sampler = FrameSampler(targetFps = 3)
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .enableTracking()
            .build()
    )

    private val deduplicator = FaceDeduplicator()

    fun updateRoster(students: List<RosterEntry>) {
        this.enrolledStudents = students
    }

    /** Updates the Enrolled_Embedding_Set used by the [OnDeviceFaceMatcher] (R6.2). */
    fun updateEnrolledEmbeddings(embeddings: List<EnrolledEmbedding>) {
        this.enrolledEmbeddings = embeddings
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (!sampler.shouldProcess()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Dimensions of image taking rotation into account
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val imageWidth = if (isRotated) imageProxy.height.toFloat() else imageProxy.width.toFloat()
        val imageHeight = if (isRotated) imageProxy.width.toFloat() else imageProxy.height.toFloat()

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                processDetectedFaces(faces, inputImage, imageWidth, imageHeight)
            }
            .addOnFailureListener {
                // Ignore transient frame analysis failures
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun processDetectedFaces(
        faces: List<Face>,
        frame: InputImage,
        imageWidth: Float,
        imageHeight: Float
    ) {
        val overlayItems = mutableListOf<FaceOverlayItem>()
        val candidates = mutableListOf<DetectedFaceCandidate>()

        if (enrolledStudents.isEmpty()) {
            onDetections(emptyList(), emptyList())
            return
        }

        for (face in faces) {
            val bounds = face.boundingBox
            // Normalize bounding box coordinates (0.0 to 1.0)
            val normalizedBox = CameraBoundingBox(
                left = bounds.left.toFloat() / imageWidth,
                top = bounds.top.toFloat() / imageHeight,
                right = bounds.right.toFloat() / imageWidth,
                bottom = bounds.bottom.toFloat() / imageHeight
            )

            val trackingId = face.trackingId
            // Correlate with the Enrolled_Embedding_Set via the On_Device_Matcher
            // (R6.1-R6.4, R12.1-R12.4). matchConfidence/confidenceBand are derived
            // solely from the embedding comparison, never from trackingId/index (R6.3).
            val matchCandidate = matchFace(face, frame, trackingId)

            if (matchCandidate != null) {
                val category = when (matchCandidate.confidenceBand) {
                    ConfidenceBand.HIGH -> ConfidenceCategory.HIGH
                    ConfidenceBand.MEDIUM -> ConfidenceCategory.MEDIUM
                }

                overlayItems.add(
                    FaceOverlayItem(
                        boundingBox = normalizedBox,
                        trackingId = trackingId,
                        matchedStudentName = matchCandidate.fullName,
                        confidence = matchCandidate.matchConfidence,
                        category = category
                    )
                )

                val candidate = DetectedFaceCandidate(
                    studentId = matchCandidate.studentId,
                    rollNumber = matchCandidate.rollNumber,
                    fullName = matchCandidate.fullName,
                    confidence = matchCandidate.matchConfidence,
                    category = category,
                    trackingId = trackingId,
                    boundingBox = normalizedBox,
                    attendanceStatus = AttendanceStatus.PRESENT
                )
                // R6.6: retains the evaluation carrying the highest Match_Confidence
                // across successive sampled frames for a tracked face.
                val deduplicated = deduplicator.registerDetection(candidate)
                candidates.add(deduplicated)
            } else {
                // R7.3: below the Unmatched_Floor_Threshold -- no Face_Match_Candidate
                // exists at all; the detected face is presented as unmatched.
                overlayItems.add(
                    FaceOverlayItem(
                        boundingBox = normalizedBox,
                        trackingId = trackingId,
                        matchedStudentName = "Unknown Face #${trackingId ?: (overlayItems.size + 1)}",
                        confidence = 0.0,
                        category = ConfidenceCategory.UNKNOWN
                    )
                )
            }
        }

        onDetections(overlayItems, candidates)
    }

    /**
     * Delegates to the [OnDeviceFaceMatcher] for both embedding generation (R6.1) and
     * comparison against the Enrolled_Embedding_Set (R6.2-R6.4), replacing the former
     * index-arithmetic placeholder (`abs(trackingId) % enrolledStudents.size`) and the
     * fixed-base confidence formula (R12.1, R12.2). Returns null when every candidate
     * falls below the Unmatched_Floor_Threshold (R7.3) -- the shipped [StubFaceMatcher]
     * always returns null, so every detected face resolves to unmatched until a real
     * matcher is wired in, which is exactly the Requirement 10 fallback scenario.
     */
    private fun matchFace(face: Face, frame: InputImage, trackingId: Int?): FaceMatchCandidate? {
        if (enrolledEmbeddings.isEmpty()) return null
        return try {
            val embedding = matcher.generateEmbedding(face, frame)
            val match = matcher.match(embedding, enrolledEmbeddings) ?: return null
            match.copy(trackingId = trackingId)
        } catch (error: Exception) {
            // R10.3: an unrecoverable matcher error is reported to the caller rather than
            // crashing the analyzer; the caller decides whether/how to fall back.
            onMatcherError(error)
            null
        }
    }
}
