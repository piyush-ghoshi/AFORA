# Architecture Revision Summary
## AFORA - Phase A0 Completion

**Version:** 1.1  
**Date:** August 25, 2026  
**Status:** Architecture Corrections Applied

---

## Purpose

This document summarizes the **architectural corrections** applied to the AFORA project specification based on stakeholder feedback. This is NOT a restart—it builds upon the existing comprehensive documentation.

---

## Key Architectural Corrections Applied

### 1. ✅ Decision Classification System

**OLD**: Binary DECIDED/OPEN status  
**NEW**: Multi-category classification

| Category | Symbol | Description | Example |
|----------|--------|-------------|---------|
| DECIDED | 🟢 | Finalized and approved | KMP architecture |
| ASSUMPTION | 📝 | Working assumption, documented | Late attendance = 50% weightage |
| CONFIGURABLE | ⚙️ | System parameter, tunable | Attendance thresholds (75%/65%) |
| REQUIRES STAKEHOLDER INPUT | 📋 | Institutional policy needed | Leave request advance notice |
| BENCHMARK-DEPENDENT | 🧪 | Requires experimental data | Face recognition model selection |
| DEFERRED | 🔮 | Later phase decision | Offline sync strategy |
| BLOCKED | 🔴 | External dependency | Legal review pending |

**Impact**:
- Development NOT blocked waiting for all institutional policies
- Clear separation between technical and business decisions
- Configurable policies allow post-deployment tuning

---

### 2. ✅ Configurable Policies (NOT Hard-Coded)

**Critical Change**: Attendance rules, thresholds, and policies are **system parameters**, not hard-coded business logic.

#### 2.1 Attendance Calculation Policy

**BAD** (Hard-coded):
```kotlin
fun calculateAttendance(present: Int, total: Int): Double {
    return (present.toDouble() / total) * 100  // LOCKED
}
```

**GOOD** (Configurable):
```kotlin
data class AttendanceCalculationPolicy(
    val method: CalculationMethod,  // SIMPLE, EXCLUDE_LEAVE, WEIGHTED_LATE
    val includeLeave: Boolean,
    val lateWeightage: Double = 0.5
)

fun calculateAttendance(
    records: List<AttendanceRecord>,
    policy: AttendanceCalculationPolicy  // Injected, configurable
): Double {
    return when (policy.method) {
        SIMPLE -> (present / total) * 100
        EXCLUDE_LEAVE -> (present / (total - onLeave)) * 100
        WEIGHTED_LATE -> ((present + lateWeightage * late) / total) * 100
    }
}
```

#### 2.2 Attendance Threshold Policy

**BAD** (Hard-coded):
```kotlin
const val SAFE_THRESHOLD = 75.0  // Immutable constant
const val WARNING_THRESHOLD = 65.0
```

**GOOD** (Configurable):
```kotlin
data class AttendanceThresholdPolicy(
    val safeThreshold: Double = 75.0,      // Default, not hard-coded
    val warningThreshold: Double = 65.0,
    val applyToAllSubjects: Boolean = true,
    val subjectOverrides: Map<Long, AttendanceThresholdPolicy>? = null
)

// Can be updated via admin configuration or database
```

#### 2.3 Recognition Confidence Thresholds

**BAD** (Arbitrary constants):
```kotlin
const val HIGH_CONFIDENCE = 0.85  // Where did 0.85 come from?
const val MEDIUM_CONFIDENCE = 0.75
const val LOW_CONFIDENCE = 0.65
```

**GOOD** (Benchmark-driven, configurable):
```kotlin
data class RecognitionThresholdPolicy(
    val highThreshold: Double,    // Determined in Phase 5 benchmarking
    val mediumThreshold: Double,  // Based on TAR@FAR curves
    val lowThreshold: Double,     // Tunable in production
    val veryLowThreshold: Double
)

// Example after benchmarking:
// highThreshold = 0.87 (TAR=91%, FAR=0.1%)
// mediumThreshold = 0.76 (TAR=88%, FAR=1%)
// Can be adjusted based on production false accept/reject rates
```

**Documents Updated**:
- `attendance-engine.md` - Section 6: Configurable policies
- `decision-log.md` - Reclassified decisions
- `face-recognition.md` - Noted thresholds are configurable

---

### 3. ✅ Model-Agnostic CV Architecture

**Critical Change**: Do NOT commit to specific face recognition models prematurely.

**OLD**: "Use MTCNN + FaceNet"  
**NEW**: "Evaluate models in Phase 5, select based on benchmarks"

#### 3.1 Detection Models - TO BE EVALUATED

| Model | Pros | Cons | Status |
|-------|------|------|--------|
| MTCNN | Accurate, landmarks | Slower | 🧪 BENCHMARK |
| BlazeFace | Very fast, mobile-optimized | Lower accuracy | 🧪 BENCHMARK |
| MediaPipe | Good tracking | Google dependency | 🧪 BENCHMARK |
| YuNet | Balanced | Newer, less tested | 🧪 BENCHMARK |

#### 3.2 Recognition Models - TO BE EVALUATED

| Model | Embedding Size | Pros | Cons | Status |
|-------|----------------|------|------|--------|
| FaceNet | 128/512 | Industry standard | Larger model | 🧪 BENCHMARK |
| MobileFaceNet | 128 | Mobile-optimized | Slightly lower accuracy | 🧪 BENCHMARK |
| ArcFace | 512 | State-of-art accuracy | Heavy computation | 🧪 BENCHMARK |
| InsightFace | 512 | Good balance | - | 🧪 BENCHMARK |

#### 3.3 Benchmark-Driven Selection

**Process**:
1. **Phase 5**: Implement all candidate models
2. Collect benchmark dataset (100-200 people, 1000+ images)
3. Measure: accuracy, latency, memory, battery, multi-face handling
4. Test in classroom conditions (lighting, angles, distance)
5. **SELECT** based on objective metrics, not popularity
6. Determine confidence thresholds from TAR@FAR curves
7. Validate in pilot (Phase 11)

**Documents Created**:
- `benchmark-plan.md` - Complete benchmarking methodology
- `face-recognition.md` - Updated to reflect model-agnostic approach

---

### 4. ✅ Attendance Session State Machine

**Critical Change**: Explicit state machine with defined transitions.

#### 4.1 States

```
SCHEDULED → CREATED → ACTIVE → SCANNING → REVIEW → FINALIZING → FINALIZED

Alternative paths:
ACTIVE → CANCELLED
SCANNING → RETRY_REQUIRED
FINALIZING → FAILED → REVIEW
```

#### 4.2 State Properties

| State | Client State | Backend State | Can Cancel | Can Modify |
|-------|--------------|---------------|------------|------------|
| SCHEDULED | - | Timetable only | Yes | Yes |
| CREATED | Session exists | Session record | Yes | Yes |
| ACTIVE | Capturing attendance | Session active | Yes | Yes |
| SCANNING | Processing frames | Session active | No | No |
| REVIEW | Teacher reviewing | Session active | Yes | Yes |
| FINALIZING | Submitting | Transaction in progress | No | No |
| FINALIZED | Complete | Attendance stored | No | No (only corrections) |
| CANCELLED | Aborted | Cancelled | - | No |
| FAILED | Error | Failed state | Yes | Yes |

#### 4.3 Validation at Each Transition

**Example: REVIEW → FINALIZING**:
```java
@Transactional
public AttendanceConfirmation finalizeAttendance(Long sessionId, AttendanceSubmission submission) {
    // 1. Validate session state
    LectureSession session = getSession(sessionId);
    if (session.status != REVIEW && session.status != ACTIVE) {
        throw new InvalidStateException("Session must be in REVIEW state");
    }
    
    // 2. Check idempotency
    if (submissionExists(submission.clientSubmissionId)) {
        return getPreviousResult(submission.clientSubmissionId);
    }
    
    // 3. Validate teacher authorization
    if (!isTeacherAssigned(currentUser, session)) {
        throw new UnauthorizedException();
    }
    
    // 4. Validate all students enrolled
    validateAllStudentsEnrolled(submission.students, session);
    
    // 5. Transition state
    session.status = FINALIZING;
    saveSession(session);
    
    // 6. Create attendance records
    List<AttendanceRecord> records = createRecords(submission);
    
    // 7. Finalize
    session.status = FINALIZED;
    session.presentCount = countPresent(records);
    saveSession(session);
    
    // 8. Audit log
    auditLog.log(AttendanceFinalized(session, records));
    
    return new AttendanceConfirmation(records);
}
```

**Document Created**:
- `attendance-engine.md` - Complete state machine definition

---

### 5. ✅ Concurrency & Idempotency

**Critical Change**: Explicit concurrency control and idempotency mechanisms.

#### 5.1 Prevent Duplicate Sessions

**Database Constraint**:
```sql
CREATE UNIQUE INDEX idx_unique_lecture_session
ON lecture_sessions (subject_id, class_section_id, teacher_id, date, start_time)
WHERE status != 'CANCELLED';
```

**Backend Logic**:
```java
public LectureSession startSession(LectureSessionRequest request) {
    // Check for existing non-cancelled session
    Optional<LectureSession> existing = findExistingSession(
        request.subjectId, request.classId, request.date, request.startTime
    );
    
    if (existing.isPresent() && !existing.get().isCancelled()) {
        if (existing.get().isFinalized()) {
            throw new SessionAlreadyFinalizedException();
        } else {
            // Resume existing session instead of creating duplicate
            return existing.get();
        }
    }
    
    // Create new session
    return createSession(request);
}
```

#### 5.2 Prevent Duplicate Attendance

**Database Constraint**:
```sql
CREATE UNIQUE INDEX idx_unique_attendance
ON attendance_records (lecture_session_id, student_id);
```

**Backend Logic**:
```java
// Idempotency via clientSubmissionId
if (submissionRepository.exists(submission.clientSubmissionId)) {
    // Return previous result instead of processing again
    return submissionRepository.getResult(submission.clientSubmissionId);
}
```

**Document Updated**:
- `attendance-engine.md` - Section 5: Concurrency & Idempotency

---

### 6. ✅ Security Threat Model

**Critical Change**: Explicit threat catalog with mitigations.

#### 6.1 Threat Categories (STRIDE)

| Category | Count | Example |
|----------|-------|---------|
| **Spoofing** | 4 threats | Brute force, session hijacking, photo spoofing |
| **Tampering** | 4 threats | Fake attendance, history manipulation, replay attack |
| **Repudiation** | 2 threats | Audit trail requirements |
| **Information Disclosure** | 3 threats | Student data access, face data exposure |
| **Denial of Service** | 2 threats | API rate abuse, resource exhaustion |
| **Elevation of Privilege** | 1 threat | Privilege escalation |

#### 6.2 Critical Mitigations

**THREAT-ATT-001: Fake Attendance Submission**
```java
@PostMapping("/lectures/{id}/confirm")
@PreAuthorize("hasRole('TEACHER')")
@Transactional
public ResponseEntity<AttendanceConfirmation> confirmAttendance(...) {
    // 1. Idempotency check
    if (submissionExists(clientSubmissionId)) { ... }
    
    // 2. Teacher assignment validation
    if (!isTeacherAssigned(user, lectureId)) { throw new UnauthorizedException(); }
    
    // 3. Session state validation
    if (session.status == FINALIZED) { throw new AlreadyFinalizedException(); }
    
    // 4. Enrollment validation
    for (student : submission.students) {
        if (!isStudentEnrolled(student, session.classId)) {
            throw new ValidationException("Student not enrolled");
        }
    }
    
    // 5. Create records with audit trail
    ...
}
```

**THREAT-DATA-002: Face Data Exposure**
- ✅ NO embeddings sent to client unnecessarily
- ✅ Face images encrypted at rest
- ✅ Object storage ACL (S3/MinIO)
- ✅ Audit log for all face data access

**Document Created**:
- `threat-model.md` - Complete security threat model

---

### 7. ✅ Offline/Pending Synchronization Architecture

**Critical Change**: Design for offline compatibility NOW, implement synchronization LATER.

#### 7.1 Attendance Submission Model

```kotlin
data class AttendanceSubmission(
    val clientSubmissionId: String,  // UUID - client-generated
    val lectureSessionId: Long,
    val attendanceCandidates: List<AttendanceCandidate>,
    val submittedAt: Instant,
    val syncState: SyncState = SyncState.PENDING,
    val deviceInfo: DeviceInfo?
)

enum class SyncState {
    PENDING,    // Not yet sent to server
    SYNCING,    // Upload in progress
    SYNCED,     // Successfully synced
    FAILED      // Sync failed, will retry
}
```

#### 7.2 Sync Strategy (Phase 8 - DEFERRED)

```
Client generates attendance locally
  ↓
Saves to local database with PENDING state
  ↓
If network available:
  → Submit to backend
  → On success: Update state to SYNCED
  → On failure: Keep as PENDING, retry later
  ↓
If network unavailable:
  → Keep as PENDING
  → Show "Pending Sync" indicator to teacher
  → Retry when network restored
```

**NOT implementing complex offline-first system in Phase 1-7.**  
**Architecture is compatible with future offline sync.**

**Document Updated**:
- `attendance-engine.md` - Noted sync state in submission model

---

### 8. ✅ Measurable Engineering Targets

**Critical Change**: Explicit performance targets, not invented numbers.

#### 8.1 Backend Targets

| Operation | Target | Max Acceptable | Measurement Method |
|-----------|--------|----------------|-------------------|
| Start session API | <200ms | <500ms | P95 latency |
| Fetch enrolled students | <300ms | <1s | P95 latency |
| Submit attendance | <500ms | <2s | P95 latency |
| Validation checks | <100ms | <300ms | Average |

#### 8.2 Face Recognition Targets

| Metric | Target | Max Acceptable | Status |
|--------|--------|----------------|--------|
| Detection rate (frontal) | >95% | >90% | 🧪 BENCHMARK Phase 5 |
| Recognition accuracy (baseline) | >90% | >85% | 🧪 BENCHMARK Phase 5 |
| TAR @ 1% FAR | >90% | >85% | 🧪 BENCHMARK Phase 5 |
| False Accept Rate | <1% | <2% | 🧪 BENCHMARK Phase 5 |
| Detection time (per frame) | <200ms | <500ms | 🧪 BENCHMARK Phase 5 |
| Embedding time (per face) | <100ms | <300ms | 🧪 BENCHMARK Phase 5 |
| End-to-end latency | <300ms | <800ms | 🧪 BENCHMARK Phase 5 |
| FPS | 2-3 fps | 1 fps minimum | 🧪 BENCHMARK Phase 5 |
| Memory usage (peak) | <300MB | <500MB | 🧪 BENCHMARK Phase 5 |
| Battery drain (10min) | <15% | <30% | 🧪 BENCHMARK Phase 5 |

**Targets marked as 🧪 BENCHMARK** will be validated/adjusted in Phase 5.

**Documents Updated**:
- `attendance-engine.md` - Section 12: Performance Targets
- `benchmark-plan.md` - Section 4: Evaluation Metrics

---

### 9. ✅ Biometric Data Governance

**Critical Change**: Biometric data retention is NOT a technical default—it's a legal/policy decision.

#### 9.1 Requirements

**Status**: 📋 REQUIRES STAKEHOLDER INPUT + Legal Review

**Must Determine**:
1. Applicable privacy laws (GDPR? CCPA? India PDPA? Local laws?)
2. Biometric data classification (sensitive? critical?)
3. Student consent process (explicit? parental for minors?)
4. Retention period (1 year? 3 years? until deletion request?)
5. Deletion workflow (right to be forgotten)
6. Data breach notification requirements
7. Third-party processor agreements (if using cloud)

#### 9.2 Architecture Support

**System must support**:
```kotlin
data class BiometricDataPolicy(
    val retentionPeriod: Duration,  // Configurable
    val purpose: DataPurpose,
    val legalBasis: LegalBasis,
    val consentRequired: Boolean,
    val deletionOnRequest: Boolean,
    val breachNotificationRequired: Boolean
)

// Face profile lifecycle
enum class FaceProfileStatus {
    PENDING_CONSENT,  // Awaiting student consent
    ACTIVE,           // Consent obtained, in use
    EXPIRED,          // Retention period ended
    DELETION_REQUESTED, // Student requested deletion
    DELETED           // Soft-deleted, audit trail preserved
}
```

**Document Updated**:
- `threat-model.md` - Section 4: Compliance & Legal
- `decision-log.md` - FR-004 reclassified as REQUIRES STAKEHOLDER INPUT

---

### 10. ✅ Clean CV/Business Logic Boundary

**Critical Change**: Face recognition is a **candidate generator**, NOT the attendance engine.

#### 10.1 Architecture Layers

```
┌────────────────────────────────────────────────┐
│     Face Recognition Module                    │
│     (Platform-specific: Android ML)            │
│                                                 │
│  Input: CameraFrame                            │
│  Output: List<RecognitionResult>               │
│  - No attendance logic                         │
│  - No database access                          │
│  - No business validation                      │
└──────────────┬─────────────────────────────────┘
               │ RecognitionResult (studentId, confidence, metadata)
               ↓
┌────────────────────────────────────────────────┐
│   Attendance Candidate Generator               │
│   (Shared KMP business logic)                  │
│                                                 │
│  Input: RecognitionResult + EnrolledStudents   │
│  Output: List<AttendanceCandidate>             │
│  - Validates student enrollment                │
│  - Categorizes by confidence                   │
│  - Applies deduplication                       │
└──────────────┬─────────────────────────────────┘
               │ AttendanceCandidate (temporary)
               ↓
┌────────────────────────────────────────────────┐
│        Attendance Engine                       │
│   (Shared KMP business logic)                  │
│                                                 │
│  - Business validation                         │
│  - Session state management                    │
│  - Teacher review workflow                     │
│  - Final confirmation                          │
│  - Backend submission                          │
└──────────────┬─────────────────────────────────┘
               │ AttendanceRecord (final)
               ↓
┌────────────────────────────────────────────────┐
│           Backend API                          │
│   (Spring Boot)                                │
│                                                 │
│  - Authorization                               │
│  - Validation                                  │
│  - Database storage                            │
│  - Audit trail                                 │
└────────────────────────────────────────────────┘
```

**Key Principle**: Face recognition NEVER directly finalizes attendance.

**Document Updated**:
- `attendance-engine.md` - Section 8: Recognition Integration Boundary

---

## Revised Development Strategy

### Track A: Product Platform

| Phase | Name | Duration | Status |
|-------|------|----------|--------|
| **A0** | Documentation & Architecture Revision | 1 week | ✅ IN PROGRESS |
| **A1** | KMP Project Foundation | 2 weeks | Planned |
| **A2** | Android Foundation | 2 weeks | Planned |
| **A3** | Backend Foundation | 2 weeks | Planned |
| **A4** | Authentication | 1 week | Planned |
| **A5** | Academic Foundation | 3 weeks | Planned |
| **A6** | Timetable & Lecture Session | 2 weeks | Planned |
| **A7** | Manual Attendance | 3 weeks | Planned |

### Track B: Computer Vision Technical Spike

| Phase | Name | Duration | Status |
|-------|------|----------|--------|
| **CV-01** | Multi-Face Detection | 1 week | Planned |
| **CV-02** | Face Tracking | 1 week | Planned |
| **CV-03** | Face Embeddings | 1 week | Planned |
| **CV-04** | Identity Matching | 1 week | Planned |
| **CV-05** | Multi-Frame Deduplication | 1 week | Planned |
| **CV-06** | Teacher Review UI | 1 week | Planned |

**Note**: Track B (CV) progresses INDEPENDENTLY from Track A. Do NOT integrate until both tracks are mature.

---

## Updated Documentation Status

| Document | Status | Key Changes |
|----------|--------|-------------|
| `requirements.md` | ✅ COMPLETE | Minor updates needed for configurable policies |
| `architecture.md` | ✅ COMPLETE | Add state machine, update CV boundary |
| `database.md` | ✅ COMPLETE | Add session state enum, sync state |
| `kmp-architecture.md` | ✅ COMPLETE | Clarify platform boundaries |
| `attendance-engine.md` | ✅ **NEW** | Complete state machine & policies |
| `face-recognition.md` | ✅ COMPLETE | Model-agnostic, benchmark-driven |
| `threat-model.md` | ✅ **NEW** | Complete threat catalog |
| `benchmark-plan.md` | ✅ **NEW** | Complete methodology |
| `security.md` | 🔮 DEFERRED | Covered in threat-model.md |
| `api-contract.md` | 🔮 DEFERRED | Create during implementation |
| `roadmap.md` | ✅ COMPLETE | Update with two-track approach |
| `decision-log.md` | ✅ UPDATED | New classification system |

---

## Critical Open Decisions Summary

### DECIDED (4)

1. KMP architecture from day one
2. Modular monolith backend
3. On-device face recognition
4. PostgreSQL + pgvector

### CONFIGURABLE (8)

1. Attendance calculation method
2. Attendance thresholds (75%/65%)
3. Correction time limit
4. Session timeout duration
5. Recognition confidence thresholds (after benchmarking)
6. Rate limits
7. Password policy
8. Notification delivery methods

### BENCHMARK-DEPENDENT (3)

1. Face detection model
2. Face recognition model
3. Confidence threshold values

### REQUIRES STAKEHOLDER INPUT (9)

1. Academic calendar structure
2. Attendance calculation method (policy)
3. Low attendance threshold values (institutional policy)
4. Leave request advance notice
5. Retroactive leave policy
6. Leave approval authority
7. Teacher assignment rules
8. Student class change handling
9. Lecture cancellation policy

### LEGAL REVIEW REQUIRED (1)

1. Biometric data retention period

### DEFERRED (3)

1. Offline synchronization implementation (architecture ready)
2. Liveness detection (evaluate if spoofing detected)
3. Parent portal

---

## Next Steps (Phase A1)

**Immediately After Phase A0 Approval**:

1. ✅ Create project repository structure
2. ✅ Set up KMP modules (shared, androidApp)
3. ✅ Configure Gradle for KMP
4. ✅ Implement platform abstractions (expect/actual interfaces)
5. ✅ Set up dependency injection (Koin + Hilt)
6. ✅ Create basic navigation structure
7. ✅ Implement design system (Material 3 theme)
8. ✅ Set up CI/CD pipeline

**Phase A1 Acceptance Criteria**:
- [ ] Project compiles successfully
- [ ] KMP module structure correct
- [ ] Platform abstractions defined
- [ ] Basic navigation works
- [ ] DI configured
- [ ] CI/CD runs
- [ ] Design system implemented

---

## Conclusion

**Phase A0 Status**: Architecture corrections applied to existing documentation.

**Key Achievements**:
✅ Configurable policies (not hard-coded)  
✅ Model-agnostic CV architecture  
✅ Explicit state machine  
✅ Threat model defined  
✅ Benchmark methodology documented  
✅ Decision classification system  
✅ Clean CV/business boundary  

**Ready to Proceed**: Phase A1 (KMP Project Foundation)

**NOT Ready Until**:
- [ ] Stakeholder review of revised architecture
- [ ] Approval to proceed
- [ ] Team assembled

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | System Architect | Initial documentation |
| 1.1 | 2026-08-25 | System Architect | Architecture corrections applied |

**Status**: Phase A0 Complete - Awaiting Approval
