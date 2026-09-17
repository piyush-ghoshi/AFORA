# Development Roadmap
## Smart Classroom Attendance Management System

**Version:** 1.0  
**Date:** August 25, 2026  
**Status:** Draft - Awaiting Approval

---

## Overview

This roadmap outlines the **phase-by-phase development** strategy for the Smart Classroom Attendance Management System. The approach emphasizes:

- **Incremental delivery** (not big bang)
- **Working software at each phase**
- **Explicit acceptance criteria**
- **Risk mitigation through early validation**

---

## Phase Overview

| Phase | Name | Duration (Est.) | Priority | Status |
|-------|------|-----------------|----------|--------|
| 0 | Specification & Architecture | 1 week | Critical | In Progress |
| 1 | Repository & Foundation | 2 weeks | Critical | Planned |
| 2 | Backend Foundation | 2 weeks | Critical | Planned |
| 3 | Academic Management | 3 weeks | Critical | Planned |
| 4 | Attendance Engine (Manual) | 3 weeks | Critical | Planned |
| 5 | CV Technical Spike & Benchmarking | 2-3 weeks | Critical | Planned |
| 6 | CV Integration (Camera Attendance) | 3-4 weeks | Critical | Planned |
| 7 | Leave & Query Management | 2 weeks | High | Planned |
| 8 | Analytics & Reports | 2 weeks | Medium | Planned |
| 9 | Leaderboard & Notifications | 2 weeks | Medium | Planned |
| 10 | Security Hardening & Production Prep | 2 weeks | Critical | Planned |
| 11 | Pilot Deployment & Iteration | 3-4 weeks | Critical | Planned |

**Total Estimated Duration**: 24-28 weeks (~6-7 months)

---

## Phase 0: Specification & Architecture ✅

### Status: IN PROGRESS

### Objectives
- Finalize requirements
- Design system architecture
- Resolve open decisions
- Create comprehensive documentation

### Deliverables
- ✅ `requirements.md`
- ✅ `architecture.md`
- ✅ `database.md`
- ✅ `kmp-architecture.md`
- ✅ `face-recognition.md`
- ✅ `decision-log.md`
- ✅ `roadmap.md`
- ⏳ `api-contract.md`
- ⏳ `security.md`

### Acceptance Criteria
- [ ] All documentation reviewed and approved
- [ ] Critical open decisions resolved
- [ ] Team aligned on architecture
- [ ] Technology stack confirmed
- [ ] Risk assessment completed

### Blockers/Risks
- None currently

### Next Steps
1. Stakeholder review of documentation
2. Resolve open questions (see decision-log.md)
3. Obtain formal approval to proceed
4. Set up development team

---

## Phase 1: Repository & Foundation

### Duration: 2 weeks

### Objectives
- Set up project repository structure
- Establish KMP module architecture
- Configure build system
- Set up CI/CD pipeline
- Implement core abstractions

### Deliverables

**Project Structure:**
```
afora/
├── shared/                 # KMP module
│   ├── commonMain/
│   ├── androidMain/
│   └── build.gradle.kts
├── androidApp/             # Android application
│   ├── src/
│   └── build.gradle.kts
├── backend/                # Spring Boot backend
│   ├── src/
│   └── build.gradle
├── .github/workflows/      # CI/CD pipelines
├── docs/
├── gradle/
├── build.gradle.kts
└── settings.gradle.kts
```

**Implementation Tasks:**
- [x] Create multi-module Gradle project
- [ ] Configure KMP module (Android target)
- [ ] Set up Android app module
- [ ] Implement platform abstractions (expect/actual interfaces)
- [ ] Set up Hilt dependency injection (Android)
- [ ] Set up Koin dependency injection (shared)
- [ ] Configure Jetpack Compose
- [ ] Set up navigation architecture
- [ ] Create design system (Material 3 theme based on Academic Precision)
- [ ] Implement basic networking layer (Ktor/Retrofit)
- [ ] Set up Git workflows
- [ ] Configure CI/CD (build, test, lint)
- [ ] Set up development environment documentation

### Acceptance Criteria
- [ ] Project compiles without errors
- [ ] Basic navigation works (empty screens)
- [ ] DI framework configured
- [ ] Network client configured
- [ ] CI/CD pipeline runs successfully
- [ ] Code quality tools configured (ktlint, detekt)
- [ ] Documentation for local development setup

### Blockers/Risks
- Team unfamiliarity with KMP
- Tooling setup challenges

### Mitigation
- Provide KMP training/resources
- Start with simple expect/actual examples
- Document setup process thoroughly

---

## Phase 2: Backend Foundation

### Duration: 2 weeks

### Objectives
- Set up Spring Boot project structure
- Implement authentication & authorization
- Configure database (PostgreSQL)
- Establish API conventions
- Implement global error handling

### Deliverables

**Backend Modules:**
- Spring Boot application structure
- Authentication module (JWT)
- User management module
- Database configuration
- Flyway migrations
- Global exception handling
- API documentation (OpenAPI/Swagger)
- Logging & monitoring setup

**Implementation Tasks:**
- [ ] Create Spring Boot project
- [ ] Configure PostgreSQL connection
- [ ] Set up Flyway migrations
- [ ] Implement authentication endpoints (/auth/login, /logout, /refresh)
- [ ] Implement JWT token generation & validation
- [ ] Configure Spring Security
- [ ] Implement RBAC (STUDENT, TEACHER, ADMIN roles)
- [ ] Create User, Student, Teacher, Admin entities
- [ ] Create user management CRUD endpoints
- [ ] Implement global exception handling
- [ ] Configure OpenAPI/Swagger documentation
- [ ] Set up logging (SLF4J + Logback)
- [ ] Configure CORS
- [ ] Set up database connection pooling (HikariCP)
- [ ] Write integration tests for auth

### Acceptance Criteria
- [ ] Users can register and login
- [ ] JWT tokens generated and validated
- [ ] RBAC enforced at API level
- [ ] Database migrations run successfully
- [ ] API documentation accessible at /swagger-ui
- [ ] Global error responses consistent
- [ ] Integration tests pass
- [ ] Logging configured and working

### Blockers/Risks
- Database setup complexity
- Security configuration errors

### Mitigation
- Use Docker for local PostgreSQL
- Follow Spring Security best practices
- Code review for security

---

## Phase 3: Academic Management

### Duration: 3 weeks

### Objectives
- Implement academic structure (departments, programs, classes, etc.)
- Student and teacher management
- Enrollment management
- Timetable management
- Admin dashboard basics

### Deliverables

**Backend:**
- Academic structure entities & APIs
- Student management APIs
- Teacher management APIs
- Enrollment APIs
- Teacher assignment APIs
- Timetable APIs
- Validation logic

**Android:**
- Admin dashboard UI
- Student management screens
- Teacher management screens
- Enrollment screens
- Timetable management screens
- Student/Teacher profile screens (basic)

**Implementation Tasks:**

**Backend:**
- [ ] Create academic structure entities (departments, programs, batches, classes, subjects, academic_years, semesters)
- [ ] Implement CRUD endpoints for all academic entities
- [ ] Implement enrollment endpoints
- [ ] Implement teacher assignment endpoints
- [ ] Implement timetable endpoints
- [ ] Detect timetable conflicts (teacher/room double-booking)
- [ ] Implement validation logic
- [ ] Write unit & integration tests

**Android (Admin):**
- [ ] Admin dashboard screen
- [ ] Student list & detail screens
- [ ] Student add/edit screens
- [ ] Teacher list & detail screens
- [ ] Teacher add/edit screens
- [ ] Class/subject management screens
- [ ] Enrollment management screens
- [ ] Timetable management screens
- [ ] ViewModels for all screens
- [ ] Form validation
- [ ] Error handling & loading states

**Shared (KMP):**
- [ ] Domain models for all entities
- [ ] Repository interfaces
- [ ] Use cases for validation
- [ ] API client methods

### Acceptance Criteria
- [ ] Admin can create departments, programs, batches, classes, subjects
- [ ] Admin can create students and teachers
- [ ] Admin can enroll students in classes/subjects
- [ ] Admin can assign teachers to classes/subjects
- [ ] Admin can create timetables
- [ ] System detects and prevents timetable conflicts
- [ ] All data validated correctly
- [ ] CRUD operations work end-to-end
- [ ] Tests pass (unit + integration)
- [ ] No unauthorized access to admin endpoints

### Blockers/Risks
- Complex validation rules
- UI complexity for timetable management

### Mitigation
- Start with simple validation, iterate
- Use existing timetable UI libraries/patterns

---

## Phase 4: Attendance Engine (Manual Only)

### Duration: 3 weeks

### Objectives
- Implement lecture session management
- Manual attendance marking
- Attendance records storage
- Attendance viewing (student & teacher)
- Attendance calculations
- **Validate core attendance workflow WITHOUT face recognition**

### Deliverables

**Backend:**
- Lecture session APIs
- Manual attendance APIs
- Attendance record storage
- Attendance calculation logic
- Attendance query APIs

**Android (Teacher):**
- Teacher dashboard (today's schedule)
- Start lecture session screen
- Manual attendance screen (student list)
- Attendance review & confirm screen
- Attendance history screen
- Class attendance analytics screen

**Android (Student):**
- Student dashboard (attendance summary)
- Subject-wise attendance screen
- Lecture-wise attendance screen
- Attendance trends screen (basic charts)

**Implementation Tasks:**

**Backend:**
- [ ] Create lecture_sessions table & entity
- [ ] Create attendance_records table & entity
- [ ] Implement POST /lectures/{id}/start
- [ ] Implement GET /lectures/{id}/enrolled-students
- [ ] Implement POST /attendance/manual
- [ ] Implement POST /lectures/{id}/confirm
- [ ] Implement attendance calculation logic
- [ ] Implement GET /attendance/student/{id}
- [ ] Implement GET /attendance/student/{id}/subject/{subjectId}
- [ ] Implement attendance percentage calculation
- [ ] Prevent duplicate attendance
- [ ] Validate teacher authorization
- [ ] Write comprehensive tests

**Android (Teacher):**
- [ ] Teacher dashboard screen (show today's timetable)
- [ ] "Take Attendance" button integration with timetable
- [ ] Start lecture session flow
- [ ] Manual attendance screen (list of enrolled students)
- [ ] Quick mark all present/absent
- [ ] Individual student toggle
- [ ] Search students
- [ ] Review screen (show present/absent counts)
- [ ] Confirm attendance action
- [ ] Handle duplicate submission prevention
- [ ] Attendance history screen
- [ ] Class attendance analytics screen

**Android (Student):**
- [ ] Student dashboard screen
- [ ] Overall attendance percentage display
- [ ] Subject-wise attendance list
- [ ] Subject attendance detail screen
- [ ] Lecture-wise attendance list
- [ ] Attendance trends chart (basic)
- [ ] Low attendance warning UI

**Shared (KMP):**
- [ ] Lecture, AttendanceRecord models
- [ ] Attendance use cases
- [ ] Attendance calculation logic
- [ ] Validation logic

### Acceptance Criteria
- [ ] Teacher can start lecture from timetable
- [ ] Teacher can manually mark attendance for all enrolled students
- [ ] System prevents duplicate attendance submission
- [ ] Only assigned teacher can mark attendance for their lecture
- [ ] Teacher can review before confirmation
- [ ] Attendance successfully stored in database
- [ ] Student can view overall attendance percentage
- [ ] Student can view subject-wise attendance
- [ ] Student can view lecture-wise attendance details
- [ ] Attendance percentage calculated correctly
- [ ] Low attendance flagged correctly
- [ ] All edge cases handled (no students, all absent, etc.)
- [ ] Tests pass
- [ ] Manual attendance workflow is smooth and fast (<2 minutes for 60 students)

### Blockers/Risks
- Complex state management for attendance session
- Performance issues with large student lists

### Mitigation
- Use ViewModel state management properly
- Implement lazy loading/pagination if needed
- Thorough testing with realistic data

### Critical Success Factor
**This phase validates the core attendance workflow.** If manual attendance doesn't work smoothly, camera attendance won't either. Do not proceed to Phase 6 until this is solid.

---

## Phase 5: Computer Vision Technical Spike & Benchmarking

### Duration: 2-3 weeks

### Objectives
- Benchmark face detection models
- Benchmark face recognition models
- Test multi-face scenarios
- Measure performance metrics
- Select optimal model combination
- Create proof-of-concept Android app
- **This is RESEARCH, not production code**

### Deliverables
- Model benchmarking report
- Performance metrics report
- Model selection recommendation
- POC Android app demonstrating:
  - Face detection
  - Face tracking
  - Face recognition
  - Multi-face handling
  - Cross-frame deduplication

### Implementation Tasks
- [ ] Set up benchmark dataset (collect 100-200 volunteer images)
- [ ] Implement MTCNN detection
- [ ] Implement BlazeFace detection
- [ ] Implement MediaPipe Face detection
- [ ] Implement FaceNet recognition
- [ ] Implement MobileFaceNet recognition
- [ ] Implement ArcFace recognition (if feasible on mobile)
- [ ] Create test harness for automated benchmarking
- [ ] Measure detection accuracy (TPR, FPR)
- [ ] Measure recognition accuracy (TAR @ 1% FAR)
- [ ] Measure processing time per face
- [ ] Measure memory usage
- [ ] Measure battery consumption (5-minute session)
- [ ] Test in classroom-like conditions (lighting, distance, angles)
- [ ] Test multi-face scenarios (10, 20, 30+ faces)
- [ ] Test occlusion scenarios (glasses, masks)
- [ ] Create POC app demonstrating camera pipeline
- [ ] Document findings

### Acceptance Criteria
- [ ] All models benchmarked with standardized dataset
- [ ] Performance metrics documented
- [ ] Model recommendation with justification
- [ ] POC app demonstrates feasibility
- [ ] Detection rate >95% in controlled environment
- [ ] Recognition accuracy >90% in controlled environment
- [ ] Processing time <300ms per face
- [ ] Memory usage acceptable (<500MB)
- [ ] Battery consumption acceptable
- [ ] Multi-face detection works (20+ faces in frame)

### Decision Outputs
- **FR-001**: Face recognition model selection → DECIDED
- Confidence thresholds (initial values)
- Performance optimization strategies

### Blockers/Risks
- Model performance insufficient
- Mobile device limitations
- Dataset quality

### Mitigation
- Test multiple model combinations
- Use mobile-optimized models
- Consider model quantization
- Test on multiple device tiers

### Critical Success Factor
**Do not integrate face recognition into production until this phase validates feasibility.** If models don't meet targets, adjust requirements or approach.

---

## Phase 6: Computer Vision Integration (Camera Attendance)

### Duration: 3-4 weeks

### Objectives
- Integrate selected models into production app
- Implement camera attendance workflow
- Implement face enrollment workflow
- Implement deduplication logic
- Implement attendance review UI

### Deliverables

**Backend:**
- Face profile management APIs
- Face image storage (object storage)
- Face embedding storage (PostgreSQL)
- Face enrollment validation

**Android:**
- Face enrollment flow
- Camera attendance flow
- Live scanner screen
- Detection visualization
- Review attendance screen
- Manual correction capabilities

**Implementation Tasks:**

**Backend:**
- [ ] Create face_profiles, face_images, face_embeddings tables
- [ ] Configure object storage (S3/MinIO)
- [ ] Implement POST /face/register
- [ ] Implement image upload to object storage
- [ ] Implement embedding storage
- [ ] Implement GET /face/embeddings (for enrolled students)
- [ ] Implement validation & quality checks
- [ ] Implement DELETE /face/profile (GDPR compliance)

**Android (Face Enrollment):**
- [ ] Face enrollment screen
- [ ] Camera preview
- [ ] Capture multiple images (3-5)
- [ ] Quality validation (lighting, size, centering, sharpness)
- [ ] Visual feedback (good/bad quality)
- [ ] Upload images to backend
- [ ] Confirmation screen

**Android (Camera Attendance):**
- [ ] Integrate selected detection model
- [ ] Integrate selected recognition model
- [ ] Implement CameraController (actual implementation)
- [ ] Implement FaceDetector (actual implementation)
- [ ] Implement FaceRecognizer (actual implementation)
- [ ] Live scanner screen (camera + overlays)
- [ ] Face detection bounding boxes (green/blue/red based on confidence)
- [ ] Real-time detection count display
- [ ] Face tracking across frames
- [ ] Cross-frame deduplication logic
- [ ] Embedding download & caching
- [ ] Recognition against enrolled students only
- [ ] "Manual Entry" button always accessible
- [ ] "Capture Frame" button for single-shot
- [ ] "Finish Session" button
- [ ] Processing/loading state

**Android (Attendance Review):**
- [ ] Review screen UI (as per design)
- [ ] "Needs Review" section (low/medium confidence)
- [ ] "Verified List" section (high confidence)
- [ ] "Unknown Faces" section
- [ ] Student photo, name, confidence % display
- [ ] Accept/reject buttons per student
- [ ] Manual add student functionality
- [ ] Search students
- [ ] Sort by confidence
- [ ] Present/Absent/Unknown counts
- [ ] "Confirm Attendance" button
- [ ] Handle backend validation errors

**Shared (KMP):**
- [ ] Face enrollment models
- [ ] Recognition result models
- [ ] Deduplication logic (shared where possible)
- [ ] Confidence categorization logic

### Acceptance Criteria
- [ ] Students can enroll face data (3-5 images)
- [ ] System validates face image quality
- [ ] Face embeddings stored securely
- [ ] Teacher can start camera attendance
- [ ] System downloads embeddings for enrolled students only
- [ ] Camera detects multiple faces in single frame
- [ ] System recognizes faces with confidence scores
- [ ] Same student detected in multiple frames is deduplicated
- [ ] High-confidence students auto-added to present list
- [ ] Low-confidence students shown in "Needs Review"
- [ ] Unknown faces shown separately
- [ ] Teacher can review all detections
- [ ] Teacher can manually add/remove students
- [ ] Teacher can switch to manual attendance anytime
- [ ] System confirms attendance with backend
- [ ] Backend validates all students are enrolled
- [ ] Workflow completes in <3 minutes for 60-student class
- [ ] Performance metrics met (from Phase 5)
- [ ] No camera crashes or freezes
- [ ] Battery consumption acceptable

### Blockers/Risks
- Camera integration complexity
- Performance issues on lower-end devices
- Recognition accuracy in real classrooms
- User experience complexity

### Mitigation
- Extensive testing on multiple devices
- Optimize frame sampling rate
- Clear UI feedback
- Manual fallback always available
- Pilot testing with real teachers

### Critical Success Factor
**This is the highest-priority feature.** Do not consider it complete until teachers can successfully use it in real classrooms. Plan for iteration based on pilot feedback.

---

## Phase 7: Leave & Query Management

### Duration: 2 weeks

### Objectives
- Implement leave request workflow
- Implement leave approval workflow
- Implement attendance query workflow
- Implement attendance correction workflow
- Implement audit trail

### Deliverables

**Backend:**
- Leave request APIs
- Leave approval APIs
- Attendance query APIs
- Query approval APIs
- Attendance correction APIs with audit trail

**Android (Student):**
- Apply leave screen
- Leave list screen
- Leave detail screen
- Raise attendance query screen
- Query list screen
- Query detail screen

**Android (Teacher):**
- Pending leave requests screen
- Leave approval/rejection screen
- Pending attendance queries screen
- Query approval/rejection screen

**Implementation Tasks:**

**Backend:**
- [ ] Create leave_requests, leave_request_subjects tables
- [ ] Create attendance_queries, attendance_corrections tables
- [ ] Implement POST /leaves (apply leave)
- [ ] Implement GET /leaves/student/{id}
- [ ] Implement PATCH /leaves/{id}/approve
- [ ] Implement PATCH /leaves/{id}/reject
- [ ] Implement POST /queries (raise query)
- [ ] Implement GET /queries/student/{id}
- [ ] Implement PATCH /queries/{id}/approve
- [ ] Implement PATCH /queries/{id}/reject
- [ ] Implement attendance correction logic (on query approval)
- [ ] Implement audit trail recording
- [ ] Prevent duplicate leave requests (overlapping dates)
- [ ] Prevent duplicate attendance queries (same record)
- [ ] Implement validation (time limits, etc.)
- [ ] Write tests

**Android (Student):**
- [ ] Apply leave screen (date picker, reason, attachments)
- [ ] Leave list screen (pending, approved, rejected)
- [ ] Leave detail screen (status, dates, reason, comments)
- [ ] Raise query screen (select lecture, reason, evidence)
- [ ] Query list screen (pending, approved, rejected)
- [ ] Query detail screen (status, response)

**Android (Teacher):**
- [ ] Pending leave requests list
- [ ] Leave request detail screen
- [ ] Approve/reject leave actions
- [ ] Pending queries list
- [ ] Query detail screen (original status, student reason)
- [ ] Approve/reject query actions
- [ ] View correction history

### Acceptance Criteria
- [ ] Student can apply for leave (single day or range)
- [ ] Student can select affected subjects
- [ ] Student can upload attachment
- [ ] System prevents overlapping leave requests
- [ ] Teacher receives leave request notification
- [ ] Teacher can approve/reject leave with comments
- [ ] Student notified of leave approval/rejection
- [ ] Approved leave updates attendance status to ON_LEAVE
- [ ] Student can raise attendance query for specific lecture
- [ ] System prevents duplicate queries for same record
- [ ] Teacher receives query notification
- [ ] Teacher can view original attendance and student reason
- [ ] Teacher can approve/reject query
- [ ] Query approval creates attendance correction
- [ ] Correction preserves audit trail (old value, new value, reason, who, when)
- [ ] Student can view correction history
- [ ] All business rules enforced
- [ ] Tests pass

### Blockers/Risks
- Complex business rules
- Edge cases in correction logic

### Mitigation
- Document business rules clearly
- Write comprehensive tests
- Test edge cases thoroughly

---

## Phase 8: Analytics & Reports

### Duration: 2 weeks

### Objectives
- Implement attendance analytics
- Student attendance trends
- Teacher class analytics
- Admin institution analytics
- Low attendance identification

### Deliverables

**Backend:**
- Analytics APIs
- Aggregation queries
- Reports generation
- Caching for performance

**Android:**
- Enhanced student dashboard with trends
- Teacher class analytics screen
- Admin institution analytics screen
- Low attendance students report

**Implementation Tasks:**

**Backend:**
- [ ] Create database views for common aggregations
- [ ] Implement GET /analytics/student/{id}
- [ ] Implement GET /analytics/class/{id}
- [ ] Implement GET /analytics/subject/{id}
- [ ] Implement GET /analytics/teacher/{id}
- [ ] Implement GET /analytics/institution (admin only)
- [ ] Implement GET /analytics/low-attendance
- [ ] Implement attendance trends (monthly, semester, yearly)
- [ ] Implement caching for expensive queries
- [ ] Optimize queries with proper indexes

**Android (Student):**
- [ ] Enhanced dashboard with trends chart
- [ ] Monthly attendance chart
- [ ] Subject comparison chart
- [ ] Attendance health indicator (SAFE/WARNING/CRITICAL)
- [ ] Predictions (if maintaining current rate)

**Android (Teacher):**
- [ ] Class attendance analytics screen
- [ ] Subject-wise attendance overview
- [ ] Low attendance students list
- [ ] Student at-risk identification
- [ ] Export capabilities (optional)

**Android (Admin):**
- [ ] Institution analytics dashboard
- [ ] Department-wise attendance
- [ ] Class-wise attendance
- [ ] Subject-wise attendance
- [ ] Overall trends
- [ ] At-risk students report

### Acceptance Criteria
- [ ] Student can view attendance trends over time
- [ ] Student can see attendance health status
- [ ] Teacher can view class attendance analytics
- [ ] Teacher can identify low-attendance students
- [ ] Admin can view institution-wide analytics
- [ ] Admin can filter by department, program, class
- [ ] Charts render correctly
- [ ] Data is accurate
- [ ] Performance is acceptable (<2s for complex queries)
- [ ] Caching reduces database load

### Blockers/Risks
- Query performance on large datasets
- Chart library integration

### Mitigation
- Database indexes
- Materialized views if needed
- Redis caching
- Use battle-tested chart libraries

---

## Phase 9: Leaderboard & Notifications

### Duration: 2 weeks

### Objectives
- Implement attendance leaderboard
- Implement notification system
- Push notifications
- Email notifications (optional)

### Deliverables

**Backend:**
- Leaderboard APIs
- Notification APIs
- Push notification service
- Email notification service (optional)

**Android:**
- Leaderboard screen
- Notification center screen
- Push notification handling

**Implementation Tasks:**

**Backend:**
- [ ] Implement leaderboard calculation logic
- [ ] Implement GET /leaderboard/monthly
- [ ] Implement GET /leaderboard/semester
- [ ] Implement GET /leaderboard/yearly
- [ ] Implement leaderboard categories (highest, most consistent, most improved)
- [ ] Create notifications table
- [ ] Implement POST /notifications (internal)
- [ ] Implement GET /notifications (user)
- [ ] Implement PATCH /notifications/{id}/read
- [ ] Integrate Firebase Cloud Messaging (FCM)
- [ ] Implement push notification sending
- [ ] Implement email notification sending (optional)
- [ ] Trigger notifications for:
  - Attendance marked
  - Leave approved/rejected
  - Query approved/rejected
  - Low attendance warning

**Android:**
- [ ] Leaderboard screen (monthly, semester, yearly tabs)
- [ ] Filter by class, department
- [ ] Categories (highest, consistent, improved)
- [ ] Display student rank, name, percentage
- [ ] Notification center screen
- [ ] Notification list (read/unread)
- [ ] Mark as read functionality
- [ ] Handle FCM token registration
- [ ] Handle push notification reception
- [ ] Deep link from notification to relevant screen

### Acceptance Criteria
- [ ] Leaderboard displays top students correctly
- [ ] Leaderboard updates periodically
- [ ] Multiple leaderboard categories work
- [ ] Leaderboard respects privacy (only show top, not bottom)
- [ ] Users receive push notifications for important events
- [ ] Notification center displays all notifications
- [ ] Users can mark notifications as read
- [ ] Notifications include relevant information
- [ ] Deep linking from notifications works
- [ ] Email notifications sent for critical events (optional)

### Blockers/Risks
- FCM integration complexity
- Leaderboard calculation performance

### Mitigation
- Follow FCM documentation carefully
- Pre-calculate leaderboards periodically (cron job)
- Cache leaderboard results

---

## Phase 10: Security Hardening & Production Prep

### Duration: 2 weeks

### Objectives
- Security audit & hardening
- Performance optimization
- Production configuration
- Monitoring & alerting setup
- Documentation finalization

### Implementation Tasks

**Security:**
- [ ] Security code review
- [ ] Dependency vulnerability scanning
- [ ] SQL injection testing
- [ ] XSS testing
- [ ] CSRF protection verification
- [ ] Rate limiting implementation
- [ ] API authentication audit
- [ ] Authorization audit (all endpoints)
- [ ] Secrets management review
- [ ] Database access control review
- [ ] Face data encryption verification
- [ ] HTTPS/TLS configuration
- [ ] Certificate pinning (Android)

**Performance:**
- [ ] Database query optimization
- [ ] Add missing indexes
- [ ] Connection pooling tuning
- [ ] Caching strategy review
- [ ] Frontend performance optimization
- [ ] Image optimization
- [ ] Bundle size optimization
- [ ] Memory leak testing
- [ ] Battery consumption testing

**Production:**
- [ ] Environment configuration (dev, staging, prod)
- [ ] Database migration scripts finalized
- [ ] Backup & restore procedures
- [ ] Disaster recovery plan
- [ ] Monitoring setup (Prometheus + Grafana)
- [ ] Logging setup (ELK stack or equivalent)
- [ ] Error tracking (Sentry or equivalent)
- [ ] APM setup (Application Performance Monitoring)
- [ ] Health check endpoints
- [ ] Database backup automation

**Documentation:**
- [ ] API documentation (OpenAPI) complete
- [ ] Deployment guide
- [ ] Operations runbook
- [ ] Troubleshooting guide
- [ ] User manuals (student, teacher, admin)
- [ ] Admin training materials

### Acceptance Criteria
- [ ] No critical security vulnerabilities
- [ ] All API endpoints properly authorized
- [ ] Rate limiting in place
- [ ] Secrets not in source code
- [ ] HTTPS enforced
- [ ] Database queries optimized
- [ ] Application performance acceptable
- [ ] Monitoring dashboards created
- [ ] Alerts configured
- [ ] Backup automation working
- [ ] All documentation complete
- [ ] Deployment pipeline tested

### Blockers/Risks
- Security issues discovered late
- Performance bottlenecks

### Mitigation
- Security review early and often
- Load testing before production
- Incremental hardening

---

## Phase 11: Pilot Deployment & Iteration

### Duration: 3-4 weeks

### Objectives
- Deploy to pilot users
- Gather real-world feedback
- Monitor performance
- Fix bugs
- Iterate on UX
- Prepare for full rollout

### Implementation Tasks

**Pilot Setup:**
- [ ] Identify pilot classes (2-3 classes, ~150-200 students)
- [ ] Identify pilot teachers (3-5 teachers)
- [ ] Deploy to staging environment
- [ ] Onboard pilot users
- [ ] Conduct training sessions
- [ ] Provide user manuals

**Monitoring:**
- [ ] Monitor system performance
- [ ] Monitor error rates
- [ ] Monitor face recognition accuracy
- [ ] Monitor teacher correction rates
- [ ] Monitor session completion times
- [ ] Monitor user feedback
- [ ] Track support tickets

**Iteration:**
- [ ] Daily stand-ups during pilot
- [ ] Bug fixes
- [ ] UX improvements based on feedback
- [ ] Performance optimizations
- [ ] Adjust confidence thresholds if needed
- [ ] Iterate on camera UX if needed

**Validation:**
- [ ] Validate attendance engine works in real classrooms
- [ ] Validate face recognition accuracy
- [ ] Validate manual fallback is sufficient
- [ ] Validate teacher workflow is smooth
- [ ] Validate student UX is clear
- [ ] Validate admin tools are effective

### Acceptance Criteria
- [ ] Pilot completes successfully (2-4 weeks)
- [ ] Face recognition accuracy meets targets (>90%)
- [ ] Teacher satisfaction >80%
- [ ] Session completion time <3 minutes
- [ ] Teacher correction rate <10%
- [ ] Manual fallback used <10% of time
- [ ] No critical bugs
- [ ] Performance meets targets
- [ ] All major feedback addressed

### Critical Go/No-Go Decision
**Proceed to full rollout only if:**
- Pilot success criteria met
- Teachers confident using system
- No critical bugs
- Performance acceptable
- Stakeholder approval

---

## Post-Phase 11: Full Rollout

### Implementation
- [ ] Deploy to production
- [ ] Onboard all teachers
- [ ] Onboard all students
- [ ] Conduct training sessions institution-wide
- [ ] Provide support resources
- [ ] Monitor closely for first 2 weeks
- [ ] Establish support process

---

## Future Phases (Post-MVP)

### Phase 12: iOS Implementation (6-8 weeks)
- Implement iOS platform abstractions
- Build SwiftUI interface
- Test shared KMP code on iOS
- Deploy to App Store

### Phase 13: Advanced Features
- Parent portal
- Advanced analytics
- ML-based attendance prediction
- Integration with LMS
- Biometric alternatives (QR codes)
- Multi-language support

---

## Risk Management

### High-Risk Items

| Risk | Phase | Impact | Mitigation |
|------|-------|--------|------------|
| Face recognition accuracy insufficient | 5, 6 | Critical | Thorough benchmarking, manual fallback |
| KMP architecture complexity | 1 | High | Training, documentation, simple abstractions |
| Performance issues on low-end devices | 6 | High | Frame sampling, optimization, graceful degradation |
| User adoption resistance | 11 | High | Training, change management, feedback loop |
| Security vulnerabilities | 10 | Critical | Security review, penetration testing |

### Mitigation Strategy
- Early validation (Phase 5 before Phase 6)
- Incremental delivery
- Pilot before full rollout
- Manual fallback always available
- Continuous monitoring

---

## Success Metrics (Post-Pilot)

### Technical Metrics
- System uptime: >99.5%
- API latency (p95): <200ms
- Face recognition accuracy: >90%
- Error rate: <1%

### User Metrics
- Teacher adoption rate: >90%
- Average session time: <3 minutes
- Manual fallback usage: <10%
- Teacher correction rate: <10%
- Student query rate: <5%

### Business Metrics
- Attendance tracking accuracy improved
- Attendance data completeness: >95%
- Reduced time spent on attendance
- Teacher satisfaction: >80%
- Student satisfaction: >70%

---

## Dependencies & Prerequisites

### External Dependencies
- Object storage (AWS S3 / MinIO)
- Email service (SendGrid / AWS SES) - optional
- Push notification service (Firebase)

### Internal Dependencies
- Academic calendar and policies defined
- Institutional attendance rules documented
- Student and teacher data available
- Face enrollment consent process
- Device procurement (teachers need Android phones)

### Team Requirements
- 2-3 backend developers (Java/Spring Boot)
- 2-3 Android developers (Kotlin, KMP)
- 1 ML engineer (face recognition)
- 1 UI/UX designer
- 1 QA engineer
- 1 DevOps engineer
- 1 Product manager
- 1 Project manager

---

## Conclusion

This roadmap provides a **phase-by-phase approach** to building a production-grade Smart Classroom Attendance Management System. The emphasis is on:

1. **Validating core assumptions early** (Phase 5 CV benchmarking)
2. **Building incrementally** (each phase delivers working software)
3. **Explicit acceptance criteria** (clear definition of done)
4. **Risk mitigation** (pilot before full rollout)
5. **Quality over speed** (security, performance, UX)

**Next Steps:**
1. Review and approve this roadmap
2. Resolve open decisions (decision-log.md)
3. Assemble development team
4. Kick off Phase 1

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | Project Lead | Initial draft |

---

**Status**: DRAFT - Awaiting approval to proceed
