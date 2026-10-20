# Requirements Document

## Introduction

Real biometric face registration and on-device face matching for AFORA's camera-based attendance flow. A student records consent and registers a Face_Profile from on-device-generated embeddings. A backend stores only the numeric embedding vectors, never a face image. When a teacher starts a camera-based lecture session, the backend distributes the enrolled students' embedding vectors to that teacher's device, and the device performs the 1:N face match locally against the live camera feed. High-confidence matches auto-populate as an overridable suggestion; medium-confidence matches queue for explicit teacher confirmation or rejection before the session can finalize; anything below the matching floor is presented as unmatched. The resulting Attendance_Record is persisted through the existing confirmation flow with `method = CAMERA` and a genuine `confidence_score`, and total matching failure falls back to the existing manual roster screen rather than failing the session outright.

This spec builds directly on the attendance-marking specification and the already-live V5–V8 schema. `Lecture_Session`, `SessionStatus`, `Enrollment`, `Attendance_Record`, `AttendanceStatus`, the `Attendance_Service`/`Attendance_Controller` confirmation flow, and the `Mark_Attendance_Screen` manual fallback are all defined there and are reused by name, not redefined here. The `attendance_records` table already carries `method` (`CAMERA`/`MANUAL`/`HYBRID`) and `confidence_score` columns from migration V7; this spec is the first feature to populate both columns with real values under `method = CAMERA`. This spec does not redesign `attendance_records`.

### Background and Correction Note

An audit document has circulated claiming that AFORA's camera attendance scanner already performs real face recognition with genuine confidence scoring. **That claim is false.** The currently merged implementation — `FaceDetectionAnalyzer.kt` and `FaceDeduplicator.kt` under `androidApp/.../ui/attendance/camera/` — detects faces with ML Kit but correlates each detected face to a roster entry with `abs(trackingId) % enrolledStudents.size`, a pure index-arithmetic placeholder with no relationship to the detected face's actual identity. Its reported confidence is computed as a fixed `0.88` base score plus a tracking bonus minus an angle penalty, a fabricated number uncorrelated with whether the match is correct. Neither function performs any biometric comparison. This spec exists to build the real replacement for both. A reader of this document should not need to re-derive this fact by reading the analyzer source themselves.

The existing `docs/face-recognition.md` sketch assumed server-side face image storage (S3/MinIO) and server-side vector similarity search (`pgvector`). This spec deliberately departs from that sketch: embeddings are generated and compared entirely on-device, and the backend's only job is to store and distribute numeric vectors. See [Resolved Decisions](#resolved-decisions).

### In Scope

- Recording explicit student consent before any face capture or embedding generation.
- On-device face image capture, on-device embedding generation, and backend storage of the resulting Face_Embedding vectors (numbers only, never images).
- Backend distribution of a Class_Section's enrolled students' embeddings to a requesting, authorized teacher device.
- On-device 1:N comparison of a live camera session's detected faces against the distributed embedding set.
- Confidence-band routing: HIGH auto-suggests (teacher-overridable), MEDIUM queues for explicit teacher confirm-or-reject, below the matching floor is presented as unmatched.
- The teacher review step that must clear before a camera session can finalize.
- Persisting the resulting Attendance_Record with `method = CAMERA` and a real `confidence_score`, through the existing attendance-marking confirmation flow.
- Deleting all of a student's Face_Embedding rows when that student's enrollment ends or the student requests deletion.
- Falling back to the existing manual roster screen (`Mark_Attendance_Screen`) on total matching failure for a session.
- Replacing the index-based placeholder matching and fabricated confidence score in `FaceDetectionAnalyzer.kt`/`FaceDeduplicator.kt`.

### Out of Scope

- Choosing or benchmarking the specific on-device detection/embedding model architecture (e.g. MobileFaceNet, ArcFace, BlazeFace). This document describes required behavior, accuracy-relevant constraints and privacy constraints, not a model name or version; model selection is a design-phase decision.
- Any backend-side embedding computation path. The backend never receives a face image and never computes an embedding from one.
- Admin or web tooling for managing face profiles.
- Liveness detection and anti-spoofing. This is flagged as a known risk for the design phase to address, not a requirement this spec satisfies.
- The number of Face_Embedding samples captured per student beyond "one or more"; the exact per-profile sample count is a design-phase decision.
- Any change to the attendance-marking specification's own requirements. `Lecture_Session`, `SessionStatus`, `Attendance_Record`, and the manual confirmation flow are reused exactly as that spec defines them.
- Redesigning the `attendance_records` table. This feature only populates the already-existing `method` and `confidence_score` columns.

## Glossary

Reused without redefinition, exactly as defined in the attendance-marking specification's Glossary: **Lecture_Session**, **Class_Section**, **Enrollment**, **Attendance_Record**, **AttendanceStatus**, **SessionStatus**, **Attendance_Service**, **Attendance_Controller**, **Mark_Attendance_Screen**, **Teacher**, **Owning_Teacher**, **Student** (a caller whose `User.role` is STUDENT).

New terms introduced by this specification:

- **Face_Consent**: A persisted record of a student's affirmative, explicit authorization for AFORA to capture a face image on-device and generate a Face_Profile from it, carrying the consenting student's identifier and a timestamp.
- **Face_Profile**: The collection of a student's Face_Embedding rows, considered as a unit for registration, replacement and deletion.
- **Face_Embedding**: A numeric vector produced on-device from a face image, stored by the backend as a plain numeric array column. No vector-similarity database extension and no face image accompanies it.
- **Face_Profile_Service**: The backend service owning Face_Consent, Face_Profile and Face_Embedding persistence, distribution and purge.
- **Face_Profile_Controller**: The backend REST controller through which a Student registers a Face_Profile and a Teacher requests an Enrolled_Embedding_Set.
- **Enrolled_Embedding_Set**: The set of Face_Embedding rows for every ACTIVE-enrolled student in one Class_Section, as distributed to a requesting teacher's device for one Camera_Session.
- **Camera_Session**: A Lecture_Session being conducted with `attendance_method = CAMERA`.
- **On_Device_Matcher**: The Android component that compares a detected face's locally generated embedding against the Enrolled_Embedding_Set and produces a Match_Confidence, replacing the index-based placeholder in `FaceDetectionAnalyzer.kt`/`FaceDeduplicator.kt`.
- **Match_Confidence**: A numeric score, produced entirely on-device, expressing how closely a detected face's embedding matches a specific enrolled student's Face_Embedding.
- **Confidence_Band**: The classification of a Match_Confidence into exactly one of HIGH, MEDIUM or UNMATCHED.
- **High_Confidence_Threshold**: The Match_Confidence value at or above which a Face_Match_Candidate's Confidence_Band is HIGH.
- **Medium_Confidence_Threshold**: The Match_Confidence value at or above which, and below the High_Confidence_Threshold, a Face_Match_Candidate's Confidence_Band is MEDIUM.
- **Unmatched_Floor_Threshold**: The Match_Confidence value below which a detected face carries no Confidence_Band and is presented as unmatched.
- **Face_Match_Candidate**: One detected face in a Camera_Session, correlated by the On_Device_Matcher to a specific enrolled student with a Match_Confidence and a Confidence_Band.
- **Review_Queue**: The set of a Camera_Session's Face_Match_Candidate values whose Confidence_Band is MEDIUM and which the teacher has not yet confirmed or rejected.
- **Face_Registration_Screen**: The Android screen on which a Student records Face_Consent and registers a Face_Profile.
- **Face_Registration_ViewModel**: The Hilt-injected `ViewModel` backing the Face_Registration_Screen.
- **Camera_Attendance_Screen**: The existing Android screen (`CameraAttendanceScreen.kt`) on which a Teacher conducts a Camera_Session; this spec requires its underlying matching logic to be rebuilt on the On_Device_Matcher.
- **Camera_Attendance_ViewModel**: The existing Hilt-injected `ViewModel` (`CameraAttendanceViewModel.kt`) backing the Camera_Attendance_Screen.

## Resolved Decisions

| # | Question | Decision | Requirement |
|---|---|---|---|
| 1 | Where does embedding comparison happen, and how are embeddings stored? | Embeddings are generated and compared entirely on-device. The backend stores each Face_Embedding as a plain numeric array column — no `pgvector`, no vector index, no server-side similarity search. Raw face images are never uploaded; no object storage (S3/MinIO) is introduced. This is a deliberate simplification versus `docs/face-recognition.md`'s server-side-image-and-pgvector sketch. | 3 |
| 2 | How are match confidences routed to the teacher? | HIGH auto-populates as an overridable suggestion. MEDIUM is withheld from auto-marking and placed in the Review_Queue, which must be cleared before the Camera_Session can finalize. Below the Unmatched_Floor_Threshold, nothing is suggested; the face is shown as unmatched. | 7, 8 |
| 3 | Is consent required before a student can register a Face_Profile? | Yes. Face_Consent must be on record before any face image is captured or any Face_Embedding is generated for that student. | 1 |
| 4 | When is a student's biometric data purged? | Within the same database transaction as the triggering event: when a student's last ACTIVE Enrollment ends, or when the student explicitly requests deletion. No asynchronous purge job is introduced. | 4 |
| 5 | What happens when matching fails entirely for a session? | The teacher is routed to the existing Mark_Attendance_Screen (attendance-marking spec) to mark the roster manually. This is a fallback to an existing screen, not a new one. | 10 |

## Requirements

### Requirement 1: Record consent before any face capture

**User Story:** As a student, I want to explicitly consent before any biometric data about my face is created, so that my face is never captured or processed without my informed agreement.

#### Acceptance Criteria

1. WHEN a Student requests registration of a Face_Profile for the first time, THE Face_Profile_Service SHALL require that Student to record Face_Consent before capturing any face image.
2. IF a Student requests registration of a Face_Profile while holding no recorded Face_Consent, THEN THE Face_Profile_Controller SHALL respond with HTTP 403 and error code `FORBIDDEN`.
3. WHEN a Student records Face_Consent, THE Face_Profile_Service SHALL persist that Student's identifier and the consent timestamp.
4. THE Face_Profile_Service SHALL generate no Face_Embedding for a Student holding no recorded Face_Consent.
5. WHERE a Student withdraws a previously recorded Face_Consent, THE Face_Profile_Service SHALL treat that withdrawal as a deletion request under Requirement 4.
6. THE Face_Registration_Screen SHALL require the Student to affirm Face_Consent through an explicit action before the camera capture step becomes available.

### Requirement 2: Register a Face_Profile from on-device embeddings

**User Story:** As a student, I want to register my face once, so that the camera attendance flow can recognize me without a teacher re-entering my status every session.

#### Acceptance Criteria

1. WHEN a Student with recorded Face_Consent submits one or more Face_Embedding vectors generated on-device, THE Face_Profile_Service SHALL persist those vectors as that Student's Face_Profile.
2. THE Face_Profile_Service SHALL persist a Face_Embedding as a numeric vector only, accepting no face image as part of a registration request.
3. THE Face_Profile_Service SHALL support storing more than one Face_Embedding per Face_Profile.
4. WHERE a Face_Profile already exists for a Student, THE Face_Profile_Service SHALL replace that Student's existing Face_Embedding rows with the newly submitted set upon a new registration request.
5. WHEN a Face_Profile registration request succeeds, THE Face_Profile_Service SHALL record the registration timestamp for that Face_Profile.
6. IF a Face_Profile registration request submits zero Face_Embedding vectors, THEN THE Face_Profile_Controller SHALL respond with HTTP 400 and error code `BAD_REQUEST`.
7. IF a Face_Profile registration request submits a vector whose dimensionality differs from the dimensionality already stored for that Student's prior Face_Embedding rows, THEN THE Face_Profile_Controller SHALL respond with HTTP 400 and error code `BAD_REQUEST`.
8. THE Face_Profile_Service SHALL permit a Student to register a Face_Profile only for that Student's own identifier.

### Requirement 3: Store Face_Embedding vectors without raw images or server-side vector search

**User Story:** As a system owner, I want biometric storage limited to numeric vectors with no image retention and no new database extension, so that the storage footprint and attack surface stay minimal.

#### Acceptance Criteria

1. THE Face_Profile_Service SHALL persist each Face_Embedding as a plain numeric array column, requiring no vector-similarity database extension.
2. THE Face_Profile_Service SHALL store no face image, in object storage or otherwise, at any point in the registration flow.
3. THE Face_Profile_Service SHALL perform no embedding comparison in the database or in any backend process; comparison occurs only on-device per Requirement 6.
4. THE Face_Profile_Service SHALL expose a Face_Embedding's vector only to a backend process serving a distribution request authorized under Requirement 5, and never to a response consumed outside that distribution path.
5. THE Face_Profile_Service SHALL associate each Face_Embedding with exactly one Student identifier.

### Requirement 4: Purge Face_Embedding data on unenrollment or deletion request

**User Story:** As a student, I want my biometric data removed once I am no longer enrolled anywhere or once I ask for it to be deleted, so that AFORA retains my face data no longer than necessary.

#### Acceptance Criteria

1. WHEN a Student's Enrollment status change results in zero remaining ACTIVE Enrollment rows for that Student across every Class_Section, THE Face_Profile_Service SHALL delete every Face_Embedding row for that Student within the same database transaction as that Enrollment status change.
2. WHEN a Student submits a deletion request for that Student's own Face_Profile, THE Face_Profile_Service SHALL delete every Face_Embedding row for that Student within the same database transaction as processing that request.
3. WHEN the Face_Profile_Service deletes a Student's Face_Embedding rows under this requirement, THE Face_Profile_Service SHALL also delete that Student's recorded Face_Consent.
4. WHEN the Face_Profile_Service deletes a Student's Face_Embedding rows under this requirement, THE Face_Profile_Service SHALL record the deletion timestamp and the triggering reason.
5. IF a Student's Enrollment status changes while at least one other ACTIVE Enrollment row remains for that Student, THEN THE Face_Profile_Service SHALL leave that Student's Face_Embedding rows unchanged.
6. THE Face_Profile_Service SHALL permit a Student to submit a deletion request only for that Student's own Face_Profile.
7. WHERE an ADMIN submits a deletion request naming a specific Student, THE Face_Profile_Service SHALL delete every Face_Embedding row for that Student within the same database transaction as processing that request.

### Requirement 5: Distribute the enrolled embedding set to the owning teacher's device

**User Story:** As a teacher, I want my device to receive the face embeddings of only the students enrolled in my own class section, so that a live session can match against the right, and only the right, students.

#### Acceptance Criteria

1. WHEN the Face_Profile_Controller receives a request for a Class_Section's Enrolled_Embedding_Set, THE Face_Profile_Service SHALL return one entry for each student holding an Enrollment with status ACTIVE in that Class_Section and holding at least one Face_Embedding.
2. THE Face_Profile_Service SHALL omit from the Enrolled_Embedding_Set every student whose Enrollment status in that Class_Section is DROPPED or COMPLETED.
3. THE Face_Profile_Service SHALL omit from the Enrolled_Embedding_Set every ACTIVE-enrolled student holding zero Face_Embedding rows.
4. WHERE the caller's role is TEACHER, THE Face_Profile_Service SHALL serve an Enrolled_Embedding_Set only when the caller is the Owning_Teacher of the requested Class_Section.
5. IF a caller whose role is TEACHER requests an Enrolled_Embedding_Set for a Class_Section of which that caller is not the Owning_Teacher, THEN THE Face_Profile_Controller SHALL respond with HTTP 403 and error code `FORBIDDEN`.
6. WHERE the caller's role is ADMIN, THE Face_Profile_Service SHALL serve the Enrolled_Embedding_Set for any Class_Section.
7. IF the caller's role is STUDENT, THEN THE Face_Profile_Controller SHALL respond with HTTP 403 and error code `FORBIDDEN` for a request for an Enrolled_Embedding_Set.
8. WHERE a Class_Section holds zero ACTIVE-enrolled students with a registered Face_Profile, THE Face_Profile_Service SHALL return an Enrolled_Embedding_Set containing zero entries.
9. THE Face_Profile_Service SHALL include each entry's student identifier alongside that student's Face_Embedding vectors in the Enrolled_Embedding_Set, so the On_Device_Matcher can report which student a match belongs to.

### Requirement 6: Match detected faces against the enrolled embedding set on-device

**User Story:** As a teacher, I want detected faces compared against my class roster using a real biometric comparison, so that the suggested matches reflect who is actually in front of the camera.

#### Acceptance Criteria

1. WHEN a Camera_Session detects a face, THE On_Device_Matcher SHALL generate a Face_Embedding from that detected face entirely on the device conducting the session.
2. WHEN the On_Device_Matcher generates a Face_Embedding for a detected face, THE On_Device_Matcher SHALL compare that embedding against every entry in the Enrolled_Embedding_Set distributed for that Camera_Session's Class_Section.
3. THE On_Device_Matcher SHALL compute a Match_Confidence solely from the comparison between a detected face's generated Face_Embedding and the Enrolled_Embedding_Set, deriving no part of a Face_Match_Candidate's identity or Match_Confidence from a face's tracking identifier or detection index.
4. WHEN the On_Device_Matcher compares a detected face against the Enrolled_Embedding_Set, THE On_Device_Matcher SHALL produce a Face_Match_Candidate naming the enrolled student whose Face_Embedding yields the highest Match_Confidence.
5. THE On_Device_Matcher SHALL transmit no face image and no detected-face embedding to the backend during a Camera_Session's matching.
6. WHILE a Camera_Session is scanning, THE On_Device_Matcher SHALL re-evaluate a tracked face's Face_Match_Candidate across successive sampled frames and SHALL retain the evaluation carrying the highest Match_Confidence for that tracked face.

### Requirement 7: Classify match confidence into HIGH, MEDIUM or unmatched

**User Story:** As a teacher, I want the system to tell me which matches it is confident about, which need my judgment, and which it cannot place at all, so that I know exactly where my attention is required.

#### Acceptance Criteria

1. WHERE a Face_Match_Candidate's Match_Confidence is at or above the High_Confidence_Threshold, THE On_Device_Matcher SHALL set that Face_Match_Candidate's Confidence_Band to HIGH.
2. WHERE a Face_Match_Candidate's Match_Confidence is at or above the Medium_Confidence_Threshold and below the High_Confidence_Threshold, THE On_Device_Matcher SHALL set that Face_Match_Candidate's Confidence_Band to MEDIUM.
3. WHERE a detected face's highest Match_Confidence is below the Unmatched_Floor_Threshold, THE On_Device_Matcher SHALL produce no Face_Match_Candidate for that detected face and SHALL present that detected face as unmatched.
4. THE Unmatched_Floor_Threshold SHALL be less than or equal to the Medium_Confidence_Threshold, and the Medium_Confidence_Threshold SHALL be less than the High_Confidence_Threshold.
5. WHEN a Face_Match_Candidate's Confidence_Band is HIGH, THE Camera_Attendance_Screen SHALL pre-select that candidate's named student as PRESENT, overridable by the Teacher.
6. WHEN a Face_Match_Candidate's Confidence_Band is MEDIUM, THE Camera_Attendance_Screen SHALL add that candidate to the Camera_Session's Review_Queue instead of pre-selecting any AttendanceStatus for the named student.
7. THE Camera_Attendance_Screen SHALL display a detected face presented as unmatched as unrecognized, pre-selecting no student and no AttendanceStatus for it.

### Requirement 8: Require teacher review of medium-confidence matches before finalization

**User Story:** As a teacher, I want to explicitly confirm or reject every medium-confidence match myself, so that a biometric guess the system is not sure about never silently becomes an attendance record.

#### Acceptance Criteria

1. WHEN a Face_Match_Candidate's Confidence_Band is MEDIUM, THE Camera_Attendance_Screen SHALL require the Teacher to either confirm it as the named student's attendance or reject it before that Face_Match_Candidate leaves the Review_Queue.
2. WHEN the Teacher confirms a Review_Queue entry, THE Camera_Attendance_ViewModel SHALL set the named student's selected status to PRESENT and SHALL remove that entry from the Review_Queue.
3. WHEN the Teacher rejects a Review_Queue entry, THE Camera_Attendance_ViewModel SHALL set the named student's selected status to no automatic value and SHALL remove that entry from the Review_Queue.
4. WHILE a Camera_Session's Review_Queue holds at least one unresolved entry, THE Camera_Attendance_Screen SHALL disable the confirm-attendance control for that Camera_Session.
5. IF a confirmation request is submitted for a Camera_Session whose Review_Queue holds at least one unresolved entry, THEN THE Attendance_Controller SHALL respond with HTTP 409 and error code `CONFLICT`.
6. THE Camera_Attendance_Screen SHALL display, for each Review_Queue entry, the candidate student's full name and roll number alongside the detected face.
7. WHERE two or more Face_Match_Candidate values in the same Review_Queue name the same student, THE Camera_Attendance_Screen SHALL present the candidate carrying that student's highest Match_Confidence and SHALL discard the others.

### Requirement 9: Persist camera-derived attendance with a real method and confidence score

**User Story:** As a developer, I want the camera flow to write genuine confidence values into the existing attendance schema, so that `attendance_records.method` and `attendance_records.confidence_score` finally carry real data under `CAMERA`.

#### Acceptance Criteria

1. WHEN the Teacher confirms a Camera_Session, THE system SHALL submit that confirmation through the Attendance_Confirmation flow defined in the attendance-marking specification's Requirement 5, carrying `method = CAMERA` for every submitted entry.
2. WHERE a confirmed entry corresponds to a Face_Match_Candidate, THE system SHALL set that entry's `confidence_score` equal to that Face_Match_Candidate's Match_Confidence.
3. WHERE a confirmed entry carries a status the Teacher set manually with no corresponding Face_Match_Candidate, THE system SHALL set that entry's `confidence_score` to null.
4. THE system SHALL persist a camera-derived Attendance_Record only for a student holding an ACTIVE Enrollment in the Camera_Session's Class_Section, consistent with the attendance-marking specification's Requirement 5.
5. WHEN a Camera_Session's confirmation succeeds, THE system SHALL finalize that Lecture_Session through the same transition and counter recomputation defined in the attendance-marking specification's Requirements 5 and 7.
6. THE system SHALL apply Requirement 8's Review_Queue gating to a Camera_Session's confirmation in addition to, and without altering, the attendance-marking specification's own confirmation preconditions.

### Requirement 10: Fall back to manual roster marking on total matching failure

**User Story:** As a teacher, I want to be dropped back into the familiar manual roster screen if camera matching cannot produce usable results, so that a bad-lighting room or an unregistered class never blocks me from marking attendance.

#### Acceptance Criteria

1. IF the Enrolled_Embedding_Set for a Camera_Session's Class_Section contains zero entries, THEN THE Camera_Attendance_Screen SHALL navigate the Teacher to the Mark_Attendance_Screen for that Lecture_Session without starting a scanning attempt.
2. IF a Camera_Session's scanning attempt completes with zero Face_Match_Candidate values produced for the entire roster, THEN THE Camera_Attendance_Screen SHALL offer the Teacher a control that navigates to the Mark_Attendance_Screen for that Lecture_Session.
3. IF the On_Device_Matcher raises an unrecoverable error during a Camera_Session, THEN THE Camera_Attendance_Screen SHALL navigate the Teacher to the Mark_Attendance_Screen for that Lecture_Session.
4. WHEN the Camera_Attendance_Screen navigates to the Mark_Attendance_Screen under this requirement, THE system SHALL leave the Lecture_Session's SessionStatus at ACTIVE, so the Mark_Attendance_Screen's own confirmation flow applies unchanged.
5. THE Camera_Attendance_Screen SHALL discard no previously Teacher-confirmed Review_Queue decision when navigating to the Mark_Attendance_Screen under this requirement.

### Requirement 11: Authorize every face-profile and camera-matching operation

**User Story:** As a system owner, I want face-registration and embedding-distribution requests restricted to the right student or the right teacher, so that one person's biometric data is never exposed to or altered by someone else.

#### Acceptance Criteria

1. IF the Face_Profile_Controller receives a request carrying no verified Firebase identifier, THEN THE Face_Profile_Controller SHALL respond with HTTP 401 and error code `UNAUTHORIZED`.
2. IF the verified Firebase identifier matches no `users` row, THEN THE Face_Profile_Controller SHALL respond with HTTP 401 and error code `UNAUTHORIZED`.
3. WHERE the caller's role is STUDENT, THE Face_Profile_Controller SHALL permit a Face_Profile registration or deletion request only when the caller's own student identifier equals the identifier named in the request.
4. IF a caller whose role is STUDENT requests registration, deletion, or Face_Consent on behalf of a different student identifier, THEN THE Face_Profile_Controller SHALL respond with HTTP 403 and error code `FORBIDDEN`.
5. WHERE the caller's role is TEACHER, THE Face_Profile_Controller SHALL permit an Enrolled_Embedding_Set request only when the caller is the Owning_Teacher of the requested Class_Section.
6. WHERE the caller's role is ADMIN, THE Face_Profile_Controller SHALL permit every operation defined in this specification.
7. THE Face_Profile_Controller SHALL wrap every response body in the existing `ApiResponse` envelope carrying the `success`, `data` and `error` members.

### Requirement 12: Replace the placeholder index-based matching logic

**User Story:** As a developer, I want the index-based placeholder matching and the fabricated confidence formula removed, so that no future contributor mistakes the placeholder for a working biometric baseline.

#### Acceptance Criteria

1. THE system SHALL replace the index-based face-to-student correlation (`abs(trackingId) % enrolledStudents.size`) in `FaceDetectionAnalyzer.kt` with the On_Device_Matcher defined in Requirement 6.
2. THE system SHALL replace the fixed-base confidence formula in `FaceDetectionAnalyzer.kt`'s `calculateConfidence` with the Match_Confidence defined in Requirement 6.
3. THE system SHALL contain no shipped code path that derives a Face_Match_Candidate's identity from a tracking identifier or frame-detection index rather than from an embedding comparison.
4. THE system SHALL contain no shipped code path that derives a Match_Confidence from a fixed base value rather than from an embedding comparison.
5. THE FaceDeduplicator cross-frame retention logic (keep the highest-confidence evaluation per tracked identity) MAY be reused unchanged, since Requirement 6.6 restates the same retention rule over genuine Match_Confidence values rather than fabricated ones.

### Requirement 13: Register a face profile on the Android screen

**User Story:** As a student, I want a guided on-device screen for consenting and registering my face, so that I can complete enrollment once, correctly, without help from a teacher or administrator.

#### Acceptance Criteria

1. WHEN a Student opens the Face_Registration_Screen, THE Face_Registration_Screen SHALL present the consent affirmation required by Requirement 1 before enabling the camera capture control.
2. WHEN a Student affirms Face_Consent on the Face_Registration_Screen, THE Face_Registration_ViewModel SHALL submit that consent to the Face_Profile_Service before enabling capture.
3. WHEN a face image is captured on the Face_Registration_Screen, THE Face_Registration_ViewModel SHALL generate the Face_Embedding on-device and SHALL transmit only that embedding, never the captured image, to the Face_Profile_Service.
4. WHEN a Face_Profile registration request succeeds, THE Face_Registration_Screen SHALL render a confirmation state naming the number of embeddings registered.
5. IF a Face_Profile registration request fails, THEN THE Face_Registration_Screen SHALL render an error state carrying the `AppError` message and SHALL permit the Student to retry without re-affirming consent already on record.
6. THE Face_Registration_ViewModel SHALL receive its repository dependency through Hilt constructor injection.

### Requirement 14: Reflect real matching and the review queue on the camera attendance screen

**User Story:** As a teacher, I want the live camera screen to show me real matches, a clear review queue, and a path to manual marking, so that I can finish attendance for my class with confidence in what the screen is telling me.

#### Acceptance Criteria

1. WHEN the Camera_Attendance_Screen starts a Camera_Session, THE Camera_Attendance_ViewModel SHALL request the Enrolled_Embedding_Set for that session's Class_Section before enabling scanning.
2. WHILE the Camera_Attendance_Screen is scanning, THE Camera_Attendance_Screen SHALL display the running counts of HIGH-confidence matches, Review_Queue entries, and unmatched detected faces.
3. THE Camera_Attendance_Screen SHALL give each Review_Queue entry a confirm control and a reject control, each satisfying the attendance-marking specification's minimum touch-target requirement.
4. WHEN all Review_Queue entries for a Camera_Session are resolved, THE Camera_Attendance_Screen SHALL enable the confirm-attendance control.
5. THE Camera_Attendance_Screen SHALL give each displayed Face_Match_Candidate a content description naming the candidate student's full name and Confidence_Band.

## Open Items for the Design Phase

These need a technical decision in `design.md` rather than a requirement change.

- The on-device detection and embedding model architecture, and the resulting embedding dimensionality.
- The exact numeric values of the High_Confidence_Threshold, Medium_Confidence_Threshold and Unmatched_Floor_Threshold.
- The exact per-profile Face_Embedding sample count captured during registration.
- The backend column type used to store a Face_Embedding vector (e.g. a delimited numeric array column versus a serialized numeric array in a text column).
- Whether Face_Profile and Face_Embedding persistence lives in a new migration alongside or independent of the attendance-marking migration (V9), and what version number it takes.
- The exact request/response shapes of the Face_Profile_Controller endpoints and of the extension to the attendance-marking confirmation request that carries `method` and `confidence_score` per entry.
- Whether the Enrollment-status-triggered purge of Requirement 4.1 is implemented as a direct call from the enrollment-update transaction or through a transactional event listener, provided the purge remains within the same database transaction.
