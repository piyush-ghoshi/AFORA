# Attendance Engine Design
## AFORA - Smart Classroom Attendance Management

**Version:** 1.1  
**Date:** August 25, 2026  
**Status:** Architecture Definition

---

## 1. Core Principle

The attendance engine treats **face recognition as a candidate generator**, NOT as the source of truth.

```
Face Recognition
  ↓
Attendance CANDIDATE
  ↓
Business Validation
  ↓
Teacher Review
  ↓
Teacher Confirmation
  ↓
Final Attendance RECORD
```

**The AI/CV system NEVER directly finalizes attendance.**

---

## 2. Attendance Session State Machine

### 2.1 State Definitions

```
SCHEDULED
  ↓ (Teacher creates session from timetable)
CREATED
  ↓ (Teacher starts attendance capture)
ACTIVE
  ↓ (Teacher is scanning classroom / marking manually)
SCANNING
  ↓ (Teacher finishes scanning, moves to review)
REVIEW
  ↓ (Teacher confirms attendance list)
FINALIZING
  ↓ (Backend validation and storage)
FINALIZED
  ↓ (Attendance permanently recorded)

Alternative paths:
ACTIVE → CANCELLED (Teacher cancels session)
SCANNING → RETRY_REQUIRED (Technical failure, needs retry)
FINALIZING → FAILED (Backend validation failed)
FAILED → REVIEW (Return to review for correction)
```

### 2.2 State Transitions

| From State | To State | Trigger | Authorization |
|------------|----------|---------|---------------|
| SCHEDULED | CREATED | Teacher starts session | Assigned teacher only |
| CREATED | ACTIVE | Session initialized | Automatic |
| ACTIVE | SCANNING | Teacher finishes capture | Teacher action |
| ACTIVE | CANCELLED | Teacher cancels | Teacher action |
| SCANNING | REVIEW | Data processed | Automatic |
| REVIEW | FINALIZING | Teacher confirms | Teacher action |
| FINALIZING | FINALIZED | Backend validates & stores | Automatic |
| FINALIZING | FAILED | Backend validation fails | Automatic |
| FAILED | REVIEW | Retry | Teacher action |
| Any | CANCELLED | Explicit cancellation | Teacher/Admin |

### 2.3 State Properties

**SCHEDULED**:
- Exists in timetable
- No session instance created yet
- Can be started by authorized teacher

**CREATED**:
- Session instance exists
- Not yet capturing attendance
- Can be cancelled without attendance data

**ACTIVE**:
- Attendance capture in progress (camera or manual)
- Temporary attendance data may exist (not persisted)
- Can be cancelled (discards temporary data)

**SCANNING**:
- Camera-based: CV processing frames
- Manual: List being marked
- Data in local/temporary state

**REVIEW**:
- Teacher reviewing attendance list
- Can modify (add/remove students)
- Can return to scanning/manual
- Must confirm to proceed

**FINALIZING**:
- Submitting to backend
- Backend validation in progress
- Cannot be cancelled
- If fails, returns to REVIEW

**FINALIZED**:
- Attendance permanently stored
- Audit trail created
- Students can view
- Cannot be cancelled (only corrected via query/admin)

**CANCELLED**:
- Session aborted
- No attendance recorded
- Audit trail records cancellation

**FAILED**:
- Validation error occurred
- Error message available
- Can retry after correction

### 2.4 State Persistence

**Client State** (Android app):
- Current session state
- Temporary attendance candidates
- Local face recognition results
- Review modifications

**Backend State** (Database):
- Session status (CREATED, FINALIZED, CANCELLED, FAILED)
- Final attendance records (only after FINALIZED)
- Audit log of state transitions

**Synchronization**:
- Client maintains local state during ACTIVE/SCANNING/REVIEW
- Backend is authoritative after FINALIZING
- Client must handle network failures gracefully

---

## 3. Attendance Record Lifecycle

### 3.1 Candidate vs Record

**AttendanceCandidate** (Temporary):
```kotlin
data class AttendanceCandidate(
    val studentId: Long,
    val source: CandidateSource,  // FACE_RECOGNITION, MANUAL
    val confidence: Double?,       // Only for FACE_RECOGNITION
    val category: ConfidenceCategory?,  // Only for FACE_RECOGNITION
    val status: AttendanceStatus,  // PRESENT, ABSENT
    val timestamp: Instant,
    val metadata: Map<String, Any>?  // Frame info, detection details
)

enum class CandidateSource {
    FACE_RECOGNITION,
    MANUAL,
    MANUAL_CORRECTION  // Added during review
}
```

**AttendanceRecord** (Final):
```kotlin
data class AttendanceRecord(
    val id: Long,
    val lectureSessionId: Long,
    val studentId: Long,
    val status: AttendanceStatus,
    val method: AttendanceMethod,
    val confidenceScore: Double?,  // Preserved from candidate if face recognition
    val markedBy: Long,            // Teacher ID
    val markedAt: Instant,
    val remarks: String?,
    val isCorrected: Boolean = false
)

enum class AttendanceMethod {
    CAMERA,
    MANUAL,
    HYBRID  // Combination of camera and manual
}

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    ON_LEAVE,
    LATE  // Optional, if supported
}
```

### 3.2 Transformation Flow

```
1. Face Recognition
   ↓
   AttendanceCandidate(source=FACE_RECOGNITION, confidence=0.92)

2. Business Validation
   ↓
   - Is student enrolled in this lecture?
   - Is student already marked?
   - Is session still ACTIVE?

3. Teacher Review
   ↓
   - Teacher sees candidate with confidence
   - Teacher confirms or removes
   - Teacher may add manual candidates

4. Confirmation
   ↓
   Candidates → AttendanceRecords

5. Backend Storage
   ↓
   INSERT INTO attendance_records
   (lecture_session_id, student_id, status, method, confidence_score, marked_by, marked_at)
```

---

## 4. Validation Rules

### 4.1 Pre-Session Validation

**Before starting session**:
- [ ] Teacher is assigned to this subject/class
- [ ] Lecture is scheduled (exists in timetable or manually created)
- [ ] No duplicate session exists for same lecture/date/time
- [ ] Academic year/semester is active
- [ ] Class has enrolled students

### 4.2 During-Session Validation

**While marking attendance**:
- [ ] Student is enrolled in the class
- [ ] Student is enrolled in the subject
- [ ] Student is not already marked in this session
- [ ] Session is in valid state (ACTIVE, SCANNING, REVIEW)

### 4.3 Finalization Validation

**Before storing final attendance**:
- [ ] Session exists and is in FINALIZING state
- [ ] All students in submission are enrolled
- [ ] No duplicate students in submission
- [ ] No duplicate submission (idempotency check)
- [ ] Teacher is still authorized (session not cancelled/expired)
- [ ] Attendance count is reasonable (not > enrolled count)

### 4.4 Post-Finalization Constraints

**After FINALIZED**:
- Session cannot be re-finalized (idempotency)
- Attendance records cannot be deleted (only corrected via audit trail)
- Corrections require new AttendanceCorrection record
- Original data preserved

---

## 5. Concurrency & Idempotency

### 5.1 Database Constraints

```sql
-- Prevent duplicate sessions
CREATE UNIQUE INDEX idx_unique_lecture_session
ON lecture_sessions (subject_id, class_section_id, teacher_id, date, start_time)
WHERE status != 'CANCELLED';

-- Prevent duplicate attendance
CREATE UNIQUE INDEX idx_unique_attendance
ON attendance_records (lecture_session_id, student_id);

-- Session state integrity
ALTER TABLE lecture_sessions
ADD CONSTRAINT chk_valid_status_transition
CHECK (
  (status IN ('SCHEDULED', 'CREATED', 'ACTIVE', 'SCANNING', 'REVIEW', 
              'FINALIZING', 'FINALIZED', 'CANCELLED', 'FAILED'))
);
```

### 5.2 Idempotency Strategy

**Client-side**:
```kotlin
data class AttendanceSubmission(
    val clientSubmissionId: String,  // UUID generated on client
    val lectureSessionId: Long,
    val attendanceCandidates: List<AttendanceCandidate>,
    val submittedAt: Instant,
    val deviceInfo: DeviceInfo?
)
```

**Backend processing**:
```java
@Transactional
public AttendanceConfirmation finalizeAttendance(AttendanceSubmission submission) {
    // 1. Check idempotency
    if (attendanceSubmissionExists(submission.clientSubmissionId)) {
        return getPreviousSubmissionResult(submission.clientSubmissionId);
    }
    
    // 2. Validate session state
    LectureSession session = getSession(submission.lectureSessionId);
    if (session.status == FINALIZED) {
        throw new AlreadyFinalizedException("Attendance already finalized");
    }
    
    // 3. Update session state
    session.status = FINALIZING;
    saveSession(session);
    
    // 4. Validate and create records
    List<AttendanceRecord> records = validateAndCreate(submission);
    
    // 5. Store submission for idempotency
    storeSubmission(submission.clientSubmissionId, records);
    
    // 6. Finalize session
    session.status = FINALIZED;
    session.presentCount = countPresent(records);
    session.absentCount = countAbsent(records);
    saveSession(session);
    
    return AttendanceConfirmation(records);
}
```

### 5.3 Concurrent Session Prevention

**Scenario**: Teacher accidentally starts same lecture twice

**Prevention**:
1. Database unique constraint (subject + class + date + time)
2. Backend validation before creating session
3. Client-side check (if session already exists, resume it)

**Handling**:
```java
public LectureSession startSession(LectureSessionRequest request) {
    // Check for existing session
    Optional<LectureSession> existing = findExistingSession(
        request.subjectId, 
        request.classId, 
        request.date,
        request.startTime
    );
    
    if (existing.isPresent() && !existing.get().isCancelled()) {
        if (existing.get().isFinalized()) {
            throw new SessionAlreadyFinalizedException();
        } else {
            // Resume existing session
            return existing.get();
        }
    }
    
    // Create new session
    return createSession(request);
}
```

---

## 6. Attendance Calculation

### 6.1 Configurable Calculation Policy

**DO NOT hard-code attendance calculation method.**

Create configurable policy:

```kotlin
data class AttendanceCalculationPolicy(
    val method: CalculationMethod,
    val includeLeave: Boolean,
    val includeLate: Boolean,
    val lateWeightage: Double = 0.5  // If late counts as partial
)

enum class CalculationMethod {
    SIMPLE,           // Present / Total
    EXCLUDE_LEAVE,    // Present / (Total - OnLeave)
    WEIGHTED_LATE     // (Present + lateWeightage * Late) / Total
}
```

**Implementation**:
```kotlin
fun calculateAttendancePercentage(
    records: List<AttendanceRecord>,
    policy: AttendanceCalculationPolicy
): Double {
    val total = records.size
    if (total == 0) return 0.0
    
    return when (policy.method) {
        CalculationMethod.SIMPLE -> {
            val present = records.count { it.status == PRESENT }
            (present.toDouble() / total) * 100
        }
        
        CalculationMethod.EXCLUDE_LEAVE -> {
            val onLeave = records.count { it.status == ON_LEAVE }
            val present = records.count { it.status == PRESENT }
            val effectiveTotal = total - onLeave
            if (effectiveTotal == 0) 0.0 
            else (present.toDouble() / effectiveTotal) * 100
        }
        
        CalculationMethod.WEIGHTED_LATE -> {
            val present = records.count { it.status == PRESENT }
            val late = records.count { it.status == LATE }
            val weighted = present + (policy.lateWeightage * late)
            (weighted / total) * 100
        }
    }
}
```

### 6.2 Attendance Threshold Policy

**DO NOT hard-code 75% as THE threshold.**

Make configurable:

```kotlin
data class AttendanceThresholdPolicy(
    val safeThreshold: Double = 75.0,      // >= SAFE (Green)
    val warningThreshold: Double = 65.0,   // >= WARNING (Amber)
    // < warningThreshold = CRITICAL (Red)
    val applyToAllSubjects: Boolean = true,
    val subjectOverrides: Map<Long, AttendanceThresholdPolicy>? = null
)

fun categorizeAttendance(
    percentage: Double,
    policy: AttendanceThresholdPolicy
): AttendanceHealthStatus {
    return when {
        percentage >= policy.safeThreshold -> AttendanceHealthStatus.SAFE
        percentage >= policy.warningThreshold -> AttendanceHealthStatus.WARNING
        else -> AttendanceHealthStatus.CRITICAL
    }
}

enum class AttendanceHealthStatus {
    SAFE,      // Green
    WARNING,   // Amber
    CRITICAL   // Red
}
```

---

## 7. Attendance Correction & Audit Trail

### 7.1 Correction Workflow

**Student raises query**:
```
Student: "I was present but marked absent"
  ↓
AttendanceQuery created (status: PENDING)
  ↓
Teacher reviews
  ↓
Teacher approves
  ↓
AttendanceCorrection created
  ↓
AttendanceRecord updated (isCorrected = true)
  ↓
Original data preserved in AttendanceCorrection
```

### 7.2 Correction Record

```kotlin
data class AttendanceCorrection(
    val id: Long,
    val attendanceRecordId: Long,
    val previousStatus: AttendanceStatus,
    val newStatus: AttendanceStatus,
    val reason: String,
    val correctedBy: Long,  // User ID (teacher/admin)
    val correctedByRole: UserRole,
    val relatedQueryId: Long?,  // If triggered by query
    val correctedAt: Instant,
    val auditMetadata: Map<String, Any>?
)
```

### 7.3 Audit Requirements

**Every attendance correction MUST log**:
- Original value
- New value
- Who changed it
- When
- Why (reason)
- Related query (if applicable)
- IP address (if available)
- Session information

**Audit logs are immutable** (no updates, no deletes).

---

## 8. Face Recognition Integration Boundary

### 8.1 Clean Separation

**DO NOT mix CV and business logic.**

```
┌─────────────────────────────────────────────┐
│         Face Recognition Module             │
│  (Platform-specific: Android ML runtime)    │
│                                              │
│  Input: CameraFrame                         │
│  Output: List<RecognitionResult>            │
│                                              │
│  - No attendance logic                      │
│  - No database access                       │
│  - No business validation                   │
└──────────────┬──────────────────────────────┘
               │ RecognitionResult
               ↓
┌──────────────────────────────────────────────┐
│      Attendance Candidate Generator          │
│  (Shared KMP business logic)                 │
│                                               │
│  Input: RecognitionResult + EnrolledStudents │
│  Output: List<AttendanceCandidate>           │
│                                               │
│  - Validates student enrollment              │
│  - Categorizes by confidence                 │
│  - Applies deduplication                     │
└──────────────┬───────────────────────────────┘
               │ AttendanceCandidate
               ↓
┌──────────────────────────────────────────────┐
│         Attendance Engine                    │
│  (Shared KMP business logic)                 │
│                                               │
│  - Business validation                       │
│  - Session state management                  │
│  - Teacher review workflow                   │
│  - Final confirmation                        │
│  - Backend submission                        │
└──────────────┬───────────────────────────────┘
               │ AttendanceRecord
               ↓
┌──────────────────────────────────────────────┐
│            Backend API                       │
│  (Spring Boot)                               │
│                                               │
│  - Authorization                             │
│  - Validation                                │
│  - Database storage                          │
│  - Audit trail                               │
└──────────────────────────────────────────────┘
```

### 8.2 Recognition Result Format

```kotlin
// Platform-specific output (Android ML)
data class RecognitionResult(
    val studentId: Long,
    val confidence: Double,  // Raw similarity score (e.g., 0.92)
    val embeddingDistance: Double?,  // Optional: cosine distance
    val detectionMetadata: DetectionMetadata?
)

data class DetectionMetadata(
    val frameTimestamp: Long,
    val boundingBox: BoundingBox,
    val trackingId: Int?,
    val faceQuality: Double?
)
```

### 8.3 Candidate Generation (Business Logic)

```kotlin
// Shared KMP business logic
class AttendanceCandidateGenerator(
    private val thresholdPolicy: RecognitionThresholdPolicy  // Configurable!
) {
    fun generateCandidates(
        recognitionResults: List<RecognitionResult>,
        enrolledStudents: Set<Long>
    ): List<AttendanceCandidate> {
        return recognitionResults
            .filter { it.studentId in enrolledStudents }  // Business rule
            .map { result ->
                AttendanceCandidate(
                    studentId = result.studentId,
                    source = CandidateSource.FACE_RECOGNITION,
                    confidence = result.confidence,
                    category = categorizeConfidence(result.confidence),
                    status = AttendanceStatus.PRESENT,
                    timestamp = Instant.now(),
                    metadata = result.detectionMetadata?.toMap()
                )
            }
    }
    
    private fun categorizeConfidence(confidence: Double): ConfidenceCategory {
        return when {
            confidence >= thresholdPolicy.highThreshold -> ConfidenceCategory.HIGH
            confidence >= thresholdPolicy.mediumThreshold -> ConfidenceCategory.MEDIUM
            confidence >= thresholdPolicy.lowThreshold -> ConfidenceCategory.LOW
            confidence >= thresholdPolicy.veryLowThreshold -> ConfidenceCategory.VERY_LOW
            else -> ConfidenceCategory.UNKNOWN
        }
    }
}
```

---

## 9. Manual Fallback Architecture

### 9.1 Principle

**Manual attendance MUST work independently of face recognition.**

If face recognition:
- Is not available
- Fails
- Produces no results
- Performs poorly

Teacher must still be able to complete attendance.

### 9.2 Manual Workflow

```
Teacher starts session
  ↓
Choose method:
  - Camera (if available)
  - Manual (always available)
  ↓
[IF MANUAL]
  ↓
Display enrolled students
  ↓
Quick actions: Mark All Present / Mark All Absent
  ↓
Individual toggles: Present / Absent
  ↓
Search student
  ↓
Review (same as camera workflow)
  ↓
Confirm
  ↓
Finalize
```

### 9.3 Hybrid Workflow

Teacher can:
1. Start with camera
2. Switch to manual mid-session
3. Manually add students missed by camera
4. Manually remove incorrect detections
5. Confirm final list (camera + manual)

Method recorded: `HYBRID`

---

## 10. Error Handling

### 10.1 Session Errors

| Error | State Transition | Recovery |
|-------|------------------|----------|
| Teacher unauthorized | - | Reject session creation |
| Session already exists | - | Resume existing or reject |
| Duplicate submission | FINALIZING → FINALIZED | Return previous result (idempotency) |
| Validation failure | FINALIZING → FAILED | Return to REVIEW with error |
| Network failure | Any → (retry) | Queue locally, retry when online |
| Camera unavailable | - | Fallback to manual |

### 10.2 Recognition Errors

| Error | Handling |
|-------|----------|
| No faces detected | Show "No faces detected" message, suggest manual |
| All unknown faces | Show "No students recognized", allow manual identification |
| Low confidence | Show in "Needs Review" section |
| Model inference failure | Log error, fallback to manual |
| Out of memory | Reduce batch size, retry, or fallback to manual |

---

## 11. Testing Strategy

### 11.1 State Machine Tests

**Test all valid transitions**:
- SCHEDULED → CREATED
- CREATED → ACTIVE
- ACTIVE → SCANNING
- SCANNING → REVIEW
- REVIEW → FINALIZING
- FINALIZING → FINALIZED

**Test invalid transitions**:
- FINALIZED → ACTIVE (should fail)
- CANCELLED → FINALIZING (should fail)

### 11.2 Validation Tests

- Duplicate session prevention
- Duplicate attendance prevention
- Unauthorized teacher rejection
- Non-enrolled student rejection
- Idempotency (same submission twice)

### 11.3 Concurrency Tests

- Two teachers starting same session simultaneously
- Two submissions for same session simultaneously
- Teacher submitting while admin cancels session

### 11.4 Manual Fallback Tests

- Complete attendance flow WITHOUT face recognition
- Verify manual attendance is identical to camera-based
- Measure manual workflow time (target: <2 minutes for 60 students)

---

## 12. Performance Targets

### 12.1 Backend Targets

| Operation | Target | Max Acceptable |
|-----------|--------|----------------|
| Start session API | <200ms | <500ms |
| Fetch enrolled students | <300ms | <1s |
| Submit attendance | <500ms | <2s |
| Validation checks | <100ms | <300ms |

### 12.2 Client Targets

| Operation | Target | Max Acceptable |
|-----------|--------|----------------|
| Manual mark all | <1s | <2s |
| Review screen load | <500ms | <1s |
| Confirmation submission | <2s | <5s |

---

## 13. Open Design Decisions

### Classification

| Decision | Status |
|----------|--------|
| Attendance calculation method | ⚙️ CONFIGURABLE |
| Attendance thresholds (75%/65%) | ⚙️ CONFIGURABLE |
| Recognition confidence thresholds | ⚙️ CONFIGURABLE + 🧪 BENCHMARK-DEPENDENT |
| Late attendance support | 📝 ASSUMPTION (included, can be disabled) |
| Correction time limit | ⚙️ CONFIGURABLE |
| Session timeout duration | ⚙️ CONFIGURABLE |
| Maximum concurrent sessions per teacher | ⚙️ CONFIGURABLE |

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | System Architect | Initial draft |
| 1.1 | 2026-08-25 | System Architect | Added state machine, configurable policies, clean boundaries |

**Status**: Architecture Definition Complete  
**Next**: Implement in Phase A6-A7
