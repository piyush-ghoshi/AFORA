# Implementation Plan: face-recognition

## Overview

This plan maps design.md's components and Settled Decisions onto executable tasks, using short unit IDs in the style of the sibling attendance-marking plan: **F1-F10** for backend units, **A1-A5** for Android/shared-client units. Unit IDs are invented for this plan (design.md itself does not number units the way its own Parallel Implementation Plan did for attendance-marking — this design has no equivalent section), but every unit still traces to a named component or Settled Decision in design.md and to requirement numbers in requirements.md.

Design.md's own "File Ownership / Where New Code Lands" table lists files roughly in the order: migration → entities → repositories → service → gate/thresholds → guard → controller → DTO → shared wire → shared repository → on-device matcher → registration screen → the three sibling-owned Android files. Real dependency order mostly agrees with that table, with one deliberate deviation flagged here the same way the sibling plan flagged B13/B14: design.md's table lists `FaceProfileController` before `FaceMatchReviewGate`/`FaceMatchingThresholds`, but the controller has no dependency on the gate at all (the gate is only ever called from `AttendanceConfirmationService`, not from `FaceProfileController`), while `FaceProfileService` genuinely cannot be finished without `FaceMatchingThresholds` existing first (it embeds the three threshold values in `EnrolledEmbeddingSetResponse`). This plan therefore sequences `FaceMatchingThresholds` into Wave 0 alongside persistence, ahead of `FaceProfileService`, rather than placing it later next to the gate as the file-ownership table's listing order might suggest.

The backend track and the Android/shared track are independent of each other except at the two points design.md itself calls out as touching sibling-owned files: the `AttendanceConfirmationService.kt` one-line edit (backend) and the `CameraAttendanceViewModel.kt`/`FaceDetectionAnalyzer.kt`/`CameraAttendanceModels.kt` edits (Android). Both are isolated into their own tasks for exactly that reason.

---

## Binding conventions

These bind every task below. Several are carried forward from the attendance-marking plan because they are project-wide rules, not per-spec choices; the rest are specific to what this design actually requires.

1. **Mocking is MockK only.** `io.mockk:mockk:1.13.8`. Use `mockk()`, `every {}`, `returns`, `verify {}`, `coEvery {}`, `coVerify {}`. **Do not use Mockito, `mockito-kotlin`, `@Mock`, `when(...)` or `thenReturn` anywhere.** This is the same project-wide rule the attendance-marking plan established; it is not negotiable per-spec.
2. **`Optional` is unwrapped with `.orElseThrow {}`, never with elvis (`?:`).** Directly relevant here: `FaceProfileAccessGuard.requireCaller()` duplicates `AttendanceAccessGuard.requireCaller()`'s resolution logic per design.md Settled Decision D, and that resolution logic depends on this exact idiom against `Optional<User>`.
3. **Migration numbering: V11 is correct, and is a restated fact, not re-derived here.** Design.md's Settled Decision A states PostgreSQL is at V10 applied, so this feature's migration is `V11__create_face_recognition_tables.sql`. No existing migration (V1-V10) is renumbered.
4. **`ddl-auto: validate` runs in dev.** V11 column names and every entity's `@Column(name = ...)` must agree exactly, or the context fails at startup rather than at first query — the same risk class design.md names for V11 and the sibling spec names for V9.
5. **Embedding vectors are `TEXT`, comma-delimited, parsed and serialized only inside `FaceProfileService`.** This restates design.md Settled Decision A's constraint as a binding rule: no task may switch `face_embeddings.embedding_vector` to a native `DOUBLE PRECISION[]` or any other array column type, because that column type's JDBC mapping already diverges between H2 (test) and PostgreSQL (dev) under `ddl-auto: validate` — the exact failure class Decision A exists to avoid.
6. **The three confidence thresholds live only in `FaceMatchingThresholds` (backend-owned).** No task may hardcode `0.80`, `0.60`, or any other threshold value anywhere else, including inside `OnDeviceFaceMatcher` or any other Android file. The device receives the three threshold values from `EnrolledEmbeddingSetResponse.highConfidenceThreshold`/`.mediumConfidenceThreshold`/`.unmatchedFloorThreshold` at runtime; it never ships its own copy of these constants, per Settled Decision B.
7. **Endpoint strings in shared repositories are full paths beginning `/api/`.** `ApiClient.buildUrl` prepends only the base URL, matching the existing convention `HttpAttendanceRepository` already uses and that `HttpFaceProfileRepository` must follow identically, e.g. `"/api/face-profiles/consent"`.
8. **The shared `Result` is `com.afora.shared.util.Result`, not `kotlin.Result`.** Failures carry an `AppError`, matching every other repository in this codebase.
9. **Regression baseline is recorded at execution time, not written down here.** Before Wave 0 begins, run the full existing test suite (`backend`, `androidApp`, `shared`) and record the passing count as the baseline for this feature. Every checkpoint task re-runs the full suite and must show the baseline count plus every new test passing, with zero regressions. This document does **not** hardcode a specific test count — any number written here at design time would already be stale by the time this spec executes, since the attendance-marking spec's own tasks may have added tests since its "87 tests" figure was recorded.
10. **Property tests, where used, follow the sibling plan's library discipline exactly, with fresh numbering.** `kotest-property` is already declared as a `testImplementation`/`commonTest` dependency in `backend/build.gradle.kts` and `shared/build.gradle.kts` by the attendance-marking plan's task 1.1 (T0) — **no task in this plan re-adds it**. `checkAll` is called from a plain JUnit test inside `runTest { }`, never via the Kotest runner. Minimum 100 iterations, set explicitly via `PropTestConfig(iterations = 100)`. Each property test carries the tag comment `// Feature: face-recognition, Property N: <property text>` and implements exactly one property. **Property numbers start fresh at 1 for this spec** — they do not continue the sibling plan's 1-48 sequence, because these are a different design document's properties.
11. **`OnDeviceFaceMatcher`'s first shipped implementation is a documented stub that returns null (no match) for every input.** No task in this plan implements real embedding generation or real cosine-similarity matching — model selection and real matching logic are explicitly out of scope per requirements.md's own "Open Items for the Design Phase" and per design.md's Overview ("the concrete on-device detection/embedding model ... are explicitly out of scope"). The task that creates this stub must say so in its own task text, not leave it implicit, and the stub class itself must carry a KDoc comment stating plainly that it is a placeholder and that the camera flow is expected to rely on the Requirement 10 fallback to manual marking (zero `FaceMatchCandidate`s produced for the whole roster, per R10.2) until a real matcher replaces this stub.

### Single-owner files — new files created by this spec

| File | Sole owner task |
|---|---|
| `backend/.../facerecognition/entity/{FaceConsent,FaceProfile,FaceEmbedding,FaceProfileDeletion}.kt` | 2.1 (F1) |
| `backend/.../facerecognition/repository/{FaceConsentRepository,FaceProfileRepository,FaceEmbeddingRepository,FaceProfileDeletionRepository}.kt` | 2.1 (F1) |
| `backend/src/main/resources/db/migration/V11__create_face_recognition_tables.sql` | 2.1 (F1) |
| `backend/.../facerecognition/service/FaceMatchingThresholds.kt` | 3.1 (F2) |
| `backend/.../facerecognition/dto/FaceProfileDto.kt` | 4.1 (F3) |
| `shared/.../data/api/FaceProfileWire.kt` | 4.1 (F3) |
| `backend/.../facerecognition/service/FaceProfileService.kt` | 5.1 (F4) |
| `backend/.../facerecognition/security/FaceProfileAccessGuard.kt` | 6.1 (F5) |
| `backend/.../facerecognition/service/FaceMatchReviewGate.kt` | 7.1 (F6) |
| `backend/.../facerecognition/controller/FaceProfileController.kt` | 8.1 (F7) |
| `shared/.../data/repository/FaceProfileRepository.kt` + `HttpFaceProfileRepository.kt` (androidMain) | 9.1 (F8) |
| `androidApp/.../ui/attendance/camera/OnDeviceFaceMatcher.kt` | 11.1 (A3) |
| `androidApp/.../ui/attendance/registration/{FaceRegistrationScreen,FaceRegistrationViewModel}.kt` | 12.1 (A4) |

### Files already owned by the attendance-marking spec — modified here

**These eight files belong to the attendance-marking spec's own single-owner table. This plan touches each of them exactly once, in an isolated task, for exactly the reason design.md's "Blast radius" / "File Ownership" language already states. If the attendance-marking spec's own tasks.md is ever re-run or re-dispatched concurrently with this plan, these files are a real conflict risk — a human must serialize the two specs' work on these specific files rather than running them in parallel.**

| File | What changes here | Why it's safe |
|---|---|---|
| `backend/.../attendance/dto/AttendanceDto.kt` | Adds `teacherConfirmed: Boolean = false` to `SubmittedAttendanceEntry` | Additive, defaulted field only; no existing field, method signature, or manual-marking call site changes (Settled Decision B) |
| `shared/.../data/api/AttendanceWire.kt` | Same field, mirrored on the shared `SubmittedAttendanceEntry` | Additive, defaulted; kotlinx.serialization decodes any payload omitting it to `false` |
| `backend/.../attendance/service/AttendanceConfirmationService.kt` | One new import (`FaceMatchReviewGate`) and one new line calling `FaceMatchReviewGate.require(request.entries)` as step 7.5, between the existing step 7 and step 8 | Zero changed signatures, zero changed existing steps; `FaceMatchReviewGate` is a parameterless `object`, not a Spring bean, so the service's constructor and every existing mock setup are untouched (Settled Decision B) |
| `androidApp/.../di/RepositoryModule.kt` | Adds one new `provideFaceProfileRepository` binding | Additive provider method, same shape as the sibling's own `provideAttendanceRepository` |
| `androidApp/.../navigation/MainNavigation.kt` | Adds one new route to `Face_Registration_Screen` | Additive route; no existing route signature changes |
| `androidApp/.../ui/attendance/camera/FaceDetectionAnalyzer.kt` | Replaces `matchStudentForFace`'s index arithmetic and `calculateConfidence`'s fixed-base formula with calls into `OnDeviceFaceMatcher` | Scoped, named replacement per Requirement 12; the file's other responsibilities (frame handling, overlay projection) are unchanged |
| `androidApp/.../ui/attendance/camera/CameraAttendanceViewModel.kt` | Adds `reviewQueue`, `confirmEnabled`, `confirmReviewEntry()`, `rejectReviewEntry()`; wires `teacherConfirmed = candidate?.teacherConfirmed ?: false` into the existing `confirmAttendance()` entry-building loop | Additive state and additive functions; the one touched line in `confirmAttendance()` is a single new field assignment, not a restructured loop |
| `androidApp/.../ui/attendance/camera/CameraAttendanceModels.kt` | Adds `teacherConfirmed: Boolean = false` to `DetectedFaceCandidate` | Additive, defaulted field; confirmed as the real file name by reading the source (design.md refers to it as `CameraAttendanceModels.kt`, matching the actual file at `androidApp/src/main/kotlin/com/afora/android/ui/attendance/camera/CameraAttendanceModels.kt`) |

**`FaceDeduplicator.kt` is NOT in this list and no task in this plan may open it (Requirement 12.5).**

---

## Tasks

### Wave 0 — independent persistence and pure constants/DTOs

- [x] 1. Checkpoint — record the regression baseline
  - Run the full existing suite: `./gradlew :backend:test :androidApp:testDebugUnitTest :shared:allTests`
  - Record the passing count. This is the baseline every later checkpoint compares against. Do not write the number into this document — it is recorded at execution time only.
  - **Blocked by: nothing**

- [x] 2. F1: Persistence for Face_Consent, Face_Profile, Face_Embedding, Face_Profile_Deletion
  - [x] 2.1 Write migration V11, the four entities and the four repositories
    - `db/migration/V11__create_face_recognition_tables.sql` — `face_consents`, `face_profiles`, `face_embeddings`, `face_profile_deletions`, exactly as design.md's Settled Decision A specifies: `embedding_vector TEXT NOT NULL`, `CHECK (dimensions > 0)` on `face_profiles` and `face_embeddings`, `CHECK (reason IN (...))` on `face_profile_deletions`, the two indexes (`idx_face_embeddings_student`, `idx_face_profile_deletions_student`), and the four table comments
    - `facerecognition/entity/FaceConsent.kt` — `@Id` on `student_id`, no `@GeneratedValue`
    - `facerecognition/entity/FaceProfile.kt` — `@Id` on `student_id`, `dimensions`/`registeredAt`/`updatedAt`
    - `facerecognition/entity/FaceEmbedding.kt` — `@GeneratedValue(IDENTITY)`, scalar `studentId: Long` FK (not `@ManyToOne`), `updatable = false` on every column except the generated id
    - `facerecognition/entity/FaceProfileDeletion.kt` — `@GeneratedValue(IDENTITY)`, `@Enumerated(EnumType.STRING)` on `reason`
    - `facerecognition/repository/FaceConsentRepository.kt`, `FaceProfileRepository.kt`, `FaceEmbeddingRepository.kt` (including `findByStudentId`, `findByStudentIdIn`, `deleteByStudentId`, `existsByStudentId`), `FaceProfileDeletionRepository.kt`
    - Migration number is **V11**; do not renumber V1-V10
    - `ddl-auto: validate` means V11 column names and `@Column(name = ...)` must agree exactly
    - **Blocked by: nothing**
    - _Requirements: 1.3, 2.4, 2.5, 2.7, 3.1, 3.2, 3.5, 4.3, 4.4_
  - [x]* 2.2 Write `@DataJpaTest` tests for the four new repositories
    - `FaceRecognitionRepositoriesTest.kt`, `@DataJpaTest` + `@ActiveProfiles("test")` on H2
    - `face_profile_deletions.reason` CHECK constraint rejects a value outside the three enum names
    - `face_profiles.dimensions` and `face_embeddings.dimensions` CHECK constraints reject non-positive values
    - `FaceEmbeddingRepository.deleteByStudentId` removes exactly that student's rows and no others
    - `FaceConsentRepository.existsById` is an O(1)-shaped existence check (asserted by behavior, not by query plan)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 2.1 (F1)**
    - _Requirements: 1.3, 2.4, 3.1, 4.4_

- [x] 3. F2: Backend-owned confidence thresholds
  - [x] 3.1 Implement `facerecognition/service/FaceMatchingThresholds.kt`
    - A dependency-free `object` with `HIGH_CONFIDENCE: Double = 0.80`, `MEDIUM_CONFIDENCE: Double = 0.60`, `UNMATCHED_FLOOR: Double = 0.60`, exactly as Settled Decision B specifies, with the KDoc note that these are placeholder values pending real model tuning (requirements.md's own Open Items)
    - This is the **single source of truth** for all three values — no other file in this plan may declare a literal threshold (Binding Convention 6)
    - **Blocked by: nothing**
    - _Requirements: 7.4_
  - [x]* 3.2 Write a unit test asserting the ordering invariant
    - `FaceMatchingThresholdsTest.kt`: `UNMATCHED_FLOOR <= MEDIUM_CONFIDENCE < HIGH_CONFIDENCE` (R7.4)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 3.1 (F2)**
    - _Requirements: 7.4_

- [x] 4. F3: Backend DTOs and shared KMP wire types
  - [x] 4.1 Write `facerecognition/dto/FaceProfileDto.kt` and `shared/.../data/api/FaceProfileWire.kt`
    - Backend DTOs: `FaceConsentResponseDto`, `RegisterFaceProfileRequestDto`, `FaceProfileResponseDto`, `FaceProfileDeletionResponseDto`, `EnrolledEmbeddingSetResponseDto`, `EnrolledEmbeddingEntryDto` — field for field from design.md's Wire Contract
    - Shared types in a new `FaceProfileWire.kt`: `RegisterFaceProfileRequest`, `FaceProfileResponse`, `FaceConsentResponse`, `FaceProfileDeletionResponse`, `EnrolledEmbeddingSetResponse`, `EnrolledEmbeddingEntry` — one `@Serializable` type per payload, no domain mirror, following `AttendanceWire.kt`'s established convention exactly
    - `RegisterFaceProfileRequest`/`Dto` carries **no `studentId` field** — derived entirely from the authenticated caller (Settled Decision D)
    - `EnrolledEmbeddingSetResponse`/`Dto` carries `entries: List<...> = emptyList()` (never null) plus the three threshold fields (`highConfidenceThreshold`, `mediumConfidenceThreshold`, `unmatchedFloorThreshold`)
    - Timestamps are plain `String` (`ISO_LOCAL_DATE_TIME`, no zone) on the shared side and `LocalDateTime` on the backend side, matching the existing convention
    - **Blocked by: nothing**
    - _Requirements: 1.3, 2.1, 2.5, 2.7, 3.4, 4.4, 5.9, 7.4, 13.3_
  - [x]* 4.2 Write DTO/wire serialization tests
    - Backend: `FaceProfileDtoSerializationTest.kt` — Jackson emits timestamps with no zone suffix; `entries` and vector lists serialize as `[]` when empty, never `null`
    - Shared: `shared/src/commonTest/.../FaceProfileWireSerializationTest.kt` — round-trip encode/decode for each of the six wire types
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 4.1 (F3)**
    - _Requirements: 3.4, 13.3_

- [x] 5. Checkpoint — end of wave 0
  - Run the full suite. Confirm baseline count plus all new wave-0 tests pass, zero regressions. Ask the user if questions arise.
  - **Blocked by: 1, 2.1, 2.2, 3.1, 3.2, 4.1, 4.2**

### Wave 1 — service, guard, and the review gate

- [x] 6. F4: `Face_Profile_Service`
  - [x] 6.1 Implement `facerecognition/service/FaceProfileService.kt`
    - `recordConsent(studentId)`: upsert `face_consents` from the existing `Clock` bean (R1.3)
    - `register(studentId, embeddings)`: no-consent → `ForbiddenException` (R1.2, R1.4); zero vectors → `BadRequestException` (R2.6); within-request dimensionality disagreement → `BadRequestException`; mismatch against the stored profile's `dimensions` → `BadRequestException` (R2.7); `deleteByStudentId` then insert the new rows (R2.4); upsert `face_profiles`, preserving the original `registeredAt` on replacement and updating only `updatedAt` (R2.5); return counts only, **never the vectors** (R3.4)
    - `deleteProfile(studentId, reason)`: delete `face_embeddings`, delete `face_consents` (R4.3), delete `face_profiles`, insert one `face_profile_deletions` row (R4.4) — all four writes in one `@Transactional` method so partial deletion cannot occur
    - `enrolledEmbeddingSet(classSectionId)`: reuse `EnrollmentRepository.findActiveRosterByClassSectionId` unchanged (R5.1, R5.2); drop students with zero embeddings (R5.3); return `entries = []` rather than null when none qualify (R5.8); include the three `FaceMatchingThresholds` values in the response (Settled Decision B)
    - `purgeIfLastActiveEnrollmentEnded(studentId)`: a `@Transactional` method with zero call sites in the current codebase today, per Settled Decision C — document in its own KDoc that any future enrollment-mutation feature must call this inline before its own transaction commits
    - **Blocked by: 2.1 (F1), 3.1 (F2), 4.1 (F3)**
    - _Requirements: 1.2, 1.3, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 3.4, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 5.1, 5.2, 5.3, 5.8, 5.9_
  - [x]* 6.2 Write the registration dimensionality and replacement property test
    - `FaceProfileRegistrationPropertyTest.kt`
    - **Property 1: Registration replaces the prior profile and preserves dimensionality rules**
    - Tag: `// Feature: face-recognition, Property 1: Registration replaces the prior profile and preserves dimensionality rules`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over arbitrary non-empty vector sets of consistent and inconsistent dimensionality, and over a pre-existing profile of a different dimensionality; assert acceptance/rejection matches R2.6/R2.7 and that an accepted registration leaves exactly the new vectors stored
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 6.1 (F4)**
    - **Validates: Requirements 2.4, 2.6, 2.7**
  - [x]* 6.3 Write the `deleteProfile()` atomicity test
    - `FaceProfileServiceDeletionAtomicityTest.kt`, following design.md Settled Decision C's own stated test plan: a `@DataJpaTest`-plus-real-transaction technique exercising the two call sites that exist today — student self-delete and admin-named-delete — asserting all four writes (embeddings deleted, consent deleted, profile deleted, one deletion row inserted with the correct reason) happen or none do, using an induced failure after the first write to assert nothing partial survives a rollback
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 6.1 (F4)**
    - _Requirements: 4.2, 4.3, 4.4, 4.6, 4.7_
  - [x]* 6.4 Write the narrow `purgeIfLastActiveEnrollmentEnded` isolation test
    - `PurgeIfLastActiveEnrollmentEndedTest.kt`, following design.md Settled Decision C's own stated scope exactly: given a studentId with zero remaining ACTIVE enrollments, assert it purges; given at least one remaining ACTIVE enrollment, assert it is a no-op
    - State explicitly in the test file's header comment, matching Decision C's own stated gap: this test **cannot** prove requirement 4.1's "same transaction as the enrollment status change" clause, because no code calls this method inside such a transaction today. That gap closes only when a future spec implements enrollment mutation and wires the call
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 6.1 (F4)**
    - _Requirements: 4.1, 4.5_
  - [x]* 6.5 Write the Enrolled_Embedding_Set assembly property test
    - `EnrolledEmbeddingSetPropertyTest.kt`, `@DataJpaTest` on H2
    - **Property 2: The Enrolled_Embedding_Set is exactly the ACTIVE-enrolled, embedding-holding student set**
    - Tag: `// Feature: face-recognition, Property 2: The Enrolled_Embedding_Set is exactly the ACTIVE-enrolled, embedding-holding student set`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over arbitrary enrollment statuses and arbitrary presence/absence of embeddings per student; assert the returned entry set equals exactly the ACTIVE-and-has-embeddings subset, including the empty case
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 6.1 (F4)**
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.8**

- [x] 7. F5: `Face_Profile_Access_Guard`
  - [x] 7.1 Implement `facerecognition/security/FaceProfileAccessGuard.kt`
    - `requireCaller(firebaseUid)`: identical resolution to `AttendanceAccessGuard.requireCaller()` **except STUDENT is permitted** rather than rejected — a deliberate, documented duplication rather than teaching `AttendanceAccessGuard` a face-recognition-specific exception (Settled Decision D)
    - `.orElseThrow {}`, never elvis, against `Optional<User>` (Binding Convention 2)
    - `requireOwnOrAdminStudentId(caller, pathStudentId)`: STUDENT acting on a `pathStudentId` other than their own → `ForbiddenException` (R11.4); ADMIN unrestricted
    - `requireClassSectionAccess(caller, classSectionId)`: TEACHER must own the Class_Section (R5.5, R11.5); STUDENT is always forbidden (R5.7, R11.7); ADMIN unrestricted (R5.6, R11.6)
    - Null uid → `UnauthorizedException` (R11.1); unknown uid → `UnauthorizedException` (R11.2)
    - This guard is reused, not extended, from `AttendanceAccessGuard` — the one shared type is the imported `CallerContext` data class from `com.afora.backend.attendance.security`, not the resolution method itself
    - **Blocked by: nothing** — depends only on `CallerContext`, `UserRepository`, `StudentRepository`, `TeacherRepository`, `ClassSectionRepository`, all of which already exist
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_
  - [x]* 7.2 Write `FaceProfileAccessGuardTest.kt`
    - Following design.md Settled Decision D's own "How this is tested" list exactly, one test per bullet: null UID, unknown UID, STUDENT acting on someone else's studentId, STUDENT calling the embeddings endpoint, TEACHER on a class section they do not own, and ADMIN bypassing every check
    - Header comment on the test file notes the known-and-accepted duplication risk Decision D names: a bug fix later applied to `AttendanceAccessGuard`'s UID-resolution logic could silently fail to be applied here, so a future reader fixing one should check the other
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 7.1 (F5)**
    - _Requirements: 11.1, 11.2, 11.4, 11.5, 11.6, 11.7_

- [x] 8. F6: `Face_Match_Review_Gate` — the security-critical component
  - [x] 8.1 Add `teacherConfirmed: Boolean = false` to `SubmittedAttendanceEntry` and implement `facerecognition/service/FaceMatchReviewGate.kt`
    - Backend DTO (`attendance/dto/AttendanceDto.kt`) and shared type (`shared/.../data/api/AttendanceWire.kt`) both gain `teacherConfirmed: Boolean = false` — additive and defaulted, per the "Files already owned by the attendance-marking spec" table above
    - `FaceMatchReviewGate.require(entries, thresholds = FaceMatchingThresholds)`: for each entry with a non-null `confidenceScore` — score `>= HIGH_CONFIDENCE` passes untouched (R7.5); score `>= MEDIUM_CONFIDENCE` and `!teacherConfirmed` collects into an `unresolved` list (R8.1, R8.2); below `MEDIUM_CONFIDENCE` (i.e. below the matching floor with a score present at all) collects into an `invalid` list. Entries with a null `confidenceScore` are skipped entirely
    - `invalid.isNotEmpty()` → `BadRequestException` naming the student ids (R7.3, R9.3, implied); `unresolved.isNotEmpty()` → `ConflictException` naming the student ids (R8.5)
    - This is gated on `confidenceScore` being present, **not** on `request.method`, so a client cannot bypass the gate by mislabeling the method (Settled Decision B's rejected-alternatives discussion)
    - **Blocked by: 2.1 (F1) is not required; this task depends only on 3.1 (F2) for `FaceMatchingThresholds` and on the `SubmittedAttendanceEntry.teacherConfirmed` field added in this same task**
    - _Requirements: 7.3, 7.5, 8.1, 8.2, 8.5, 9.3_
  - [x]* 8.2 Write `FaceMatchReviewGateTest.kt` — the three branches
    - Branch 1 (HIGH): an entry with `confidenceScore >= HIGH_CONFIDENCE` and `teacherConfirmed = false` passes through `require()` with no exception
    - Branch 2 (MEDIUM, unconfirmed): an entry with `confidenceScore` in `[MEDIUM_CONFIDENCE, HIGH_CONFIDENCE)` and `teacherConfirmed = false` throws `ConflictException` naming that student id
    - Branch 3 (below floor with a score): an entry with a non-null `confidenceScore` below `MEDIUM_CONFIDENCE` throws `BadRequestException` naming that student id
    - Also assert: an entry with `confidenceScore` in the MEDIUM band and `teacherConfirmed = true` passes; an entry with `confidenceScore = null` (manual marking) is never inspected regardless of its `teacherConfirmed` value; a batch mixing all three branches reports every offending id, not just the first
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 8.1 (F6)**
    - _Requirements: 7.3, 7.5, 8.1, 8.2, 8.5, 9.3_
  - [x]* 8.3 Write the gate property test
    - `FaceMatchReviewGatePropertyTest.kt`
    - **Property 3: The review gate's verdict is a pure function of each entry's score and confirmation flag**
    - Tag: `// Feature: face-recognition, Property 3: The review gate's verdict is a pure function of each entry's score and confirmation flag`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over arbitrary entry lists combining confidence scores across and outside the three bands with arbitrary `teacherConfirmed` values; assert the gate accepts if and only if every scored entry is either `>= HIGH_CONFIDENCE`, or in `[MEDIUM_CONFIDENCE, HIGH_CONFIDENCE)` with `teacherConfirmed = true`, and rejects with the correct exception type and the exact offending id set otherwise
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 8.1 (F6)**
    - **Validates: Requirements 7.3, 7.5, 8.1, 8.2, 8.5**

- [x] 9. Checkpoint — end of wave 1
  - Run the full suite. Confirm baseline count plus all new wave-0 and wave-1 tests pass, zero regressions. Ask the user if questions arise.
  - **Blocked by: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 8.1, 8.2, 8.3**

### Wave 2 — controller, the sibling-file gate wiring, and the shared client repository

- [x] 10. F7: `Face_Profile_Controller`
  - [x] 10.1 Implement `facerecognition/controller/FaceProfileController.kt`
    - `@RequestMapping("/face-profiles")`; four endpoints: `POST /consent`, `POST /embeddings`, `DELETE /{studentId}`, `GET /class-sections/{classSectionId}/enrolled-embeddings`
    - Each method: resolve the caller through `FaceProfileAccessGuard`, resolve/compare the identifier the guard requires for that operation, delegate to `FaceProfileService`, wrap in `ApiResponse.success(...)`
    - `recordConsent`/`register` resolve the studentId from the authenticated caller only — never from a request body field (Settled Decision D)
    - `DELETE /{studentId}` is the one operation naming a student in the path, so an admin can target someone other than themselves (R4.7)
    - **Blocked by: 6.1 (F4), 7.1 (F5)**
    - _Requirements: 1.1, 1.2, 2.1, 2.6, 2.7, 2.8, 4.2, 4.7, 5.4, 5.5, 5.6, 5.7, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_
  - [x]* 10.2 Write MockMvc authorization and status-code tests
    - `FaceProfileControllerAuthorizationTest.kt` with a MockK'd `FaceProfileService` and `FaceProfileAccessGuard`: 401 (no principal), 401 (unknown uid), 403 (STUDENT on another student's id), 403 (STUDENT on the embeddings-distribution endpoint), 403 (TEACHER on a non-owned class section), 400 (zero embeddings, dimensionality mismatch), 403 (register with no consent), success paths for STUDENT/TEACHER/ADMIN as applicable per endpoint
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 10.1 (F7)**
    - _Requirements: 1.2, 2.6, 2.7, 4.7, 5.5, 5.7, 11.1, 11.2, 11.4, 11.5, 11.6, 11.7_

- [x] 11. F6-wiring: the one-line edit to the sibling-owned `AttendanceConfirmationService.kt`
  - [x] 11.1 Add the `FaceMatchReviewGate.require(...)` call as step 7.5
    - **This task touches a file owned by the attendance-marking spec and is deliberately isolated into its own task for exactly that reason — do not bundle it with any other change.**
    - One new import: `com.afora.backend.facerecognition.service.FaceMatchReviewGate`
    - One new line, placed immediately after the existing step 7 (the ACTIVE-status gate / replay-or-conflict resolution) and before the existing step 8 (membership and completeness validation): `FaceMatchReviewGate.require(request.entries)`
    - Zero changed signatures, zero changed existing steps, zero new constructor dependencies (the gate is a parameterless `object`)
    - **Blocked by: 8.1 (F6)**
    - **Sole owner of this edit to `attendance/service/AttendanceConfirmationService.kt` for this spec.** No other task in this plan opens this file
    - _Requirements: 8.5, 9.6_
  - [x]* 11.2 Write an integration test proving step ordering and transaction placement
    - `AttendanceConfirmationFaceGateOrderingTest.kt`: a confirmation with an unresolved MEDIUM entry is rejected with 409 and writes nothing; a replay of an already-accepted confirmation (identical fingerprint) returns the stored response without re-running the gate; the gate runs inside the same transaction and behind the same pessimistic lock as the rest of `confirm()` — a rejection here leaves no attendance record behind
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 11.1 (F6-wiring)**
    - _Requirements: 8.5, 9.6_

- [x] 12. F8: Shared `FaceProfileRepository` interface and `HttpFaceProfileRepository`
  - [x] 12.1 Implement `shared/.../data/repository/FaceProfileRepository.kt` and `HttpFaceProfileRepository.kt` (androidMain)
    - Interface: `recordConsent()`, `registerFaceProfile(embeddings)`, `deleteFaceProfile(studentId)`, `getEnrolledEmbeddingSet(classSectionId)`, each returning `com.afora.shared.util.Result<...>` (Binding Convention 8)
    - `HttpFaceProfileRepository` follows `HttpAttendanceRepository`'s pattern: constructor-injected `ApiClient`, full `/api/face-profiles/...` endpoint strings (Binding Convention 7), `result.data.data` unwrapping, `AppError` on failure
    - **Blocked by: 4.1 (F3)**
    - _Requirements: 1.3, 2.1, 2.5, 4.2, 4.7, 5.9, 13.3_
  - [x]* 12.2 Write `HttpFaceProfileRepositoryTest.kt`
    - `MockEngine`-based: one example per member asserting the exact `/api/face-profiles/...` path and method; a malformed 200 body is handled consistently with `HttpAttendanceRepository`'s existing `ApiClient` deserialization-failure behavior (no new branch added here — this task only verifies the existing shared `ApiClient` branch applies to this repository too)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 12.1 (F8)**
    - _Requirements: 1.3, 2.1, 4.2_

- [x] 13. A1: Hilt binding for `HttpFaceProfileRepository`
  - [x] 13.1 Add `provideFaceProfileRepository` to `RepositoryModule.kt`
    - One additive provider method binding `HttpFaceProfileRepository` to the shared `FaceProfileRepository` interface
    - **This touches a file owned by the attendance-marking spec; isolated into its own task per the single-owner table above.**
    - **Blocked by: 12.1 (F8)**
    - **Sole owner of this edit to `androidApp/.../di/RepositoryModule.kt` for this spec.** No other task in this plan opens this file
    - _Requirements: 13.6_

- [x] 14. Checkpoint — end of wave 2
  - Run the full suite. Confirm baseline count plus all new wave-0, wave-1 and wave-2 tests pass, zero regressions. Ask the user if questions arise.
  - **Blocked by: 10.1, 10.2, 11.1, 11.2, 12.1, 12.2, 13.1**

### Wave 3 — on-device matcher stub, camera wiring, registration screen, navigation

- [x] 15. A3: `OnDeviceFaceMatcher` interface and the no-match stub
  - [x] 15.1 Implement `OnDeviceFaceMatcher.kt`, `EnrolledEmbedding`, `FaceMatchCandidate`, `ConfidenceBand`
    - **This task implements a documented stub, not real matching — stated explicitly here per Binding Convention 11.** `OnDeviceFaceMatcher` is declared as an interface (`generateEmbedding(face, frame): FloatArray`, `match(embedding, enrolledSet): FaceMatchCandidate?`); the shipped implementation class (e.g. `StubFaceMatcher`) returns an empty/zero `FloatArray` from `generateEmbedding` and `null` from `match` for every input, unconditionally
    - The stub class carries a KDoc comment stating plainly: this is a placeholder with no real embedding generation or comparison; until a real matcher replaces it, every detected face resolves to "no candidate," and the camera flow is expected to rely on the Requirement 10 fallback to `Mark_Attendance_Screen` (R10.2: zero `FaceMatchCandidate`s produced for the whole roster)
    - `EnrolledEmbedding(studentId, rollNumber, fullName, vectors: List<FloatArray>)` and `FaceMatchCandidate(studentId, rollNumber, fullName, matchConfidence, confidenceBand, trackingId)` are defined in this same file, in the `androidApp/.../ui/attendance/camera` package, since they are consumed only by Android camera-flow code and have no shared-KMP consumer
    - `ConfidenceBand` has exactly two values, `HIGH` and `MEDIUM` — below-floor never materializes a candidate at all, by contract
    - No threshold value is hardcoded anywhere in this file (Binding Convention 6)
    - **Blocked by: nothing structurally; sequenced after wave 2 because `EnrolledEmbedding` is conceptually the on-device mirror of the wire-level `EnrolledEmbeddingEntry` from F3/F8**
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 7.1, 7.2, 7.3, 12.1, 12.2, 12.3, 12.4_
  - [x]* 15.2 Write `OnDeviceFaceMatcherStubTest.kt`
    - Asserts the stub returns `null` from `match(...)` for every generated input, including an empty `enrolledSet` and a non-empty one, and that `generateEmbedding(...)` never throws
    - Asserts, by reading the stub class's own KDoc via a simple string/documentation-presence check or by convention review comment, that the placeholder status is documented — this is a cheap guard against a future edit silently turning the stub into a half-real matcher without updating its documentation
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 15.1 (A3)**
    - _Requirements: 6.4, 7.3_

- [x] 16. A2: Camera flow wiring — `FaceDetectionAnalyzer.kt`, `CameraAttendanceViewModel.kt`, `CameraAttendanceModels.kt`
  - [x] 16.1 Replace the index-arithmetic placeholder and wire the Review_Queue
    - **This task touches three files owned by the attendance-marking spec; isolated into its own task per the single-owner table above, and bundled together here only because all three changes are one coherent feature slice (the camera matching replacement) rather than three unrelated edits.**
    - `CameraAttendanceModels.kt`: add `teacherConfirmed: Boolean = false` to `DetectedFaceCandidate`
    - `FaceDetectionAnalyzer.kt`: replace `matchStudentForFace`'s `abs(trackingId) % enrolledStudents.size` and `calculateConfidence`'s fixed `0.88` base with calls into `OnDeviceFaceMatcher.generateEmbedding(...)`/`.match(...)` from task 15.1. Since the stub always returns `null`, every detected face in the current shipped state resolves to unmatched — this is expected and is exactly what Requirement 10's fallback exists for
    - `CameraAttendanceViewModel.kt`: add `reviewQueue: Map<Long, FaceMatchCandidate>` to `Ready` state, `confirmEnabled: Boolean get() = reviewQueue.isEmpty()` (R8.4); route each detection batch — HIGH into `candidates` pre-selected PRESENT (R7.5), MEDIUM into `reviewQueue` only (R7.6), null/no-candidate touches neither map; `confirmReviewEntry(studentId)` moves an entry into `candidates` with `teacherConfirmed = true` (R8.2); `rejectReviewEntry(studentId)` removes it from `reviewQueue` with no automatic status (R8.3); where two MEDIUM candidates in the queue name the same student, keep the higher `matchConfidence` one (R8.7); wire `teacherConfirmed = candidate?.teacherConfirmed ?: false` into the existing `confirmAttendance()` entry-building loop — the one line connecting the client-side UX gate to the real server-side `FaceMatchReviewGate`
    - Fallback routing (R10.1-R10.5): an empty Enrolled_Embedding_Set or a zero-candidate scanning pass or an unrecoverable matcher error all navigate to `Mark_Attendance_Screen` carrying the `sessionId`; `SessionStatus` stays ACTIVE; `reviewQueue`/`candidates` are not cleared before navigating
    - `ConfidenceCategory.LOW` is left in place but unused, not deleted (out of scope per design.md)
    - **Blocked by: 12.1 (F8) for fetching the Enrolled_Embedding_Set, 15.1 (A3) for the matcher interface**
    - **Sole owner of this edit to `FaceDetectionAnalyzer.kt`, `CameraAttendanceViewModel.kt`, `CameraAttendanceModels.kt` for this spec.** No other task in this plan opens these three files
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 7.1, 7.2, 7.3, 7.5, 7.6, 7.7, 8.2, 8.3, 8.4, 8.7, 9.2, 9.3, 10.1, 10.2, 10.3, 10.4, 10.5, 12.1, 12.2, 12.3_
  - [x]* 16.2 Write the Review_Queue routing property test
    - `ReviewQueueRoutingPropertyTest.kt`
    - **Property 4: Confidence band determines routing between `candidates` and `reviewQueue`, exclusively**
    - Tag: `// Feature: face-recognition, Property 4: Confidence band determines routing between candidates and reviewQueue, exclusively`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over arbitrary detection batches with HIGH/MEDIUM/null candidates; assert every HIGH lands only in `candidates` pre-selected PRESENT, every MEDIUM lands only in `reviewQueue`, every null touches neither, and `confirmEnabled` is false if and only if `reviewQueue` is non-empty
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 16.1 (A2)**
    - **Validates: Requirements 7.5, 7.6, 7.7, 8.4**
  - [x]* 16.3 Write the duplicate-MEDIUM-candidate resolution property test
    - `DuplicateMediumCandidatePropertyTest.kt`
    - **Property 5: Among duplicate MEDIUM candidates for one student, the highest-confidence entry wins**
    - Tag: `// Feature: face-recognition, Property 5: Among duplicate MEDIUM candidates for one student, the highest-confidence entry wins`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over pairs/sets of MEDIUM candidates naming the same student with distinct `matchConfidence` values
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 16.1 (A2)**
    - **Validates: Requirements 8.7**
  - [x]* 16.4 Write fallback-navigation unit tests
    - `CameraAttendanceFallbackTest.kt`: empty Enrolled_Embedding_Set navigates without starting scanning (R10.1); zero candidates across the whole roster after a scanning pass offers the fallback control (R10.2); a simulated unrecoverable matcher error navigates immediately (R10.3); in every case `SessionStatus` remains ACTIVE and `reviewQueue`/`candidates` are not cleared (R10.4, R10.5)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 16.1 (A2)**
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_
  - [x]* 16.5 Write the `teacherConfirmed` wiring unit test
    - `ConfirmAttendanceTeacherConfirmedWiringTest.kt`: a `confirmAttendance()` call with a mix of HIGH-auto, teacher-confirmed-MEDIUM, and manually-set-with-no-candidate entries produces `SubmittedAttendanceEntry.teacherConfirmed` values that exactly match each entry's resolved candidate state
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 16.1 (A2)**
    - _Requirements: 9.2, 9.3_

- [x] 17. A4: `Face_Registration_Screen` and `Face_Registration_ViewModel`
  - [x] 17.1 Implement the registration screen and view model
    - `Face_Registration_ViewModel`: Hilt constructor injection of `FaceProfileRepository` (R13.6); on consent affirmation, calls `recordConsent()` before enabling capture (R13.2); on capture, generates the embedding on-device via `OnDeviceFaceMatcher.generateEmbedding(...)` and transmits only the embedding, never the image, to `registerFaceProfile(...)` (R13.3); on success, exposes a confirmation state naming the embedding count (R13.4); on failure, exposes an error state carrying the `AppError` message and permits retry without re-affirming already-recorded consent (R13.5)
    - `Face_Registration_Screen`: presents the consent affirmation before enabling the camera capture control (R1.6, R13.1)
    - Because the matcher is the task-15 stub, `generateEmbedding` returns a placeholder vector during this wave — registration still exercises the full consent/capture/transmit/confirm flow end to end, it just transmits a stub-generated vector until a real matcher replaces task 15's implementation. State this plainly in the task's own commit/PR description, matching Binding Convention 11
    - **Blocked by: 12.1 (F8), 15.1 (A3)**
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6, 2.1, 2.2, 2.3, 13.1, 13.2, 13.3, 13.4, 13.5, 13.6_
  - [x]* 17.2 Write `FaceRegistrationViewModelTest.kt`
    - Consent-before-capture ordering (R1.6, R13.2); the embedding-only transmission assertion — no image bytes ever reach the repository call (R13.3); the success state's embedding count (R13.4); the error state retains the already-recorded consent and permits retry without re-affirming it (R13.5)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 17.1 (A4)**
    - _Requirements: 1.6, 13.2, 13.3, 13.4, 13.5_

- [x] 18. A5: Navigation wiring
  - [x] 18.1 Add the route to `Face_Registration_Screen`
    - One new route in `MainNavigation.kt` for `Face_Registration_Screen`
    - **This touches a file owned by the attendance-marking spec; isolated into its own task per the single-owner table above.**
    - **Blocked by: 17.1 (A4)**
    - **Sole owner of this edit to `androidApp/.../navigation/MainNavigation.kt` for this spec.** No other task in this plan opens this file
    - _Requirements: 13.1_
  - [x]* 18.2 Write a navigation unit test
    - `FaceRegistrationNavigationTest.kt`: the new route resolves to `Face_Registration_Screen`
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 18.1 (A5)**
    - _Requirements: 13.1_

- [x] 19. Final checkpoint — end-to-end regression guarantee
  - Run the full suite: `./gradlew :backend:test :androidApp:testDebugUnitTest :shared:allTests`
  - Confirm the baseline count recorded in task 1, plus every new test from waves 0-3, all pass with zero regressions. If any pre-existing test fails, the cause is a contract change this plan expected to be additive — report it rather than editing the existing test
  - Confirm by inspection that `FaceDeduplicator.kt` was opened by no task in this plan
  - Ask the user if questions arise
  - **Blocked by: 14, 15.1, 15.2, 16.1, 16.2, 16.3, 16.4, 16.5, 17.1, 17.2, 18.1, 18.2**

---

## Notes

- Tasks marked with `*` are optional test sub-tasks and can be skipped for a faster MVP. One of them is load-bearing for the stub-documentation guarantee: **15.2** is the only automated check that the `OnDeviceFaceMatcher` stub stays a documented no-match placeholder. If optional tests are skipped, skip them as a group rather than individually, and keep 15.2 regardless.
- Unit IDs (`F1`-`F8`, `A1`-`A5`) are invented for this plan; design.md has no equivalent unit-numbering section of its own for this feature, unlike the attendance-marking design's "Parallel Implementation Plan."
- Eight files are shared with the attendance-marking spec's own single-owner table (see "Files already owned by the attendance-marking spec" above). Each is touched by exactly one isolated task in this plan. Running this plan concurrently with a live re-dispatch of the attendance-marking spec's own tasks is a real conflict risk on those eight files; serialize the two specs' work on them.
- `FaceMatchingThresholds` is the single source of the three confidence values; no task may hardcode a threshold anywhere else, including in Android code (Binding Convention 6).
- `OnDeviceFaceMatcher`'s shipped implementation in this plan is a documented no-match stub (Binding Convention 11); real embedding generation and comparison are out of scope per requirements.md's Open Items.
- The regression baseline is recorded at execution time (task 1), not written into this document, because no fresh test count for this repository has been verified as of when this plan was authored.
- Property tests use `kotest-property` (already declared by the sibling spec's T0 task) as a library with `checkAll` inside `runTest { }`. Property numbering for this spec starts fresh at 1 and does not continue the sibling plan's 1-48 sequence.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["2.1", "3.1", "4.1"] },
    { "id": 1, "tasks": ["2.2", "3.2", "4.2", "6.1", "7.1", "8.1"] },
    { "id": 2, "tasks": ["6.2", "6.3", "6.4", "6.5", "7.2", "8.2", "8.3", "10.1", "12.1"] },
    { "id": 3, "tasks": ["10.2", "11.1", "12.2", "15.1"] },
    { "id": 4, "tasks": ["11.2", "13.1", "16.1", "17.1"] },
    { "id": 5, "tasks": ["15.2", "16.2", "16.3", "16.4", "16.5", "17.2", "18.1"] },
    { "id": 6, "tasks": ["18.2"] }
  ]
}
```

## Completion Summary (Task 19 Final Checkpoint)

- **All 53 tasks completed (100%)**.
- **Final Test Suite Counts**:
  - **Backend**: 165 / 165 passing (100% success rate, 0 failures, 0 ignored)
  - **AndroidApp**: 143 / 143 passing (100% success rate, 0 failures, 0 ignored)
  - **Shared**: 34 / 34 passing (100% success rate, 0 failures, 0 ignored)
  - **Total**: 342 / 342 tests passing across the entire repository.
- **Task 16.2 Resolution**:
  - Restored and fixed [ReviewQueueRoutingPropertyTest.kt](file:///androidApp/src/test/kotlin/com/afora/android/ui/attendance/camera/ReviewQueueRoutingPropertyTest.kt).
  - Corrected `createViewModel` helper to return the `CameraAttendanceViewModel` instance with non-empty enrolled embedding set mock.
  - Fixed coroutine scope dispatcher advancement to `testDispatcher.scheduler.advanceUntilIdle()`.
  - All 5 Property 4 test assertions passed.
- **Zero Regressions**:
  - Verified no regressions across all backend, shared, and androidApp suites.
  - Verified [FaceDeduplicator.kt](file:///androidApp/src/main/kotlin/com/afora/android/ui/attendance/camera/FaceDeduplicator.kt) was untouched.
