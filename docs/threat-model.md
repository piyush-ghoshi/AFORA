# Security Threat Model
## AFORA - Smart Classroom Attendance Management

**Version:** 1.0  
**Date:** August 25, 2026  
**Status:** Security Architecture Definition

---

## 1. Threat Modeling Approach

We use **STRIDE** methodology to categorize threats:

- **S**poofing (Identity)
- **T**ampering (Data)
- **R**epudiation (Actions)
- **I**nformation Disclosure
- **D**enial of Service
- **E**levation of Privilege

---

## 2. Asset Classification

### 2.1 Critical Assets

| Asset | Sensitivity | Impact if Compromised |
|-------|-------------|----------------------|
| **Biometric Data** (face images, embeddings) | CRITICAL | Privacy violation, legal liability, identity theft |
| **Attendance Records** | HIGH | Academic fraud, grade manipulation |
| **Authentication Credentials** | HIGH | Unauthorized access, impersonation |
| **Student Personal Data** | HIGH | Privacy violation, identity theft |
| **Teacher Assignment Data** | MEDIUM | Unauthorized attendance marking |
| **Audit Logs** | MEDIUM | Loss of accountability |

### 2.2 Trust Boundaries

```
┌────────────────────────────────────────────────────┐
│                 Internet                            │
└────────────────┬───────────────────────────────────┘
                 │ HTTPS/TLS
                 ↓
┌────────────────────────────────────────────────────┐
│          Load Balancer / API Gateway                │
│             (Trust Boundary 1)                      │
└────────────────┬───────────────────────────────────┘
                 │ JWT Token
                 ↓
┌────────────────────────────────────────────────────┐
│         Spring Boot Backend                         │
│     (Authentication & Authorization)                │
│             (Trust Boundary 2)                      │
└────────────────┬───────────────────────────────────┘
                 │ Validated Identity
                 ↓
┌────────────────────────────────────────────────────┐
│              Database                               │
│      (PostgreSQL - Trusted Zone)                    │
└─────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────┐
│          Android Client                             │
│        (Untrusted / User Device)                    │
└─────────────────────────────────────────────────────┘
```

---

## 3. Threat Catalog

### 3.1 Authentication & Authorization Threats

#### THREAT-AUTH-001: Brute Force Attack
**Category**: Spoofing  
**Description**: Attacker attempts to guess user credentials through repeated login attempts

**Attack Vector**:
```
Attacker → Multiple login attempts → Backend
```

**Impact**: HIGH  
**Likelihood**: HIGH

**Mitigations**:
1. ✅ **Rate Limiting**: 5 failed attempts per 15 minutes per user
2. ✅ **Account Lockout**: Lock account after 5 consecutive failures
3. ✅ **CAPTCHA**: After 3 failed attempts
4. ✅ **Monitoring**: Alert on suspicious patterns
5. ✅ **Strong Password Policy**: Enforce minimum complexity
6. ⚙️ **Optional 2FA**: For admin accounts

**Implementation Status**: Planned for Phase 2

---

#### THREAT-AUTH-002: Session Hijacking
**Category**: Spoofing  
**Description**: Attacker steals or intercepts session token

**Attack Vectors**:
- Man-in-the-middle attack
- XSS (if web interface)
- Malware on user device
- Token leaked in logs

**Impact**: CRITICAL  
**Likelihood**: MEDIUM

**Mitigations**:
1. ✅ **HTTPS Only**: Enforce TLS 1.3
2. ✅ **Certificate Pinning**: Android client pins server certificate
3. ✅ **Secure Token Storage**: Android Keystore / iOS Keychain
4. ✅ **Token Expiry**: Access token valid for 1 hour
5. ✅ **Refresh Token Rotation**: Refresh token rotated on each use
6. ✅ **HTTP-Only Cookies** (if web): Prevent JavaScript access
7. ✅ **Token Binding**: Bind token to device/IP (optional)
8. ✅ **No Tokens in Logs**: Never log JWT tokens

**Implementation Status**: Planned for Phase 2

---

#### THREAT-AUTH-003: Privilege Escalation
**Category**: Elevation of Privilege  
**Description**: User gains unauthorized access to higher-privilege operations

**Attack Scenarios**:
- Student modifies API request to access teacher endpoints
- Teacher modifies request to access admin endpoints
- Modified Android client bypasses client-side checks

**Impact**: CRITICAL  
**Likelihood**: HIGH (if not properly implemented)

**Mitigations**:
1. ✅ **Server-Side Authorization**: NEVER trust client
2. ✅ **Role-Based Access Control** (RBAC): Enforce at API level
3. ✅ **Resource-Level Authorization**: Check ownership/assignment
4. ✅ **Consistent Enforcement**: Every endpoint validates role + resource access
5. ✅ **Principle of Least Privilege**: Users only see/do what they need
6. ✅ **Audit Logging**: Log all privilege-sensitive operations

**Example Backend Enforcement**:
```java
@PreAuthorize("hasRole('TEACHER')")
@PostMapping("/lectures/{id}/confirm")
public ResponseEntity<AttendanceConfirmation> confirmAttendance(
    @PathVariable Long id,
    @RequestBody AttendanceSubmission submission,
    @AuthenticationPrincipal UserDetails user
) {
    // Additional check: Is this teacher assigned to this lecture?
    if (!isTeacherAssignedToLecture(user.getUserId(), id)) {
        throw new UnauthorizedException("Teacher not assigned to this lecture");
    }
    // Proceed...
}
```

**Implementation Status**: CRITICAL - Phase 2

---

### 3.2 Data Access Threats

#### THREAT-DATA-001: Unauthorized Student Data Access
**Category**: Information Disclosure  
**Description**: User accesses another student's attendance/personal data

**Attack Scenarios**:
- Student A modifies API request to fetch Student B's data
- Teacher accesses student from unassigned class
- Modified client bypasses UI restrictions

**Impact**: HIGH  
**Likelihood**: HIGH (if not properly implemented)

**Mitigations**:
1. ✅ **Resource Ownership Check**: Backend validates requester owns resource
2. ✅ **Teacher Assignment Check**: Backend validates teacher assigned to class
3. ✅ **No Direct Student ID Access**: Use session user ID, not client-provided ID
4. ✅ **Audit Logging**: Log all student data access

**Example**:
```java
@GetMapping("/students/{id}/attendance")
@PreAuthorize("hasRole('STUDENT')")
public ResponseEntity<AttendanceData> getStudentAttendance(
    @PathVariable Long id,
    @AuthenticationPrincipal UserDetails user
) {
    // Student can only access their own data
    if (!user.getUserId().equals(id)) {
        throw new ForbiddenException("Cannot access other student's data");
    }
    // Proceed...
}
```

**Implementation Status**: CRITICAL - Phase 4

---

#### THREAT-DATA-002: Face Data Exposure
**Category**: Information Disclosure  
**Description**: Unauthorized access to biometric data (face images, embeddings)

**Attack Scenarios**:
- API endpoint leaks face embeddings
- Face images downloadable without authorization
- Embeddings transmitted unnecessarily to client

**Impact**: CRITICAL  
**Likelihood**: MEDIUM

**Mitigations**:
1. ✅ **No Embedding Exposure**: Never send embeddings to client
2. ✅ **Encrypted Storage**: Face images encrypted at rest
3. ✅ **Encrypted Transit**: HTTPS for all face data
4. ✅ **Access Control**: Only authorized endpoints access face data
5. ✅ **Object Storage ACL**: Strict access control on S3/MinIO
6. ✅ **Audit Logging**: Log all face data access
7. ✅ **Minimal Retention**: Delete face data per retention policy
8. ✅ **No Caching**: Face images not cached on client

**Architecture Decision**:
```
Face Recognition Processing:
1. Client requests: GET /lectures/{id}/enrolled-students
2. Backend returns: List<EnrolledStudent> (NO embeddings)
3. Client needs embeddings for local recognition
4. Backend OPTIONALLY sends encrypted embeddings ONLY for the specific lecture
5. Embeddings stored in memory only, not persisted on device
6. Embeddings cleared after session
```

**Alternative** (if embeddings must be on device):
```
1. Embeddings encrypted before transmission
2. Encryption key derived from user session + device ID
3. Embeddings decrypted in memory only
4. Never written to disk
5. Cleared immediately after session
```

**Implementation Status**: CRITICAL - Phase 6

---

### 3.3 Attendance Manipulation Threats

#### THREAT-ATT-001: Fake Attendance Submission
**Category**: Tampering  
**Description**: Attacker submits fake attendance without proper authorization

**Attack Scenarios**:
- Modified Android client submits attendance for unauthorized lecture
- API request forged without valid session
- Replay attack (submit same attendance twice)

**Impact**: CRITICAL  
**Likelihood**: HIGH (if not properly implemented)

**Mitigations**:
1. ✅ **Authentication Required**: All attendance APIs require valid JWT
2. ✅ **Teacher Assignment Validation**: Backend checks teacher assigned to lecture
3. ✅ **Session State Validation**: Lecture must be in valid state (ACTIVE/SCANNING/REVIEW)
4. ✅ **Idempotency**: clientSubmissionId prevents duplicate submission
5. ✅ **Enrollment Validation**: All students must be enrolled
6. ✅ **Timestamp Validation**: Submission timestamp within reasonable window
7. ✅ **Audit Logging**: Complete audit trail of all attendance submissions

**Backend Validation**:
```java
@PostMapping("/lectures/{id}/confirm")
@PreAuthorize("hasRole('TEACHER')")
@Transactional
public ResponseEntity<AttendanceConfirmation> confirmAttendance(
    @PathVariable Long id,
    @RequestBody AttendanceSubmission submission,
    @AuthenticationPrincipal UserDetails user
) {
    // 1. Check idempotency
    if (submissionExists(submission.clientSubmissionId)) {
        return ResponseEntity.ok(getPreviousResult(submission.clientSubmissionId));
    }
    
    // 2. Validate teacher assignment
    if (!isTeacherAssigned(user.getUserId(), id)) {
        throw new UnauthorizedException();
    }
    
    // 3. Validate session state
    LectureSession session = getSession(id);
    if (session.status == FINALIZED || session.status == CANCELLED) {
        throw new InvalidSessionStateException();
    }
    
    // 4. Validate all students enrolled
    for (AttendanceCandidate candidate : submission.candidates) {
        if (!isStudentEnrolled(candidate.studentId, session.classId, session.subjectId)) {
            throw new ValidationException("Student not enrolled");
        }
    }
    
    // 5. Create attendance records
    List<AttendanceRecord> records = createRecords(submission);
    
    // 6. Audit log
    auditLog.log(AttendanceSubmitted(user, session, records));
    
    return ResponseEntity.ok(new AttendanceConfirmation(records));
}
```

**Implementation Status**: CRITICAL - Phase 7

---

#### THREAT-ATT-002: Attendance History Manipulation
**Category**: Tampering  
**Description**: Attacker modifies or deletes historical attendance records

**Attack Scenarios**:
- Direct database access (SQL injection)
- API vulnerability allows UPDATE/DELETE
- Modified client sends delete request

**Impact**: CRITICAL  
**Likelihood**: LOW (with proper architecture)

**Mitigations**:
1. ✅ **Immutable Records**: Attendance records never deleted (only corrected)
2. ✅ **Correction Audit Trail**: All corrections create AttendanceCorrection record
3. ✅ **No DELETE Endpoints**: No API endpoint deletes attendance
4. ✅ **Database Constraints**: Prevent direct deletion at DB level
5. ✅ **Audit Logging**: Immutable audit logs
6. ✅ **SQL Injection Prevention**: Parameterized queries only

**Database Design**:
```sql
-- Prevent direct deletion
CREATE RULE no_delete_attendance AS
ON DELETE TO attendance_records
DO INSTEAD NOTHING;

-- Audit table is append-only
CREATE RULE no_update_audit AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE no_delete_audit AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
```

**Implementation Status**: CRITICAL - Phase 4, 7

---

#### THREAT-ATT-003: Replay Attack
**Category**: Tampering  
**Description**: Attacker captures and replays valid attendance submission

**Attack Scenario**:
```
1. Teacher submits attendance for Lecture A (captured by attacker)
2. Attacker replays same request for Lecture B
3. If not prevented, creates duplicate/unauthorized attendance
```

**Impact**: HIGH  
**Likelihood**: LOW (with proper mitigation)

**Mitigations**:
1. ✅ **Idempotency Key**: clientSubmissionId must be unique
2. ✅ **Timestamp Validation**: Reject old submissions
3. ✅ **Nonce**: One-time token per submission (optional)
4. ✅ **Session Binding**: Bind submission to active session state

**Implementation Status**: Phase 7

---

### 3.4 Face Recognition Threats

#### THREAT-FACE-001: Photo Spoofing
**Category**: Spoofing  
**Description**: Attacker uses printed photo or phone screen to spoof attendance

**Attack Scenarios**:
- Student holds printed photo of another student
- Student shows photo on phone screen
- Photo from social media used for spoofing

**Impact**: HIGH  
**Likelihood**: MEDIUM

**Mitigations**:
1. ⚠️ **Liveness Detection**: Detect 2D vs 3D face (optional, resource-intensive)
2. ✅ **Teacher Review**: Teacher manually verifies suspicious detections
3. ✅ **Low Confidence Flagging**: Photo spoofs typically have lower confidence
4. ✅ **Multiple Angles**: Classroom scanning from different angles makes spoofing harder
5. ✅ **Audit Logging**: Log all face recognition results with confidence scores
6. ⚙️ **Manual Verification**: Teacher can manually verify student identity

**Current Stance**:
- **Phase 6**: No liveness detection (rely on teacher review)
- **Future**: Evaluate liveness detection if spoofing becomes problem

**Implementation Status**: Phase 6 (teacher review), Future (liveness detection)

---

#### THREAT-FACE-002: Face Embedding Theft
**Category**: Information Disclosure  
**Description**: Attacker steals face embeddings and uses for unauthorized recognition

**Attack Scenarios**:
- Man-in-the-middle captures embeddings during transmission
- Compromised device leaks embeddings from storage
- Database breach exposes embeddings

**Impact**: CRITICAL  
**Likelihood**: LOW (with proper mitigations)

**Mitigations**:
1. ✅ **Encrypted Transmission**: HTTPS + certificate pinning
2. ✅ **Encrypted Storage**: Database-level encryption (TDE)
3. ✅ **No Client Storage**: Embeddings not persisted on device
4. ✅ **Memory-Only Processing**: Embeddings cleared after session
5. ✅ **Access Control**: Strict API authorization for embedding access
6. ✅ **Audit Logging**: Log all embedding retrieval

**Implementation Status**: CRITICAL - Phase 6

---

#### THREAT-FACE-003: Unauthorized Face Registration
**Category**: Tampering  
**Description**: Attacker registers face for someone else

**Attack Scenarios**:
- Student A registers Student B's face under Student A's account
- Modified client bypasses enrollment validation
- API vulnerability allows unauthorized enrollment

**Impact**: CRITICAL  
**Likelihood**: MEDIUM

**Mitigations**:
1. ✅ **User Verification**: Only logged-in student can register own face
2. ✅ **Session Binding**: Face enrollment tied to authenticated session
3. ✅ **Rate Limiting**: Max 5 enrollments per day per student
4. ✅ **Admin Oversight**: Admins can view/invalidate enrollments
5. ✅ **Audit Logging**: Log all face registrations
6. ⚙️ **Optional**: In-person verification during enrollment (institutional policy)

**Backend Validation**:
```java
@PostMapping("/face/register")
@PreAuthorize("hasRole('STUDENT')")
public ResponseEntity<FaceProfile> registerFace(
    @RequestBody FaceRegistrationRequest request,
    @AuthenticationPrincipal UserDetails user
) {
    // Student can only register own face
    if (!user.getUserId().equals(request.studentId)) {
        throw new ForbiddenException("Cannot register face for another student");
    }
    
    // Rate limiting
    if (hasTooManyRecentEnrollments(user.getUserId())) {
        throw new RateLimitException("Too many enrollment attempts");
    }
    
    // Proceed with enrollment
    FaceProfile profile = faceService.registerFace(request);
    auditLog.log(FaceRegistered(user, profile));
    
    return ResponseEntity.ok(profile);
}
```

**Implementation Status**: CRITICAL - Phase 6

---

### 3.5 Denial of Service Threats

#### THREAT-DOS-001: API Rate Abuse
**Category**: Denial of Service  
**Description**: Attacker floods API with requests, degrading service

**Attack Scenarios**:
- Automated script floods login endpoint
- Modified client sends thousands of attendance queries
- Bot repeatedly registers/deletes face profiles

**Impact**: HIGH  
**Likelihood**: MEDIUM

**Mitigations**:
1. ✅ **Rate Limiting**: Per-user, per-IP, per-endpoint limits
2. ✅ **Request Throttling**: Gradual backoff for repeated requests
3. ✅ **Load Balancing**: Distribute load across multiple servers
4. ✅ **CAPTCHA**: For login, face enrollment after threshold
5. ✅ **WAF**: Web Application Firewall to detect/block malicious patterns
6. ✅ **Monitoring & Alerting**: Detect unusual traffic patterns

**Rate Limits** (configurable):
```
Login: 5 attempts per 15 minutes per user
General API: 200 requests per minute per user
Face enrollment: 5 per day per student
Attendance queries: 10 per day per student
File uploads: 10 per hour per user
```

**Implementation Status**: Phase 10 (Security Hardening)

---

#### THREAT-DOS-002: Resource Exhaustion
**Category**: Denial of Service  
**Description**: Attacker causes excessive resource consumption (CPU, memory, storage)

**Attack Scenarios**:
- Upload extremely large face images (100MB+)
- Submit attendance with thousands of students
- Create thousands of lecture sessions
- Generate excessive audit logs

**Impact**: MEDIUM  
**Likelihood**: LOW

**Mitigations**:
1. ✅ **File Size Limits**: Max 10MB per face image, max 5 images per enrollment
2. ✅ **Request Size Limits**: Max payload size enforced at API gateway
3. ✅ **Batch Size Limits**: Max attendance records per submission
4. ✅ **Query Pagination**: Enforce pagination on list endpoints
5. ✅ **Database Query Timeout**: Prevent runaway queries
6. ✅ **Connection Pooling**: Limit database connections
7. ✅ **Log Rotation**: Prevent log storage exhaustion

**Implementation Status**: Phase 2-3

---

### 3.6 Injection Threats

#### THREAT-INJ-001: SQL Injection
**Category**: Tampering + Information Disclosure  
**Description**: Attacker injects SQL commands through user input

**Attack Example**:
```
POST /api/students?name='; DROP TABLE students; --
```

**Impact**: CRITICAL  
**Likelihood**: LOW (with proper practices)

**Mitigations**:
1. ✅ **Parameterized Queries**: NEVER concatenate SQL strings
2. ✅ **ORM Framework**: Use JPA/Hibernate (prevents most SQL injection)
3. ✅ **Input Validation**: Validate all inputs at API layer
4. ✅ **Least Privilege**: Database user has minimal permissions
5. ✅ **WAF**: Filter malicious patterns

**Example (Secure)**:
```java
// SECURE: Parameterized query
@Query("SELECT s FROM Student s WHERE s.rollNumber = :rollNumber")
Student findByRollNumber(@Param("rollNumber") String rollNumber);

// INSECURE: String concatenation (NEVER DO THIS)
// String sql = "SELECT * FROM students WHERE roll_number = '" + rollNumber + "'";
```

**Implementation Status**: CRITICAL - All phases

---

#### THREAT-INJ-002: Path Traversal
**Category**: Information Disclosure  
**Description**: Attacker accesses files outside intended directory

**Attack Example**:
```
GET /api/face/images/../../etc/passwd
```

**Impact**: HIGH  
**Likelihood**: LOW (with proper mitigation)

**Mitigations**:
1. ✅ **No Direct File Access**: Use object storage keys, not file paths
2. ✅ **Path Validation**: Validate/sanitize all file paths
3. ✅ **Whitelist Approach**: Only allow specific file patterns
4. ✅ **Object Storage ACL**: S3/MinIO access control prevents direct file access

**Implementation Status**: Phase 6

---

### 3.7 Client-Side Threats

#### THREAT-CLIENT-001: Modified Android Client
**Category**: Elevation of Privilege + Tampering  
**Description**: Attacker modifies Android APK to bypass restrictions

**Attack Scenarios**:
- Remove client-side validation
- Bypass rate limiting
- Inject fake face recognition results
- Submit attendance without teacher review

**Impact**: MEDIUM (if backend properly validates)  
**Likelihood**: HIGH (Android apps can be decompiled)

**Mitigations**:
1. ✅ **Server-Side Validation**: NEVER trust client
2. ✅ **Backend Authorization**: Validate all operations server-side
3. ✅ **Certificate Pinning**: Detect MITM attacks
4. ✅ **Code Obfuscation**: ProGuard/R8 (minor deterrent)
5. ⚙️ **Root Detection** (optional): Detect rooted devices
6. ⚙️ **App Signing**: Verify app signature (Google Play)

**Architecture Decision**:
```
CLIENT-SIDE VALIDATION = UX IMPROVEMENT ONLY
SERVER-SIDE VALIDATION = SECURITY BOUNDARY
```

**Implementation Status**: All phases - backend validation is critical

---

#### THREAT-CLIENT-002: Insecure Data Storage
**Category**: Information Disclosure  
**Description**: Sensitive data stored insecurely on device

**Attack Scenarios**:
- JWT token stored in SharedPreferences (plaintext)
- Face images cached on disk
- Biometric data not encrypted

**Impact**: HIGH  
**Likelihood**: MEDIUM

**Mitigations**:
1. ✅ **Android Keystore**: Store JWT in encrypted storage
2. ✅ **No Face Image Caching**: Never persist face images on device
3. ✅ **Memory-Only Embeddings**: Embeddings cleared after session
4. ✅ **Encrypted SharedPreferences**: For non-sensitive data
5. ✅ **No Logs**: Never log sensitive data (tokens, passwords, biometrics)

**Implementation Status**: Phase 2, 6

---

## 4. Compliance & Legal

### 4.1 Data Protection Requirements

**Status**: 🔴 REQUIRES STAKEHOLDER INPUT

The following must be determined:

| Requirement | Status | Notes |
|-------------|--------|-------|
| Applicable privacy laws | ⚠️ UNKNOWN | GDPR? CCPA? India PDPA? Local laws? |
| Biometric data consent | ⚠️ REQUIRED | Must obtain explicit student consent |
| Data retention period | ⚠️ UNKNOWN | How long to keep face data after graduation? |
| Right to deletion | ⚠️ LIKELY REQUIRED | Student can request biometric data deletion |
| Data breach notification | ⚠️ DEPENDS ON JURISDICTION | Must have incident response plan |
| Parental consent (minors) | ⚠️ DEPENDS ON JURISDICTION | If students are minors |

**Actions Required**:
1. Legal review of applicable laws
2. Draft data protection policy
3. Create student consent form
4. Implement data deletion workflow
5. Establish incident response plan

---

## 5. Security Controls Summary

### 5.1 Implemented Controls (Planned)

| Control | Phase | Priority |
|---------|-------|----------|
| HTTPS/TLS enforcement | Phase 2 | CRITICAL |
| JWT authentication | Phase 2 | CRITICAL |
| Role-based access control | Phase 2 | CRITICAL |
| Server-side authorization | All phases | CRITICAL |
| Parameterized SQL queries | All phases | CRITICAL |
| Input validation | All phases | CRITICAL |
| Rate limiting | Phase 10 | HIGH |
| Audit logging | All phases | HIGH |
| Password hashing (bcrypt/argon2) | Phase 2 | CRITICAL |
| Certificate pinning (Android) | Phase 2 | HIGH |
| Secure token storage (Keystore) | Phase 2 | HIGH |
| Biometric data encryption | Phase 6 | CRITICAL |
| Idempotency protection | Phase 7 | HIGH |
| Session state validation | Phase 6-7 | HIGH |

### 5.2 Optional Controls (Future)

| Control | Rationale |
|---------|-----------|
| Two-Factor Authentication | Optional for admin accounts |
| Liveness Detection | If photo spoofing becomes problem |
| Root/Jailbreak Detection | Optional security enhancement |
| Runtime Application Self-Protection (RASP) | Advanced threat protection |
| Anomaly Detection | ML-based threat detection |

---

## 6. Incident Response Plan

### 6.1 Security Incident Categories

| Severity | Examples | Response Time |
|----------|----------|---------------|
| **CRITICAL** | Data breach, unauthorized admin access | Immediate (< 1 hour) |
| **HIGH** | Compromised teacher account, SQL injection attempt | < 4 hours |
| **MEDIUM** | Failed login spikes, API abuse | < 24 hours |
| **LOW** | Minor security misconfiguration | < 1 week |

### 6.2 Incident Response Steps

1. **Detection**: Monitoring/alerting detects anomaly
2. **Containment**: Isolate affected systems
3. **Analysis**: Determine scope and impact
4. **Eradication**: Remove threat
5. **Recovery**: Restore normal operations
6. **Lessons Learned**: Update security controls

### 6.3 Breach Notification

If biometric data breach occurs:
1. Notify affected students within 72 hours (GDPR requirement, if applicable)
2. Notify regulatory authorities per local requirements
3. Provide breach details and mitigation steps
4. Offer identity protection services if needed

---

## 7. Penetration Testing Plan

### 7.1 Phase 10 Testing Scope

**Before Production**:
- [ ] Authentication & authorization testing
- [ ] SQL injection testing
- [ ] XSS testing (if web interface)
- [ ] CSRF testing
- [ ] API fuzzing
- [ ] Rate limiting testing
- [ ] Session management testing
- [ ] File upload testing
- [ ] Privilege escalation testing
- [ ] Biometric data protection testing

### 7.2 Ongoing Security

- **Quarterly**: Vulnerability scanning
- **Annually**: Full penetration test
- **Continuous**: Dependency vulnerability scanning (Dependabot, Snyk)
- **Continuous**: SAST (Static Application Security Testing)

---

## 8. Open Security Decisions

| Decision | Status | Notes |
|----------|--------|-------|
| Two-factor authentication | ⚙️ CONFIGURABLE | Recommended for admins |
| Password expiry policy | 📝 ASSUMPTION | No expiry (modern practice) |
| Liveness detection | 🔮 DEFERRED | Evaluate in pilot |
| Root detection | ⚙️ CONFIGURABLE | Optional security layer |
| API key authentication (future) | 🔮 DEFERRED | If integrations needed |
| Biometric retention period | 📋 REQUIRES STAKEHOLDER INPUT | Legal review required |

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | Security Architect | Initial threat model |

**Status**: Security Architecture Complete  
**Next**: Review with security team, implement controls phase-by-phase
