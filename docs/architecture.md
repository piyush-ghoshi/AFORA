# Architecture Document
## Smart Classroom Attendance Management System

**Version:** 1.0  
**Date:** August 25, 2026  
**Status:** Draft - Awaiting Review

---

## 1. Executive Summary

This document defines the system architecture for a production-grade Smart Classroom Attendance Management System. The architecture is designed with the following core principles:

1. **Android-first, iOS-ready**: KMP architecture from day one
2. **Modular monolith**: Not microservices
3. **AI-assisted, human-validated**: Recognition suggests, humans confirm
4. **Security-first**: RBAC, audit trails, biometric protection
5. **Fail-safe design**: Manual fallback always available

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     CLIENT LAYER                             │
├──────────────────────┬──────────────────────────────────────┤
│                      │                                       │
│   Android App        │   (Future: iOS App)                  │
│   (Kotlin + Compose) │   (Swift + SwiftUI)                  │
│                      │                                       │
└──────────┬───────────┴──────────────────┬───────────────────┘
           │                               │
           │          Shared KMP           │
           │     (Business Logic)          │
           │                               │
           └───────────────┬───────────────┘
                           │
                    REST API (HTTPS)
                           │
┌──────────────────────────┴────────────────────────────────┐
│                   API GATEWAY / BACKEND                    │
│                 (Spring Boot - Modular)                    │
├────────────────────────────────────────────────────────────┤
│  ┌──────────┬──────────┬──────────┬──────────┬─────────┐ │
│  │   Auth   │ Academic │ Attendance│   Face  │  Admin  │ │
│  │  Module  │  Module  │  Module   │  Module │ Module  │ │
│  └──────────┴──────────┴──────────┴──────────┴─────────┘ │
│  ┌──────────┬──────────┬──────────┬──────────┬─────────┐ │
│  │  Users   │  Classes │  Leaves  │ Queries  │ Notif   │ │
│  │  Module  │  Module  │  Module  │  Module  │ Module  │ │
│  └──────────┴──────────┴──────────┴──────────┴─────────┘ │
└────────────────────┬──────────────────┬───────────────────┘
                     │                  │
         ┌───────────┴────────┐    ┌───┴──────────┐
         │                    │    │              │
    PostgreSQL             Redis   Object Storage
    (Primary DB)          (Cache)   (Face Images)
```

---

## 3. Client Architecture (Kotlin Multiplatform)

### 3.1 KMP Module Structure

```
project/
├── shared/                          # Kotlin Multiplatform Module
│   ├── commonMain/                  # Platform-independent code
│   │   ├── domain/                  # Domain models
│   │   ├── business/                # Business logic
│   │   ├── repository/              # Repository interfaces
│   │   ├── network/                 # API client
│   │   ├── validation/              # Validation logic
│   │   └── utils/                   # Common utilities
│   ├── androidMain/                 # Android-specific implementations
│   │   ├── camera/                  # CameraX implementation
│   │   ├── ml/                      # Android ML runtime
│   │   └── platform/                # Platform utilities
│   └── iosMain/                     # iOS-specific (future)
│       ├── camera/                  # AVFoundation
│       └── ml/                      # iOS ML runtime
│
├── androidApp/                      # Android Application
│   ├── ui/                          # Jetpack Compose screens
│   ├── viewmodel/                   # ViewModels
│   ├── navigation/                  # Navigation graph
│   ├── di/                          # Hilt dependency injection
│   └── theme/                       # Material 3 theme
│
└── iosApp/                          # iOS Application (future)
    └── (Swift/SwiftUI code)
```

### 3.2 Shared KMP Components

**Platform-Independent (commonMain):**

- Domain models (Student, Teacher, Lecture, Attendance, etc.)
- Business rules (attendance calculation, leave validation)
- Repository interfaces
- Network layer (Ktor client or Retrofit interface)
- API models (DTOs)
- Validation logic
- Error models
- State management
- Session management

**Platform-Specific (androidMain/iosMain):**

- Camera abstraction implementation
- ML runtime integration
- Image processing
- Platform-specific permissions
- Platform-specific lifecycle

### 3.3 Platform Abstraction Pattern

```kotlin
// shared/commonMain
expect interface CameraController {
    fun startCamera(config: CameraConfig): Flow<CameraFrame>
    fun stopCamera()
    suspend fun captureFrame(): CameraFrame
}

expect interface FaceRecognitionEngine {
    suspend fun detectFaces(image: PlatformImage): List<DetectedFace>
    suspend fun recognizeFaces(
        faces: List<DetectedFace>,
        enrolledStudents: List<FaceEmbedding>
    ): List<RecognitionResult>
}

// androidMain
actual class AndroidCameraController : CameraController {
    // CameraX implementation
}

actual class AndroidFaceRecognitionEngine : FaceRecognitionEngine {
    // Android ML runtime implementation
}
```

---

## 4. Backend Architecture (Spring Boot)

### 4.1 Modular Monolith Structure

```
backend/
├── application/                     # Spring Boot application
│   ├── config/                      # Configuration
│   ├── security/                    # Security config
│   └── Application.java             # Main class
│
├── auth/                            # Authentication module
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── model/
│
├── users/                           # User management
│   ├── student/
│   ├── teacher/
│   └── admin/
│
├── academic/                        # Academic structure
│   ├── department/
│   ├── program/
│   ├── batch/
│   ├── class/
│   ├── subject/
│   ├── semester/
│   └── academicyear/
│
├── enrollment/                      # Student enrollment
│   ├── controller/
│   ├── service/
│   └── repository/
│
├── timetable/                       # Timetable management
│   ├── controller/
│   ├── service/
│   └── repository/
│
├── attendance/                      # Attendance engine
│   ├── lecture/                     # Lecture sessions
│   ├── record/                      # Attendance records
│   ├── correction/                  # Corrections & audit
│   ├── analytics/                   # Attendance analytics
│   └── validation/                  # Business rules
│
├── face/                            # Face recognition
│   ├── registration/                # Face enrollment
│   ├── recognition/                 # Recognition service
│   ├── storage/                     # Image/embedding storage
│   └── model/                       # Model management
│
├── leave/                           # Leave management
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── workflow/                    # Approval workflow
│
├── query/                           # Attendance queries
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── workflow/                    # Approval workflow
│
├── notification/                    # Notification service
│   ├── controller/
│   ├── service/
│   └── provider/                    # Push, email, SMS
│
├── leaderboard/                     # Leaderboard service
│   ├── controller/
│   ├── service/
│   └── calculator/
│
├── admin/                           # Admin operations
│   ├── controller/
│   ├── service/
│   └── reports/
│
└── common/                          # Shared components
    ├── exception/                   # Global exception handling
    ├── validation/                  # Validators
    ├── audit/                       # Audit logging
    ├── util/                        # Utilities
    └── model/                       # Base models
```

### 4.2 Module Principles

1. **High Cohesion**: Related functionality grouped together
2. **Low Coupling**: Minimal dependencies between modules
3. **Clear Boundaries**: Well-defined interfaces
4. **Independent Testing**: Each module testable in isolation
5. **Future Extraction**: Modules can become microservices if needed

---

## 5. Data Architecture

### 5.1 Database Design (PostgreSQL)

**Core Entities:**

```sql
-- User Management
users
students (FK: user_id)
teachers (FK: user_id)
admins (FK: user_id)

-- Academic Structure
departments
programs (FK: department_id)
batches (FK: program_id)
classes (FK: batch_id)
sections (FK: class_id)
subjects
academic_years
semesters (FK: academic_year_id)

-- Enrollment & Assignment
enrollments (FK: student_id, class_id, subject_id)
teacher_assignments (FK: teacher_id, subject_id, class_id)

-- Timetable
timetable_slots (FK: class_id, subject_id, teacher_id)

-- Attendance
lecture_sessions (FK: subject_id, class_id, teacher_id, semester_id)
attendance_records (FK: lecture_session_id, student_id)
attendance_corrections (FK: attendance_record_id, corrected_by)

-- Face Recognition
face_profiles (FK: student_id)
face_embeddings (FK: face_profile_id)
face_images (FK: face_profile_id, storage_reference)

-- Leave Management
leave_requests (FK: student_id, approved_by)
leave_request_subjects (FK: leave_request_id, subject_id)

-- Queries
attendance_queries (FK: attendance_record_id, student_id, reviewed_by)

-- Notifications
notifications (FK: user_id)

-- Audit
audit_logs
```

### 5.2 Data Storage Strategy

| Data Type | Storage | Rationale |
|-----------|---------|-----------|
| Structured data | PostgreSQL | ACID, relations, constraints |
| Face images | Object Storage (S3) | Large binary files, scalable |
| Face embeddings | PostgreSQL (vector) | Fast similarity search (pgvector) |
| Session data | Redis | Fast access, expiry |
| Audit logs | PostgreSQL | Immutable, queryable |
| Temporary files | Local filesystem | Short-lived |

### 5.3 Database Indexing Strategy

```sql
-- High-priority indexes
CREATE INDEX idx_attendance_student_date ON attendance_records(student_id, date);
CREATE INDEX idx_attendance_lecture ON attendance_records(lecture_session_id);
CREATE INDEX idx_lecture_date_class ON lecture_sessions(date, class_id);
CREATE INDEX idx_enrollment_student ON enrollments(student_id);
CREATE INDEX idx_enrollment_class ON enrollments(class_id);
CREATE INDEX idx_timetable_class_day ON timetable_slots(class_id, day_of_week);

-- Face recognition indexes
CREATE INDEX idx_face_profile_student ON face_profiles(student_id);
-- Vector similarity index (pgvector)
CREATE INDEX idx_face_embedding_vector ON face_embeddings 
  USING ivfflat (embedding vector_cosine_ops);
```

---

## 6. Camera & Face Recognition Architecture

### 6.1 Camera Pipeline (Android)

```
CameraX
  ↓
ImageAnalysis UseCas
  ↓
Frame Sampling (Target: 2-3 fps)
  ↓
YUV to RGB Conversion
  ↓
Image Preprocessing
  ↓
Face Detection
  ↓
Face Tracking (Cross-frame correlation)
  ↓
Face Alignment & Cropping
  ↓
Face Embedding Generation
  ↓
Local Identity Matching
  ↓
Confidence Evaluation
  ↓
Deduplication
  ↓
Attendance Candidate
```

### 6.2 Recognition Service Architecture

```
┌─────────────────────────────────────────────────────┐
│              Android Recognition Service             │
├─────────────────────────────────────────────────────┤
│                                                      │
│  ┌──────────────┐      ┌──────────────┐           │
│  │ Face Detector│      │ Face Tracker │           │
│  │  (MTCNN/     │──────│  (IoU-based) │           │
│  │  BlazeFace)  │      │              │           │
│  └──────────────┘      └──────────────┘           │
│                              │                      │
│                              ↓                      │
│                    ┌──────────────────┐            │
│                    │ Face Embedder    │            │
│                    │ (FaceNet/ArcFace)│            │
│                    └──────────────────┘            │
│                              │                      │
│                              ↓                      │
│                    ┌──────────────────┐            │
│                    │ Identity Matcher │            │
│                    │ (Cosine Distance)│            │
│                    └──────────────────┘            │
│                              │                      │
│                              ↓                      │
│                    ┌──────────────────┐            │
│                    │ Deduplicator     │            │
│                    │ (Identity + IoU) │            │
│                    └──────────────────┘            │
│                                                      │
└──────────────────────────────────────────────────────┘
```

### 6.3 Recognition Flow

```
Teacher starts attendance for "DBMS - CSE-3A"
  ↓
Backend: Fetch enrolled students (60 students)
  ↓
Backend: Fetch face embeddings for these 60 students
  ↓
Client: Download embeddings (one-time per session)
  ↓
Client: Start camera
  ↓
FOR EACH FRAME:
  │
  ├─ Detect faces
  ├─ Track faces (assign IDs)
  ├─ Generate embeddings
  ├─ Match against enrolled students (local)
  ├─ Calculate confidence
  ├─ Add to detection list
  └─ Update UI (detected count)
  ↓
Teacher: Finish scanning
  ↓
Client: Deduplicate across all frames
  ↓
Client: Categorize by confidence
  ↓
UI: Show review screen
  ↓
Teacher: Review and confirm
  ↓
Client: Send final list to backend
  ↓
Backend: Validate students are enrolled
  ↓
Backend: Create attendance records
  ↓
Backend: Return confirmation
```

### 6.4 Face Recognition Models (Options)

**Detection Models:**
- MTCNN (Multi-task Cascaded Convolutional Networks)
- BlazeFace (Google)
- MediaPipe Face Detection
- YuNet

**Recognition Models:**
- FaceNet (Google)
- ArcFace
- CosFace
- MobileFaceNet (mobile-optimized)

**Decision**: Model selection requires benchmarking phase (Phase 5)

### 6.5 Confidence Scoring

```kotlin
data class RecognitionResult(
    val studentId: String,
    val confidence: Double,  // 0.0 to 1.0
    val category: ConfidenceCategory
)

enum class ConfidenceCategory {
    HIGH,           // confidence >= 0.85 (configurable)
    MEDIUM,         // 0.70 <= confidence < 0.85
    LOW,            // 0.60 <= confidence < 0.70
    VERY_LOW,       // confidence < 0.60
    UNKNOWN         // No match found
}

// Business rules (configurable)
val AUTO_ACCEPT_THRESHOLD = 0.85
val NEEDS_REVIEW_THRESHOLD = 0.70
val REJECT_THRESHOLD = 0.60
```

---

## 7. Security Architecture

### 7.1 Authentication Flow

```
User enters credentials
  ↓
POST /api/v1/auth/login
  ↓
Backend validates credentials
  ↓
Generate JWT token (access + refresh)
  ↓
Return tokens + user profile
  ↓
Client stores tokens securely
  ↓
Client includes token in Authorization header
  ↓
Backend validates token on each request
  ↓
Backend extracts user role
  ↓
Backend enforces RBAC
```

### 7.2 Authorization Matrix

| Resource | Student | Teacher | Admin |
|----------|---------|---------|-------|
| Own profile | R/W | R/W | R/W |
| Other profiles | R (limited) | R (assigned students) | R/W |
| Attendance records (own) | R | - | R |
| Take attendance | - | W (assigned classes) | - |
| Attendance records (class) | - | R (assigned classes) | R |
| Leave requests (own) | R/W | - | R/W |
| Leave approval | - | R/W (assigned) | R/W |
| Attendance queries (own) | R/W | - | R/W |
| Query approval | - | R/W (assigned) | R/W |
| Academic structure | R | R | R/W |
| Timetable | R | R (own) | R/W |
| Analytics (own) | R | - | - |
| Analytics (class) | - | R (assigned) | R |
| Analytics (institution) | - | - | R |
| Face registration (own) | R/W | - | R/W |
| User management | - | - | R/W |

### 7.3 API Security

```java
@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {
    
    @PostMapping("/lecture/{lectureId}/start")
    @PreAuthorize("hasRole('TEACHER')")
    @RequireTeacherAssignment  // Custom annotation
    public ResponseEntity<LectureSession> startLectureSession(
        @PathVariable Long lectureId,
        @AuthenticationPrincipal UserDetails user
    ) {
        // Validate teacher is assigned to this lecture
        // Create lecture session
        // Return enrolled students + face embeddings
    }
    
    @PostMapping("/lecture/{lectureId}/confirm")
    @PreAuthorize("hasRole('TEACHER')")
    @RequireSessionOwnership
    public ResponseEntity<AttendanceConfirmation> confirmAttendance(
        @PathVariable Long lectureId,
        @RequestBody @Valid AttendanceSubmission submission,
        @AuthenticationPrincipal UserDetails user
    ) {
        // Validate all students are enrolled
        // Prevent duplicates
        // Create attendance records
        // Audit log
    }
}
```

### 7.4 Data Protection

**At Rest:**
- Database encryption (PostgreSQL TDE)
- Face image encryption in object storage
- Secure key management (AWS KMS / Vault)

**In Transit:**
- HTTPS/TLS 1.3
- Certificate pinning (client)
- Encrypted API payloads for sensitive data

**Biometric Data:**
- Face images: encrypted, access-controlled
- Face embeddings: never exposed to clients
- Audit log for all face data access
- Deletion workflow (GDPR compliance)

---

## 8. API Architecture

### 8.1 API Design Principles

1. **RESTful**: Resource-oriented URLs
2. **Versioned**: `/api/v1/`, `/api/v2/`
3. **Consistent**: Uniform error responses, pagination, filtering
4. **Documented**: OpenAPI 3.0 specification
5. **Stateless**: No server-side session state

### 8.2 API Structure

```
/api/v1
├── /auth
│   ├── POST /login
│   ├── POST /logout
│   ├── POST /refresh
│   └── POST /change-password
│
├── /students
│   ├── GET /{id}
│   ├── GET /{id}/attendance
│   ├── GET /{id}/attendance/subject/{subjectId}
│   ├── GET /{id}/attendance/trends
│   └── GET /{id}/profile
│
├── /teachers
│   ├── GET /{id}
│   ├── GET /{id}/schedule
│   └── GET /{id}/classes
│
├── /classes
│   ├── GET /{id}
│   ├── GET /{id}/students
│   ├── GET /{id}/subjects
│   └── GET /{id}/attendance
│
├── /subjects
│   ├── GET /{id}
│   └── GET /{id}/attendance
│
├── /timetable
│   ├── GET /teacher/{teacherId}/today
│   ├── GET /class/{classId}
│   └── GET /student/{studentId}
│
├── /lectures
│   ├── POST /start                    # Start lecture session
│   ├── GET /{id}
│   ├── GET /{id}/enrolled-students    # With face embeddings
│   └── POST /{id}/confirm             # Confirm attendance
│
├── /attendance
│   ├── POST /manual                   # Manual attendance
│   ├── GET /student/{id}
│   ├── GET /lecture/{id}
│   ├── GET /class/{id}
│   └── GET /subject/{id}
│
├── /leaves
│   ├── POST /                         # Apply for leave
│   ├── GET /{id}
│   ├── GET /student/{id}
│   ├── PATCH /{id}/approve
│   └── PATCH /{id}/reject
│
├── /queries
│   ├── POST /                         # Raise query
│   ├── GET /{id}
│   ├── GET /student/{id}
│   ├── PATCH /{id}/approve
│   └── PATCH /{id}/reject
│
├── /leaderboard
│   ├── GET /monthly
│   ├── GET /semester
│   └── GET /yearly
│
├── /notifications
│   ├── GET /
│   ├── GET /{id}
│   └── PATCH /{id}/read
│
├── /face
│   ├── POST /register                 # Face registration
│   ├── GET /profile/{studentId}
│   └── DELETE /profile/{studentId}
│
└── /admin
    ├── /students
    ├── /teachers
    ├── /departments
    ├── /programs
    ├── /batches
    ├── /classes
    ├── /subjects
    ├── /enrollments
    ├── /assignments
    ├── /timetable
    └── /analytics
```

### 8.3 Standard Response Formats

**Success Response:**
```json
{
  "success": true,
  "data": { /* resource data */ },
  "message": "Operation completed successfully"
}
```

**Error Response:**
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request data",
    "details": [
      {
        "field": "studentId",
        "message": "Student not enrolled in class"
      }
    ]
  },
  "timestamp": "2026-08-25T10:30:00Z",
  "path": "/api/v1/attendance/confirm"
}
```

**Pagination Response:**
```json
{
  "success": true,
  "data": [ /* items */ ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 156,
    "totalPages": 8
  }
}
```

---

## 9. Deployment Architecture

### 9.1 Production Environment

```
┌────────────────────────────────────────────────┐
│                 Load Balancer                  │
│                  (HTTPS/TLS)                   │
└──────────────┬─────────────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
   ┌───▼────┐      ┌───▼────┐
   │  App   │      │  App   │
   │Server 1│      │Server 2│
   │(Spring)│      │(Spring)│
   └───┬────┘      └───┬────┘
       │                │
       └───────┬────────┘
               │
   ┌───────────┼───────────┐
   │           │           │
┌──▼──┐    ┌──▼──┐    ┌──▼──────┐
│ PG  │    │Redis│    │ Object  │
│ DB  │    │Cache│    │ Storage │
└─────┘    └─────┘    └─────────┘
```

### 9.2 Scalability Strategy

**Horizontal Scaling:**
- Stateless application servers (scale via load balancer)
- Database read replicas for read-heavy queries
- Redis cluster for distributed caching

**Vertical Scaling:**
- Database instance sizing based on data volume
- ML processing offloaded to client devices

**Caching Strategy:**
- Session data: Redis (1 hour TTL)
- Timetable data: Redis (daily refresh)
- Enrolled students: Redis per lecture session
- Face embeddings: Client-side (session-scoped)

---

## 10. Monitoring & Observability

### 10.1 Application Monitoring

- **Metrics**: Prometheus + Grafana
  - API response times
  - Error rates
  - Attendance session durations
  - Face recognition success rates
  - Database query performance

- **Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
  - Application logs
  - Audit logs
  - Error logs
  - Security events

- **Tracing**: Distributed tracing (Jaeger/Zipkin)
  - Request flow across services
  - Performance bottlenecks

### 10.2 Key Metrics

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| API latency (p95) | < 200ms | > 500ms |
| API error rate | < 1% | > 5% |
| Database query time | < 50ms | > 200ms |
| Face detection time | < 200ms | > 500ms |
| Attendance session success rate | > 95% | < 90% |
| System uptime | > 99.5% | < 99% |

---

## 11. Disaster Recovery

### 11.1 Backup Strategy

- **Database**: Daily full backup + continuous WAL archiving
- **Object Storage**: Cross-region replication
- **Configuration**: Version-controlled in Git
- **Secrets**: Backed up in secure vault

### 11.2 Recovery Procedures

- **Database Recovery**: Point-in-time restore from backups
- **Data Corruption**: Restore from last known good backup
- **Face Data Loss**: Re-enrollment workflow
- **Server Failure**: Load balancer redirects to healthy instances

---

## 12. Development Workflow

### 12.1 Branching Strategy

```
main
 ├── develop
 │    ├── feature/attendance-camera
 │    ├── feature/leave-management
 │    └── feature/face-recognition
 ├── release/v1.0
 └── hotfix/critical-bug
```

### 12.2 CI/CD Pipeline

```
Commit → PR → Tests → Code Review → Merge → Build → Deploy
```

**Automated Tests:**
- Unit tests
- Integration tests
- API tests
- Security scans
- Code quality checks

**Deployment Stages:**
1. Development
2. Staging
3. Production

---

## 13. Technology Stack Summary

### 13.1 Client (Android)

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| Multiplatform | Kotlin Multiplatform (KMP) |
| UI Framework | Jetpack Compose |
| Camera | CameraX |
| ML Runtime | TensorFlow Lite / ONNX Runtime |
| Dependency Injection | Hilt |
| Navigation | Compose Navigation |
| Networking | Ktor Client / Retrofit |
| Image Processing | OpenCV (Android) |
| State Management | ViewModel + StateFlow |

### 13.2 Backend

| Component | Technology |
|-----------|-----------|
| Language | Java 17+ |
| Framework | Spring Boot 3.x |
| API | Spring Web (REST) |
| Security | Spring Security + JWT |
| Database | PostgreSQL 14+ |
| Database Migration | Flyway |
| Cache | Redis |
| Object Storage | AWS S3 / MinIO |
| Vector Search | pgvector |
| Documentation | OpenAPI 3.0 (Springdoc) |
| Testing | JUnit 5, Mockito, Testcontainers |

### 13.3 DevOps

| Component | Technology |
|-----------|-----------|
| Version Control | Git |
| CI/CD | GitHub Actions / GitLab CI |
| Containerization | Docker |
| Orchestration | Kubernetes (optional) |
| Monitoring | Prometheus + Grafana |
| Logging | ELK Stack |
| Tracing | Jaeger |

---

## 14. Key Architectural Decisions

### 14.1 Why Kotlin Multiplatform?

**Decision**: Use KMP from day one, not pure Android

**Rationale**:
- Avoid costly rewrite when adding iOS
- Share business logic (60-70% code reuse)
- Consistent behavior across platforms
- Type-safe API contracts

**Trade-offs**:
- Slightly higher initial complexity
- Learning curve for team
- Platform abstractions required

### 14.2 Why Modular Monolith?

**Decision**: Modular monolith, not microservices

**Rationale**:
- Simpler deployment
- Lower operational overhead
- Easier development/debugging
- Sufficient for expected scale (50K students)
- Can extract modules later if needed

**Trade-offs**:
- Single deployment unit
- Shared database
- Less independent scaling

### 14.3 Why On-Device Face Recognition?

**Decision**: Process face recognition on client device, not server

**Rationale**:
- Lower backend load
- Works offline (temporary network issues)
- Faster processing (no network latency)
- Privacy (embeddings stay on device during session)

**Trade-offs**:
- Client device requirements (CPU, memory)
- Battery consumption
- Model distribution/updates

### 14.4 Why PostgreSQL + pgvector?

**Decision**: PostgreSQL with pgvector extension

**Rationale**:
- Single database for relational + vector data
- ACID guarantees
- Mature, battle-tested
- Good vector search performance
- Lower operational complexity vs. separate vector DB

**Trade-offs**:
- Vector search slower than specialized DBs (Pinecone, Milvus)
- Sufficient for initial scale (hundreds of searches, not millions)

---

## 15. Risk Mitigation

### 15.1 Technical Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Face recognition accuracy insufficient | High | Manual fallback, model benchmarking, user feedback |
| Device performance issues | Medium | Frame sampling, model optimization, manual fallback |
| Network unavailability | Medium | Offline capability, sync queue, clear UX |
| Database scalability | Low | Read replicas, caching, indexed queries |
| KMP complexity | Medium | Phased adoption, team training, clear abstractions |

### 15.2 Operational Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Data loss | Critical | Automated backups, replication, DR plan |
| Security breach | Critical | Security audits, penetration testing, monitoring |
| System downtime | High | Load balancing, redundancy, monitoring |
| Face data privacy violation | Critical | Encryption, access control, audit logs, GDPR compliance |

---

## 16. Future Considerations

### 16.1 Potential Enhancements

- **Multi-camera support**: Multiple teachers scanning same class
- **Edge ML acceleration**: GPU/NPU utilization
- **Advanced analytics**: ML-based attendance prediction
- **Integration**: LMS, ERP systems
- **Biometric alternatives**: Fingerprint, QR codes
- **Real-time collaboration**: Multiple devices in same session

### 16.2 Scalability Beyond Phase 1

- **Microservices extraction**: Face recognition service, notification service
- **Event-driven architecture**: Kafka/RabbitMQ for async processing
- **Data lake**: Long-term analytics and reporting
- **Multi-tenancy**: Support multiple institutions

---

## 17. Non-Goals

The following are explicitly **out of scope** for this architecture:

- Multi-tenancy (single institution only)
- Exam/assessment management
- Learning management (LMS features)
- Fee management
- Library management
- Hostel management
- Transport management
- Real-time video streaming
- AR/VR features
- Blockchain-based records

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | System Architect | Initial draft |

---

**Status**: DRAFT - Awaiting technical review and validation
