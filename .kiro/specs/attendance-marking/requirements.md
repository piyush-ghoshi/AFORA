# Requirements Document

## Introduction

Teacher-driven **manual** attendance marking for AFORA. A teacher opens one of their own scheduled lecture sessions, sees the roster of actively enrolled students, sets a status for each student, and confirms. Confirmation persists one attendance record per student, finalizes the session, and refreshes the session's denormalised counters.

The data layer this feature writes against already exists and is live in PostgreSQL through migrations V5–V8: `lecture_sessions`, `attendance_records` (already unique on `(lecture_session_id, student_id)`), `enrollments`, `leave_requests`, plus the `LectureSessionRepository`, `AttendanceRecordRepository`, `EnrollmentRepository`, `LeaveRequestRepository`, `TimetableSlotRepository`, `ClassSectionRepository` and `SemesterRepository` Spring Data interfaces. This spec adds the service, controller, shared Kotlin Multiplatform contract and Android screen on top of that foundation. No new session or enrollment tables are in scope.

This spec also resolves eight decisions that the existing schema left open (session status transitions, amendment of finalized sessions, defaulting of unmarked students, idempotency, interaction with approved leave, the marking window, attendance-percentage denominator stability, and counter consistency) and corrects a known type defect in the shared `AttendanceRepository` stub.

### In Scope

- Reading a session roster from ACTIVE enrollments.
- Activating, confirming and cancelling a lecture session through the manual flow.
- Persisting attendance records with `method = MANUAL`.
- Amending a finalized session with an audit trail.
- Idempotent confirmation keyed on a client-generated submission identifier.
- Correcting the shared KMP attendance contract.
- The Android teacher marking screen.

### Out of Scope

- Camera capture, face detection and face recognition. The later roadmap phase owns those. This spec keeps the `AttendanceMethod` enum intact with `CAMERA`/`MANUAL`/`HYBRID` and keeps all nine `SessionStatus` values so the camera flow can land without a schema or enum change.
- Creating or regenerating lecture sessions from timetable slots. Sessions already exist.
- Leave request submission and review. Only the *reading* of already-APPROVED leave is in scope.
- Student-facing attendance history screens, notifications and attendance queries.

## Glossary

- **Attendance_Controller**: The Spring `@RestController` mapped at `/attendance`, which the `/api` server context path exposes as `/api/attendance/...`.
- **Attendance_Service**: The Spring `@Service` holding all attendance marking business logic, called only by the Attendance_Controller.
- **Dashboard_Service**: The existing `com.afora.backend.dashboard.service.DashboardService`.
- **Lecture_Session**: A row of `lecture_sessions`; one dated occurrence of a class.
- **Class_Section**: A row of `class_sections`; carries the owning teacher.
- **Enrollment**: A row of `enrollments`, carrying an `EnrollmentStatus` of ACTIVE, DROPPED or COMPLETED.
- **Attendance_Record**: A row of `attendance_records`; one student's outcome for one Lecture_Session.
- **AttendanceStatus**: The backend enum with values PRESENT, ABSENT, LATE, ON_LEAVE and EXCUSED. PRESENT and LATE satisfy `countsAsAttended`; ON_LEAVE and EXCUSED satisfy `isNeutral`.
- **SessionStatus**: The nine-value enum SCHEDULED, CREATED, ACTIVE, SCANNING, REVIEW, FINALIZING, FINALIZED, CANCELLED, FAILED.
- **LeaveRequest**: A row of `leave_requests`, carrying a `LeaveStatus` of PENDING, APPROVED, REJECTED or CANCELLED.
- **Manual_Marking_Flow**: The teacher-driven marking path this spec defines, distinguished from the later camera flow.
- **Roster**: The ordered list of Roster_Entry values the Attendance_Service returns for one Lecture_Session.
- **Roster_Entry**: One student's row within a Roster.
- **Suggested_Status**: An advisory AttendanceStatus the Attendance_Service places on a Roster_Entry, which the teacher may replace.
- **Attendance_Confirmation**: The response the Attendance_Service returns after persisting a confirmation.
- **Submission_Record**: A persisted row keyed on a client-generated submission identifier, holding the stored Attendance_Confirmation for replay.
- **Request_Fingerprint**: A SHA-256 digest over a confirmation request's session identifier and submitted entries sorted by student identifier.
- **Attendance_Amendment**: A change to one or more Attendance_Records belonging to a FINALIZED Lecture_Session.
- **Attendance_Audit_Entry**: A persisted record of one Attendance_Amendment.
- **Shared_Attendance_Repository**: The `commonMain` interface `com.afora.shared.data.repository.AttendanceRepository`.
- **Http_Attendance_Repository**: The `androidMain` Ktor-backed implementation of Shared_Attendance_Repository.
- **Shared_Serializer**: The `kotlinx.serialization` JSON serializer configured on the shared Ktor `ApiClient`.
- **Mark_Attendance_Screen**: The Jetpack Compose screen on which a teacher marks a Roster.
- **Mark_Attendance_ViewModel**: The Hilt-injected `ViewModel` backing the Mark_Attendance_Screen.
- **Mark_Attendance_UiState**: The sealed UI state class with Loading, Success and Error cases.
- **Teacher**: A caller whose `User.role` is TEACHER.
- **Owning_Teacher**: A Teacher whose own `teacher.id` equals the `teacher_id` of a Lecture_Session's Class_Section.

## Resolved Decisions

The eight questions the existing schema left open, and where each decision is made testable.

| # | Question | Decision | Requirement |
|---|---|---|---|
| 1 | Which SessionStatus transitions are legal? | Exactly four: SCHEDULED→ACTIVE, SCHEDULED→CANCELLED, ACTIVE→CANCELLED, ACTIVE→FINALIZED. CREATED, SCANNING, REVIEW, FINALIZING and FAILED stay unwritten and reserved for the camera flow; all nine enum values are retained. Only the Owning_Teacher or an ADMIN may trigger a transition. | 3 |
| 2 | May a FINALIZED session be amended? | Yes. The Owning_Teacher may amend within 168 hours of `completed_at`; an ADMIN may amend with no time limit. The session stays FINALIZED, a reason is mandatory, and every amendment writes an Attendance_Audit_Entry. | 9 |
| 3 | What happens to unmarked students on confirm? | Incomplete submissions are rejected with HTTP 400 naming the omitted students. Silent ABSENT defaulting is refused because an invisible absence is the most damaging error available. A caller may opt in explicitly through `markUnspecifiedAsAbsent`, and the response then names every defaulted student. The Mark_Attendance_Screen pre-selects a status on every row, so a complete payload is the normal case. | 5 |
| 4 | What must `submissionId` guarantee? | A replay carrying the same submission identifier and the same Request_Fingerprint returns the stored Attendance_Confirmation and writes nothing. The same identifier with a different fingerprint returns HTTP 409. A fresh identifier against an already-FINALIZED session returns HTTP 409 and directs the caller to the amendment path. The `(lecture_session_id, student_id)` unique constraint remains the final guard and surfaces as HTTP 409 rather than HTTP 500. | 6 |
| 5 | Does approved leave auto-populate? | The Roster suggests ON_LEAVE for a student covered by an APPROVED LeaveRequest, as advice only. The teacher may override to any status, and nothing persists without a teacher submission. | 4 |
| 6 | What is the marking window? | A future-dated session cannot be activated. A session must be activated before confirmation is accepted. The Owning_Teacher may activate a session dated up to 14 days before the current server date; beyond 14 days only an ADMIN may activate. | 2 |
| 7 | How is the dashboard denominator protected? | The student percentage continues to derive from `attendance_records` through `countAttended`/`countCountable`, never from Lecture_Session counters. SCHEDULED sessions hold zero records, records exist only for ACTIVE enrollments, and the neutral statuses stay out of both numerator and denominator. | 8 |
| 8 | When are the denormalised counters recalculated? | On every record write and every amendment, inside the same transaction, by counting stored records rather than incrementing. `on_leave_count` covers both neutral statuses (ON_LEAVE and EXCUSED) so the three counters sum to the record count. | 7 |

## Requirements

### Requirement 1: Read the roster for a lecture session

**User Story:** As a teacher, I want to see the students actually enrolled in the session I am about to mark, so that I mark the right people and nobody who dropped the class.

#### Acceptance Criteria

1. WHEN the Attendance_Controller receives a roster request for a Lecture_Session, THE Attendance_Service SHALL return one Roster_Entry for each student holding an Enrollment with status ACTIVE in that session's Class_Section.
2. THE Attendance_Service SHALL omit from the Roster every student whose Enrollment status in that Class_Section is DROPPED or COMPLETED.
3. THE Attendance_Service SHALL populate each Roster_Entry with the student identifier, the roll number, the student's full name, the stored AttendanceStatus when an Attendance_Record exists, and a Suggested_Status.
4. WHERE an Attendance_Record already exists for a student and the requested Lecture_Session, THE Attendance_Service SHALL set that Roster_Entry's stored status from that Attendance_Record.
5. WHERE no Attendance_Record exists for a student and the requested Lecture_Session, THE Attendance_Service SHALL set that Roster_Entry's stored status to null.
6. THE Attendance_Service SHALL order the Roster by roll number in ascending lexicographic order.
7. THE Attendance_Service SHALL include the Lecture_Session's current SessionStatus, session date, start time, end time, subject code and subject name in the roster response.
8. IF the requested Lecture_Session identifier matches no `lecture_sessions` row, THEN THE Attendance_Controller SHALL respond with HTTP 404 and error code `RESOURCE_NOT_FOUND`.
9. WHERE a Class_Section holds zero ACTIVE Enrollments, THE Attendance_Service SHALL return a Roster containing zero Roster_Entry values.

### Requirement 2: Activate a session and enforce the marking window

**User Story:** As a teacher, I want to explicitly open a session before marking, so that an accidental submission against the wrong day or a class that has not happened yet is impossible.

#### Acceptance Criteria

1. WHEN an Owning_Teacher requests activation of a Lecture_Session whose status is SCHEDULED and whose session date is the current server date or earlier, THE Attendance_Service SHALL set that session's status to ACTIVE.
2. WHEN the Attendance_Service activates a Lecture_Session, THE Attendance_Service SHALL set `started_at` to the current server timestamp.
3. WHEN the Attendance_Service activates a Lecture_Session, THE Attendance_Service SHALL set `attendance_method` to MANUAL.
4. IF a caller requests activation of a Lecture_Session whose session date is later than the current server date, THEN THE Attendance_Controller SHALL respond with HTTP 400 and error code `BAD_REQUEST`.
5. WHERE the caller's role is TEACHER, THE Attendance_Service SHALL permit activation only while the current server date is at most 14 days after the Lecture_Session's session date.
6. IF a caller whose role is TEACHER requests activation of a Lecture_Session whose session date is more than 14 days before the current server date, THEN THE Attendance_Controller SHALL respond with HTTP 403 and error code `FORBIDDEN`.
7. WHERE the caller's role is ADMIN, THE Attendance_Service SHALL permit activation of any Lecture_Session whose session date is the current server date or earlier.
8. WHEN a caller requests activation of a Lecture_Session whose status is already ACTIVE, THE Attendance_Service SHALL return that session with `started_at`, `attendance_method` and `total_students` unchanged.
9. IF the Attendance_Controller receives a confirmation request for a Lecture_Session whose status is not ACTIVE, THEN THE Attendance_Controller SHALL respond with HTTP 409 and error code `CONFLICT`.
10. THE Attendance_Service SHALL evaluate the current server date and the current server timestamp through a single injected time source.

### Requirement 3: Constrain session status transitions

**User Story:** As a developer, I want the legal session transitions written down, so that the nine-value enum cannot be driven into a state the manual flow does not understand.

#### Acceptance Criteria

1. THE Attendance_Service SHALL treat exactly four SessionStatus transitions as legal in the Manual_Marking_Flow: SCHEDULED to ACTIVE, SCHEDULED to CANCELLED, ACTIVE to CANCELLED, and ACTIVE to FINALIZED.
2. IF the Attendance_Controller receives a request implying a SessionStatus transition outside the four legal transitions, THEN THE Attendance_Controller SHALL respond with HTTP 409 and error code `CONFLICT`.
3. THE Attendance_Service SHALL leave the SessionStatus values CREATED, SCANNING, REVIEW, FINALIZING and FAILED unwritten in the Manual_Marking_Flow, reserving those five values for the later camera flow.
4. THE backend SessionStatus enum SHALL retain all nine values so the backend enum and the shared Kotlin Multiplatform SessionStatus enum stay identical value for value.
5. WHERE the caller's role is TEACHER, THE Attendance_Service SHALL permit a SessionStatus transition only when the caller is the Owning_Teacher of that Lecture_Session.
6. WHERE the caller's role is ADMIN, THE Attendance_Service SHALL permit every one of the four legal SessionStatus transitions.
7. WHEN an authorized caller cancels a Lecture_Session whose status is SCHEDULED or ACTIVE, THE Attendance_Service SHALL set that session's status to CANCELLED.
8. WHEN the Attendance_Service cancels a Lecture_Session, THE Attendance_Service SHALL leave every existing Attendance_Record for that session in place.
9. WHEN the Attendance_Service applies an Attendance_Amendment to a FINALIZED Lecture_Session, THE Attendance_Service SHALL keep that session's status at FINALIZED.
10. THE Attendance_Service SHALL expose the legal transition set as a single declared mapping so that a reader can enumerate every legal transition from one place.

### Requirement 4: Suggest ON_LEAVE for approved leave

**User Story:** As a teacher, I want students with approved leave flagged before I start marking, so that I do not record an absence against a student whose leave I already granted.

#### Acceptance Criteria

1. WHERE a student holds a LeaveRequest with status APPROVED whose `from_date` and `to_date` inclusively span the Lecture_Session's session date, and whose `class_section_id` is either absent or equal to the session's Class_Section identifier, THE Attendance_Service SHALL set that student's Suggested_Status to ON_LEAVE.
2. WHERE no LeaveRequest with status APPROVED covers the Lecture_Session's session date for a student, THE Attendance_Service SHALL set that student's Suggested_Status to PRESENT.
3. THE Attendance_Service SHALL ignore LeaveRequest rows whose status is PENDING, REJECTED or CANCELLED when deriving a Suggested_Status.
4. THE Attendance_Service SHALL flag each Roster_Entry whose Suggested_Status is ON_LEAVE with the identifier and the reason text of the covering LeaveRequest.
5. THE Attendance_Service SHALL accept any AttendanceStatus value a Teacher submits for a student, including a value differing from that student's Suggested_Status.
6. WHEN a Teacher submits PRESENT for a student whose Suggested_Status is ON_LEAVE, THE Attendance_Service SHALL persist PRESENT for that student.
7. THE Attendance_Service SHALL persist an Attendance_Record only from a submitted entry, deriving no Attendance_Record from a Suggested_Status alone.

### Requirement 5: Confirm attendance for a complete roster

**User Story:** As a teacher, I want confirmation to cover every student on the roster, so that no student silently acquires an absence I never looked at.

#### Acceptance Criteria

1. WHEN the Attendance_Controller receives a confirmation request, THE Attendance_Service SHALL require one submitted entry for each student holding an ACTIVE Enrollment in the Lecture_Session's Class_Section.
2. IF a confirmation request omits at least one ACTIVE-enrolled student while the request's `markUnspecifiedAsAbsent` flag is false, THEN THE Attendance_Controller SHALL respond with HTTP 400, error code `BAD_REQUEST`, and the omitted student identifiers in the error details.
3. THE `markUnspecifiedAsAbsent` flag SHALL carry the value false when a confirmation request omits that flag.
4. WHERE a confirmation request sets `markUnspecifiedAsAbsent` to true, THE Attendance_Service SHALL persist an Attendance_Record with status ABSENT for each omitted ACTIVE-enrolled student.
5. WHERE the Attendance_Service defaults omitted students to ABSENT, THE Attendance_Service SHALL list those student identifiers in the Attendance_Confirmation response.
6. IF a confirmation request carries an entry for a student holding no ACTIVE Enrollment in the Lecture_Session's Class_Section, THEN THE Attendance_Controller SHALL respond with HTTP 400 and error code `BAD_REQUEST`.
7. IF a confirmation request carries two or more entries bearing the same student identifier, THEN THE Attendance_Controller SHALL respond with HTTP 400 and error code `BAD_REQUEST`.
8. IF a confirmation request carries an entry whose status is outside the five AttendanceStatus values, THEN THE Attendance_Controller SHALL respond with HTTP 400 and error code `BAD_REQUEST`.
9. WHEN the Attendance_Service persists a confirmation, THE Attendance_Service SHALL set each new Attendance_Record's `method` to MANUAL.
10. WHEN the Attendance_Service persists a confirmation, THE Attendance_Service SHALL set each new Attendance_Record's `confidence_score` to null.
11. WHEN the Attendance_Service persists a confirmation, THE Attendance_Service SHALL set each new Attendance_Record's `marked_by_teacher_id` to the acting Teacher's own teacher identifier.
12. WHEN the Attendance_Service persists a confirmation, THE Attendance_Service SHALL set each new Attendance_Record's `marked_at` to the current server timestamp.
13. WHEN a confirmation succeeds, THE Attendance_Service SHALL set the Lecture_Session's status to FINALIZED.
14. WHEN a confirmation succeeds, THE Attendance_Service SHALL set the Lecture_Session's `completed_at` to the current server timestamp.
15. THE Attendance_Service SHALL persist the Attendance_Records, the Lecture_Session counters and the Lecture_Session status of one confirmation within a single database transaction.
16. WHEN a confirmation succeeds, THE Attendance_Service SHALL return an Attendance_Confirmation carrying the Lecture_Session identifier, the total marked count, the present count, the absent count, the on-leave count and the confirmation timestamp.
17. WHERE an ADMIN acts as the caller, THE Attendance_Service SHALL set each new Attendance_Record's `marked_by_teacher_id` to null, recording the acting ADMIN in the Attendance_Audit_Entry instead, so that no Attendance_Record attributes a marking to a Teacher who did not perform it.

### Requirement 6: Make confirmation idempotent

**User Story:** As a teacher on an unreliable classroom network, I want a retried confirmation to be harmless, so that a lost response does not double-mark my class or lose my work.

#### Acceptance Criteria

1. THE Attendance_Service SHALL require each confirmation request to carry a `submissionId` parsing as a UUID.
2. IF a confirmation request carries a `submissionId` that fails UUID parsing, THEN THE Attendance_Controller SHALL respond with HTTP 400 and error code `BAD_REQUEST`.
3. THE Request_Fingerprint SHALL be a SHA-256 digest computed over the Lecture_Session identifier and the submitted entries sorted by student identifier in ascending order.
4. WHEN the Attendance_Service accepts a confirmation, THE Attendance_Service SHALL persist a Submission_Record holding the `submissionId`, the Lecture_Session identifier, the acting user identifier, the Request_Fingerprint, the returned Attendance_Confirmation and the creation timestamp.
5. WHEN the Attendance_Controller receives a confirmation request whose `submissionId` matches a Submission_Record and whose Request_Fingerprint equals that Submission_Record's fingerprint, THE Attendance_Controller SHALL respond with HTTP 200 carrying the stored Attendance_Confirmation.
6. WHEN the Attendance_Controller receives a confirmation request whose `submissionId` matches a Submission_Record and whose Request_Fingerprint equals that Submission_Record's fingerprint, THE Attendance_Service SHALL leave every stored Attendance_Record for that Lecture_Session unchanged.
7. IF the Attendance_Controller receives a confirmation request whose `submissionId` matches a Submission_Record and whose Request_Fingerprint differs from that Submission_Record's fingerprint, THEN THE Attendance_Controller SHALL respond with HTTP 409 and error code `CONFLICT`.
8. IF the Attendance_Controller receives a confirmation request bearing a `submissionId` matching no Submission_Record for a Lecture_Session whose status is FINALIZED, THEN THE Attendance_Controller SHALL respond with HTTP 409, error code `CONFLICT`, and an error message naming the amendment path.
9. THE Attendance_Service SHALL persist the Submission_Record and the Attendance_Records of one confirmation within a single database transaction.
10. IF persisting a confirmation violates the `attendance_records` unique constraint on `(lecture_session_id, student_id)`, THEN THE Attendance_Controller SHALL respond with HTTP 409 and error code `CONFLICT`.
11. WHEN two confirmation requests bearing different `submissionId` values for the same Lecture_Session execute concurrently, THE Attendance_Service SHALL persist the entries of exactly one of those two requests.
12. THE Submission_Record store SHALL hold `submissionId` as a primary key so that a duplicate insert fails at the database level.

### Requirement 7: Keep the denormalised session counters consistent

**User Story:** As a developer, I want the session counters recomputed from the records, so that `present_count`, `absent_count` and `on_leave_count` cannot drift away from `attendance_records`.

#### Acceptance Criteria

1. WHEN the Attendance_Service persists or amends any Attendance_Record for a Lecture_Session, THE Attendance_Service SHALL recompute that session's `present_count`, `absent_count` and `on_leave_count` within the same database transaction.
2. THE Attendance_Service SHALL set `present_count` to the number of that session's Attendance_Records whose status is PRESENT or LATE.
3. THE Attendance_Service SHALL set `absent_count` to the number of that session's Attendance_Records whose status is ABSENT.
4. THE Attendance_Service SHALL set `on_leave_count` to the number of that session's Attendance_Records whose status is ON_LEAVE or EXCUSED.
5. THE Attendance_Service SHALL derive each counter from a count over stored Attendance_Records rather than from an increment applied to the counter's previous value.
6. THE sum of a Lecture_Session's `present_count`, `absent_count` and `on_leave_count` SHALL equal the number of Attendance_Records stored for that Lecture_Session.
7. WHEN the Attendance_Service activates a Lecture_Session, THE Attendance_Service SHALL set `total_students` to the number of ACTIVE Enrollments in that session's Class_Section.
8. THE Attendance_Service SHALL set `updated_at` on a Lecture_Session to the current server timestamp whenever that session's counters change.
9. WHERE a Lecture_Session holds zero Attendance_Records, THE Attendance_Service SHALL report `present_count`, `absent_count` and `on_leave_count` as zero.

> Note on existing data: the V7 seed computed `on_leave_count` from ON_LEAVE alone, leaving EXCUSED outside all three counters. Criterion 7.4 brings EXCUSED into `on_leave_count` so that criterion 7.6 holds. The V7 seed inserts no EXCUSED row, so no seeded value changes.

### Requirement 8: Preserve the attendance percentage denominator

**User Story:** As a student, I want my attendance percentage to reflect only classes that actually happened, so that a future class on the timetable and an approved leave neither of which I could attend do not drag my percentage down.

#### Acceptance Criteria

1. THE Dashboard_Service SHALL derive a student's attendance percentage from `attendance_records` through `AttendanceRecordRepository.countAttended` and `AttendanceRecordRepository.countCountable`, rather than from any Lecture_Session counter.
2. THE Attendance_Service SHALL persist an Attendance_Record only for a student holding an ACTIVE Enrollment in the Lecture_Session's Class_Section.
3. WHILE a Lecture_Session's status is SCHEDULED, THE Attendance_Service SHALL hold zero Attendance_Records for that Lecture_Session.
4. THE Attendance_Service SHALL exclude every Attendance_Record whose status is ON_LEAVE or EXCUSED from both the numerator and the denominator of a student's attendance percentage.
5. WHEN a Lecture_Session reaches FINALIZED, THE Dashboard_Service SHALL report each affected student's attendance percentage as the count of that student's PRESENT and LATE records divided by the count of that student's records whose status is neither ON_LEAVE nor EXCUSED, rounded to two decimal places.
6. WHEN a Lecture_Session transitions to CANCELLED, THE Dashboard_Service SHALL report each affected student's attendance percentage unchanged from the value reported before that transition.
7. WHEN the Attendance_Service persists a confirmation marking a student ON_LEAVE, THE Dashboard_Service SHALL report that student's attendance percentage unchanged from the value reported before that confirmation.
8. WHERE a student holds zero countable Attendance_Records, THE Dashboard_Service SHALL report that student's attendance percentage as 0.0.
9. THE Attendance_Service SHALL set each Attendance_Record's `lecture_session_id` to a Lecture_Session whose `semester_id` scopes that record, so that the semester-scoped dashboard queries stay correct.

### Requirement 9: Amend a finalized session with an audit trail

**User Story:** As a teacher who mis-tapped a row, I want to correct a finalized session within a bounded window, so that a student's record is right and the correction is attributable.

#### Acceptance Criteria

1. WHERE the caller is the Owning_Teacher, THE Attendance_Service SHALL permit an Attendance_Amendment while the current server timestamp is at most 168 hours after the Lecture_Session's `completed_at`.
2. IF the Owning_Teacher submits an Attendance_Amendment more than 168 hours after the Lecture_Session's `completed_at`, THEN THE Attendance_Controller SHALL respond with HTTP 403, error code `FORBIDDEN`, and an error message naming the administrator correction path.
3. WHERE the caller's role is ADMIN, THE Attendance_Service SHALL permit an Attendance_Amendment at any time after the Lecture_Session's `completed_at`.
4. THE Attendance_Service SHALL require each Attendance_Amendment to carry a reason holding at least one non-whitespace character.
5. IF an Attendance_Amendment carries a reason holding only whitespace characters, THEN THE Attendance_Controller SHALL respond with HTTP 400 and error code `BAD_REQUEST`.
6. WHEN the Attendance_Service applies an Attendance_Amendment, THE Attendance_Service SHALL persist one Attendance_Audit_Entry holding the Attendance_Record identifier, the previous AttendanceStatus, the new AttendanceStatus, the acting user identifier, the reason and the change timestamp.
7. WHEN the Attendance_Service applies an Attendance_Amendment, THE Attendance_Service SHALL preserve that Attendance_Record's `marked_at` value.
8. WHEN the Attendance_Service applies an Attendance_Amendment, THE Attendance_Service SHALL preserve that Attendance_Record's `marked_by_teacher_id` value, leaving the Attendance_Audit_Entry as the record of who changed the status.
9. WHEN the Attendance_Service applies an Attendance_Amendment, THE Attendance_Service SHALL set that Attendance_Record's `updated_at` to the current server timestamp.
10. THE Attendance_Service SHALL persist the amended Attendance_Records, the Attendance_Audit_Entry values and the recomputed Lecture_Session counters of one Attendance_Amendment within a single database transaction.
11. IF an Attendance_Amendment names a student holding no Attendance_Record for the Lecture_Session, THEN THE Attendance_Controller SHALL respond with HTTP 404 and error code `RESOURCE_NOT_FOUND`.
12. IF the Attendance_Controller receives an Attendance_Amendment for a Lecture_Session whose status is not FINALIZED, THEN THE Attendance_Controller SHALL respond with HTTP 409 and error code `CONFLICT`.
13. WHEN an Attendance_Amendment submits a status equal to the Attendance_Record's current status, THE Attendance_Service SHALL leave that Attendance_Record unchanged and SHALL persist no Attendance_Audit_Entry for it.
14. THE Attendance_Service SHALL report an Attendance_Record as corrected while at least one Attendance_Audit_Entry referencing that Attendance_Record carries a previous AttendanceStatus differing from its new AttendanceStatus.
15. THE Attendance_Service SHALL retain every Attendance_Audit_Entry, applying no deletion and no overwrite to a persisted Attendance_Audit_Entry.
16. WHEN the Attendance_Controller receives a request for a Lecture_Session's audit history from the Owning_Teacher or an ADMIN, THE Attendance_Service SHALL return every Attendance_Audit_Entry for that session ordered by change timestamp ascending.

### Requirement 10: Authorize every attendance-marking operation

**User Story:** As a system owner, I want attendance writes restricted to the teacher who actually teaches the class, so that one teacher cannot alter another teacher's attendance.

#### Acceptance Criteria

1. IF the Attendance_Controller receives a request carrying no verified Firebase identifier, THEN THE Attendance_Controller SHALL respond with HTTP 401 and error code `UNAUTHORIZED`.
2. IF the verified Firebase identifier matches no `users` row through `UserRepository.findByFirebaseUid`, THEN THE Attendance_Controller SHALL respond with HTTP 401 and error code `UNAUTHORIZED`.
3. WHERE the caller's role is ADMIN, THE Attendance_Controller SHALL permit every attendance-marking operation.
4. IF the caller's role is STUDENT, THEN THE Attendance_Controller SHALL respond with HTTP 403 and error code `FORBIDDEN` for every attendance-marking operation.
5. WHERE the caller's role is TEACHER, THE Attendance_Controller SHALL resolve the caller's own teacher identifier through `TeacherRepository.findByUserId`.
6. IF the caller's role is TEACHER and `TeacherRepository` holds no teacher row for the caller's user identifier, THEN THE Attendance_Controller SHALL respond with HTTP 403 and error code `FORBIDDEN`.
7. IF the caller's role is TEACHER and the caller's own teacher identifier differs from the `teacher_id` of the Lecture_Session's Class_Section, THEN THE Attendance_Controller SHALL respond with HTTP 403 and error code `FORBIDDEN`.
8. THE Attendance_Controller SHALL apply the same authorization checks to the roster read, the activation, the confirmation, the cancellation, the amendment and the audit-history operations.
9. THE Attendance_Controller SHALL map every request path under `/attendance` so that the deployed routes resolve under `/api/attendance`.
10. THE Attendance_Controller SHALL wrap every response body in the `ApiResponse` envelope carrying the `success`, `data` and `error` members.
11. THE Attendance_Controller SHALL raise `UnauthorizedException`, `ForbiddenException`, `ConflictException`, `BadRequestException` and `ResourceNotFoundException` so that `GlobalExceptionHandler` produces the status codes and error codes named in this document.
12. THE Attendance_Controller SHALL delegate every authorization decision and every business rule to the Attendance_Service or to the authorization helpers, issuing no repository query beyond resolving the caller.

### Requirement 11: Correct the shared attendance contract

**User Story:** As a developer, I want the shared attendance contract to match the database, so that a semester identifier and an attendance status survive the trip between the Android client and the backend.

#### Acceptance Criteria

1. THE Shared_Attendance_Repository SHALL declare the `semesterId` parameter of `getStudentAttendance` as a `String` holding a semester UUID in text form, matching the `uuid` type of `semesters.id`.
2. THE Shared_Attendance_Repository SHALL declare the `semesterId` parameter of `getStudentAttendanceStats` as a `String` holding a semester UUID in text form.
3. THE `AttendanceStats` type SHALL declare `semesterId` as a `String` holding a semester UUID in text form.
4. THE `StartLectureSessionRequest` type SHALL declare `semesterId` as a `String` holding a semester UUID in text form.
5. THE shared `AttendanceStatus` enum SHALL declare the values PRESENT, ABSENT, LATE, ON_LEAVE and EXCUSED so that the shared enum matches the backend `AttendanceStatus` enum value for value.
6. THE shared `AttendanceRecord` type SHALL declare `markedByTeacherId` as a nullable `Long`, matching the nullable `attendance_records.marked_by_teacher_id` column.
7. THE shared `AttendanceRecord` type SHALL declare `method` as a nullable `AttendanceMethod`, matching the nullable `attendance_records.method` column.
8. THE shared `AttendanceRecord` type SHALL name the free-text member `notes`, matching the `attendance_records.notes` column.
9. THE shared `AttendanceRecord` type SHALL derive `isCorrected` from the presence of at least one Attendance_Audit_Entry for that record, as supplied by the backend.
10. THE Http_Attendance_Repository SHALL implement the Shared_Attendance_Repository members the Manual_Marking_Flow uses against the shared Ktor `ApiClient`.
11. THE Http_Attendance_Repository SHALL return the project's `com.afora.shared.util.Result` type carrying an `AppError` on failure, rather than `kotlin.Result`.
12. WHERE the Manual_Marking_Flow leaves a Shared_Attendance_Repository member unimplemented, THE Http_Attendance_Repository SHALL return a `Result.Error` carrying `AppError.UnknownError` naming the unimplemented member.
13. THE Shared_Attendance_Repository SHALL declare a roster member, an activation member, a confirmation member and an amendment member corresponding to the Attendance_Controller operations.

### Requirement 12: Mark attendance on the Android screen

**User Story:** As a teacher standing in front of a class, I want to mark the whole roster on one screen and confirm once, so that marking a class takes seconds and my taps are never lost.

#### Acceptance Criteria

1. WHEN a Teacher selects a Lecture_Session on the teacher dashboard, THE Mark_Attendance_Screen SHALL open for that Lecture_Session.
2. THE Mark_Attendance_ViewModel SHALL expose screen state as a `StateFlow` of Mark_Attendance_UiState carrying Loading, Success and Error cases.
3. WHILE the Mark_Attendance_ViewModel awaits the Roster, THE Mark_Attendance_Screen SHALL render the Loading state.
4. WHEN the Roster arrives, THE Mark_Attendance_Screen SHALL render one row for each Roster_Entry showing the roll number, the full name and a status selector.
5. WHEN the Roster arrives, THE Mark_Attendance_ViewModel SHALL set each row's selected status to that Roster_Entry's stored status where a stored status exists, and to that Roster_Entry's Suggested_Status otherwise.
6. THE Mark_Attendance_Screen SHALL offer each row a selector covering the five AttendanceStatus values.
7. THE Mark_Attendance_Screen SHALL display the running count of rows selected as PRESENT, as ABSENT and as ON_LEAVE.
8. WHEN a Teacher activates the confirm control, THE Mark_Attendance_ViewModel SHALL submit one entry for every Roster_Entry on the screen.
9. WHEN a Teacher activates the confirm control, THE Mark_Attendance_ViewModel SHALL generate one `submissionId` as a fresh UUID for that confirmation attempt.
10. WHILE a confirmation for a given attempt remains unacknowledged, THE Mark_Attendance_ViewModel SHALL reuse the `submissionId` generated for that attempt on every retry.
11. WHILE a confirmation is in flight, THE Mark_Attendance_Screen SHALL disable the confirm control.
12. WHEN a confirmation succeeds, THE Mark_Attendance_Screen SHALL render the Success state showing the persisted present, absent and on-leave counts.
13. IF a confirmation fails, THEN THE Mark_Attendance_Screen SHALL render the Error state carrying the `AppError` message.
14. IF a confirmation fails, THEN THE Mark_Attendance_ViewModel SHALL retain every status the Teacher selected.
15. WHERE a Roster_Entry's Suggested_Status is ON_LEAVE, THE Mark_Attendance_Screen SHALL display the approved-leave reason text on that row.
16. WHERE a Lecture_Session's status is FINALIZED, THE Mark_Attendance_Screen SHALL render each row's stored status.
17. WHERE a Lecture_Session's status is FINALIZED and the amendment window remains open, THE Mark_Attendance_Screen SHALL offer an amendment control.
18. WHEN a Teacher activates the amendment control, THE Mark_Attendance_Screen SHALL require a reason holding at least one non-whitespace character before enabling submission.
19. THE Mark_Attendance_Screen SHALL give each status selector a content description naming the student's full name and the selected status.
20. THE Mark_Attendance_Screen SHALL give the confirm control a minimum touch target of 48 density-independent pixels in both dimensions.
21. THE Mark_Attendance_ViewModel SHALL receive the Shared_Attendance_Repository dependency through Hilt constructor injection.

### Requirement 13: Serialize the attendance contract without loss

**User Story:** As a developer, I want the attendance payloads to survive a serialization round trip, so that an enum value or a UUID string added on one side cannot be silently dropped on the other.

#### Acceptance Criteria

1. THE Shared_Serializer SHALL produce, for every valid Attendance_Confirmation value, JSON that deserializes into an equivalent Attendance_Confirmation value.
2. THE Shared_Serializer SHALL produce, for every valid Roster response value, JSON that deserializes into an equivalent Roster response value.
3. THE Shared_Serializer SHALL produce, for every valid confirmation request value, JSON that deserializes into an equivalent confirmation request value.
4. THE Shared_Serializer SHALL produce, for every valid Attendance_Amendment request value, JSON that deserializes into an equivalent Attendance_Amendment request value.
5. THE serialized names of the shared `AttendanceStatus` enum SHALL equal the names of the backend `AttendanceStatus` enum.
6. THE serialized names of the shared `SessionStatus` enum SHALL equal the names of the backend `SessionStatus` enum.
7. THE serialized names of the shared `AttendanceMethod` enum SHALL equal the names of the backend `AttendanceMethod` enum.
8. THE Shared_Serializer SHALL deserialize every semester identifier the backend emits as a UUID text string into the corresponding `String` member.
9. IF the Shared_Serializer receives a status name outside the declared enum values, THEN THE Http_Attendance_Repository SHALL return a `Result.Error` carrying `AppError.ApiError`.
10. THE Attendance_Service SHALL emit every timestamp in the attendance payloads as an ISO 8601 string.
11. THE Attendance_Service SHALL emit every session date in the attendance payloads as an ISO 8601 date string in `YYYY-MM-DD` form.

## Open Items for the Design Phase

These need a technical decision in `design.md` rather than a requirement change.

- Whether the Submission_Record store and the Attendance_Audit_Entry store land as two new tables in migration V9, and the retention policy for each.
- Whether the time source of criterion 2.10 is a `java.time.Clock` bean or a narrower interface.
- Whether the concurrency guarantee of criterion 6.11 rests on a pessimistic row lock over the Lecture_Session, on the Submission_Record primary key, or on the existing `attendance_records` unique constraint alone.
- Whether the roster, activation, confirmation and amendment operations sit on one controller or split across two.
