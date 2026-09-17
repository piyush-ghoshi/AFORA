# Stakeholder Questionnaire
## Smart Classroom Attendance Management System

**Purpose**: Resolve critical open decisions before Phase 1 implementation  
**Date**: August 25, 2026  
**Status**: Awaiting Stakeholder Response  
**Priority**: CRITICAL - Required for Phase 1-3 Implementation

---

## Instructions for ChatGPT / Stakeholders

This document contains **critical questions** that must be answered before proceeding to Phase 1 implementation. These decisions affect:

- Core business logic
- Database schema design
- System behavior and validation rules
- User workflows
- Compliance and legal requirements

**Please review each section carefully and provide clear, definitive answers.**

---

## 📋 Section 1: Academic Structure & Calendar (CRITICAL)

### Q1.1: Academic Calendar Structure
**Decision ID**: OQ-001  
**Impact**: Affects semester management, timetable, attendance calculations  
**Phase Impact**: Phase 3 (Academic Management)

**Question**: What is your institution's academic calendar structure?

**Options**:
- [ ] **Semester System** (2 semesters per year: Fall/Spring or Odd/Even)
- [ ] **Trimester System** (3 terms per year)
- [ ] **Quarter System** (4 quarters per year)
- [ ] **Other** (Please specify): _______________

**Follow-up Questions**:
- How many weeks per semester/term/quarter? _______________
- What are the typical start/end months? _______________
- Are there summer sessions? [ ] Yes [ ] No
- If yes, are summer sessions tracked separately? [ ] Yes [ ] No

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]




```

---

### Q1.2: Academic Year Designation
**Impact**: Database structure, reporting, historical data

**Question**: How do you designate academic years?

**Options**:
- [ ] Single year (e.g., "2024")
- [ ] Year range (e.g., "2024-2025")
- [ ] Custom format (Please specify): _______________

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]


```

---

### Q1.3: Class/Section Structure
**Impact**: Database relationships, student enrollment, timetable

**Question**: How are students organized?

**Example Structure**:
```
Department → Program → Batch → Year → Section
CSE → B.Tech CSE → 2023 Batch → 3rd Year → Section A
```

**Please describe your institution's hierarchy**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]






```

**Follow-up**:
- Average students per section: _______________
- Maximum section capacity: _______________
- Do students remain in same section for entire program? [ ] Yes [ ] No

---

## 📊 Section 2: Attendance Business Rules (CRITICAL)

### Q2.1: Attendance Percentage Calculation Method
**Decision ID**: OQ-002  
**Impact**: Core business logic, attendance percentage shown to students  
**Phase Impact**: Phase 4 (Attendance Engine)

**Question**: How should attendance percentage be calculated?

**Options**:

**Option A - Simple Calculation**:
```
Attendance % = (Present) / (Total Lectures) × 100
```
- Example: 40 present out of 50 lectures = 80%
- Absent and On-Leave both count against percentage

**Option B - Exclude Approved Leave**:
```
Attendance % = (Present) / (Total Lectures - On Leave) × 100
```
- Example: 40 present, 5 on leave, 5 absent out of 50 lectures
- Attendance % = 40 / (50 - 5) × 100 = 88.89%
- Approved leave doesn't count against student

**Option C - Treat Late as Partial**:
```
Attendance % = (Present + 0.5 × Late) / (Total Lectures) × 100
```
- Example: 38 present, 4 late, 8 absent = (38 + 2) / 50 = 80%
- Late attendance counts as 50%

**Your Selection**: [ ] Option A [ ] Option B [ ] Option C

**If different formula, please specify**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]


```

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]


```

---

### Q2.2: Low Attendance Thresholds
**Decision ID**: AR-001  
**Impact**: Student warnings, UI indicators, notifications  
**Phase Impact**: Phase 4 (Attendance Engine), Phase 8 (Analytics)

**Question**: What attendance percentage thresholds should trigger warnings?

**Common Academic Standard**:
- **SAFE** (Green): ≥ 75%
- **WARNING** (Amber): 65-74%
- **CRITICAL** (Red): < 65%

**Your Institution's Thresholds**:

| Status | Threshold | Color Indicator |
|--------|-----------|-----------------|
| SAFE | ≥ ____% | Green |
| WARNING | ____% to ____% | Amber |
| CRITICAL | < ____% | Red |

**Follow-up Questions**:
- Are these thresholds the same for all subjects? [ ] Yes [ ] No
- If no, do they vary by:
  - [ ] Subject type (theory vs lab)
  - [ ] Department
  - [ ] Program (UG vs PG)
  - [ ] Other: _______________

- Should students be **barred from exams** if below threshold? [ ] Yes [ ] No
- If yes, at what percentage? _____%

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]






```

---

### Q2.3: Attendance Correction Time Limit
**Decision ID**: AR-002  
**Impact**: Attendance query workflow, data integrity  
**Phase Impact**: Phase 7 (Leave & Query Management)

**Question**: How long after a lecture can students dispute attendance?

**Options**:
- [ ] **24 hours** after lecture
- [ ] **48 hours** after lecture (Recommended)
- [ ] **1 week** after lecture
- [ ] **Until semester end** (no time limit)
- [ ] **Other**: _______________ (Please specify)

**Follow-up**:
- Should emergency/medical cases have **extended time limit**? [ ] Yes [ ] No
- If yes, how many days? _______________

**Rationale for Recommendation**: 48 hours balances:
- Students need time to notice incorrect attendance
- Too long reduces proof reliability and memory accuracy
- Prevents end-of-semester bulk disputes

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]




```

---

### Q2.4: Lecture Cancellation Handling
**Decision ID**: OQ-005  
**Impact**: Attendance calculation accuracy  
**Phase Impact**: Phase 4 (Attendance Engine)

**Question**: How should cancelled lectures be handled in attendance calculations?

**Options**:
- [ ] **Option A**: Exclude from total lecture count (don't count cancelled lectures)
- [ ] **Option B**: Mark all students as Present for cancelled lectures
- [ ] **Option C**: Mark as "N/A" (separate status, not counted)
- [ ] **Option D**: Other approach (Please specify): _______________

**Example Scenario**:
```
Total scheduled lectures: 50
Cancelled lectures: 3
Student actually attended: 42 out of 47 conducted lectures

Option A: 42/47 = 89.36%
Option B: 45/50 = 90%
Option C: 42/(50-3) = 89.36% (same as A)
```

**Your Selection**: [ ] Option A [ ] Option B [ ] Option C [ ] Option D

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]




```

---

## 🏥 Section 3: Leave Management Policies (HIGH PRIORITY)

### Q3.1: Leave Request Advance Notice
**Decision ID**: AR-003  
**Impact**: Leave application workflow, validation rules  
**Phase Impact**: Phase 7 (Leave & Query Management)

**Question**: How far in advance must students request leave?

**Options**:
- [ ] **No minimum** (same-day allowed for all leave types)
- [ ] **1 day advance** for planned leave
- [ ] **3 days advance** for planned leave
- [ ] **1 week advance** for planned leave
- [ ] **Depends on leave type** (different rules for different categories)

**If depends on leave type, please specify**:

| Leave Type | Advance Notice | Documentation Required? |
|------------|----------------|-------------------------|
| Medical | _______ days | [ ] Yes [ ] No |
| Personal | _______ days | [ ] Yes [ ] No |
| Emergency | _______ days | [ ] Yes [ ] No |
| Religious/Festival | _______ days | [ ] Yes [ ] No |
| Other: _______ | _______ days | [ ] Yes [ ] No |

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]






```

---

### Q3.2: Retroactive Leave Application
**Decision ID**: AR-004  
**Impact**: Leave workflow complexity, abuse prevention  
**Phase Impact**: Phase 7 (Leave & Query Management)

**Question**: Can students apply for leave AFTER the lecture has occurred?

**Options**:
- [ ] **No retroactive leave** (must apply before lecture)
- [ ] **Retroactive allowed with medical certificate** (doctor's note required)
- [ ] **Retroactive allowed within 24 hours** (with any valid reason)
- [ ] **Teacher discretion** (teacher decides case-by-case)
- [ ] **Other**: _______________ (Please specify)

**Follow-up**:
- Maximum days backward for retroactive leave? _______________
- Required documentation for retroactive:
  - [ ] Medical certificate (for medical leave)
  - [ ] Written explanation (for all retroactive leave)
  - [ ] Parent/guardian signature
  - [ ] Other: _______________

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]





```

---

### Q3.3: Leave Approval Authority
**Impact**: Workflow routing, authorization rules

**Question**: Who can approve leave requests?

**Options**:
- [ ] **Subject teacher only** (for specific subject leave)
- [ ] **Class coordinator** (for multi-day/multi-subject leave)
- [ ] **Department HOD** (for extended leave)
- [ ] **Hierarchical** (depends on duration - specify below)
- [ ] **Any of above** (flexible approval)

**If hierarchical, please specify**:
```
Single day: Approved by _______________
2-3 days: Approved by _______________
4-7 days: Approved by _______________
More than 7 days: Approved by _______________
```

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]






```

---

## 👨‍🏫 Section 4: Teacher Assignment & Timetable (HIGH PRIORITY)

### Q4.1: Teacher Multiple Section Assignment
**Decision ID**: OQ-003  
**Impact**: Database constraints, teacher assignment validation  
**Phase Impact**: Phase 3 (Academic Management)

**Question**: Can a teacher be assigned to multiple sections of the same subject?

**Example**: Can Prof. Sharma teach "DBMS" to both CSE-3A and CSE-3B?

- [ ] **Yes** - Teachers can teach same subject to multiple sections
- [ ] **No** - Each subject-section combination must have unique teacher
- [ ] **Depends** (Please explain): _______________

**Follow-up**:
- Maximum sections a teacher can handle per subject: _______________
- Maximum total sections per teacher (across all subjects): _______________

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]




```

---

### Q4.2: Lecture Duration & Slots
**Impact**: Timetable structure, attendance session timing

**Question**: What are your standard lecture durations?

**Common Patterns**:
```
Theory: 60 minutes
Lab: 120 minutes (2 hours)
Tutorial: 45 minutes
```

**Your Institution's Pattern**:
```
Theory lectures: _______ minutes
Lab sessions: _______ minutes
Tutorial sessions: _______ minutes
Other (specify): _______ minutes
```

**Typical daily schedule**:
```
First lecture starts at: _______
Last lecture ends at: _______
Break timings: _______
Total lecture slots per day: _______
```

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]







```

---

## 👤 Section 5: Student Class Changes (MEDIUM PRIORITY)

### Q5.1: Mid-Semester Section Changes
**Decision ID**: OQ-004  
**Impact**: Attendance history, enrollment management  
**Phase Impact**: Phase 3 (Academic Management)

**Question**: Can students change sections mid-semester?

- [ ] **Yes** - Students can change sections
- [ ] **No** - Students remain in assigned section for entire semester
- [ ] **Rarely** - Only under exceptional circumstances

**If YES, how should attendance history be handled?**:

**Options**:
- [ ] **Option A**: Transfer all attendance history to new section
- [ ] **Option B**: Archive old attendance, start fresh in new section
- [ ] **Option C**: Maintain separate attendance records per section
- [ ] **Option D**: Other (Please specify): _______________

**Example Scenario**:
```
Student: John Doe
Original: CSE-3A (attended 20/25 lectures = 80%)
Transfers to: CSE-3B (after 25 lectures in semester)

Option A: John starts in CSE-3B with 20/25 attendance (80%)
Option B: John starts in CSE-3B with 0/0 attendance (fresh start)
Option C: Two separate records maintained
```

**Your Selection**: [ ] Option A [ ] Option B [ ] Option C [ ] Option D

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]






```

---

## 🔐 Section 6: Security & Privacy (CRITICAL)

### Q6.1: Password Policy
**Decision ID**: SE-001  
**Impact**: User authentication, security level  
**Phase Impact**: Phase 2 (Backend Foundation)

**Question**: What password strength requirements should be enforced?

**Recommended (Moderate)**:
- Minimum length: 8 characters
- Require uppercase: Yes
- Require lowercase: Yes
- Require number: Yes
- Require special character: Optional
- Password expiry: No (modern security practice)
- Password history: Last 3 passwords

**Your Requirements**:
```
Minimum length: _______ characters
Require uppercase: [ ] Yes [ ] No
Require lowercase: [ ] Yes [ ] No
Require number: [ ] Yes [ ] No
Require special character: [ ] Yes [ ] No
Dictionary word check: [ ] Yes [ ] No
Password expiry: [ ] Yes [ ] No
If yes, every _______ days
Password history: Last _______ passwords
```

**Additional Security**:
- [ ] Account lockout after _______ failed attempts
- [ ] Lockout duration: _______ minutes
- [ ] Two-factor authentication for admins: [ ] Yes [ ] No

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]







```

---

### Q6.2: Biometric Data Retention Period
**Decision ID**: FR-004  
**Impact**: Data privacy, GDPR compliance, storage costs  
**Phase Impact**: Phase 6 (Face Recognition Integration)  
**Legal Review Required**: ⚠️ YES

**Question**: How long should student face data be retained after graduation?

**Options**:
- [ ] **Delete immediately** upon graduation
- [ ] **1 year** after graduation
- [ ] **3 years** after graduation (matches typical attendance record retention)
- [ ] **Until student requests deletion** (GDPR right to be forgotten)
- [ ] **Other**: _______________ (Please specify)

**Legal Considerations**:
- Face images and embeddings are **biometric data**
- Subject to GDPR (EU), CCPA (California), and local privacy laws
- Requires explicit student consent
- Students have right to access, correct, and delete their data

**Follow-up Questions**:
- Have you consulted legal counsel on biometric data? [ ] Yes [ ] No [ ] Pending
- Do you have a student consent form prepared? [ ] Yes [ ] No [ ] Pending
- Are there local/regional laws on biometric data? [ ] Yes [ ] No [ ] Unknown

**Response** (Consult legal before answering):
```
[PLEASE FILL IN YOUR RESPONSE HERE]






```

---

### Q6.3: API Rate Limiting
**Decision ID**: SE-002  
**Impact**: Security, abuse prevention, system stability  
**Phase Impact**: Phase 10 (Security Hardening)

**Question**: What rate limits should be enforced?

**Recommended Values**:

| Action | Rate Limit | Reason |
|--------|-----------|--------|
| Login attempts | 5 per 15 minutes per user | Prevent brute force |
| General API calls | 200 per minute per user | Allow normal usage |
| Face enrollment | 5 per day per student | Prevent spam enrollment |
| Attendance queries | 10 per day per student | Prevent query spam |

**Your Values** (Leave blank to use recommended):

| Action | Rate Limit |
|--------|-----------|
| Login attempts | _______ per _______ minutes |
| General API calls | _______ per minute |
| Face enrollment | _______ per day |
| Attendance queries | _______ per day |

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]




```

---

## 📱 Section 7: System Configuration (MEDIUM PRIORITY)

### Q7.1: Student Mobile App Requirement
**Decision ID**: FS-002  
**Impact**: Development scope, app complexity, timeline  
**Phase Impact**: All phases

**Question**: Should students have a mobile app or web-only access?

**Options**:
- [ ] **Option A**: Same Android app for students and teachers (role-based UI)
- [ ] **Option B**: Web-only for students (responsive web app)
- [ ] **Option C**: Separate Android app for students
- [ ] **Option D**: Both Android app and web for students

**Considerations**:

**Option A (Recommended)**:
- ✅ Shared KMP code (same codebase for both roles)
- ✅ Push notifications for students
- ✅ Better mobile UX
- ⚠️ Larger app size

**Option B**:
- ✅ Simpler development
- ✅ No app installation needed
- ⚠️ No push notifications
- ⚠️ Weaker mobile UX

**Your Selection**: [ ] Option A [ ] Option B [ ] Option C [ ] Option D

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]




```

---

### Q7.2: Notification Delivery Methods
**Decision ID**: SC-003  
**Impact**: User engagement, notification reliability, operational costs  
**Phase Impact**: Phase 9 (Notifications)

**Question**: How should notifications be delivered?

**Options** (Select all that apply):
- [ ] **In-app notifications** (always shown when user opens app)
- [ ] **Push notifications** (Android notification tray)
- [ ] **Email notifications**
- [ ] **SMS notifications** (additional cost)

**Recommended**: In-app + Push + Email (for critical notifications)

**Follow-up**:
Which notifications are **critical** (deserve email/SMS)?
- [ ] Leave approved/rejected
- [ ] Attendance query approved/rejected
- [ ] Attendance marked
- [ ] Low attendance warning
- [ ] Lecture cancelled
- [ ] Other: _______________

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]





```

---

### Q7.3: Session Timeout Duration
**Decision ID**: SC-001  
**Impact**: User experience, security  
**Phase Impact**: Phase 2 (Backend Foundation)

**Question**: How long before user session expires?

**Options**:
- [ ] **30 minutes** (high security, frequent re-login)
- [ ] **1 hour** (balanced - recommended)
- [ ] **2 hours**
- [ ] **8 hours** (full academic day)
- [ ] **Other**: _______________ (Please specify)

**Note**: We'll implement JWT with access token + refresh token, allowing seamless renewal without user noticing.

**Your Selection**: [ ] 30 min [ ] 1 hour [ ] 2 hours [ ] 8 hours [ ] Other: _______

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]


```

---

## 🏗️ Section 8: Infrastructure & Deployment (MEDIUM PRIORITY)

### Q8.1: Hosting Environment
**Impact**: Deployment architecture, costs, operations

**Question**: Where will the system be hosted?

**Options**:
- [ ] **On-premise** (institution's own servers)
- [ ] **Cloud** (AWS, Azure, Google Cloud)
- [ ] **Hybrid** (some on-premise, some cloud)
- [ ] **Not decided yet**

**If cloud, which provider?**:
- [ ] AWS
- [ ] Azure
- [ ] Google Cloud
- [ ] Other: _______________

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]




```

---

### Q8.2: Object Storage for Face Images
**Impact**: Face image storage, costs

**Question**: Where should face images be stored?

**Options**:
- [ ] **AWS S3** (if using AWS)
- [ ] **Azure Blob Storage** (if using Azure)
- [ ] **Google Cloud Storage** (if using GCP)
- [ ] **Self-hosted MinIO** (on-premise S3-compatible storage)
- [ ] **Not decided yet**

**Estimated Storage**:
```
Students: 10,000
Images per student: 5
Average image size: 2 MB
Total storage: ~100 GB

Growth per year: ~20 GB
```

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]




```

---

### Q8.3: Deployment Environments
**Decision ID**: DO-001  
**Impact**: Development workflow, testing, risk mitigation  
**Phase Impact**: Phase 1-2 setup

**Question**: How many environments should we maintain?

**Options**:
- [ ] **Production only** (⚠️ risky, not recommended)
- [ ] **Staging + Production** (minimum recommended)
- [ ] **Development + Staging + Production** (recommended)
- [ ] **Development + QA + Staging + Production** (enterprise-grade)

**Recommended**: Development + Staging + Production

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]



```

---

## 🎯 Section 9: Scope Confirmation (HIGH PRIORITY)

### Q9.1: Parent Portal
**Decision ID**: FS-001  
**Impact**: Scope, timeline, development effort  
**Phase Impact**: Scope definition

**Question**: Should parents have access to view their child's attendance?

**Options**:
- [ ] **Yes - Include in initial release** (adds 3-4 weeks to timeline)
- [ ] **Yes - Add in Phase 2** (after initial release)
- [ ] **No - Not planned**
- [ ] **Not decided yet**

**If YES, what should parents see?**:
- [ ] Overall attendance percentage
- [ ] Subject-wise attendance
- [ ] Lecture-wise attendance details
- [ ] Leave requests and status
- [ ] Attendance queries and status
- [ ] Notifications
- [ ] Ability to apply for leave on behalf of student
- [ ] Other: _______________

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]





```

---

### Q9.2: Feature Priority
**Impact**: Development sequence, resource allocation

**Question**: Rank these features by priority (1 = highest priority)

**Core Features**:
```
___ Manual attendance (Phase 4)
___ Camera-based attendance (Phase 6)
___ Leave management (Phase 7)
___ Attendance queries/disputes (Phase 7)
___ Analytics and trends (Phase 8)
___ Leaderboard (Phase 9)
___ Notifications (Phase 9)
```

**Optional Features** (Would you like these?):
```
[ ] Yes [ ] No - Student timetable view
[ ] Yes [ ] No - Teacher timetable view
[ ] Yes [ ] No - Low attendance email alerts to students
[ ] Yes [ ] No - Monthly attendance reports (PDF export)
[ ] Yes [ ] No - Bulk operations (bulk enrollment, bulk assignment)
[ ] Yes [ ] No - Academic calendar management (holidays, exam schedules)
[ ] Yes [ ] No - Attendance reports for administration
[ ] Yes [ ] No - Integration with existing student information system
```

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]









```

---

## 📅 Section 10: Timeline & Resources

### Q10.1: Target Launch Date
**Impact**: Project planning, resource allocation, phase scheduling

**Question**: When do you need this system operational?

**Estimated Timeline** (from approval to pilot):
- Phase 1-10: 24-28 weeks (~6-7 months)
- Pilot: 3-4 weeks
- **Total: ~7-8 months from approval**

**Your Target**:
```
Desired pilot start date: _______________
Desired full rollout date: _______________
Critical date (cannot miss): _______________
```

**Constraints**:
```
[Are there any specific semester/academic year milestones we must hit?]




```

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]






```

---

### Q10.2: Team & Resources
**Impact**: Development capacity, timeline feasibility

**Question**: What development team is available?

**Team Members Available**:
```
Backend developers: _______
Android developers: _______
ML/AI engineers: _______
UI/UX designers: _______
QA engineers: _______
DevOps engineers: _______
```

**Team Experience**:
- Kotlin experience: [ ] Expert [ ] Intermediate [ ] Beginner [ ] None
- Kotlin Multiplatform experience: [ ] Expert [ ] Intermediate [ ] Beginner [ ] None
- Spring Boot experience: [ ] Expert [ ] Intermediate [ ] Beginner [ ] None
- Face recognition experience: [ ] Expert [ ] Intermediate [ ] Beginner [ ] None

**Budget Constraints**:
- Cloud hosting budget: $_______ / month
- External tools/services budget: $_______ / month
- Training budget: $_______

**Response**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]









```

---

## ✅ Section 11: Final Confirmation

### Q11.1: Architecture Approval
**Question**: Do you approve the proposed architecture?

**Architecture Summary**:
- ✅ Kotlin Multiplatform (Android-first, iOS-ready)
- ✅ Modular monolith backend (Spring Boot)
- ✅ PostgreSQL with pgvector
- ✅ On-device face recognition
- ✅ AI-assisted, teacher-validated attendance
- ✅ Manual fallback always available

**Approval**: [ ] Approved [ ] Approved with changes [ ] Needs discussion

**If changes needed, please describe**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]



```

---

### Q11.2: Proceed to Phase 1?
**Question**: Based on this questionnaire, are you ready to proceed to Phase 1 (Repository & Foundation)?

- [ ] **Yes - Proceed immediately**
- [ ] **Yes - After follow-up meeting to discuss specific items**
- [ ] **No - More information needed**
- [ ] **No - Need to revisit requirements**

**If follow-up needed, which topics?**:
```
[PLEASE FILL IN YOUR RESPONSE HERE]




```

---

## 📊 Response Summary

**Please complete this questionnaire and return it to the development team.**

**Completed By**:
```
Name: _______________________________
Title: _______________________________
Department: _______________________________
Email: _______________________________
Date: _______________________________
```

**Review Status**:
```
[ ] Reviewed by Academic Administration
[ ] Reviewed by IT Department
[ ] Reviewed by Legal Counsel (for biometric data questions)
[ ] Reviewed by Security Officer
[ ] Final approval by: _______________________________
```

---

## 📎 Attachments Needed

Please provide the following documents if available:

- [ ] Institution's attendance policy document
- [ ] Academic calendar for current/upcoming year
- [ ] Biometric data consent form (if exists)
- [ ] Existing student information system documentation (if integration needed)
- [ ] IT infrastructure details (servers, network, etc.)

---

## 🔄 Next Steps After Submission

Once this questionnaire is completed:

1. **Development team reviews responses** (1-2 days)
2. **Update decision log** with resolved decisions
3. **Schedule kickoff meeting** if needed
4. **Obtain final approval** to proceed to Phase 1
5. **Begin Phase 1 implementation** (Repository & Foundation)

**Questions?** Contact: [Development Team Lead Email]

---

**Document Version**: 1.0  
**Last Updated**: August 25, 2026  
**Status**: ⏳ Awaiting Stakeholder Response
