# Kotlin Multiplatform Architecture
## Smart Classroom Attendance Management System

**Version:** 1.0  
**Date:** August 25, 2026  
**Status:** Draft - Awaiting Review

---

## 1. Executive Summary

This document defines the Kotlin Multiplatform (KMP) architecture strategy for the Smart Classroom Attendance Management System. The architecture is designed to maximize code sharing between Android and iOS while maintaining platform-specific optimizations where necessary.

### Key Principles

1. **Android First, iOS Ready**: Implement and stabilize Android before iOS
2. **Maximum Code Sharing**: Target 60-70% shared code
3. **Clean Abstractions**: Platform-specific functionality behind interfaces
4. **Type Safety**: Leverage Kotlin's type system across platforms
5. **No Compromise**: Never sacrifice user experience for code sharing

---

## 2. KMP Module Structure

```
project-root/
│
├── shared/                                    # Kotlin Multiplatform Module
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── commonMain/                        # Platform-independent code
│   │   │   ├── kotlin/
│   │   │   │   ├── com/academia/shared/
│   │   │   │   │   ├── domain/                # Domain models
│   │   │   │   │   │   ├── model/             # Data classes
│   │   │   │   │   │   │   ├── Student.kt
│   │   │   │   │   │   │   ├── Teacher.kt
│   │   │   │   │   │   │   ├── Lecture.kt
│   │   │   │   │   │   │   ├── Attendance.kt
│   │   │   │   │   │   │   ├── AttendanceStatus.kt
│   │   │   │   │   │   │   ├── LeaveRequest.kt
│   │   │   │   │   │   │   └── AttendanceQuery.kt
│   │   │   │   │   │   └── usecase/           # Business logic
│   │   │   │   │   │       ├── AttendanceUseCase.kt
│   │   │   │   │   │       ├── LeaveUseCase.kt
│   │   │   │   │   │       └── FaceRecognitionUseCase.kt
│   │   │   │   │   ├── data/                  # Data layer
│   │   │   │   │   │   ├── repository/        # Repository interfaces
│   │   │   │   │   │   │   ├── AttendanceRepository.kt
│   │   │   │   │   │   │   ├── StudentRepository.kt
│   │   │   │   │   │   │   ├── TeacherRepository.kt
│   │   │   │   │   │   │   └── FaceRepository.kt
│   │   │   │   │   │   ├── api/               # API models & client
│   │   │   │   │   │   │   ├── ApiClient.kt
│   │   │   │   │   │   │   ├── request/       # Request DTOs
│   │   │   │   │   │   │   └── response/      # Response DTOs
│   │   │   │   │   │   └── cache/             # Caching interfaces
│   │   │   │   │   ├── platform/              # Platform abstractions
│   │   │   │   │   │   ├── CameraController.kt        # expect
│   │   │   │   │   │   ├── FaceDetector.kt            # expect
│   │   │   │   │   │   ├── FaceRecognizer.kt          # expect
│   │   │   │   │   │   ├── ImageProcessor.kt          # expect
│   │   │   │   │   │   ├── PlatformImage.kt           # expect
│   │   │   │   │   │   ├── FileStorage.kt             # expect
│   │   │   │   │   │   └── NetworkMonitor.kt          # expect
│   │   │   │   │   ├── validation/            # Validation logic
│   │   │   │   │   │   ├── AttendanceValidator.kt
│   │   │   │   │   │   ├── LeaveValidator.kt
│   │   │   │   │   │   └── EmailValidator.kt
│   │   │   │   │   ├── util/                  # Utilities
│   │   │   │   │   │   ├── DateTimeUtil.kt
│   │   │   │   │   │   ├── Result.kt
│   │   │   │   │   │   └── Error.kt
│   │   │   │   │   └── di/                    # Dependency injection
│   │   │   │   │       └── Koin.kt
│   │   │   └── resources/
│   │   │
│   │   ├── androidMain/                       # Android-specific implementations
│   │   │   ├── kotlin/
│   │   │   │   ├── com/academia/shared/platform/
│   │   │   │   │   ├── AndroidCameraController.kt    # actual
│   │   │   │   │   ├── AndroidFaceDetector.kt        # actual
│   │   │   │   │   ├── AndroidFaceRecognizer.kt      # actual
│   │   │   │   │   ├── AndroidImageProcessor.kt      # actual
│   │   │   │   │   ├── AndroidPlatformImage.kt       # actual
│   │   │   │   │   ├── AndroidFileStorage.kt         # actual
│   │   │   │   │   └── AndroidNetworkMonitor.kt      # actual
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── iosMain/                           # iOS-specific implementations (Future)
│   │   │   └── kotlin/
│   │   │       └── com/academia/shared/platform/
│   │   │           ├── IOSCameraController.kt        # actual
│   │   │           ├── IOSFaceDetector.kt            # actual
│   │   │           ├── IOSFaceRecognizer.kt          # actual
│   │   │           ├── IOSImageProcessor.kt          # actual
│   │   │           ├── IOSPlatformImage.kt           # actual
│   │   │           ├── IOSFileStorage.kt             # actual
│   │   │           └── IOSNetworkMonitor.kt          # actual
│   │   │
│   │   ├── commonTest/                        # Shared tests
│   │   ├── androidUnitTest/                   # Android-specific tests
│   │   └── iosTest/                           # iOS-specific tests
│   │
├── androidApp/                                # Android Application
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/
│   │   │   │   ├── com/academia/android/
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── AforaApplication.kt
│   │   │   │   │   ├── ui/                    # Jetpack Compose UI
│   │   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── screen/
│   │   │   │   │   │   │   ├── auth/
│   │   │   │   │   │   │   ├── student/
│   │   │   │   │   │   │   ├── teacher/
│   │   │   │   │   │   │   └── admin/
│   │   │   │   │   │   └── navigation/
│   │   │   │   │   ├── viewmodel/             # Android ViewModels
│   │   │   │   │   │   ├── AttendanceViewModel.kt
│   │   │   │   │   │   ├── CameraViewModel.kt
│   │   │   │   │   │   └── StudentDashboardViewModel.kt
│   │   │   │   │   ├── di/                    # Hilt modules
│   │   │   │   │   │   ├── AppModule.kt
│   │   │   │   │   │   ├── NetworkModule.kt
│   │   │   │   │   │   └── RepositoryModule.kt
│   │   │   │   │   └── util/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   │
└── iosApp/                                    # iOS Application (Future)
    ├── iosApp.xcodeproj
    └── iosApp/
        ├── App.swift
        ├── ContentView.swift
        └── ...
```

---

## 3. Shared Code (commonMain)

### 3.1 Domain Models

All core business models are fully shared:

```kotlin
// shared/commonMain
package com.afora.shared.domain.model

data class Student(
    val id: Long,
    val rollNumber: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val dateOfBirth: LocalDate,
    val enrollmentDate: LocalDate,
    val isActive: Boolean
)

data class Lecture(
    val id: Long,
    val subjectId: Long,
    val classId: Long,
    val teacherId: Long,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val status: LectureStatus,
    val attendanceMethod: AttendanceMethod?
)

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    ON_LEAVE,
    LATE
}

enum class LectureStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

enum class AttendanceMethod {
    CAMERA,
    MANUAL,
    HYBRID
}

data class AttendanceRecord(
    val id: Long,
    val lectureId: Long,
    val studentId: Long,
    val status: AttendanceStatus,
    val method: AttendanceMethod,
    val confidenceScore: Double?,
    val markedAt: Instant
)

data class RecognitionResult(
    val studentId: Long,
    val confidence: Double,
    val category: ConfidenceCategory
)

enum class ConfidenceCategory {
    HIGH,
    MEDIUM,
    LOW,
    VERY_LOW,
    UNKNOWN
}
```

### 3.2 Repository Interfaces

```kotlin
// shared/commonMain
package com.afora.shared.data.repository

interface AttendanceRepository {
    suspend fun startLectureSession(lectureId: Long): Result<LectureSession>
    suspend fun getEnrolledStudents(lectureId: Long): Result<List<StudentWithEmbedding>>
    suspend fun confirmAttendance(
        lectureId: Long,
        attendanceRecords: List<AttendanceSubmission>
    ): Result<AttendanceConfirmation>
    suspend fun getStudentAttendance(
        studentId: Long,
        subjectId: Long?
    ): Result<List<AttendanceRecord>>
}

interface FaceRepository {
    suspend fun getFaceEmbeddings(studentIds: List<Long>): Result<Map<Long, List<FaceEmbedding>>>
    suspend fun registerFace(studentId: Long, images: List<PlatformImage>): Result<Unit>
}
```

### 3.3 Use Cases (Business Logic)

```kotlin
// shared/commonMain
package com.afora.shared.domain.usecase

class AttendanceUseCase(
    private val repository: AttendanceRepository,
    private val validator: AttendanceValidator
) {
    suspend fun confirmAttendance(
        lectureId: Long,
        detectedStudents: List<RecognitionResult>
    ): Result<AttendanceConfirmation> {
        // Validate attendance data
        val validation = validator.validateAttendance(lectureId, detectedStudents)
        if (!validation.isValid) {
            return Result.Error(ValidationError(validation.errors))
        }
        
        // Map to submission format
        val submissions = detectedStudents.map { result ->
            AttendanceSubmission(
                studentId = result.studentId,
                status = AttendanceStatus.PRESENT,
                method = AttendanceMethod.CAMERA,
                confidenceScore = result.confidence
            )
        }
        
        // Submit to repository
        return repository.confirmAttendance(lectureId, submissions)
    }
    
    suspend fun calculateAttendancePercentage(
        studentId: Long,
        subjectId: Long
    ): Result<Double> {
        return repository.getStudentAttendance(studentId, subjectId)
            .map { records ->
                val total = records.size
                val present = records.count { it.status == AttendanceStatus.PRESENT }
                if (total == 0) 0.0 else (present.toDouble() / total) * 100
            }
    }
}
```

### 3.4 Validation Logic

```kotlin
// shared/commonMain
package com.afora.shared.validation

class AttendanceValidator {
    fun validateAttendance(
        lectureId: Long,
        detectedStudents: List<RecognitionResult>
    ): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Check for duplicates
        val duplicates = detectedStudents
            .groupBy { it.studentId }
            .filter { it.value.size > 1 }
        
        if (duplicates.isNotEmpty()) {
            errors.add("Duplicate students detected: ${duplicates.keys}")
        }
        
        // Check confidence scores
        val lowConfidence = detectedStudents.filter { 
            it.category == ConfidenceCategory.VERY_LOW 
        }
        
        if (lowConfidence.isNotEmpty()) {
            errors.add("${lowConfidence.size} students have very low confidence")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
```

### 3.5 API Client (Ktor)

```kotlin
// shared/commonMain
package com.afora.shared.data.api

class ApiClient(private val httpClient: HttpClient) {
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return safeApiCall {
            httpClient.post("$BASE_URL/api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(username, password))
            }
        }
    }
    
    suspend fun startLectureSession(lectureId: Long): Result<LectureSessionResponse> {
        return safeApiCall {
            httpClient.post("$BASE_URL/api/v1/lectures/$lectureId/start") {
                bearerAuth(getAuthToken())
            }
        }
    }
    
    private suspend inline fun <reified T> safeApiCall(
        crossinline call: suspend () -> HttpResponse
    ): Result<T> {
        return try {
            val response = call()
            when (response.status) {
                HttpStatusCode.OK -> Result.Success(response.body())
                HttpStatusCode.Unauthorized -> Result.Error(UnauthorizedError())
                else -> Result.Error(ApiError(response.status.value, response.bodyAsText()))
            }
        } catch (e: Exception) {
            Result.Error(NetworkError(e))
        }
    }
    
    companion object {
        const val BASE_URL = "https://api.academia.example.com"
    }
}
```

---

## 4. Platform Abstractions (Expect/Actual)

### 4.1 Camera Controller

```kotlin
// shared/commonMain
package com.afora.shared.platform

import kotlinx.coroutines.flow.Flow

expect class CameraController {
    fun startCamera(config: CameraConfig): Flow<CameraFrame>
    fun stopCamera()
    suspend fun captureFrame(): CameraFrame
    fun isAvailable(): Boolean
}

data class CameraConfig(
    val targetResolution: Resolution,
    val frameRate: Int,
    val autoFocus: Boolean
)

data class CameraFrame(
    val image: PlatformImage,
    val timestamp: Long,
    val rotation: Int
)

data class Resolution(val width: Int, val height: Int)
```

**Android Implementation:**

```kotlin
// shared/androidMain
package com.afora.shared.platform

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    
    actual fun startCamera(config: CameraConfig): Flow<CameraFrame> = callbackFlow {
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(
                android.util.Size(config.targetResolution.width, config.targetResolution.height)
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    val frame = CameraFrame(
                        image = AndroidPlatformImage(imageProxy),
                        timestamp = System.currentTimeMillis(),
                        rotation = imageProxy.imageInfo.rotationDegrees
                    )
                    trySend(frame)
                }
            }
        
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        cameraProvider?.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            imageAnalysis
        )
        
        awaitClose {
            cameraProvider?.unbindAll()
        }
    }
    
    actual fun stopCamera() {
        cameraProvider?.unbindAll()
    }
    
    actual suspend fun captureFrame(): CameraFrame {
        // Implementation for single frame capture
        TODO("Implement single frame capture")
    }
    
    actual fun isAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_ANY)
    }
}
```

**iOS Implementation (Future):**

```kotlin
// shared/iosMain
package com.afora.shared.platform

import kotlinx.coroutines.flow.Flow

actual class CameraController {
    actual fun startCamera(config: CameraConfig): Flow<CameraFrame> {
        // AVFoundation implementation
        TODO("Implement iOS camera")
    }
    
    actual fun stopCamera() {
        // Implementation
    }
    
    actual suspend fun captureFrame(): CameraFrame {
        // Implementation
    }
    
    actual fun isAvailable(): Boolean {
        // Implementation
        return true
    }
}
```

### 4.2 Face Detector

```kotlin
// shared/commonMain
package com.afora.shared.platform

expect class FaceDetector {
    suspend fun detectFaces(image: PlatformImage): List<DetectedFace>
    fun isAvailable(): Boolean
}

data class DetectedFace(
    val boundingBox: BoundingBox,
    val landmarks: List<FaceLandmark>?,
    val confidence: Double,
    val trackingId: Int?
)

data class BoundingBox(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)

data class FaceLandmark(
    val type: LandmarkType,
    val x: Int,
    val y: Int
)

enum class LandmarkType {
    LEFT_EYE,
    RIGHT_EYE,
    NOSE_BASE,
    MOUTH_LEFT,
    MOUTH_RIGHT
}
```

**Android Implementation:**

```kotlin
// shared/androidMain
package com.afora.shared.platform

import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

actual class FaceDetector {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.1f)
            .enableTracking()
            .build()
    )
    
    actual suspend fun detectFaces(image: PlatformImage): List<DetectedFace> {
        val androidImage = (image as AndroidPlatformImage).toInputImage()
        val task = detector.process(androidImage)
        
        return suspendCancellableCoroutine { continuation ->
            task.addOnSuccessListener { faces ->
                val detectedFaces = faces.map { face ->
                    DetectedFace(
                        boundingBox = BoundingBox(
                            left = face.boundingBox.left,
                            top = face.boundingBox.top,
                            width = face.boundingBox.width(),
                            height = face.boundingBox.height()
                        ),
                        landmarks = face.allLandmarks.map { landmark ->
                            FaceLandmark(
                                type = landmark.landmarkType.toCommonType(),
                                x = landmark.position.x.toInt(),
                                y = landmark.position.y.toInt()
                            )
                        },
                        confidence = 0.9,  // ML Kit doesn't provide face confidence
                        trackingId = face.trackingId
                    )
                }
                continuation.resume(detectedFaces)
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }
    
    actual fun isAvailable(): Boolean = true
}
```

### 4.3 Face Recognizer

```kotlin
// shared/commonMain
package com.afora.shared.platform

expect class FaceRecognizer {
    suspend fun generateEmbedding(face: DetectedFace, image: PlatformImage): FloatArray
    suspend fun matchFaces(
        embedding: FloatArray,
        knownEmbeddings: Map<Long, List<FloatArray>>
    ): RecognitionResult?
    fun isAvailable(): Boolean
}
```

**Android Implementation:**

```kotlin
// shared/androidMain
package com.afora.shared.platform

import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer

actual class FaceRecognizer(private val modelPath: String) {
    private val interpreter: Interpreter by lazy {
        Interpreter(loadModelFile(modelPath))
    }
    
    actual suspend fun generateEmbedding(
        face: DetectedFace,
        image: PlatformImage
    ): FloatArray {
        // Crop face from image
        val faceCrop = cropFaceImage(image, face.boundingBox)
        
        // Preprocess (normalize, resize)
        val inputBuffer = preprocessImage(faceCrop)
        
        // Run inference
        val outputBuffer = Array(1) { FloatArray(512) }  // 512-dim embedding
        interpreter.run(inputBuffer, outputBuffer)
        
        return outputBuffer[0]
    }
    
    actual suspend fun matchFaces(
        embedding: FloatArray,
        knownEmbeddings: Map<Long, List<FloatArray>>
    ): RecognitionResult? {
        var bestMatch: Pair<Long, Double>? = null
        
        for ((studentId, embeddings) in knownEmbeddings) {
            for (knownEmbedding in embeddings) {
                val similarity = cosineSimilarity(embedding, knownEmbedding)
                
                if (bestMatch == null || similarity > bestMatch.second) {
                    bestMatch = studentId to similarity
                }
            }
        }
        
        return bestMatch?.let { (studentId, confidence) ->
            RecognitionResult(
                studentId = studentId,
                confidence = confidence,
                category = categorizeConfidence(confidence)
            )
        }
    }
    
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        return dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
    }
    
    private fun categorizeConfidence(confidence: Double): ConfidenceCategory {
        return when {
            confidence >= 0.85 -> ConfidenceCategory.HIGH
            confidence >= 0.75 -> ConfidenceCategory.MEDIUM
            confidence >= 0.65 -> ConfidenceCategory.LOW
            confidence >= 0.50 -> ConfidenceCategory.VERY_LOW
            else -> ConfidenceCategory.UNKNOWN
        }
    }
    
    actual fun isAvailable(): Boolean = true
}
```

### 4.4 Platform Image

```kotlin
// shared/commonMain
package com.afora.shared.platform

expect class PlatformImage {
    fun getWidth(): Int
    fun getHeight(): Int
    fun crop(boundingBox: BoundingBox): PlatformImage
    fun toByteArray(): ByteArray
}
```

**Android Implementation:**

```kotlin
// shared/androidMain
package com.afora.shared.platform

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

actual class AndroidPlatformImage(private val imageProxy: ImageProxy) : PlatformImage() {
    private val bitmap: Bitmap by lazy {
        imageProxy.toBitmap()
    }
    
    actual override fun getWidth(): Int = bitmap.width
    actual override fun getHeight(): Int = bitmap.height
    
    actual override fun crop(boundingBox: BoundingBox): PlatformImage {
        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            boundingBox.left,
            boundingBox.top,
            boundingBox.width,
            boundingBox.height
        )
        return AndroidPlatformImage(croppedBitmap)
    }
    
    actual override fun toByteArray(): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return stream.toByteArray()
    }
    
    fun toInputImage(): InputImage {
        return InputImage.fromBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
    }
}
```

---

## 5. Android-Specific Code

### 5.1 ViewModels

```kotlin
// androidApp/
package com.afora.android.viewmodel

@HiltViewModel
class CameraAttendanceViewModel @Inject constructor(
    private val cameraController: CameraController,
    private val faceDetector: FaceDetector,
    private val faceRecognizer: FaceRecognizer,
    private val attendanceUseCase: AttendanceUseCase
) : ViewModel() {
    
    private val _detectedStudents = MutableStateFlow<List<RecognitionResult>>(emptyList())
    val detectedStudents = _detectedStudents.asStateFlow()
    
    private val _scanningState = MutableStateFlow<ScanningState>(ScanningState.Idle)
    val scanningState = _scanningState.asStateFlow()
    
    fun startScanning(lectureId: Long, enrolledStudents: Map<Long, List<FloatArray>>) {
        viewModelScope.launch {
            _scanningState.value = ScanningState.Scanning
            
            cameraController.startCamera(CameraConfig.default())
                .collect { frame ->
                    processFrame(frame, enrolledStudents)
                }
        }
    }
    
    private suspend fun processFrame(
        frame: CameraFrame,
        enrolledStudents: Map<Long, List<FloatArray>>
    ) {
        try {
            // Detect faces
            val faces = faceDetector.detectFaces(frame.image)
            
            // Recognize each face
            for (face in faces) {
                val embedding = faceRecognizer.generateEmbedding(face, frame.image)
                val result = faceRecognizer.matchFaces(embedding, enrolledStudents)
                
                if (result != null) {
                    addDetectedStudent(result)
                }
            }
        } catch (e: Exception) {
            _scanningState.value = ScanningState.Error(e.message ?: "Unknown error")
        }
    }
    
    private fun addDetectedStudent(result: RecognitionResult) {
        val current = _detectedStudents.value.toMutableList()
        
        // Deduplication logic
        val existing = current.find { it.studentId == result.studentId }
        if (existing == null) {
            current.add(result)
        } else if (result.confidence > existing.confidence) {
            current.remove(existing)
            current.add(result)
        }
        
        _detectedStudents.value = current
    }
    
    fun confirmAttendance(lectureId: Long) {
        viewModelScope.launch {
            _scanningState.value = ScanningState.Confirming
            
            val result = attendanceUseCase.confirmAttendance(
                lectureId,
                _detectedStudents.value
            )
            
            when (result) {
                is Result.Success -> _scanningState.value = ScanningState.Success
                is Result.Error -> _scanningState.value = ScanningState.Error(result.error.message)
            }
        }
    }
}

sealed class ScanningState {
    object Idle : ScanningState()
    object Scanning : ScanningState()
    object Confirming : ScanningState()
    object Success : ScanningState()
    data class Error(val message: String) : ScanningState()
}
```

---

## 6. Code Sharing Breakdown

### 6.1 Estimated Code Distribution

| Layer | Shared % | Platform-Specific % | Notes |
|-------|----------|---------------------|-------|
| Domain Models | 100% | 0% | Fully shared |
| Business Logic | 95% | 5% | Slight platform differences in error handling |
| Validation | 100% | 0% | Fully shared |
| Repository Interfaces | 100% | 0% | Fully shared |
| Network Layer | 90% | 10% | Ktor client mostly shared |
| Camera | 0% | 100% | Platform-specific (CameraX vs AVFoundation) |
| Face Detection | 0% | 100% | Platform-specific (ML Kit vs Vision) |
| Face Recognition | 20% | 80% | Algorithm shared, ML runtime platform-specific |
| Image Processing | 10% | 90% | Some shared utilities |
| UI Layer | 0% | 100% | Platform-specific (Compose vs SwiftUI) |
| **Overall** | **60-70%** | **30-40%** | |

---

## 7. Dependency Injection Strategy

### 7.1 Shared DI (Koin)

```kotlin
// shared/commonMain
package com.afora.shared.di

import org.koin.core.module.Module
import org.koin.dsl.module

val sharedModule = module {
    // Use cases
    single { AttendanceUseCase(get(), get()) }
    single { LeaveUseCase(get(), get()) }
    
    // Validators
    single { AttendanceValidator() }
    single { LeaveValidator() }
    
    // Repositories (expect/actual for implementations)
    single<AttendanceRepository> { AttendanceRepositoryImpl(get()) }
    
    // API Client
    single { ApiClient(get()) }
    single { createHttpClient() }
}

expect fun createHttpClient(): HttpClient
```

### 7.2 Android DI (Hilt)

```kotlin
// androidApp/
package com.afora.android.di

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideCameraController(
        @ApplicationContext context: Context
    ): CameraController {
        return CameraController(context, /* lifecycle owner */)
    }
    
    @Provides
    @Singleton
    fun provideFaceDetector(): FaceDetector {
        return FaceDetector()
    }
    
    @Provides
    @Singleton
    fun provideFaceRecognizer(@ApplicationContext context: Context): FaceRecognizer {
        return FaceRecognizer(context.getModelPath())
    }
    
    // Koin integration
    @Provides
    fun provideAttendanceUseCase(): AttendanceUseCase {
        return KoinJavaComponent.get(AttendanceUseCase::class.java)
    }
}
```

---

## 8. Testing Strategy

### 8.1 Shared Tests

```kotlin
// shared/commonTest
package com.afora.shared.domain.usecase

class AttendanceUseCaseTest {
    
    @Test
    fun `confirmAttendance should reject duplicate students`() = runTest {
        // Given
        val useCase = AttendanceUseCase(mockRepository, AttendanceValidator())
        val duplicates = listOf(
            RecognitionResult(studentId = 1, confidence = 0.9, category = ConfidenceCategory.HIGH),
            RecognitionResult(studentId = 1, confidence = 0.85, category = ConfidenceCategory.HIGH)
        )
        
        // When
        val result = useCase.confirmAttendance(lectureId = 123, detectedStudents = duplicates)
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).error is ValidationError)
    }
    
    @Test
    fun `calculateAttendancePercentage should handle empty records`() = runTest {
        // Given
        val useCase = AttendanceUseCase(mockRepository, AttendanceValidator())
        coEvery { mockRepository.getStudentAttendance(any(), any()) } returns Result.Success(emptyList())
        
        // When
        val result = useCase.calculateAttendancePercentage(studentId = 1, subjectId = 1)
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(0.0, (result as Result.Success).data)
    }
}
```

---

## 9. Build Configuration

### 9.1 Shared Module build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    androidTarget()
    
    // Future iOS targets
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
                
                // Networking (Ktor)
                implementation("io.ktor:ktor-client-core:2.3.5")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
                
                // DI (Koin)
                implementation("io.insert-koin:koin-core:3.5.0")
            }
        }
        
        val androidMain by getting {
            dependencies {
                // Android-specific
                implementation("androidx.camera:camera-core:1.3.0")
                implementation("androidx.camera:camera-camera2:1.3.0")
                implementation("androidx.camera:camera-lifecycle:1.3.0")
                
                // ML Kit
                implementation("com.google.mlkit:face-detection:16.1.5")
                
                // TensorFlow Lite
                implementation("org.tensorflow:tensorflow-lite:2.14.0")
                implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
                
                // Ktor Android engine
                implementation("io.ktor:ktor-client-okhttp:2.3.5")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("io.mockk:mockk:1.13.8")
            }
        }
    }
}

android {
    namespace = "com.afora.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

---

## 10. Migration Path to iOS

### 10.1 Phase 1: Android Stabilization
- Complete Android implementation
- Stabilize shared business logic
- Comprehensive testing
- Production deployment

### 10.2 Phase 2: iOS Preparation
- Add iOS targets to shared module
- Implement iOS platform abstractions
- Test shared code on iOS simulator

### 10.3 Phase 3: iOS Development
- Build SwiftUI interface
- Integrate shared KMP module
- Implement iOS-specific features
- Testing and QA

### 10.4 Phase 4: iOS Production
- Beta testing
- Production deployment
- Maintain both platforms

---

## 11. Benefits & Trade-offs

### 11.1 Benefits

✅ **Code Reuse**: 60-70% of code shared  
✅ **Consistent Logic**: Same business rules on both platforms  
✅ **Type Safety**: Kotlin's type system across platforms  
✅ **Faster iOS Development**: Once Android is stable  
✅ **Single API Client**: One network layer  
✅ **Unified Testing**: Test business logic once  

### 11.2 Trade-offs

⚠️ **Learning Curve**: Team needs KMP knowledge  
⚠️ **Tooling Maturity**: KMP tooling still evolving  
⚠️ **Build Complexity**: More complex build setup  
⚠️ **Debugging**: Cross-platform debugging can be challenging  
⚠️ **Platform Limitations**: Can't use all platform-specific APIs directly  

---

## 12. Key Architectural Decisions

### Decision 1: Koin for Shared DI

**Rationale**: Lightweight, KMP-friendly, integrates well with Hilt on Android

### Decision 2: Ktor for Networking

**Rationale**: First-class KMP support, suspend function support, lightweight

### Decision 3: Expect/Actual for Platform Code

**Rationale**: Clean abstraction, type-safe, compile-time verification

### Decision 4: Platform-Specific ML Runtimes

**Rationale**: Leverage platform-optimized models (ML Kit, CoreML), better performance

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | KMP Architect | Initial draft |

---

**Status**: DRAFT - Awaiting technical review
