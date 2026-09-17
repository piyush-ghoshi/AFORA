# Face Recognition Technical Document
## Smart Classroom Attendance Management System

**Version:** 1.0  
**Date:** August 25, 2026  
**Status:** Draft - Requires Model Benchmarking

---

## 1. Executive Summary

This document outlines the face recognition strategy for the Smart Classroom Attendance Management System. The approach prioritizes **teacher validation over AI automation**, treating face recognition as an **attendance candidate generator**, not the source of truth.

### Core Principles

1. **AI-Assisted, Human-Validated**: Recognition suggests, teacher confirms
2. **Multi-Frame Scanning**: Continuous detection across camera rotation
3. **Local Processing**: On-device recognition for privacy and performance
4. **Search Space Limitation**: Match only against enrolled students
5. **Graceful Degradation**: Manual fallback always available

---

## 2. Recognition Pipeline Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    CAMERA PIPELINE                          │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ▼
          ┌────────────────┐
          │  CameraX       │
          │  Frame Stream  │
          └───────┬────────┘
                  │ 2-3 fps sampling
                  ▼
          ┌───────────────────┐
          │  Frame Sampling   │
          │  & Preprocessing  │
          └───────┬───────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────┐
│                   DETECTION STAGE                           │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────┐     ┌──────────────────┐           │
│  │  Face Detection  │────▶│   Face Tracking  │           │
│  │  (MTCNN/BlazeFace)│     │   (IoU-based)    │           │
│  └──────────────────┘     └──────────────────┘           │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│                   RECOGNITION STAGE                         │
├─────────────────────────────────────────────────────────────┤
│  ┌────────────────────┐                                     │
│  │  Face Alignment    │                                     │
│  │  & Cropping        │                                     │
│  └──────┬─────────────┘                                     │
│         │                                                    │
│         ▼                                                    │
│  ┌────────────────────┐                                     │
│  │  Face Embedding    │                                     │
│  │  (FaceNet/ArcFace) │                                     │
│  └──────┬─────────────┘                                     │
│         │                                                    │
│         ▼                                                    │
│  ┌────────────────────┐     ┌──────────────────┐          │
│  │  Cosine Similarity │────▶│  Confidence Score│          │
│  │  (vs Enrolled)     │     │  Classification   │          │
│  └────────────────────┘     └──────────────────┘          │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│                   DEDUPLICATION STAGE                       │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────┐              │
│  │  Cross-Frame Identity Tracking           │              │
│  │  - Tracking ID correlation               │              │
│  │  - IoU-based face position matching      │              │
│  │  - Temporal consistency checking         │              │
│  │  - Keep highest confidence match         │              │
│  └──────────────────────────────────────────┘              │
└──────────────┬──────────────────────────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│                   OUTPUT                                    │
├─────────────────────────────────────────────────────────────┤
│  HIGH CONFIDENCE → Auto-add to present list                │
│  MEDIUM CONFIDENCE → Needs review section                  │
│  LOW CONFIDENCE → Needs review section                     │
│  VERY LOW/UNKNOWN → Unknown faces section                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Model Selection Criteria (Phase 5 Benchmarking)

### 3.1 Detection Models to Evaluate

| Model | Pros | Cons | Mobile-Friendly |
|-------|------|------|-----------------|
| **MTCNN** | Good accuracy, landmark detection | Slower (3 networks) | Medium |
| **BlazeFace** | Very fast, Google-optimized | Lower accuracy than MTCNN | High |
| **MediaPipe Face** | Fast, good tracking | Google dependency | High |
| **YuNet** | Balanced speed/accuracy | Newer, less tested | High |

### 3.2 Recognition Models to Evaluate

| Model | Pros | Cons | Mobile-Friendly |
|-------|------|------|-----------------|
| **FaceNet** | Industry standard, good accuracy | Larger model size | Medium |
| **ArcFace** | State-of-art accuracy | Heavy computation | Low |
| **MobileFaceNet** | Mobile-optimized | Slightly lower accuracy | High |
| **InsightFace** | Good balance | Integration complexity | Medium |

### 3.3 Benchmarking Metrics (Phase 5)

**Accuracy Metrics:**
- True Positive Rate (TPR) @ 1% FPR
- False Accept Rate (FAR)
- False Reject Rate (FRR)
- Recognition accuracy in controlled environment
- Recognition accuracy in classroom conditions

**Performance Metrics:**
- Average inference time per face
- Memory usage
- Battery consumption over 5-minute session
- Frames processed per second

**Robustness Metrics:**
- Performance under varying lighting
- Performance at different angles (±30°, ±45°)
- Performance at different distances (2m, 5m, 10m)
- Occlusion handling (mask, glasses, hat)
- Multi-face detection capacity

**Real-World Metrics:**
- Average detection count in 60-student classroom
- False positive rate in production
- Teacher correction rate
- Session completion time

---

## 4. Face Enrollment Workflow

```
Student Login
  ↓
Navigate to Face Registration
  ↓
Camera Permission Request
  ↓
Show Instructions:
  - Remove glasses/hat
  - Good lighting
  - Face camera directly
  - Neutral expression
  ↓
Capture Frame 1 (Front)
  ↓
Quality Check:
  - Face detected?
  - Single face?
  - Sufficient size?
  - Good lighting?
  - Sharp/not blurry?
  ↓
[If Pass] Capture Frame 2 (Slight Left)
  ↓
Quality Check
  ↓
[If Pass] Capture Frame 3 (Slight Right)
  ↓
Quality Check
  ↓
[If Pass] Capture Frame 4 (Optional: Slight Up)
  ↓
Quality Check
  ↓
[If Pass] Capture Frame 5 (Optional: Slight Down)
  ↓
Generate Embeddings:
  - One embedding per image
  - Total 3-5 embeddings per student
  ↓
Upload to Backend:
  - Images → Object Storage (encrypted)
  - Embeddings → PostgreSQL (pgvector)
  - Metadata → face_profiles table
  ↓
Confirmation
```

### 4.1 Quality Validation Rules

```kotlin
fun validateFaceQuality(image: PlatformImage, face: DetectedFace): QualityResult {
    val issues = mutableListOf<String>()
    
    // Face size check
    val faceArea = face.boundingBox.width * face.boundingBox.height
    val imageArea = image.getWidth() * image.getHeight()
    val faceRatio = faceArea.toDouble() / imageArea
    
    if (faceRatio < 0.15) {
        issues.add("Face too small - move closer to camera")
    }
    
    // Face position check (centered)
    val centerX = image.getWidth() / 2
    val centerY = image.getHeight() / 2
    val faceCenterX = face.boundingBox.left + face.boundingBox.width / 2
    val faceCenterY = face.boundingBox.top + face.boundingBox.height / 2
    
    val xOffset = abs(faceCenterX - centerX)
    val yOffset = abs(faceCenterY - centerY)
    
    if (xOffset > image.getWidth() * 0.2 || yOffset > image.getHeight() * 0.2) {
        issues.add("Face not centered - adjust position")
    }
    
    // Brightness check (approximate)
    val brightness = estimateBrightness(image, face.boundingBox)
    if (brightness < 0.3) {
        issues.add("Too dark - improve lighting")
    } else if (brightness > 0.85) {
        issues.add("Too bright - reduce lighting")
    }
    
    // Sharpness check (approximate using edge detection)
    val sharpness = estimateSharpness(image, face.boundingBox)
    if (sharpness < 0.4) {
        issues.add("Image blurry - hold phone steady")
    }
    
    return QualityResult(
        isValid = issues.isEmpty(),
        score = calculateQualityScore(faceRatio, brightness, sharpness),
        issues = issues
    )
}
```

---

## 5. Recognition Matching Algorithm

### 5.1 Search Space Limitation

```kotlin
suspend fun getRecognitionSearchSpace(lectureId: Long): Map<Long, List<FloatArray>> {
    // Step 1: Get enrolled students for lecture's class
    val enrolledStudents = repository.getEnrolledStudents(lectureId)
    
    // Step 2: Fetch face embeddings only for these students
    val studentIds = enrolledStudents.map { it.id }
    val embeddings = repository.getFaceEmbeddings(studentIds)
    
    // Result: Map of studentId to list of embeddings (3-5 per student)
    return embeddings
}
```

### 5.2 Matching Logic

```kotlin
suspend fun recognizeFace(
    faceEmbedding: FloatArray,
    searchSpace: Map<Long, List<FloatArray>>
): RecognitionResult? {
    
    var bestMatch: Triple<Long, Double, Int>? = null  // studentId, score, embeddingIndex
    
    // Compare against all enrolled students
    for ((studentId, embeddings) in searchSpace) {
        for ((index, knownEmbedding) in embeddings.withIndex()) {
            val similarity = cosineSimilarity(faceEmbedding, knownEmbedding)
            
            if (bestMatch == null || similarity > bestMatch.second) {
                bestMatch = Triple(studentId, similarity, index)
            }
        }
    }
    
    return bestMatch?.let { (studentId, confidence, _) ->
        RecognitionResult(
            studentId = studentId,
            confidence = confidence,
            category = categorizeConfidence(confidence)
        )
    }
}

fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
    require(a.size == b.size) { "Embeddings must have same dimension" }
    
    var dotProduct = 0.0
    var normA = 0.0
    var normB = 0.0
    
    for (i in a.indices) {
        dotProduct += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }
    
    return if (normA == 0.0 || normB == 0.0) {
        0.0
    } else {
        dotProduct / (sqrt(normA) * sqrt(normB))
    }
}

fun categorizeConfidence(confidence: Double): ConfidenceCategory {
    return when {
        confidence >= HIGH_CONFIDENCE_THRESHOLD -> ConfidenceCategory.HIGH
        confidence >= MEDIUM_CONFIDENCE_THRESHOLD -> ConfidenceCategory.MEDIUM
        confidence >= LOW_CONFIDENCE_THRESHOLD -> ConfidenceCategory.LOW
        confidence >= VERY_LOW_CONFIDENCE_THRESHOLD -> ConfidenceCategory.VERY_LOW
        else -> ConfidenceCategory.UNKNOWN
    }
}

// Configurable thresholds (to be determined in benchmarking)
const val HIGH_CONFIDENCE_THRESHOLD = 0.85
const val MEDIUM_CONFIDENCE_THRESHOLD = 0.75
const val LOW_CONFIDENCE_THRESHOLD = 0.65
const val VERY_LOW_CONFIDENCE_THRESHOLD = 0.50
```

---

## 6. Cross-Frame Deduplication

### 6.1 Deduplication Strategy

```kotlin
class FaceDeduplicator {
    private val detectionHistory = mutableMapOf<Int, MutableList<DetectionRecord>>()
    private val confirmedIdentities = mutableMapOf<Long, RecognitionResult>()
    
    fun addDetection(
        trackingId: Int?,
        recognitionResult: RecognitionResult,
        boundingBox: BoundingBox,
        frameTimestamp: Long
    ) {
        val record = DetectionRecord(
            recognitionResult = recognitionResult,
            boundingBox = boundingBox,
            frameTimestamp = frameTimestamp
        )
        
        // Strategy 1: Use tracking ID if available
        if (trackingId != null) {
            detectionHistory.getOrPut(trackingId) { mutableListOf() }.add(record)
            updateConfirmedIdentity(trackingId, recognitionResult)
        } 
        // Strategy 2: Use student ID for deduplication
        else {
            val studentId = recognitionResult.studentId
            updateConfirmedIdentity(studentId.toInt(), recognitionResult)
        }
    }
    
    private fun updateConfirmedIdentity(key: Int, newResult: RecognitionResult) {
        val studentId = newResult.studentId
        val existing = confirmedIdentities[studentId]
        
        // Keep the result with highest confidence
        if (existing == null || newResult.confidence > existing.confidence) {
            confirmedIdentities[studentId] = newResult
        }
    }
    
    fun getFinalIdentities(): List<RecognitionResult> {
        return confirmedIdentities.values.toList()
    }
    
    fun clear() {
        detectionHistory.clear()
        confirmedIdentities.clear()
    }
}

data class DetectionRecord(
    val recognitionResult: RecognitionResult,
    val boundingBox: BoundingBox,
    val frameTimestamp: Long
)
```

### 6.2 Spatial Deduplication (Fallback)

If tracking IDs are unreliable, use spatial correlation:

```kotlin
fun isSameFace(box1: BoundingBox, box2: BoundingBox, threshold: Double = 0.5): Boolean {
    val iou = calculateIoU(box1, box2)
    return iou > threshold
}

fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Double {
    val x1 = max(box1.left, box2.left)
    val y1 = max(box1.top, box2.top)
    val x2 = min(box1.left + box1.width, box2.left + box2.width)
    val y2 = min(box1.top + box1.height, box2.top + box2.height)
    
    val intersection = max(0, x2 - x1) * max(0, y2 - y1)
    val area1 = box1.width * box1.height
    val area2 = box2.width * box2.height
    val union = area1 + area2 - intersection
    
    return if (union == 0) 0.0 else intersection.toDouble() / union
}
```

---

## 7. Performance Optimization

### 7.1 Frame Sampling Strategy

```kotlin
class FrameSampler(private val targetFps: Int = 2) {
    private var lastProcessedTime = 0L
    private val intervalMs = 1000L / targetFps
    
    fun shouldProcess(currentTimeMs: Long): Boolean {
        return if (currentTimeMs - lastProcessedTime >= intervalMs) {
            lastProcessedTime = currentTimeMs
            true
        } else {
            false
        }
    }
}
```

**Rationale**: Process 2-3 frames per second instead of 30fps to reduce:
- CPU usage
- Battery drain
- Thermal throttling
- Still captures sufficient data as teacher rotates slowly

### 7.2 Batch Processing

Process multiple faces in parallel where possible:

```kotlin
suspend fun processFacesInParallel(
    faces: List<DetectedFace>,
    image: PlatformImage,
    searchSpace: Map<Long, List<FloatArray>>
): List<RecognitionResult> = coroutineScope {
    faces.map { face ->
        async {
            val embedding = faceRecognizer.generateEmbedding(face, image)
            faceRecognizer.matchFaces(embedding, searchSpace)
        }
    }.awaitAll().filterNotNull()
}
```

### 7.3 Memory Management

```kotlin
// Limit embedding cache size
class EmbeddingCache(private val maxEntries: Int = 100) {
    private val cache = LruCache<Long, List<FloatArray>>(maxEntries)
    
    fun get(studentId: Long): List<FloatArray>? = cache.get(studentId)
    fun put(studentId: Long, embeddings: List<FloatArray>) = cache.put(studentId, embeddings)
}
```

---

## 8. Error Handling & Edge Cases

### 8.1 Recognition Failures

| Scenario | Detection | Mitigation |
|----------|-----------|------------|
| No faces detected | Camera feedback | Suggest better angle/lighting |
| Multiple unknown faces | UI indication | Show as "Unknown Face #1, #2..." |
| Very low confidence | Review section | Teacher manual verification |
| Model inference error | Catch exception | Graceful fallback to manual |
| Out of memory | Reduce batch size | Process fewer faces per frame |

### 8.2 Deduplication Edge Cases

| Scenario | Solution |
|----------|----------|
| Student moves between frames | Use highest confidence match |
| Tracking ID switches | Fall back to student ID deduplication |
| Multiple students with similar faces | Confidence threshold + teacher review |
| Student leaves and re-enters camera | Detect based on tracking or spatial correlation |

---

## 9. Privacy & Security

### 9.1 Face Data Protection

**Storage:**
- Images: Encrypted in object storage (S3/MinIO)
- Embeddings: PostgreSQL with TDE (Transparent Data Encryption)
- Never exposed to clients unnecessarily

**Access Control:**
- Student can only access own face data
- Teacher cannot access face images directly
- Admin can manage face profiles (re-enrollment, deletion)

**Audit:**
- Log all face data access
- Log all enrollments and deletions
- Immutable audit trail

### 9.2 GDPR/Privacy Compliance

**Consent:**
- Explicit consent during enrollment
- Clear explanation of usage
- Right to withdraw

**Data Minimization:**
- Only store necessary images (3-5 per student)
- Delete face data upon graduation + retention period
- No facial analysis beyond recognition (no age, gender, emotion)

**Security:**
- Encryption at rest
- Encryption in transit (HTTPS)
- No third-party face recognition services (on-device only)

---

## 10. Testing Strategy

### 10.1 Unit Tests

- Cosine similarity calculation
- Confidence categorization
- Deduplication logic
- Quality validation logic

### 10.2 Integration Tests

- End-to-end recognition pipeline
- Face enrollment workflow
- Cross-frame tracking

### 10.3 Benchmarking Tests (Phase 5)

**Dataset:**
- Collect sample images from 100-200 volunteers
- Multiple images per person (frontal, angles, lighting variations)
- Classroom environment images

**Test Scenarios:**
- Controlled environment (good lighting, frontal)
- Classroom environment (varied lighting, angles, distances)
- Occlusion scenarios (glasses, masks)
- Multi-face detection (10, 20, 30+ faces in frame)
- Performance over time (5-minute continuous scanning)

**Metrics:**
- True Positive Rate
- False Positive Rate
- Processing time per face
- Memory usage
- Battery consumption

---

## 11. Model Deployment Strategy

### 11.1 Model Versioning

```sql
-- Track which model generated each embedding
ALTER TABLE face_embeddings ADD COLUMN model_name VARCHAR(100);
ALTER TABLE face_embeddings ADD COLUMN model_version VARCHAR(50);

-- Example
INSERT INTO face_embeddings (face_profile_id, embedding_vector, model_name, model_version)
VALUES (123, '[...]', 'MobileFaceNet', 'v1.0');
```

### 11.2 Model Updates

When upgrading to a new model:

1. Deploy new model to app
2. Generate new embeddings for all students (background task)
3. Keep old embeddings temporarily
4. Gradually phase out old model
5. Delete old embeddings after transition period

### 11.3 A/B Testing

Compare models in production:

```kotlin
// Route 10% of sessions to new model
val useNewModel = Random.nextDouble() < 0.10
val recognizer = if (useNewModel) {
    FaceRecognizerV2(modelPathV2)
} else {
    FaceRecognizer(modelPath)
}
```

---

## 12. Open Questions & Risks

### 12.1 OPEN DECISIONS

| Decision | Options | Status |
|----------|---------|--------|
| Detection model | MTCNN, BlazeFace, MediaPipe, YuNet | OPEN - Requires Phase 5 benchmarking |
| Recognition model | FaceNet, ArcFace, MobileFaceNet, InsightFace | OPEN - Requires Phase 5 benchmarking |
| Embedding dimension | 128, 256, 512 | OPEN - Model-dependent |
| Confidence thresholds | 0.85/0.75/0.65 vs other values | OPEN - Requires real-world testing |
| Enrollment image count | 3, 5, 7 images per student | OPEN - Balance accuracy vs enrollment UX |

### 12.2 Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Recognition accuracy insufficient | High | Manual fallback, teacher validation, iterative model improvement |
| Device performance varies | Medium | Adaptive frame rate, graceful degradation |
| Poor classroom lighting | High | Enrollment in varied conditions, teacher feedback in UI |
| Students change appearance | Medium | Re-enrollment capability, periodic updates |
| Model size/performance | Medium | Mobile-optimized models, quantization |

---

## 13. Success Criteria

### 13.1 Technical Metrics

- **Detection Rate**: >95% of visible faces detected
- **Recognition Accuracy**: >90% correct identifications in controlled environment
- **False Positive Rate**: <2%
- **Processing Time**: <300ms per face
- **Deduplication Success**: <1% duplicate students in final list

### 13.2 User Experience Metrics

- **Teacher Correction Rate**: <10% of detected students require manual correction
- **Manual Fallback Usage**: <10% of sessions
- **Session Completion Time**: <3 minutes for 60-student classroom
- **Teacher Satisfaction**: Positive feedback on usability

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | CV Engineer | Initial draft |

---

**Status**: DRAFT - Awaiting Phase 5 model selection and benchmarking

**Next Steps**:
1. Complete Phase 1-4 (backend, attendance, academic structure)
2. Phase 5: Conduct comprehensive model benchmarking
3. Select optimal detection + recognition model combination
4. Implement and test with pilot users
5. Iterate based on real-world performance
