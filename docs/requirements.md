# Requirements Document
## Smart Classroom Attendance Management System

**Version:** 1.0  
**Date:** August 25, 2026  
**Status:** Draft - Awaiting Architectural Review

---

## 1. Executive Summary

This document defines the requirements for a production-grade **Smart Classroom Attendance Management System** designed for colleges and universities. The system uses Android-first Kotlin Multiplatform (KMP) architecture with intelligent face recognition for automated classroom attendance while maintaining manual fallback capabilities.

### Key Differentiators

- **Multi-student, multi-frame camera scanning** (not single-photo attendance)
- **Teacher review and validation** (AI-assisted, not AI-automated)
- **KMP architecture from day one** (Android production, iOS-ready)
- **Modular backend** (not microservices)
- **Production-grade security and audit trail**

---

## 2. Stakeholders

### 2.1 Students (Primary Users)
- View attendance across multiple dimensions
- Apply for leave and track status
- Raise attendance queries
- Monitor attendance health
- Register biometric face data

### 2.2 Teachers (Primary Users)
- Take attendance via camera or manual entry
- Review and validate AI-detected students
- Approve/reject leave requests
- Approve/reject attendance queries
- Monitor class attendance analytics

### 2.3 Administrators (System Managers)
- Manage academic structure
- Manage users and assignments
- Configure timetables
- View institution-wide analytics
- Configure system parameters

---

## 3. Functional Requirements

### 3.1 Authentication & Authorization

#### FR-AUTH-001: User Authentication
- **Priority:** Critical
- **Description:** All users must authenticate using secure credentials
- **Acceptance Criteria:**
  - Secure password hashing (bcrypt/argon2)
  - Session/token-based authentication
  - Role-based access control (STUDENT, TEACHER, ADMIN)
  - Account lockout after failed attempts
  - Password reset workflow

#### FR-AUTH-002: Role-Based Access Control
- **Priority:** Critical
- **Description:** Enforce authorization at backend level
- **Acceptance Criteria:**
  - Students access only their own data
  - Teachers access only assigned classes/subjects
  - Admins access all administrative functions
  - API endpoints validate permissions
  - UI elements reflect user role

---

### 3.2 Camera-Based Attendance (Core Feature)

#### FR-CAM-001: Multi-Frame Face Detection
- **Priority:** Critical
- **Description:** Teacher can scan classroom using phone camera across multiple frames
- **Acceptance Criteria:**
  - CameraX integration on Android
  - Real-time face detection overlay
  - Multiple faces detected per frame
  - Continuous scanning as teacher rotates phone
  - Visual feedback (bounding boxes) on detected faces
  - Performance: Process frame within 300ms (target)

#### FR-CAM-002: Face Recognition & Matching
- **Priority:** Critical
- **Description:** Recognize detected faces against enrolled students in the lecture's class
- **Acceptance Criteria:**
  - Match faces only against enrolled students in the specific class
  - Generate confidence scores for each match
  - Support for multiple recognition models (configurable)
  - Handle unknown faces gracefully
  - No direct database writes from recognition engine

#### FR-CAM-003: Cross-Frame Deduplication
- **Priority:** Critical
- **Description:** Prevent same student being counted multiple times across frames
- **Acceptance Criteria:**
  - Track student identities across frames
  - Merge duplicate detections
  - Use highest confidence score when duplicates exist
  - Handle student moving between frames
  - Display final unique student count

#### FR-CAM-004: Confidence-Based Categorization
- **Priority:** Critical
- **Description:** Categorize recognition results by confidence level
- **Acceptance Criteria:**
  - HIGH CONFIDENCE: Auto-add to present candidates
  - LOW CONFIDENCE: Flag for teacher review
  - UNKNOWN: Show as unrecognized face
  - Configurable confidence thresholds
  - Visual distinction in review UI

#### FR-CAM-005: Live Scanner UI
- **Priority:** High
- **Description:** Real-time camera interface showing detection status
- **Acceptance Criteria:**
  - Display lecture information (subject, class)
  - Show live detection count
  - Display "LIVE" indicator
  - Show detected/total enrolled ratio (e.g., "43/60")
  - Green bounding boxes for recognized faces
  - Blue/red boxes for unknown/low-confidence faces
  - Manual entry button always accessible
  - Settings/options accessible
  - Capture frame button for manual review

---

### 3.3 Attendance Review & Confirmation

#### FR-REV-001: Attendance Review Screen
- **Priority:** Critical
- **Description:** Teacher reviews and validates AI-detected attendance before confirmation
- **Acceptance Criteria:**
  - Display summary: Present, Absent, Unknown counts
  - Show lecture details (subject, class, date, time)
  - Separate sections: "Needs Review" and "Verified List"
  - Display student photo, name, roll number, confidence %
  - Sort by confidence score
  - Quick accept/reject controls per student
  - Search functionality
  - Manual add student capability

#### FR-REV-002: Manual Attendance Correction
- **Priority:** Critical
- **Description:** Teacher can manually override AI suggestions
- **Acceptance Criteria:**
  - Add students not detected
  - Remove incorrectly detected students
  - Mark students as late (if supported)
  - Search students by name/roll number
  - Cannot mark students not enrolled in class

#### FR-REV-003: Attendance Confirmation
- **Priority:** Critical
- **Description:** Finalize attendance after teacher review
- **Acceptance Criteria:**
  - Single "Confirm Attendance" action
  - Prevent duplicate confirmation
  - Backend validation of all students
  - Atomic transaction (all or nothing)
  - Generate audit log entry
  - Show success/failure feedback
  - Handle network failures gracefully

---

### 3.4 Manual Attendance (Fallback)

#### FR-MAN-001: Full Manual Attendance Entry
- **Priority:** Critical
- **Description:** Teacher can mark attendance completely manually without camera
- **Acceptance Criteria:**
  - List all enrolled students
  - Quick mark all present/absent
  - Individual student status toggle
  - Search/filter students
  - Same confirmation workflow as camera attendance
  - No dependency on face recognition

#### FR-MAN-002: Hybrid Attendance
- **Priority:** High
- **Description:** Support mixing camera and manual attendance in same session
- **Acceptance Criteria:**
  - Start with camera, switch to manual
  - Manually add students missed by camera
  - Preserve camera-detected students
  - Single final confirmation

---

### 3.5 Lecture Sessions

#### FR-LEC-001: Lecture Session Management
- **Priority:** Critical
- **Description:** Attendance tied to formal lecture sessions
- **Acceptance Criteria:**
  - Create session from timetable
  - Session includes: teacher, subject, class, date, time, academic year, semester
  - Session states: SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
  - Only assigned teacher can start session for their lecture
  - Prevent duplicate sessions for same lecture slot

#### FR-LEC-002: Timetable Integration
- **Priority:** High
- **Description:** Teacher dashboard shows today's lectures from timetable
- **Acceptance Criteria:**
  - Display teacher's schedule for current day
  - Show subject, class, time, room
  - One-click "Take Attendance" from timetable
  - Highlight next class
  - Show past/future classes differently

---

### 3.6 Student Attendance Views

#### FR-STU-001: Attendance Dashboard
- **Priority:** High
- **Description:** Student views overall attendance summary
- **Acceptance Criteria:**
  - Overall attendance percentage
  - Trend indicator (improving/declining)
  - Classes attended vs missed count
  - Monthly attendance percentage
  - Semester attendance percentage
  - Academic year attendance percentage

#### FR-STU-002: Subject-Wise Attendance
- **Priority:** High
- **Description:** Student views attendance breakdown by subject
- **Acceptance Criteria:**
  - List all enrolled subjects
  - Show: subject name, teacher, total lectures, present, absent, leave, percentage
  - Color-coded status: SAFE (green), WARNING (amber), CRITICAL (red)
  - Configurable thresholds
  - Tap subject for lecture-wise detail

#### FR-STU-003: Lecture-Wise Attendance
- **Priority:** High
- **Description:** Student views individual lecture attendance records
- **Acceptance Criteria:**
  - List all lectures for a subject
  - Show: date, time, status, attendance method (camera/manual)
  - Filter by date range
  - Sort by date (newest first)
  - Visual status indicators

#### FR-STU-004: Attendance Trends
- **Priority:** Medium
- **Description:** Student views attendance trends over time
- **Acceptance Criteria:**
  - Monthly attendance chart
  - Subject-wise comparison
  - Identify improving/declining trends

---

### 3.7 Leave Management

#### FR-LEAVE-001: Apply for Leave
- **Priority:** High
- **Description:** Student applies for leave for specific dates
- **Acceptance Criteria:**
  - Select single date or date range
  - Select affected subjects (if applicable)
  - Enter reason (mandatory)
  - Optional attachment upload
  - Submit for approval
  - Cannot apply for past dates beyond threshold (configurable)

#### FR-LEAVE-002: Leave Approval Workflow
- **Priority:** High
- **Description:** Teacher/Admin approves or rejects leave requests
- **Acceptance Criteria:**
  - View pending leave requests
  - Filter by student, date, subject
  - View reason and attachments
  - Approve or reject with optional comment
  - Notification sent to student
  - Approved leave updates attendance status

#### FR-LEAVE-003: Leave Status Tracking
- **Priority:** High
- **Description:** Student tracks leave request status
- **Acceptance Criteria:**
  - View all leave requests
  - Filter by status: PENDING, APPROVED, REJECTED
  - See approval/rejection date and comments
  - Optional cancellation (if pending)

#### FR-LEAVE-004: Leave Integration with Attendance
- **Priority:** Critical
- **Description:** Approved leave reflected in attendance records
- **Acceptance Criteria:**
  - Status: ON_LEAVE (distinct from PRESENT/ABSENT)
  - ON_LEAVE not counted as absent in calculations
  - ON_LEAVE lectures shown separately in statistics
  - Cannot mark attendance for ON_LEAVE lectures

---

### 3.8 Attendance Queries/Disputes

#### FR-QUERY-001: Raise Attendance Query
- **Priority:** High
- **Description:** Student disputes incorrect attendance record
- **Acceptance Criteria:**
  - Select specific lecture
  - Current status shown
  - Enter reason for dispute
  - Optional evidence/attachment
  - Cannot raise duplicate query for same lecture
  - Cannot query lectures beyond threshold (configurable)

#### FR-QUERY-002: Query Approval Workflow
- **Priority:** High
- **Description:** Teacher reviews and approves/rejects queries
- **Acceptance Criteria:**
  - View pending queries
  - See original status and requested change
  - View student's reason
  - Approve or reject with comment
  - Approval creates attendance correction

#### FR-QUERY-003: Attendance Correction Audit Trail
- **Priority:** Critical
- **Description:** Track all attendance corrections with full audit history
- **Acceptance Criteria:**
  - Record original value
  - Record new value
  - Record who changed (teacher)
  - Record when changed
  - Record reason/related query
  - Immutable audit log
  - Display correction history to student

---

### 3.9 Leaderboard

#### FR-LEAD-001: Attendance Leaderboard
- **Priority:** Medium
- **Description:** Display attendance rankings
- **Acceptance Criteria:**
  - Monthly leaderboard
  - Semester leaderboard
  - Academic year leaderboard
  - Categories: Highest Attendance, Most Consistent, Most Improved
  - Filter by class, department, program
  - Display student name, attendance %, rank
  - Privacy: Only show top performers (not bottom)

---

### 3.10 Notifications

#### FR-NOTIF-001: Real-Time Notifications
- **Priority:** Medium
- **Description:** Notify users of important events
- **Acceptance Criteria:**
  - Leave approved/rejected
  - Attendance query approved/rejected
  - Attendance marked
  - Low attendance warning
  - Lecture scheduled/cancelled
  - Push notifications on Android
  - In-app notification center

---

### 3.11 Admin - Academic Structure

#### FR-ADMIN-001: Student Management
- **Priority:** High
- **Description:** Admins manage student records
- **Acceptance Criteria:**
  - CRUD operations on students
  - Import students via CSV/Excel
  - Assign to class/section
  - Enroll in subjects
  - Change section/class
  - Deactivate students

#### FR-ADMIN-002: Teacher Management
- **Priority:** High
- **Description:** Admins manage teacher records
- **Acceptance Criteria:**
  - CRUD operations on teachers
  - Assign to department
  - Assign subjects to teachers
  - Assign classes to teachers
  - View teacher workload

#### FR-ADMIN-003: Academic Structure Management
- **Priority:** High
- **Description:** Admins manage departments, programs, batches, classes, subjects
- **Acceptance Criteria:**
  - CRUD operations on all entities
  - Define hierarchy: Department → Program → Batch → Class → Section
  - Create subjects with code, name, credits
  - Define academic years and semesters
  - Activate/deactivate entities

#### FR-ADMIN-004: Timetable Management
- **Priority:** High
- **Description:** Admins create and manage timetables
- **Acceptance Criteria:**
  - Create timetable for class
  - Assign subject, teacher, time slot, room
  - Detect conflicts (teacher/room double-booking)
  - Clone timetable from previous semester
  - Export timetable

#### FR-ADMIN-005: Institution Analytics
- **Priority:** Medium
- **Description:** Admins view institution-wide analytics
- **Acceptance Criteria:**
  - Overall attendance trends
  - Department-wise attendance
  - Class-wise attendance
  - Subject-wise attendance
  - Low-attendance students report
  - Teacher performance metrics

---

### 3.12 Face Registration

#### FR-FACE-001: Student Face Enrollment
- **Priority:** Critical
- **Description:** Students register face data for recognition
- **Acceptance Criteria:**
  - Capture multiple face images (3-5)
  - Real-time quality validation (lighting, angle, clarity)
  - Generate face embeddings
  - Store securely (images in object storage, embeddings in DB)
  - Re-enrollment capability
  - Admin can trigger re-enrollment

#### FR-FACE-002: Face Data Security
- **Priority:** Critical
- **Description:** Protect biometric data
- **Acceptance Criteria:**
  - Encrypted storage
  - Access control (student + admin only)
  - Audit log for access
  - Deletion workflow (GDPR compliance)
  - No exposure of embeddings to clients

---

## 4. Non-Functional Requirements

### 4.1 Performance

#### NFR-PERF-001: Camera Performance
- **Target:** Process frame within 300ms
- **Max Acceptable:** 500ms per frame
- **Rationale:** Maintain smooth scanning experience

#### NFR-PERF-002: API Response Time
- **Target:** 95% of API calls < 200ms
- **Max Acceptable:** 500ms for complex queries
- **Rationale:** Responsive user experience

#### NFR-PERF-003: Face Recognition Search Space
- **Requirement:** Limit recognition search to enrolled students in lecture's class
- **Rationale:** Improve accuracy and performance

### 4.2 Scalability

#### NFR-SCALE-001: Concurrent Users
- **Target:** Support 500 concurrent teachers taking attendance
- **Rationale:** Handle multiple lecture slots simultaneously

#### NFR-SCALE-002: Data Volume
- **Target:** Handle 50,000 students, 2,000 teachers, 100,000 lectures/semester
- **Rationale:** Large university scale

### 4.3 Security

#### NFR-SEC-001: Authentication
- **Requirement:** Secure password hashing (bcrypt/argon2)
- **Requirement:** Token-based authentication with expiry
- **Requirement:** HTTPS only

#### NFR-SEC-002: Authorization
- **Requirement:** Role-based access control enforced at API level
- **Requirement:** Prevent privilege escalation

#### NFR-SEC-003: Data Protection
- **Requirement:** Encrypt biometric data at rest
- **Requirement:** Encrypt sensitive data in transit
- **Requirement:** No face embeddings exposed to clients

#### NFR-SEC-004: Audit Logging
- **Requirement:** Log all authentication attempts
- **Requirement:** Log all attendance corrections
- **Requirement:** Log all admin actions
- **Requirement:** Immutable audit trail

### 4.4 Availability

#### NFR-AVAIL-001: System Uptime
- **Target:** 99.5% uptime during academic hours
- **Rationale:** Critical for daily attendance operations

#### NFR-AVAIL-002: Offline Capability
- **Requirement:** Camera detection works without network
- **Requirement:** Attendance queued for sync when network restored
- **Rationale:** Handle temporary network issues

### 4.5 Usability

#### NFR-USAB-001: Camera Workflow
- **Requirement:** Minimal teacher interaction
- **Requirement:** Clear visual feedback
- **Requirement:** Fallback to manual always available

#### NFR-USAB-002: Response Time Perception
- **Requirement:** Loading states for operations > 100ms
- **Requirement:** Progress indicators for long operations
- **Requirement:** Optimistic UI updates where safe

### 4.6 Maintainability

#### NFR-MAINT-001: Code Architecture
- **Requirement:** KMP-compatible from day one
- **Requirement:** Clear separation of concerns
- **Requirement:** Platform-specific code isolated
- **Requirement:** Comprehensive unit tests

#### NFR-MAINT-002: Documentation
- **Requirement:** API documentation (OpenAPI/Swagger)
- **Requirement:** Architecture documentation
- **Requirement:** Deployment documentation
- **Requirement:** Database schema documentation

### 4.7 Portability

#### NFR-PORT-001: Kotlin Multiplatform
- **Requirement:** Shared business logic in KMP
- **Requirement:** Platform abstractions for camera, ML
- **Requirement:** Android implementation first
- **Requirement:** iOS-compatible architecture

---

## 5. Data Requirements

### 5.1 Data Retention
- Attendance records: 7 years (regulatory compliance)
- Audit logs: 3 years
- Face images: Duration of enrollment + 1 year
- Leave records: 3 years
- Query records: 3 years

### 5.2 Data Privacy
- GDPR/data protection compliance
- Student consent for biometric data
- Right to deletion
- Data access logs

### 5.3 Backup & Recovery
- Daily automated backups
- Point-in-time recovery capability
- Backup retention: 30 days
- DR recovery time objective (RTO): 4 hours
- DR recovery point objective (RPO): 1 hour

---

## 6. Integration Requirements

### 6.1 External Systems
- **Email Service**: Leave/query notifications
- **SMS Gateway** (Optional): Critical notifications
- **Object Storage**: Face images, attachments
- **Analytics Platform** (Future): Advanced reporting

### 6.2 APIs
- RESTful JSON APIs
- OpenAPI 3.0 specification
- Versioned APIs (v1, v2, etc.)
- Consistent error responses

---

## 7. Constraints

### 7.1 Technical Constraints
- Android minimum SDK: API 24 (Android 7.0) - Target
- Backend: Java 17+, Spring Boot 3.x
- Database: PostgreSQL 14+
- Object Storage: S3-compatible

### 7.2 Regulatory Constraints
- Educational data privacy laws
- Biometric data regulations
- Attendance tracking compliance

### 7.3 Business Constraints
- Phase-based delivery (not big bang)
- Android before iOS
- Manual attendance must always work

---

## 8. Assumptions

1. Students have registered face data before attendance scanning
2. Classroom lighting is reasonable (not pitch dark)
3. Teachers have Android devices with working cameras
4. Internet connectivity available for final sync (temporary offline OK)
5. Students enrolled in classes before attendance can be taken
6. Timetable created before attendance operations

---

## 9. Dependencies

1. Face detection model selection and benchmarking
2. Face recognition model selection and benchmarking
3. Object storage provider selection
4. Android ML runtime selection
5. Academic calendar and policies definition
6. Attendance calculation rules definition
7. Low attendance threshold configuration

---

## 10. Out of Scope (Phase 1)

- iOS application
- Student mobile app (Android-only for now if needed)
- Advanced analytics/ML for attendance prediction
- Integration with learning management systems (LMS)
- Biometric alternatives (fingerprint, iris)
- Parent portal
- Automated report generation
- WhatsApp/Telegram notifications
- Multi-language support

---

## 11. Success Criteria

### Technical Success
- ✓ All critical functional requirements implemented
- ✓ Performance targets met
- ✓ Security requirements satisfied
- ✓ 80%+ unit test coverage
- ✓ Zero critical security vulnerabilities

### Business Success
- ✓ Teachers can take attendance in < 3 minutes for 60-student class
- ✓ Face recognition accuracy > 90% in controlled environment
- ✓ System handles peak load (500 concurrent teachers)
- ✓ < 5% attendance corrections required
- ✓ Manual fallback used < 10% of time

### User Satisfaction
- ✓ Positive feedback from pilot testing
- ✓ Teacher adoption rate > 90%
- ✓ Student query rate < 5% of total attendance records

---

## 12. Glossary

- **Lecture Session**: A single class period with defined subject, teacher, class, and time
- **Attendance Candidate**: A recognized student pending teacher confirmation
- **Face Embedding**: Mathematical representation of face for recognition
- **Confidence Score**: Percentage indicating recognition certainty
- **Cross-Frame Deduplication**: Removing duplicate identities across multiple camera frames
- **ON_LEAVE**: Attendance status for approved leave (distinct from absent)
- **Attendance Query**: Student-initiated dispute of attendance record
- **Attendance Correction**: Formal change to finalized attendance with audit trail

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | System Architect | Initial draft |

---

**Status**: DRAFT - Awaiting stakeholder review and architectural validation
