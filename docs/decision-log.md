# Requirements Decision Log
## Smart Classroom Attendance Management System

**Version:** 1.0  
**Date:** August 25, 2026  
**Status:** Draft - Awaiting Stakeholder Review

---

## Decision Status Legend

- 🟢 **DECIDED**: Decision finalized and approved
- 📝 **ASSUMPTION**: Working assumption, may be revised
- ⚙️ **CONFIGURABLE**: System parameter, configurable at runtime/deployment
- 📋 **REQUIRES STAKEHOLDER INPUT**: Policy/business decision requiring institutional input
- 🧪 **BENCHMARK-DEPENDENT**: Technical decision requiring experimental validation
- 🔮 **DEFERRED**: Will be decided in later phase
- 🔴 **BLOCKED**: Waiting on external dependency

---

## 1. Architecture Decisions

### AD-001: Kotlin Multiplatform from Day One
**Status**: 🟢 DECIDED  
**Decision**: Use KMP architecture from the start, not pure Android  
**Options**:
1. Pure Android → Convert to KMP later
2. KMP from day one (Android first, iOS-ready)
3. Separate Android and iOS codebases

**Selected**: Option 2 - KMP from day one

**Rationale**:
- Avoid costly rewrite when adding iOS
- Share 60-70% of business logic
- Consistent behavior across platforms
- Type-safe API contracts
- Long-term maintenance benefits

**Trade-offs**:
- Higher initial complexity
- Team learning curve
- Platform abstractions required

**Approved By**: [Pending stakeholder approval]  
**Date**: 2026-08-25

---

### AD-002: Modular Monolith vs Microservices
**Status**: 🟢 DECIDED  
**Decision**: Start with modular monolith, not microservices  
**Options**:
1. Microservices architecture
2. Modular monolith
3. Traditional layered monolith

**Selected**: Option 2 - Modular monolith

**Rationale**:
- Simpler deployment and operations
- Easier development and debugging
- Sufficient for expected scale (50K students, 2K teachers)
- Lower operational overhead
- Modules can be extracted to microservices later if needed
- Single database reduces complexity

**Trade-offs**:
- Single deployment unit
- Shared database
- Less independent scaling per module

**Approved By**: [Pending]  
**Date**: 2026-08-25

---

### AD-003: On-Device vs Server-Side Face Recognition
**Status**: 🟢 DECIDED  
**Decision**: Process face recognition on client device  
**Options**:
1. Server-side recognition (upload frames to backend)
2. On-device recognition (client-side processing)
3. Hybrid (detection on device, recognition on server)

**Selected**: Option 2 - On-device recognition

**Rationale**:
- Lower backend computational load
- Works offline (temporary network issues)
- Faster processing (no network latency)
- Privacy (embeddings stay on device during session)
- Scalability (no server GPU requirements)

**Trade-offs**:
- Client device requirements (CPU, memory)
- Battery consumption
- Model distribution and updates
- Varied device performance

**Approved By**: [Pending]  
**Date**: 2026-08-25

---

### AD-004: Database Selection
**Status**: 🟢 DECIDED  
**Decision**: PostgreSQL with pgvector extension  
**Options**:
1. PostgreSQL + separate vector database (Pinecone, Milvus)
2. PostgreSQL with pgvector extension
3. MongoDB + vector search
4. MySQL + separate vector DB

**Selected**: Option 2 - PostgreSQL + pgvector

**Rationale**:
- Single database for relational + vector data
- ACID guarantees
- Mature, battle-tested
- Good vector search performance for our scale
- Lower operational complexity
- Team familiarity

**Trade-offs**:
- Vector search slower than specialized DBs for massive scale
- Sufficient for initial scope (hundreds of vector searches, not millions)

**Approved By**: [Pending]  
**Date**: 2026-08-25

---

## 2. Face Recognition Decisions

### FR-001: Face Recognition Model Selection
**Status**: 🧪 BENCHMARK-DEPENDENT - Requires Phase 5 benchmarking  
**Decision**: Which detection and recognition models to use  
**Options**:

**Detection Models**:
1. MTCNN (Multi-task Cascaded CNN)
2. BlazeFace (Google)
3. MediaPipe Face Detection
4. YuNet

**Recognition Models**:
1. FaceNet (Google)
2. ArcFace
3. MobileFaceNet
4. InsightFace

**Recommendation**: Benchmark in Phase 5 with actual classroom data

**Evaluation Criteria**:
- Detection accuracy (TPR, FPR)
- Recognition accuracy in classroom conditions
- Processing speed (<300ms target per face)
- Memory footprint
- Battery consumption
- Multi-face handling
- Robustness (lighting, angles, distance, occlusion)
- Mobile compatibility (Android + future iOS)

**Next Steps**:
1. Complete backend foundation (Phase 1-4)
2. Collect benchmark dataset (100-200 volunteers)
3. Test all model combinations
4. Measure performance metrics
5. Select optimal combination

**Decision Date**: TBD (After Phase 5 benchmarking)

---

### FR-002: Confidence Thresholds
**Status**: ⚙️ CONFIGURABLE + 🧪 BENCHMARK-DEPENDENT  
**Decision**: What confidence thresholds to use for categorization

**IMPORTANT**: These thresholds are NOT hard-coded business rules. They are system parameters that must be:
1. Determined through Phase 5 benchmarking
2. Configurable at deployment
3. Tunable based on production data  
**Options**:

| Category | Option A | Option B | Option C |
|----------|----------|----------|----------|
| HIGH (Auto-accept) | ≥0.85 | ≥0.90 | ≥0.80 |
| MEDIUM (Review) | 0.75-0.84 | 0.80-0.89 | 0.70-0.79 |
| LOW (Review) | 0.65-0.74 | 0.70-0.79 | 0.60-0.69 |
| VERY LOW | 0.50-0.64 | 0.60-0.69 | 0.50-0.59 |
| UNKNOWN | <0.50 | <0.60 | <0.50 |

**Recommendation**: Option A initially, then adjust based on pilot testing

**Rationale**:
- Start conservative (fewer false positives)
- Measure false accept/reject rates in production
- Adjust thresholds based on teacher feedback
- Make configurable for future tuning

**Next Steps**:
1. Implement with Option A as default
2. Log all confidence scores in production
3. Analyze false positive/negative rates
4. Adjust thresholds based on data
5. Consider per-subject or per-class thresholds if needed

**Decision Date**: TBD (After pilot deployment)

---

### FR-003: Face Enrollment Image Count
**Status**: 🟡 OPEN - Balance accuracy vs UX  
**Decision**: How many images to capture during enrollment  
**Options**:
1. 3 images (front, slight left, slight right)
2. 5 images (front, left, right, up, down)
3. 7 images (more angle variations)
4. 1 image + data augmentation

**Recommendation**: Option 1 (3 images) initially

**Rationale**:
- Balance between accuracy and enrollment UX
- Students more likely to complete quick enrollment
- Can increase if accuracy is insufficient
- Quality over quantity (3 good images > 7 poor images)

**Trade-offs**:
- Fewer images = potentially lower accuracy
- More images = better coverage but longer enrollment time
- Risk of student fatigue/poor later images

**Next Steps**:
1. Implement 3-image enrollment
2. Measure recognition accuracy
3. Increase to 5 if needed

**Decision Date**: TBD (After initial testing)

---

### FR-004: Face Data Retention Period
**Status**: 🟡 OPEN - Legal/regulatory consideration  
**Decision**: How long to retain face data after student graduation  
**Options**:
1. Delete immediately upon graduation
2. Retain for 1 year after graduation
3. Retain for 3 years (match attendance record retention)
4. Retain indefinitely (with consent)

**Recommendation**: Option 2 (1 year) or Option 3 (3 years) based on legal requirements

**Rationale**:
- Biometric data is sensitive (GDPR, local privacy laws)
- May need data for transcript verification
- Align with attendance record retention
- Support alumni re-enrollment scenarios

**Dependencies**:
- Legal review of biometric data regulations
- Institution policy
- Student consent terms

**Next Steps**:
1. Consult legal team
2. Review local biometric data laws
3. Draft consent form
4. Implement deletion workflow

**Decision Date**: TBD (Before production deployment)

---

## 3. Attendance Business Rules

### AR-001: Low Attendance Thresholds
**Status**: 🟡 OPEN - Institution policy dependent  
**Decision**: What constitutes SAFE, WARNING, CRITICAL attendance  
**Options**:

| Status | Option A | Option B | Option C |
|--------|----------|----------|----------|
| SAFE | ≥75% | ≥80% | ≥70% |
| WARNING | 65-74% | 70-79% | 60-69% |
| CRITICAL | <65% | <70% | <60% |

**Recommendation**: Option A (75% threshold) - common academic standard

**Rationale**:
- 75% is common minimum attendance requirement in many institutions
- Configurable per institution
- May vary by subject/program
- Should be configurable by admin

**Trade-offs**:
- Too high: Many students flagged, notification fatigue
- Too low: Students don't get early warning

**Next Steps**:
1. Confirm institution's attendance policy
2. Implement as configurable setting
3. Allow per-subject overrides if needed

**Decision Date**: TBD (Institution policy review)

---

### AR-002: Attendance Correction Time Limit
**Status**: 🟡 OPEN - Balance flexibility vs integrity  
**Decision**: How long after lecture can attendance queries be raised  
**Options**:
1. 24 hours after lecture
2. 48 hours after lecture
3. 1 week after lecture
4. Until semester end (no time limit)

**Recommendation**: Option 2 (48 hours)

**Rationale**:
- Students need reasonable time to notice and dispute
- Too long: Memory fades, proof harder to provide
- Too short: Students may not check immediately
- Prevents end-of-semester bulk disputes

**Trade-offs**:
- Shorter: More integrity, less disputes
- Longer: More flexibility, potential abuse

**Next Steps**:
1. Confirm with academic administration
2. Implement as configurable setting
3. Monitor dispute patterns

**Decision Date**: TBD (Academic policy review)

---

### AR-003: Leave Request Advance Notice Requirement
**Status**: 🟡 OPEN - Policy dependent  
**Decision**: How far in advance must leave be requested  
**Options**:
1. No minimum (same-day allowed)
2. 1 day advance notice
3. 3 days advance notice
4. 1 week advance notice
5. Exception for emergency/medical

**Recommendation**: Option 1 (no minimum) with Option 5 (emergency category)

**Rationale**:
- Students may have genuine last-minute emergencies
- Medical situations can't always be predicted
- Categorize as "planned" vs "emergency"
- Emergency may require documentation

**Next Steps**:
1. Define leave categories (planned, medical, emergency, other)
2. Set documentation requirements per category
3. Implement approval workflow

**Decision Date**: TBD (Policy review)

---

### AR-004: Retroactive Leave Application
**Status**: 🟡 OPEN - Policy dependent  
**Decision**: Can students apply for leave after lecture has occurred  
**Options**:
1. No retroactive leave
2. Retroactive allowed with documentation (medical certificate)
3. Retroactive allowed within 24 hours
4. Retroactive allowed at teacher discretion

**Recommendation**: Option 2 (with documentation) or Option 4 (teacher discretion)

**Rationale**:
- Medical emergencies happen unexpectedly
- Require proof for retroactive leave
- Prevent abuse of retroactive system
- Give teachers flexibility

**Next Steps**:
1. Define documentation requirements
2. Implement approval workflow
3. Add attachment upload to leave requests

**Decision Date**: TBD (Policy review)

---

## 4. System Configuration Decisions

### SC-001: Session Timeout Duration
**Status**: 🟡 OPEN - Balance security vs UX  
**Decision**: How long before user session expires  
**Options**:
1. 30 minutes
2. 1 hour
3. 2 hours
4. 8 hours (academic day)

**Recommendation**: Option 2 (1 hour) with refresh token

**Rationale**:
- Teachers shouldn't be logged out mid-session
- Students may leave app open between classes
- Refresh token allows seamless renewal
- Security vs convenience balance

**Next Steps**:
1. Implement JWT with access token (1 hour) + refresh token (7 days)
2. Auto-refresh before expiry
3. Force logout on security events

**Decision Date**: TBD (After initial implementation)

---

### SC-002: Offline Attendance Sync Strategy
**Status**: 🟡 OPEN - Technical complexity vs reliability  
**Decision**: How to handle attendance taken offline  
**Options**:
1. No offline support (require network)
2. Queue attendance, sync when network available
3. Full offline mode with conflict resolution
4. Offline for camera only, require network for confirmation

**Recommendation**: Option 2 (queue and sync)

**Rationale**:
- Classrooms may have poor network
- Camera/recognition can work offline
- Final confirmation needs backend validation
- Queue locally, sync when network restored

**Trade-offs**:
- Complexity in sync logic
- Potential conflicts if data changes
- Need clear UI for "pending sync" state

**Next Steps**:
1. Implement Phase 1-4 with online-only
2. Add offline capability in later phase
3. Design sync conflict resolution

**Decision Date**: ⚪ DEFERRED to Phase 8

---

### SC-003: Notification Delivery Method
**Status**: 🟡 OPEN - Cost vs reach  
**Decision**: How to deliver notifications  
**Options**:
1. In-app only
2. In-app + push notifications
3. In-app + push + email
4. In-app + push + email + SMS

**Recommendation**: Option 3 (in-app + push + email)

**Rationale**:
- In-app: Always available when user opens app
- Push: Immediate attention for urgent items
- Email: Reliable fallback, documentation trail
- SMS: Additional cost, may not be necessary

**Trade-offs**:
- More channels = higher implementation/operational cost
- Users may experience notification fatigue
- Need preference management

**Next Steps**:
1. Implement in-app notifications first
2. Add push notifications (Phase 9)
3. Add email for critical notifications (leave approval, etc.)
4. Let users configure preferences

**Decision Date**: TBD (After Phase 9 planning)

---

## 5. Security Decisions

### SE-001: Password Policy
**Status**: 🟡 OPEN - Balance security vs usability  
**Decision**: Password strength requirements  
**Options**:

| Requirement | Option A (Strict) | Option B (Moderate) | Option C (Lenient) |
|-------------|-------------------|---------------------|---------------------|
| Min length | 12 characters | 8 characters | 6 characters |
| Uppercase | Required | Required | Optional |
| Lowercase | Required | Required | Required |
| Number | Required | Required | Optional |
| Special char | Required | Optional | Optional |
| Dictionary check | Yes | Yes | No |
| Password history | Last 5 | Last 3 | None |
| Expiry | 90 days | 180 days | Never |

**Recommendation**: Option B (Moderate) without expiry

**Rationale**:
- Balance security and usability
- Password expiry often leads to weak, predictable patterns
- Focus on breach detection, not forced resets
- Multi-factor authentication more effective than complex passwords

**Next Steps**:
1. Implement Option B requirements
2. Consider adding 2FA for admin accounts
3. Implement breach detection monitoring
4. Allow biometric login on mobile (Android)

**Decision Date**: TBD (Security review)

---

### SE-002: API Rate Limiting
**Status**: 🟡 OPEN - Prevent abuse vs performance  
**Decision**: Rate limit thresholds  
**Options**:

| Action | Option A | Option B | Option C |
|--------|----------|----------|----------|
| Login attempts | 5 per 15 min | 10 per hour | 3 per 5 min |
| API calls (general) | 100 per min | 200 per min | 50 per min |
| Face enrollment | 3 per hour | 5 per day | 1 per day |
| Attendance queries | 5 per day | 10 per day | 20 per day |

**Recommendation**: Option A for login, Option B for general API

**Rationale**:
- Strict login rate limit prevents brute force
- General API limit allows normal usage
- Face enrollment doesn't need to be repeated frequently
- Prevent query spam

**Next Steps**:
1. Implement rate limiting middleware
2. Monitor actual usage patterns
3. Adjust thresholds based on data

**Decision Date**: TBD (Before production)

---

## 6. Deployment & Operations

### DO-001: Deployment Environment Strategy
**Status**: 🟡 OPEN - Cost vs risk mitigation  
**Decision**: What environments to maintain  
**Options**:
1. Production only
2. Production + staging
3. Development + staging + production
4. Development + QA + staging + production

**Recommendation**: Option 3 (dev + staging + prod)

**Rationale**:
- Development: For active development and debugging
- Staging: Pre-production testing, mirror production
- Production: Live system
- QA can share staging environment

**Trade-offs**:
- More environments = higher cost
- Better risk mitigation
- Standard industry practice

**Next Steps**:
1. Set up development environment
2. Set up staging (before production)
3. Define promotion pipeline

**Decision Date**: TBD (DevOps planning)

---

### DO-002: Database Backup Strategy
**Status**: 🟢 DECIDED  
**Decision**: Backup frequency and retention  
**Selected**:
- Full backup: Daily at 2:00 AM
- Incremental backup: Every 6 hours
- WAL archiving: Continuous
- Retention: 30 days

**Rationale**:
- Daily full backup provides restore points
- Incremental reduces backup time and storage
- WAL archiving enables point-in-time recovery
- 30-day retention balances storage cost and recovery needs

**Approved By**: [Pending]  
**Date**: 2026-08-25

---

## 7. Feature Scope Decisions

### FS-001: Parent Portal
**Status**: ⚪ DEFERRED - Out of scope for Phase 1  
**Decision**: Include parent portal in initial release  
**Options**:
1. Include parent portal in Phase 1
2. Add in Phase 2/3
3. Not planned

**Recommendation**: Option 2 (Add later)

**Rationale**:
- Focus on core users first (students, teachers, admins)
- Parent portal adds significant scope
- Can be added incrementally
- Not critical for MVP

**Decision Date**: ⚪ DEFERRED

---

### FS-002: Mobile App for Students
**Status**: 🟡 OPEN - Scope consideration  
**Decision**: Separate student mobile app or web-only  
**Options**:
1. Android app for students (same codebase as teacher app)
2. Web-only for students (responsive PWA)
3. Android app for teachers, web for students
4. Full mobile apps for both (Android + iOS)

**Recommendation**: Option 1 (Android app for both, using KMP shared code)

**Rationale**:
- Shared KMP code benefits both user types
- Mobile-first for attendance viewing
- Push notifications for leaves/queries
- Better UX than mobile web
- Single codebase for Android

**Trade-offs**:
- More complex app (role-based UI)
- Larger app size vs web

**Next Steps**:
1. Confirm requirement with stakeholders
2. Design role-based navigation
3. Implement in same codebase with role switching

**Decision Date**: TBD (Stakeholder input needed)

---

## 8. Open Questions for Stakeholders

### OQ-001: Academic Calendar
**Question**: What is the institution's academic calendar structure?  
**Impact**: Affects semester management, date validation  
**Options**: Semester system, trimester system, quarter system  
**Needed By**: Phase 3 (Academic management implementation)

---

### OQ-002: Attendance Calculation Method
**Question**: How should attendance percentage be calculated?  
**Impact**: Core business logic  
**Options**:
- Simple: Present / Total lectures
- Exclude leaves: Present / (Total - On Leave)
- Include late as partial: (Present + 0.5 * Late) / Total

**Needed By**: Phase 4 (Attendance engine)

---

### OQ-003: Teacher Assignment Rules
**Question**: Can a teacher be assigned to multiple sections of same subject?  
**Impact**: Database constraints, assignment validation  
**Needed By**: Phase 3 (Academic management)

---

### OQ-004: Student Class Changes
**Question**: How to handle students changing sections mid-semester?  
**Impact**: Attendance history, enrollment management  
**Options**: Transfer attendance, archive old, start fresh  
**Needed By**: Phase 3 (Academic management)

---

### OQ-005: Lecture Cancellation
**Question**: How should cancelled lectures affect attendance calculation?  
**Impact**: Attendance percentage logic  
**Options**: Exclude from calculation, mark all as present, mark as N/A  
**Needed By**: Phase 4 (Attendance engine)

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | System Architect | Initial draft |

---

## Summary of Decision Status

| Status | Count | Decisions |
|--------|-------|-----------|
| 🟢 DECIDED | 4 | AD-001, AD-002, AD-003, AD-004, DO-002 |
| 🟡 OPEN | 15 | FR-001, FR-002, FR-003, FR-004, AR-001, AR-002, AR-003, AR-004, SC-001, SC-002, SC-003, SE-001, SE-002, DO-001, FS-002 |
| 🔴 BLOCKED | 0 | None |
| ⚪ DEFERRED | 2 | SC-002 (partial), FS-001 |

**Next Steps**:
1. Review and approve DECIDED items
2. Prioritize OPEN items by implementation phase
3. Gather stakeholder input for policy-dependent decisions
4. Schedule Phase 5 model benchmarking
5. Update log as decisions are made

---

**Status**: DRAFT - Awaiting stakeholder review
