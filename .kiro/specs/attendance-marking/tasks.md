# Implementation Plan: attendance-marking

## Overview

This plan maps the design's **Parallel Implementation Plan** onto executable tasks one-for-one. The design's unit IDs are preserved in every task title (`B1`–`B14` backend, `C1`–`C8` client, `T0` test dependency) so a task and its design section stay cross-referenceable.

Every task states the task IDs that block it. Every task that touches one of the ten contested files declares sole ownership of that file. Those two facts are what make concurrent dispatch safe, and they are the highest-risk failure mode if ignored.

Execution order follows the design's six waves. The one place this plan sequences more strictly than the design's wave table: the design lists **B13** and **B14** both in wave 3 while its own dependency table has B14 blocked by B13. The dependency is real, so B14 moves one wave later here.

---

## Binding conventions

These bind every task below. They exist because two of them have already cost this project debugging cycles.

1. **Mocking is MockK.** `io.mockk:mockk:1.13.8`. Use `mockk()`, `every {}`, `returns`, `verify {}`, `coEvery {}`, `coVerify {}`. **Do not use Mockito, `mockito-kotlin`, `@Mock`, `when(...)` or `thenReturn` anywhere.** A test written against `mockito-kotlin` will not compile.
2. **`semesterId` is `String` on both sides**, `uuid` in the database. Never `Long`. If a signature seems to want a `Long`, the signature is wrong.
3. **`Optional` is unwrapped with `.orElseThrow {}`**, never with elvis (`?:`). `findByFirebaseUid`, `studentRepository.findByUserId` and `teacherRepository.findByUserId` all return `Optional`.
4. **JPQL enum literals are fully qualified**, e.g. `com.afora.backend.attendance.entity.LeaveStatus.APPROVED`. Prefer a derived query or a bound parameter. Avoid entity `JOIN ... ON`.
5. **`ddl-auto: validate` runs in dev.** V9 column names and `@Column(name = ...)` must agree exactly, or the context fails at startup rather than at first query.
6. **Endpoint strings in shared repositories are full paths beginning `/api/`**, because `ApiClient.buildUrl` prepends only the base URL.
7. **The shared `Result` is `com.afora.shared.util.Result`**, not `kotlin.Result`, and failures carry an `AppError`.
8. **Migration numbering:** PostgreSQL is at **V8 applied**. The new migration is **V9**. No existing migration is renumbered.
9. **Types come from the design's Wire Contract**, never from inference. If an implementation disagrees with a Wire Contract table, the implementation is wrong.
10. **Regression guard:** 87 tests currently pass (55 backend, 32 Android). The design verified by search that the R11 shared-contract corrections break zero existing call sites. Any task touching a shared type **re-verifies by search** rather than assuming, and reports if a call site exists.
11. **Property tests use `kotest-property` 5.8.0 as a library, not as a runner.** `checkAll` is called from a plain JUnit test inside `runTest { }`, so the existing JUnit suites keep running unchanged. Minimum **100 iterations**, set explicitly via `PropTestConfig(iterations = 100)`. Each property test carries the tag comment `// Feature: attendance-marking, Property N: <property text>` and implements exactly one design property.

### Single-owner files

Ten contested files, one owner each. **No task other than the declared owner may open these files.**

| File | Sole owner task |
|---|---|
| `backend/.../academic/repository/LectureSessionRepository.kt` | 4.1 (B3) |
| `backend/.../academic/repository/EnrollmentRepository.kt` | 4.1 (B3) |
| `backend/.../attendance/repository/AttendanceRecordRepository.kt` | 4.1 (B3) |
| `backend/.../attendance/repository/LeaveRequestRepository.kt` | 4.1 (B3) |
| `shared/.../domain/model/AttendanceRecord.kt` | 8.1 (C1) |
| `shared/.../data/api/request/AttendanceRequests.kt` | 8.1 (C1) |
| `shared/.../data/api/ApiClient.kt` | 9.1 (C3) |
| `shared/.../data/repository/AttendanceRepository.kt` | 15.1 (C2) |
| `androidApp/.../di/RepositoryModule.kt` | 24.1 (C5) |
| `androidApp/.../navigation/MainNavigation.kt` | 26.1 (C8) |

Plus: `backend/build.gradle.kts` and `shared/build.gradle.kts` are owned solely by **1.1 (T0)**, and the existing `DashboardServiceTest.kt` is owned solely by **27.4**. Everything else in this feature is a new file with exactly one author.

---

## Tasks

### Wave 0 — independent foundations (nine units, no blockers)

- [x] 1. T0: Add the property-testing dependency
  - [x] 1.1 Declare `kotest-property` 5.8.0 in the two build files
    - Add `testImplementation("io.kotest:kotest-property-jvm:5.8.0")` to `backend/build.gradle.kts`
    - Add `io.kotest:kotest-property:5.8.0` to the `commonTest` source set in `shared/build.gradle.kts`
    - Library only — do **not** add the Kotest runner, the Kotest Gradle plugin, or change the JUnit platform configuration. The 55 backend and 32 Android JUnit tests must keep running unchanged
    - Android unit tests reuse `kotest-property` transitively; `androidApp/build.gradle.kts` is not modified
    - **Blocked by: nothing**
    - **Sole owner of `backend/build.gradle.kts` and `shared/build.gradle.kts`.** No other task may modify a Gradle build file in this feature
    - Verify with `./gradlew :backend:test :androidApp:testDebugUnitTest :shared:allTests` that all 87 existing tests still pass after the dependency change
    - _Requirements: 11.x test infrastructure (supports every property task below)_

- [x] 2. B1: Persistence for the two new stores
  - [x] 2.1 Write migration V9, the two entities and the two repositories
    - `backend/src/main/resources/db/migration/V9__create_attendance_marking_tables.sql` — `attendance_submissions` and `attendance_audit_entries` exactly as the design's Data Models section specifies, including `CONSTRAINT chk_audit_status_changed CHECK (previous_status <> new_status)`, the four indexes, and the retention comments
    - `attendance/entity/AttendanceSubmission.kt` — `@Id` on `submission_id`, **no `@GeneratedValue`** (the key is client-supplied), scalar `Long` FK columns, every constructor parameter defaulted
    - `attendance/entity/AttendanceAuditEntry.kt` — `@GeneratedValue(IDENTITY)`, scalar FK columns, `updatable = false` on every column as the JPA-level expression of append-only
    - `attendance/repository/AttendanceSubmissionRepository.kt` and `attendance/repository/AttendanceAuditEntryRepository.kt`, including `findByLectureSessionIdOrderByChangedAtAsc`, `findStatusChangedRecordIds` and `countByAttendanceRecordId`
    - Migration number is **V9**; do not renumber V1–V8
    - `ddl-auto: validate` means V9 column names and `@Column(name = ...)` must agree exactly — mismatch fails at startup
    - Neither table gets `updated_at` or the update trigger; both are append-only
    - **Blocked by: nothing**
    - _Requirements: 6.4, 6.12, 9.6, 9.13, 9.15, 9.16_
  - [ ]* 2.2 Write `@DataJpaTest` tests for the two new repositories
    - `AttendanceSubmissionRepositoryTest.kt` and `AttendanceAuditEntryRepositoryTest.kt`, `@DataJpaTest` + `@ActiveProfiles("test")` on H2
    - Smoke test for 6.12: inserting the same `submissionId` twice fails at the database level
    - Test that `chk_audit_status_changed` rejects an entry whose previous and new status are equal (9.13)
    - Test `findByLectureSessionIdOrderByChangedAtAsc` ordering (9.16) and `findStatusChangedRecordIds` distinctness (9.14)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 2.1 (B1)**
    - _Requirements: 6.12, 9.13, 9.14, 9.16_

- [x] 3. B2: The injected time source
  - [x] 3.1 Add the `Clock` bean
    - `common/config/TimeConfig.kt` with a single `@Bean fun clock(): Clock = Clock.systemDefaultZone()`
    - This is the one time source of criterion 2.10; every collaborator derives both `LocalDate.now(clock)` and `LocalDateTime.now(clock)` from it
    - **Blocked by: nothing**
    - _Requirements: 2.10_
  - [ ]* 3.2 Write the single-bean context test
    - `TimeConfigTest.kt` — a Spring context test asserting exactly one `Clock` bean is contributed (2.10)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 3.1 (B2)**
    - _Requirements: 2.10_

- [x] 4. B3: Add query methods to four existing repositories
  - [x] 4.1 Add the four additive queries
    - `LectureSessionRepository.findByIdForUpdate` — `@Lock(LockModeType.PESSIMISTIC_WRITE)` with an **explicit `@Query`**; `@Lock` on a derived `findById` does not reliably emit `FOR UPDATE`
    - `EnrollmentRepository.findActiveRosterByClassSectionId` — fetch-joins student → user to avoid N+1
    - `AttendanceRecordRepository.countByLectureSessionIdAndStatusIn`, `countByLectureSessionId`, `findByLectureSessionIdAndStudentIdIn` — **derived** queries, so statuses travel as bound parameters and no enum literal appears in JPQL
    - `LeaveRequestRepository.findApprovedCovering` — carries the whole covering predicate including the nullable section qualifier, with the enum literal **fully qualified** as `com.afora.backend.attendance.entity.LeaveStatus.APPROVED`, and both date comparisons inclusive
    - Additive only: do not alter an existing method signature or query. The 55 backend tests depend on them
    - **Blocked by: nothing**
    - **Sole owner of `LectureSessionRepository.kt`, `EnrollmentRepository.kt`, `AttendanceRecordRepository.kt` and `LeaveRequestRepository.kt`.** No other task may open these four files
    - _Requirements: 1.1, 4.1, 6.11, 7.2, 7.3, 7.4, 7.9, 9.5_
  - [ ]* 4.2 Write `@DataJpaTest` tests for the four added queries
    - `AttendanceMarkingQueriesTest.kt`, `@DataJpaTest` + `@ActiveProfiles("test")` on H2
    - `findApprovedCovering`: inclusive `fromDate`/`toDate` bounds, null `classSectionId` matching any section, PENDING/REJECTED/CANCELLED excluded
    - `countByLectureSessionId*`: returns 0 over an empty record set (7.9)
    - `findActiveRosterByClassSectionId`: DROPPED and COMPLETED enrollments excluded
    - `findByIdForUpdate`: returns the session row (lock behaviour itself is proven by 27.5, not here)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 4.1 (B3)**
    - _Requirements: 1.1, 1.2, 4.1, 4.3, 7.9_

- [x] 5. B4: Backend DTOs from the normative Wire Contract tables
  - [x] 5.1 Write `attendance/dto/AttendanceDto.kt`
    - `RosterResponse`, `RosterEntry`, `SessionSummary`, `ConfirmAttendanceRequestDto`, `SubmittedAttendanceEntry`, `AttendanceConfirmationResponse`, `AmendAttendanceRequestDto`, `AmendmentEntry`, `AmendmentResultResponse`, `AuditHistoryResponse`, `AuditEntry` — field for field and type for type from the Wire Contract
    - `submissionId` is `String` on the wire, parsed to `UUID` in the service. `semesterId`, wherever it appears, is `String`
    - Request `status` is declared **`String`**, not the typed enum: a typed enum makes Jackson throw `HttpMessageNotReadableException`, which `GlobalExceptionHandler` does not handle, producing HTTP 500 and violating 5.8
    - `ConfirmAttendanceRequestDto` carries **no `sessionId`** (path only) and **no `method`** (fixed server-side to MANUAL)
    - `markUnspecifiedAsAbsent` defaults to `false` (5.3)
    - `defaultedStudentIds` and `entries` are `[]` when empty, never null
    - **Blocked by: nothing**
    - _Requirements: 1.3, 1.7, 5.3, 5.8, 5.16, 6.1, 9.6, 13.10, 13.11_
  - [ ]* 5.2 Write DTO serialization tests and the shared property generators
    - `AttendanceDtoSerializationTest.kt`: Jackson emits `LocalDate` as `YYYY-MM-DD` and `LocalDateTime` as `ISO_LOCAL_DATE_TIME` with no zone suffix; `error.details` serializes as a JSON **object** keyed `omittedStudentIds` / `unknownStudentIds` / `duplicateStudentIds` / `invalidStatuses` / `missingRecordStudentIds`
    - Also create `backend/src/test/.../attendance/AttendanceArbs.kt` holding the `Arb` generators the backend property tests share (statuses, rosters, entry lists, instants). **Sole owner of this generators file**
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.1 (B4)**
    - _Requirements: 5.2, 13.10, 13.11_

- [x] 6. B5: Authorization
  - [x] 6.1 Implement `attendance/security/AttendanceAccessGuard.kt`
    - `requireCaller(firebaseUid)` → `CallerContext(userId, role, teacherId?)`; `requireSessionAccess(caller, sessionId)` → the loaded `LectureSession`
    - Null uid → `UnauthorizedException`; unknown uid → `UnauthorizedException`; STUDENT → `ForbiddenException`; TEACHER with no teacher row → `ForbiddenException`; TEACHER ≠ owning teacher → `ForbiddenException`; ADMIN → `teacherId = null`, no further check
    - `userRepository.findByFirebaseUid(uid).orElseThrow { UnauthorizedException(...) }` — **`.orElseThrow {}`, not elvis.** Elvis does not compile against `Optional<User>` and has already produced one bug here
    - Ordering is normative: the 401 check precedes the 404, so an unauthenticated request against a nonexistent session returns 401
    - **Blocked by: nothing**
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6, 10.7, 10.8, 1.8_
  - [ ]* 6.2 Write the authorization property test
    - `AttendanceAuthorizationPropertyTest.kt`
    - **Property 33: Ownership gates all six operations identically**
    - Tag: `// Feature: attendance-marking, Property 33: Ownership gates all six operations identically`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over (caller teacher id, owning teacher id) × the six operations
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 6.1 (B5)**
    - **Validates: Requirements 3.5, 10.7, 10.8**
  - [ ]* 6.3 Write guard unit tests for the error branches
    - `AttendanceAccessGuardTest.kt`: the 401-before-404 ordering, the unknown-uid 401, the STUDENT 403, the missing-teacher-row 403, and the ADMIN bypass
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 6.1 (B5)**
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.6, 1.8_

- [x] 7. B6: The transition map
  - [x] 7.1 Implement `attendance/service/SessionTransitions.kt`
    - A dependency-free `object` declaring `LEGAL` as exactly the four pairs, plus `isLegal` and `require` (which throws `ConflictException`)
    - `FINALIZED to FINALIZED` is deliberately **absent**: an amendment is not a transition, so the amend path never consults this map
    - CREATED, SCANNING, REVIEW, FINALIZING and FAILED are the target of no edge
    - **Blocked by: nothing**
    - _Requirements: 3.1, 3.2, 3.3, 3.10_
  - [ ]* 7.2 Write the transition property test
    - `SessionTransitionsPropertyTest.kt`
    - **Property 7: Only the four declared transitions are legal**
    - Tag: `// Feature: attendance-marking, Property 7: Only the four declared transitions are legal`
    - `checkAll(PropTestConfig(iterations = 100), Arb.enum<SessionStatus>(), Arb.enum<SessionStatus>())` over all 81 ordered pairs
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 7.1 (B6)**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.7**
  - [ ]* 7.3 Write transition unit tests
    - `SessionTransitionsTest.kt`: the nine `SessionStatus` names in the normative order (3.4) and the four transitions an ADMIN may make (3.6)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 7.1 (B6)**
    - _Requirements: 3.4, 3.6_

- [x] 8. C1: Shared payload types and enum corrections
  - [x] 8.1 Write the shared wire types and apply the R11 corrections
    - New `shared/.../data/api/AttendanceWire.kt`: `@Serializable` `RosterResponse`, `RosterEntry`, `SessionSummary`, `ConfirmAttendanceRequest`, `SubmittedAttendanceEntry`, `AttendanceConfirmationResponse`, `AmendAttendanceRequest`, `AmendmentEntry`, `AmendmentResultResponse`, `AuditHistoryResponse`, `AuditEntry` — one type per payload, no domain mirror, no `@SerialName`
    - `shared/.../domain/model/AttendanceRecord.kt`: add `EXCUSED`, reorder `AttendanceStatus` to `PRESENT, ABSENT, LATE, ON_LEAVE, EXCUSED`, `method` → `AttendanceMethod?`, `markedByTeacherId` → `Long?`, rename `remarks` → `notes`, keep `isCorrected`
    - `shared/.../data/api/request/AttendanceRequests.kt`: `StartLectureSessionRequest.semesterId` `Long` → `String`. Leave the existing camera-flow `ConfirmAttendanceRequest` in this file untouched
    - Shared-side `status` keeps the **typed enum** (kotlinx.serialization can only encode a declared constant), unlike the backend DTO
    - Timestamps and dates are `String` on the shared side; payload timestamps carry no `Z`
    - **Re-verify the regression guard by search** before and after: `AttendanceStatus`, `markedByTeacherId`, `AttendanceStats`, `StartLectureSessionRequest`, `remarks` across `androidApp/src` and `shared/src`. The design found zero call sites; report it if that has changed
    - **Blocked by: nothing**
    - **Sole owner of `shared/.../domain/model/AttendanceRecord.kt` and `shared/.../data/api/request/AttendanceRequests.kt`.** No other task may open either file
    - _Requirements: 11.4, 11.5, 11.6, 11.7, 11.8, 11.9, 13.5, 13.6, 13.7_
  - [ ]* 8.2 Write the confirmation-response round-trip property test
    - `shared/src/commonTest/.../ConfirmationResponseSerializationTest.kt`
    - **Property 43: The confirmation response round-trips**
    - Tag: `// Feature: attendance-marking, Property 43: The confirmation response round-trips`
    - `checkAll(PropTestConfig(iterations = 100), ...)` including empty `defaultedStudentIds` and arbitrary UUID text
    - **Blocked by: 1.1 (T0), 8.1 (C1)**
    - **Validates: Requirements 13.1, 13.8**
  - [ ]* 8.3 Write the roster-response round-trip property test
    - `shared/src/commonTest/.../RosterResponseSerializationTest.kt`
    - **Property 44: The roster response round-trips**
    - Tag: `// Feature: attendance-marking, Property 44: The roster response round-trips`
    - Generators must produce null `storedStatus`, null `method`, null `markedByTeacherId`, null `leaveRequestId` and null `leaveReason`
    - **Blocked by: 1.1 (T0), 8.1 (C1)**
    - **Validates: Requirements 13.2, 13.8, 11.6, 11.7**
  - [ ]* 8.4 Write the confirmation-request round-trip property test
    - `shared/src/commonTest/.../ConfirmRequestSerializationTest.kt`
    - **Property 45: The confirmation request round-trips**
    - Tag: `// Feature: attendance-marking, Property 45: The confirmation request round-trips`
    - Also assert the decoded `markUnspecifiedAsAbsent` is `false` when the encoded form omitted it
    - **Blocked by: 1.1 (T0), 8.1 (C1)**
    - **Validates: Requirements 13.3, 5.3**
  - [ ]* 8.5 Write the amendment-request round-trip property test
    - `shared/src/commonTest/.../AmendRequestSerializationTest.kt`
    - **Property 46: The amendment request round-trips**
    - Tag: `// Feature: attendance-marking, Property 46: The amendment request round-trips`
    - **Blocked by: 1.1 (T0), 8.1 (C1)**
    - **Validates: Requirements 13.4**
  - [ ]* 8.6 Write the date and timestamp format property test
    - `shared/src/commonTest/.../DateTimeFormatPropertyTest.kt`
    - **Property 48: Dates and timestamps serialize as ISO 8601 local forms**
    - Tag: `// Feature: attendance-marking, Property 48: Dates and timestamps serialize as ISO 8601 local forms`
    - Assert `ISO_LOCAL_DATE` and `ISO_LOCAL_DATE_TIME` shapes, no zone or offset suffix, and a parse-back equality
    - **Blocked by: 1.1 (T0), 8.1 (C1)**
    - **Validates: Requirements 13.10, 13.11**
  - [ ]* 8.7 Write the enum parity tests
    - `shared/src/commonTest/.../EnumParityTest.kt`: assert `AttendanceStatus`, `SessionStatus` and `AttendanceMethod` entry names and order against the normative lists index by index
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 8.1 (C1)**
    - _Requirements: 13.5, 13.6, 13.7, 11.5_

- [x] 9. C3: The `ApiClient` deserialization-failure branch
  - [x] 9.1 Add the `SerializationException` branch
    - In the 2xx success branch of `safeApiCall`, wrap `response.body<T>()` so a `SerializationException` yields `Result.Error(AppError.ApiError(code = response.status.value, message = "Malformed response body: ..."))` instead of falling to the generic catch and becoming `AppError.NetworkError`
    - Strictly narrowing: only 2xx responses whose body fails to deserialize change behaviour
    - **Re-verify the regression guard:** `shared/src/commonTest` holds only `DateTimeUtilTest` and `ResultTest`; confirm neither touches `ApiClient` before editing
    - **Blocked by: nothing**
    - **Sole owner of `shared/.../data/api/ApiClient.kt`.** Six repositories depend on this file; no other task may open it
    - _Requirements: 13.9_
  - [ ]* 9.2 Write the deserialization-failure unit test
    - `shared/src/androidUnitTest/.../ApiClientSerializationFailureTest.kt` using Ktor `MockEngine`: a 200 response with a malformed body returns `AppError.ApiError`, not `NetworkError`, and throws nothing
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 9.1 (C3)**
    - _Requirements: 13.9_

- [ ] 10. Checkpoint — end of wave 0
  - Run `./gradlew :backend:test :androidApp:testDebugUnitTest :shared:allTests`
  - All 87 pre-existing tests must still pass alongside the new ones. Ensure all tests pass, ask the user if questions arise.

### Wave 1 — pure logic, counters, read and lifecycle services

- [x] 11. B7: The request fingerprint
  - [x] 11.1 Implement `attendance/service/RequestFingerprint.kt`
    - Pure `object`, no dependencies. SHA-256 hex (lowercase, 64 characters, matching `CHAR(64)`) over the session id and the entries **sorted by student id ascending**
    - Canonical form one line per entry: `"<sessionId>\n<studentId>:<status>:<notes ?: "">\n..."`
    - `notes` is included (a replay that changes a note must conflict under 6.7). `markUnspecifiedAsAbsent` is **not** included — the effective entry set already captures its effect
    - **Blocked by: 5.1 (B4)** — hashes `SubmittedAttendanceEntry`, which B4 defines
    - _Requirements: 6.3_
  - [ ]* 11.2 Write the fingerprint property test
    - `RequestFingerprintPropertyTest.kt`
    - **Property 18: The fingerprint is permutation-invariant and change-sensitive**
    - Tag: `// Feature: attendance-marking, Property 18: The fingerprint is permutation-invariant and change-sensitive`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over entry lists and their permutations, plus mutations of session id, student id, status and notes
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 11.1 (B7)**
    - **Validates: Requirements 6.3**

- [x] 12. B8: The counter recalculator
  - [x] 12.1 Implement `attendance/service/SessionCounterRecalculator.kt`
    - `recompute(sessionId): SessionCounters` using the derived count queries from B3; never an increment
    - `presentCount` counts PRESENT **and** LATE; `onLeaveCount` counts ON_LEAVE **and** EXCUSED. These two definitions are the ones most often got wrong
    - Writes `updatedAt` from the injected `Clock` (7.8)
    - Callers use `saveAllAndFlush` before `recompute`; the flush is explicit, not left to `FlushModeType.AUTO`
    - Ends with `check(present + absent + onLeave == total) { ... }` as the partition tripwire; it surfaces as HTTP 500 because an unpartitioned enum is a programming error
    - **Blocked by: 3.1 (B2), 4.1 (B3)**
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.8, 7.9_
  - [ ]* 12.2 Write the counter reference-count property test
    - `CounterReferenceCountPropertyTest.kt`
    - **Property 21: Counters equal the reference counts, and recomputation is idempotent**
    - Tag: `// Feature: attendance-marking, Property 21: Counters equal the reference counts, and recomputation is idempotent`
    - `checkAll(PropTestConfig(iterations = 100), Arb.list(Arb.enum<AttendanceStatus>(), 0..40))`, comparing against an independently computed reference count and recomputing a second time
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`; inject `Clock.fixed`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 12.1 (B8)**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 7.5**
  - [ ]* 12.3 Write the counter partition property test
    - `CounterPartitionPropertyTest.kt`
    - **Property 22: The three counters partition the record set**
    - Tag: `// Feature: attendance-marking, Property 22: The three counters partition the record set`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over sequences of confirmations and amendments; assert the sum equals the record count and all three are zero when no records exist
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 12.1 (B8)**
    - **Validates: Requirements 7.6, 7.9**
  - [ ]* 12.4 Write counter unit tests
    - `SessionCounterRecalculatorTest.kt`: `updatedAt` written from the fixed clock (7.8) and the empty-session example (7.9)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 12.1 (B8)**
    - _Requirements: 7.8, 7.9_

- [x] 13. B9: The roster service
  - [x] 13.1 Implement `attendance/service/AttendanceRosterService.kt`
    - `@Transactional(readOnly = true)`. Flow: active roster → index stored records by student → approved covering leave → suggested status → `isCorrected` from `findStatusChangedRecordIds` → sort by `rollNumber` ascending → project the session header
    - `storedStatus` is null exactly when no record exists (1.5); `entries` is `[]` for a section with no ACTIVE enrollments (1.9)
    - Where several APPROVED requests cover the date, the lowest `id` supplies `leaveRequestId` and `leaveReason` — a stated tie-break so the response is deterministic
    - `amendmentWindowOpen` is computed **here**, server-side, from the single `Clock`: `FINALIZED && completedAt != null && HOURS.between(completedAt, now) <= 168`. The client never evaluates the 168-hour rule
    - Criterion 4.7 is structural: this service is read-only and calls no save method
    - **Blocked by: 2.1 (B1), 3.1 (B2), 4.1 (B3), 5.1 (B4)**
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.9, 4.1, 4.2, 4.3, 4.4, 4.7, 9.14, 12.17_
  - [ ]* 13.2 Write the roster membership property test
    - `RosterMembershipPropertyTest.kt`, `@DataJpaTest` on H2 (the design's repository test home, so the real queries run)
    - **Property 1: The roster is exactly the ACTIVE enrollment set**
    - Tag: `// Feature: attendance-marking, Property 1: The roster is exactly the ACTIVE enrollment set`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over assignments of enrollment statuses, including the empty ACTIVE set
    - **Blocked by: 1.1 (T0), 5.2 (generators), 13.1 (B9)**
    - **Validates: Requirements 1.1, 1.2, 1.9**
  - [ ]* 13.3 Write the roster-mirrors-persistence property test
    - `RosterEntryMirrorsPersistencePropertyTest.kt`, `@DataJpaTest` on H2
    - **Property 2: Every roster entry mirrors persistence**
    - Tag: `// Feature: attendance-marking, Property 2: Every roster entry mirrors persistence`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over subsets of students holding a record; assert `storedStatus` is null exactly when no record exists
    - **Blocked by: 1.1 (T0), 5.2 (generators), 13.1 (B9)**
    - **Validates: Requirements 1.3, 1.4, 1.5**
  - [ ]* 13.4 Write the roster ordering property test
    - `RosterOrderingPropertyTest.kt`, `@DataJpaTest` on H2
    - **Property 3: The roster is ordered by roll number**
    - Tag: `// Feature: attendance-marking, Property 3: The roster is ordered by roll number`
    - `checkAll(PropTestConfig(iterations = 100), ...)` with enrollments created in arbitrary order
    - **Blocked by: 1.1 (T0), 5.2 (generators), 13.1 (B9)**
    - **Validates: Requirements 1.6**
  - [ ]* 13.5 Write the leave-suggestion property test
    - `LeaveSuggestionPropertyTest.kt`, `@DataJpaTest` on H2
    - **Property 4: The leave suggestion is exactly the approved-covering predicate**
    - Tag: `// Feature: attendance-marking, Property 4: The leave suggestion is exactly the approved-covering predicate`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over arbitrary statuses, date ranges and section qualifiers; assert the identifier and reason are carried whenever the suggestion is ON_LEAVE
    - **Blocked by: 1.1 (T0), 5.2 (generators), 13.1 (B9)**
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4**
  - [ ]* 13.6 Write roster unit tests
    - `AttendanceRosterServiceTest.kt`: the session header projection (1.7), the lowest-id leave tie-break, and the `amendmentWindowOpen` boundary at exactly 168 hours against a fixed clock
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`; inject `Clock.fixed`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 13.1 (B9)**
    - _Requirements: 1.7, 4.4, 12.17_

- [x] 14. B10: The session lifecycle service
  - [x] 14.1 Implement `attendance/service/SessionLifecycleService.kt`
    - `activate`, in this order: already-ACTIVE no-op return (2.8) **before** the window checks → `SessionTransitions.require` → future-date `BadRequestException` (2.4) → TEACHER 14-day `ForbiddenException` (2.5, 2.6), skipped for ADMIN (2.7) → write `status`, `startedAt`, `attendanceMethod = MANUAL`, `totalStudents`, `updatedAt`
    - The 400-before-403 ordering is deliberate and testable: a future-dated session is malformed regardless of who asks
    - `cancel`: `SessionTransitions.require(status, CANCELLED)` then write `status` and `updatedAt` only. It touches **no attendance record and no counter** — that is how 3.8 and 8.6 are satisfied, by writing less
    - All dates and timestamps come from the injected `Clock`
    - **Blocked by: 3.1 (B2), 4.1 (B3), 5.1 (B4), 7.1 (B6)**
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 3.5, 3.6, 3.7, 3.8, 7.7_
  - [ ]* 14.2 Write the activation window property test
    - `ActivationWindowPropertyTest.kt`
    - **Property 5: Activation is a function of the caller's role and the session's age**
    - Tag: `// Feature: attendance-marking, Property 5: Activation is a function of the caller's role and the session's age`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over role × a session-date offset of −30 to +30 days; generate the instant and build `Clock.fixed(generated, ZoneOffset.UTC)` so time needs no mocking
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 14.1 (B10)**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 7.7**
  - [ ]* 14.3 Write the activation idempotence property test
    - `ActivationIdempotencePropertyTest.kt`
    - **Property 6: Activation is idempotent**
    - Tag: `// Feature: attendance-marking, Property 6: Activation is idempotent`
    - `checkAll(PropTestConfig(iterations = 100), ...)`; assert `startedAt`, `attendanceMethod` and `totalStudents` are identical after the second activation
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 14.1 (B10)**
    - **Validates: Requirements 2.8**
  - [ ]* 14.4 Write the cancellation-preserves-records property test
    - `CancellationPreservesRecordsPropertyTest.kt`
    - **Property 8: Cancellation preserves the attendance record set**
    - Tag: `// Feature: attendance-marking, Property 8: Cancellation preserves the attendance record set`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over arbitrary record sets; assert membership and field values unchanged
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 14.1 (B10)**
    - **Validates: Requirements 3.8**
  - [ ]* 14.5 Write lifecycle unit tests
    - `SessionLifecycleServiceTest.kt`: `attendanceMethod = MANUAL` on activation (2.3), the 400-before-403 ordering, the re-activation no-op succeeding even past the 14-day window, and the four ADMIN transitions (3.6)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`; inject `Clock.fixed`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 14.1 (B10)**
    - _Requirements: 2.3, 2.4, 2.6, 2.8, 3.6_

- [x] 15. C2: Shared repository interface corrections
  - [x] 15.1 Correct and extend `shared/.../data/repository/AttendanceRepository.kt`
    - Add the manual-flow members: `getSessionRoster`, `activateSession`, `cancelSession`, `confirmManualAttendance(lectureSessionId, request)`, `amendAttendance(lectureSessionId, request)`, `getAuditHistory`
    - `confirmManualAttendance` takes the whole request object, replacing the old `confirmAttendance(lectureSessionId, attendanceCandidates, submissionId)` — passing one object keeps a client from assembling a payload whose parts disagree with the fingerprint
    - Correct `getStudentAttendance` and `getStudentAttendanceStats` to take `semesterId: String`; `AttendanceStats.semesterId` becomes `String`
    - Leave the five camera-flow members declared and unimplemented
    - Every member returns `com.afora.shared.util.Result`, not `kotlin.Result`
    - **Re-verify the regression guard:** the design found nothing implements this interface today — no `HttpAttendanceRepository` and no fake. Confirm by search before editing and report if that has changed
    - **Blocked by: 8.1 (C1)** — the signatures reference the C1 payload types
    - **Sole owner of `shared/.../data/repository/AttendanceRepository.kt`.** No other task may open it
    - _Requirements: 11.1, 11.2, 11.3, 11.11, 11.12, 11.13_

- [x] 16. Checkpoint — end of wave 1
  - Run `./gradlew :backend:test :androidApp:testDebugUnitTest :shared:allTests`
  - Ensure all tests pass, ask the user if questions arise.

### Wave 2 — transactional services and the client data layer

- [x] 17. B11: The confirmation service
  - [x] 17.1 Implement `attendance/service/AttendanceConfirmationService.kt`
    - One `@Transactional fun confirm(...)`, ordered exactly as the design's step list: (1) `findByIdForUpdate` as the **first statement** — the pessimistic lock is what makes 6.11 deterministic; (2) parse `submissionId` as UUID; (3) parse each `status` string into `AttendanceStatus`; (4) reject duplicate student ids; (5) compute the fingerprint; (6) look up the submission record — equal fingerprint replays with `replayed = true` and writes nothing, different fingerprint conflicts; (7) non-ACTIVE status conflicts, with the FINALIZED message naming the amendment path; (8) membership and completeness validation; (9) build records; (10) `saveAllAndFlush` then `counters.recompute`; (11) `status = FINALIZED`, `completedAt`; (12) insert the submission record with the serialized response
    - Steps 2–7 precede step 8 so a replay never re-runs validation and a stale client gets the amendment-path message
    - Records: `method = MANUAL`, `confidenceScore = null`, `markedAt` from the clock, `markedByTeacher` = the caller's teacher for TEACHER and **null for ADMIN** (5.17 is easy to get backwards)
    - Catch `DataIntegrityViolationException` at the `saveAllAndFlush` site and rethrow `ConflictException` — that type is **not** handled by `GlobalExceptionHandler` and would otherwise return HTTP 500 (6.10)
    - `GlobalExceptionHandler`, `JacksonConfig` and `DashboardService` are **not** modified
    - **Blocked by: 2.1 (B1), 3.1 (B2), 4.1 (B3), 5.1 (B4), 7.1 (B6), 11.1 (B7), 12.1 (B8)**
    - _Requirements: 5.1, 5.2, 5.4, 5.5, 5.6, 5.7, 5.8, 5.9, 5.10, 5.11, 5.12, 5.13, 5.14, 5.15, 5.16, 5.17, 6.1, 6.2, 6.4, 6.5, 6.6, 6.7, 6.8, 6.9, 6.10, 2.9, 7.1, 8.2_
  - [ ]* 17.2 Write the ACTIVE-session gate property test
    - `ConfirmationRequiresActiveSessionPropertyTest.kt`
    - **Property 9: Confirmation requires an ACTIVE session**
    - Tag: `// Feature: attendance-marking, Property 9: Confirmation requires an ACTIVE session`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over every non-ACTIVE `SessionStatus`; assert conflict and zero records written
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 2.9**
  - [ ]* 17.3 Write the no-record-without-confirmation property test
    - `NoRecordWithoutConfirmationPropertyTest.kt`
    - **Property 10: No attendance record exists without a successful confirmation**
    - Tag: `// Feature: attendance-marking, Property 10: No attendance record exists without a successful confirmation`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over sequences of roster reads and rejected confirmations against a SCHEDULED session
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 4.7, 8.3**
  - [ ]* 17.4 Write the submitted-status-wins property test
    - `SubmittedStatusWinsPropertyTest.kt`
    - **Property 11: The submitted status always wins over the suggestion**
    - Tag: `// Feature: attendance-marking, Property 11: The submitted status always wins over the suggestion`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over all 25 (suggested, submitted) pairs
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 4.5, 4.6**
  - [ ]* 17.5 Write the incomplete-submission property test
    - `IncompleteSubmissionPropertyTest.kt`
    - **Property 12: An incomplete submission is rejected and names every omission**
    - Tag: `// Feature: attendance-marking, Property 12: An incomplete submission is rejected and names every omission`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over omitted subsets; assert the `omittedStudentIds` detail equals the omitted subset exactly
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 5.1, 5.2**
  - [ ]* 17.6 Write the absent-defaulting property test
    - `AbsentDefaultingPropertyTest.kt`
    - **Property 13: Opt-in defaulting marks exactly the omitted students ABSENT**
    - Tag: `// Feature: attendance-marking, Property 13: Opt-in defaulting marks exactly the omitted students ABSENT`
    - `checkAll(PropTestConfig(iterations = 100), ...)` with `markUnspecifiedAsAbsent = true`; assert `defaultedStudentIds` equals the omitted subset exactly
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 5.4, 5.5**
  - [ ]* 17.7 Write the malformed-payload property test
    - `MalformedPayloadPropertyTest.kt`
    - **Property 14: Malformed payloads are rejected as bad requests**
    - Tag: `// Feature: attendance-marking, Property 14: Malformed payloads are rejected as bad requests`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over mutations adding an unenrolled student, duplicating an id, or using a status name outside the five; assert HTTP 400 with the matching detail key and **never** HTTP 500
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 5.6, 5.7, 5.8**
  - [ ]* 17.8 Write the persisted-record-shape property test
    - `PersistedRecordShapePropertyTest.kt`
    - **Property 15: The persisted record shape is fixed by the flow and the caller's role**
    - Tag: `// Feature: attendance-marking, Property 15: The persisted record shape is fixed by the flow and the caller's role`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over caller roles; assert `method = MANUAL`, `confidenceScore = null`, `markedAt` equals the fixed clock, and `markedByTeacherId` is null for ADMIN
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 5.9, 5.10, 5.11, 5.12, 5.17**
  - [ ]* 17.9 Write the finalization property test
    - `ConfirmationFinalizesSessionPropertyTest.kt`
    - **Property 16: A successful confirmation finalizes the session and reports what was persisted**
    - Tag: `// Feature: attendance-marking, Property 16: A successful confirmation finalizes the session and reports what was persisted`
    - `checkAll(PropTestConfig(iterations = 100), ...)`; assert FINALIZED, `completedAt` from the clock, and the four counts equal aggregates over the stored records
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 5.13, 5.14, 5.16**
  - [ ]* 17.10 Write the submission-id parsing property test
    - `SubmissionIdUuidPropertyTest.kt`
    - **Property 17: The submission identifier must parse as a UUID**
    - Tag: `// Feature: attendance-marking, Property 17: The submission identifier must parse as a UUID`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over strings failing UUID parsing; assert bad request and nothing written
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 6.1, 6.2**
  - [ ]* 17.11 Write the replay property test
    - `ReplayReturnsStoredConfirmationPropertyTest.kt`
    - **Property 19: Replay returns the stored confirmation and writes nothing**
    - Tag: `// Feature: attendance-marking, Property 19: Replay returns the stored confirmation and writes nothing`
    - `checkAll(PropTestConfig(iterations = 100), ...)`; assert field equality except `replayed`, and byte-identical records, counters and status
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 6.4, 6.5, 6.6**
  - [ ]* 17.12 Write the reused-identifier conflict property test
    - `ReusedIdentifierConflictPropertyTest.kt`
    - **Property 20: A reused identifier with a changed payload conflicts**
    - Tag: `// Feature: attendance-marking, Property 20: A reused identifier with a changed payload conflicts`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over fingerprint-changing mutations; assert conflict and unchanged records
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - **Validates: Requirements 6.7**
  - [ ]* 17.13 Write confirmation unit tests
    - `AttendanceConfirmationServiceTest.kt`: the `markUnspecifiedAsAbsent` default of false (5.3), the FINALIZED-session 409 whose message names the amendment path (6.8), and the `DataIntegrityViolationException` → `ConflictException` translation (6.10)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`; inject `Clock.fixed`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 17.1 (B11)**
    - _Requirements: 5.3, 6.8, 6.10_

- [x] 18. B12: The amendment service
  - [x] 18.1 Implement `attendance/service/AttendanceAmendmentService.kt`
    - `@Transactional fun amend(...)` in order: `findByIdForUpdate` first (so an amendment cannot interleave with a confirmation) → non-FINALIZED conflicts (9.12) → blank reason bad request using Kotlin's Unicode-aware `isBlank()` (9.4, 9.5) → window check, unlimited for ADMIN and 168 hours for TEACHER with a message naming the administrator path (9.1–9.3) → load records, missing student 404 (9.11) → per entry, skip no-ops with no write and no audit entry (9.13), otherwise write `status` and `updatedAt` while **preserving `markedAt` and `markedByTeacher`** (9.7, 9.8) and append one audit entry (9.6) → `saveAllAndFlush` then `counters.recompute` → status stays FINALIZED because the service simply does not write it (3.9)
    - `updatedAt` is written **explicitly**: PostgreSQL has a `BEFORE UPDATE` trigger on `attendance_records` and H2 does not, so the explicit write makes the behaviour identical on both engines
    - `@Transactional(readOnly = true) fun auditHistory(...)` via `findByLectureSessionIdOrderByChangedAtAsc` (9.16). No delete or update path exists anywhere — that is how 9.15 is satisfied structurally
    - **Blocked by: 2.1 (B1), 3.1 (B2), 4.1 (B3), 5.1 (B4), 12.1 (B8)**
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8, 9.9, 9.10, 9.11, 9.12, 9.13, 9.15, 9.16, 3.9, 7.1_
  - [ ]* 18.2 Write the amendment permission property test
    - `AmendmentPermissionPropertyTest.kt`
    - **Property 26: Amendment permission is a function of role, elapsed time and session status**
    - Tag: `// Feature: attendance-marking, Property 26: Amendment permission is a function of role, elapsed time and session status`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over role × elapsed 0–400 hours × every `SessionStatus`, generating the instant into `Clock.fixed`
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 18.1 (B12)**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.12**
  - [ ]* 18.3 Write the amendment reason property test
    - `AmendmentReasonPropertyTest.kt`
    - **Property 27: An amendment reason must contain a non-whitespace character**
    - Tag: `// Feature: attendance-marking, Property 27: An amendment reason must contain a non-whitespace character`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over arbitrary strings including tabs, newlines and Unicode space characters
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 18.1 (B12)**
    - **Validates: Requirements 9.4, 9.5**
  - [ ]* 18.4 Write the audit entry completeness property test
    - `AuditEntryCompletenessPropertyTest.kt`
    - **Property 28: Each status change writes one complete audit entry**
    - Tag: `// Feature: attendance-marking, Property 28: Each status change writes one complete audit entry`
    - `checkAll(PropTestConfig(iterations = 100), ...)`; assert exactly one entry per changed record and every field equal to that change
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 18.1 (B12)**
    - **Validates: Requirements 9.6**
  - [ ]* 18.5 Write the amendment-preserves-marking property test
    - `AmendmentPreservesMarkingPropertyTest.kt`
    - **Property 29: Amendment preserves the original marking and the session status**
    - Tag: `// Feature: attendance-marking, Property 29: Amendment preserves the original marking and the session status`
    - `checkAll(PropTestConfig(iterations = 100), ...)`; assert `markedAt` and `markedByTeacherId` unchanged, `updatedAt` from the clock, status still FINALIZED
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 18.1 (B12)**
    - **Validates: Requirements 9.7, 9.8, 9.9, 3.9**
  - [ ]* 18.6 Write the no-op amendment property test
    - `NoOpAmendmentPropertyTest.kt`
    - **Property 30: A no-op amendment changes nothing**
    - Tag: `// Feature: attendance-marking, Property 30: A no-op amendment changes nothing`
    - `checkAll(PropTestConfig(iterations = 100), Arb.enum<AttendanceStatus>())`; assert the record is unchanged in every field and no audit entry is written
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 18.1 (B12)**
    - **Validates: Requirements 9.13**
  - [ ]* 18.7 Write the `isCorrected` property test
    - `IsCorrectedPropertyTest.kt`
    - **Property 31: `isCorrected` is exactly "a status-changing amendment occurred"**
    - Tag: `// Feature: attendance-marking, Property 31: isCorrected is exactly "a status-changing amendment occurred"`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over amendment sequences including no-ops
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 18.1 (B12)**
    - **Validates: Requirements 9.14, 11.9**
  - [ ]* 18.8 Write the append-only audit log property test
    - `AuditLogAppendOnlyPropertyTest.kt`
    - **Property 32: The audit log is append-only and time-ordered**
    - Tag: `// Feature: attendance-marking, Property 32: The audit log is append-only and time-ordered`
    - `checkAll(PropTestConfig(iterations = 100), ...)`; assert each step's history contains the previous step's as an unmodified prefix, never shrinks, and has non-decreasing timestamps
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 18.1 (B12)**
    - **Validates: Requirements 9.15, 9.16**
  - [ ]* 18.9 Write amendment unit tests
    - `AttendanceAmendmentServiceTest.kt`: the 403 message naming the administrator correction path (9.2), the explicit `updatedAt` write (9.9), and the 404 with `missingRecordStudentIds` (9.11)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`; inject `Clock.fixed`
    - **Blocked by: 1.1 (T0), 5.2 (generators), 18.1 (B12)**
    - _Requirements: 9.2, 9.9, 9.11_

- [x] 19. C4: The HTTP attendance repository
  - [x] 19.1 Implement `shared/.../data/repository/HttpAttendanceRepository.kt`
    - Follow `HttpDashboardRepository`: constructor-injected `ApiClient`, unwrap `result.data.data`, return `com.afora.shared.util.Result` with `AppError` on failure
    - **Endpoint strings are full paths beginning `/api/`**, e.g. `"/api/attendance/sessions/$lectureSessionId/roster"`, because `ApiClient.buildUrl` prepends only the base URL
    - The five camera-flow members **return** `Result.Error(AppError.UnknownError("<memberName> is not implemented in the manual marking flow"))`. No `TODO()`, no `throw` — a thrown `NotImplementedError` inside a coroutine surfaces as a crash rather than a handled error
    - Add the private tolerant error decoder (`BackendErrorEnvelope`, `Json { ignoreUnknownKeys = true; isLenient = true }`, `humanise(raw)`) so the Compose banner shows `error.message` prose rather than raw JSON. `ignoreUnknownKeys` is what skips `details` whatever its shape, sidestepping the object-versus-array mismatch without touching the shared `ErrorResponse`
    - **Blocked by: 8.1 (C1), 15.1 (C2), 9.1 (C3)**
    - _Requirements: 11.10, 11.11, 11.12, 13.9_
  - [ ]* 19.2 Write the unimplemented-members property test
    - `shared/src/androidUnitTest/.../UnimplementedMembersPropertyTest.kt`
    - **Property 34: Unimplemented members return a naming error and never throw**
    - Tag: `// Feature: attendance-marking, Property 34: Unimplemented members return a naming error and never throw`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over the unimplemented member set; assert `AppError.UnknownError` whose message contains the member name and that nothing is thrown
    - **Blocked by: 1.1 (T0), 19.1 (C4)**
    - **Validates: Requirements 11.12**
  - [ ]* 19.3 Write the unknown-status API error property test
    - `shared/src/androidUnitTest/.../UnknownStatusApiErrorPropertyTest.kt` using Ktor `MockEngine`
    - **Property 47: An unknown status name yields an API error**
    - Tag: `// Feature: attendance-marking, Property 47: An unknown status name yields an API error`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over status strings outside the five names in a 200 payload; assert `AppError.ApiError`, not `NetworkError`, and no thrown exception
    - **Blocked by: 1.1 (T0), 9.1 (C3), 19.1 (C4)**
    - **Validates: Requirements 13.9**
  - [ ]* 19.4 Write repository unit tests
    - `HttpAttendanceRepositoryTest.kt` with `MockEngine`: one example per implemented member asserting the exact `/api/attendance/...` path and method (11.10), plus the `humanise` fallback to raw text when the body is not a backend envelope
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 19.1 (C4)**
    - _Requirements: 11.10, 11.11_

- [x] 20. C6: UiState and ViewModel
  - [x] 20.1 Implement the UiState, the ViewModel and the test fake
    - `androidApp/.../ui/attendance/MarkAttendanceUiState.kt`: sealed interface with `Loading`, `Success` and `Error`; `Error` carries `rows` so 12.14 is guaranteed by the type rather than by remembering to copy state. `AttendanceRow` as specified
    - `androidApp/.../ui/attendance/MarkAttendanceViewModel.kt`: `@HiltViewModel` with constructor injection (12.21), `StateFlow<MarkAttendanceUiState>` (12.2)
    - Row init: `selectedStatus = entry.storedStatus ?: entry.suggestedStatus` — one expression covering the stored branch, the suggested branch and the FINALIZED case (12.5, 12.16)
    - `private var attemptSubmissionId: String?` with `attemptSubmissionId ?: UUID.randomUUID().toString().also { attemptSubmissionId = it }`: generated once per attempt (12.9), reused by every retry (12.10), nulled on success or on an explicit edit after a failure
    - Running counts use the **server's definitions**: present counts PRESENT and LATE, on-leave counts ON_LEAVE and EXCUSED, so pre-submit numbers match post-submit numbers (12.7)
    - `amendmentWindowOpen` is read off the payload; the client never evaluates the 168-hour rule, because device clock skew on classroom tablets is common
    - Also create `androidApp/src/test/.../FakeAttendanceRepository.kt`, following the existing `FakeDashboardRepository` precedent. **Sole owner of the fake.** A fake is easier to drive from a generator across 100 iterations than a mock with 100 stubbings
    - **Blocked by: 15.1 (C2)** — the ViewModel depends on the **interface**, not on the HTTP implementation, so it proceeds in parallel with C4
    - _Requirements: 12.2, 12.5, 12.7, 12.8, 12.9, 12.10, 12.13, 12.14, 12.16, 12.17, 12.21_
  - [ ]* 20.2 Write the row-state property test
    - `RowStateMirrorsRosterPropertyTest.kt`
    - **Property 35: Row state mirrors the roster**
    - Tag: `// Feature: attendance-marking, Property 35: Row state mirrors the roster`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over roster responses; assert order, `rollNumber`, `fullName`, `leaveReason` carriage and the selected-status rule
    - Drive the fake repository, not a mock. MockK only if a mock is needed — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 20.1 (C6)**
    - **Validates: Requirements 12.4, 12.5, 12.15, 12.16**
  - [ ]* 20.3 Write the running-counts property test
    - `RunningCountsPropertyTest.kt`
    - **Property 36: The running counts equal the reference counts over the rows**
    - Tag: `// Feature: attendance-marking, Property 36: The running counts equal the reference counts over the rows`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over selection sequences, using the server's present/on-leave definitions
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 20.1 (C6)**
    - **Validates: Requirements 12.7**
  - [ ]* 20.4 Write the submission-coverage property test
    - `SubmissionCoversAllRowsPropertyTest.kt`
    - **Property 37: Confirmation submits one entry per row**
    - Tag: `// Feature: attendance-marking, Property 37: Confirmation submits one entry per row`
    - `checkAll(PropTestConfig(iterations = 100), ...)`; assert equal size and exactly equal student identifier sets
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 20.1 (C6)**
    - **Validates: Requirements 12.8**
  - [ ]* 20.5 Write the submission-id reuse property test
    - `SubmissionIdReusePropertyTest.kt`
    - **Property 38: A retry reuses the attempt's submission identifier**
    - Tag: `// Feature: attendance-marking, Property 38: A retry reuses the attempt's submission identifier`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over consecutive failure counts; assert one identifier across the attempt and that it parses as a UUID
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 20.1 (C6)**
    - **Validates: Requirements 12.10**
  - [ ]* 20.6 Write the failure-retains-selections property test
    - `FailureRetainsSelectionsPropertyTest.kt`
    - **Property 39: A failure surfaces the error and retains every selection**
    - Tag: `// Feature: attendance-marking, Property 39: A failure surfaces the error and retains every selection`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over `AppError` variants; assert the Error message and a row-for-row identical row list
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 20.1 (C6)**
    - **Validates: Requirements 12.13, 12.14**
  - [ ]* 20.7 Write the amendment-control property test
    - `AmendmentControlPropertyTest.kt`
    - **Property 40: The amendment control is offered exactly when the window is open**
    - Tag: `// Feature: attendance-marking, Property 40: The amendment control is offered exactly when the window is open`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over (session status, elapsed 0–400 hours); the flag is server-supplied
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 20.1 (C6)**
    - **Validates: Requirements 12.17**
  - [ ]* 20.8 Write the client reason-validation property test
    - `AmendmentReasonValidationPropertyTest.kt`
    - **Property 41: Client-side reason validation matches the server rule**
    - Tag: `// Feature: attendance-marking, Property 41: Client-side reason validation matches the server rule`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over arbitrary strings; the predicate must match the server's `isBlank()` rule exactly
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 20.1 (C6)**
    - **Validates: Requirements 12.18**
  - [ ]* 20.9 Write ViewModel unit tests
    - `MarkAttendanceViewModelTest.kt`: the Loading state while the roster is awaited (12.3), the in-flight disable flag (12.11) and the success counts (12.12)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 20.1 (C6)**
    - _Requirements: 12.3, 12.11, 12.12_

- [ ] 21. Checkpoint — end of wave 2
  - Run `./gradlew :backend:test :androidApp:testDebugUnitTest :shared:allTests`
  - Ensure all tests pass, ask the user if questions arise.

### Wave 3 — facade, DI and screen

- [ ] 22. B13: The service facade
  - [ ] 22.1 Implement `attendance/service/AttendanceService.kt`
    - Roughly forty lines of delegation to the four collaborators. No logic, no repositories, and **no `@Transactional` of its own**, so each operation runs in exactly one transaction with no nesting
    - This is the only service type the controller references, which is what keeps criterion 10.12 and the glossary's single Attendance_Service true
    - **Blocked by: 13.1 (B9), 14.1 (B10), 17.1 (B11), 18.1 (B12)**
    - _Requirements: 5.15, 6.9, 9.10, 10.12_
  - [ ]* 22.2 Write facade delegation tests
    - `AttendanceServiceTest.kt`: each of the six methods delegates to the right collaborator with the right arguments and does nothing else; use `confirmVerified` to prove no extra interaction
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 22.1 (B13)**
    - _Requirements: 10.12_

- [x] 23. C5: The Hilt binding
  - [x] 23.1 Add `provideAttendanceRepository` to `RepositoryModule.kt`
    - One additive provider method binding `HttpAttendanceRepository` to the shared `AttendanceRepository` interface
    - **Blocked by: 19.1 (C4)**
    - **Sole owner of `androidApp/.../di/RepositoryModule.kt`.** No other task may open it
    - _Requirements: 12.21_

- [ ] 24. C7: The Compose screen
  - [ ] 24.1 Implement `androidApp/.../ui/attendance/MarkAttendanceScreen.kt`
    - `collectAsStateWithLifecycle()` + `when (state)` over the three cases
    - Each status selector carries `contentDescription = "${row.fullName}, marked ${row.selectedStatus.name}"` (12.19). Extract this into a named pure function so it is assertable without an emulator
    - `object MarkAttendanceDimens { val ConfirmMinTouchTarget = 48.dp }`, applied as `Modifier.defaultMinSize(...)` (12.20) — a named constant so the value is assertable in a plain unit test
    - Confirm control disabled while `isSubmitting` (12.11); selector covers all five statuses (12.6); rows whose suggested status is ON_LEAVE show `leaveReason` as supporting text (12.15); FINALIZED sessions render stored statuses (12.16); the amendment control appears only when `status == FINALIZED && amendmentWindowOpen` (12.17); the amendment sheet's submit is enabled only while the reason is not blank (12.18)
    - **Blocked by: 20.1 (C6)**
    - _Requirements: 12.4, 12.6, 12.7, 12.11, 12.12, 12.15, 12.16, 12.17, 12.18, 12.19, 12.20_
  - [ ]* 24.2 Write the content-description property test
    - `StatusSelectorDescriptionPropertyTest.kt` against the extracted pure function
    - **Property 42: Every status selector names its student and selection**
    - Tag: `// Feature: attendance-marking, Property 42: Every status selector names its student and selection`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over row lists; assert the description contains the full name and the selected status name
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 24.1 (C7)**
    - **Validates: Requirements 12.19**
  - [ ]* 24.3 Write screen constant unit tests
    - `MarkAttendanceScreenTest.kt`: `MarkAttendanceDimens.ConfirmMinTouchTarget == 48.dp` (12.20) and the five-option selector list (12.6), both as plain unit tests — `androidApp` has no `androidTest` source set and CI has no emulator, so an instrumented Compose test stays out of scope
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 24.1 (C7)**
    - _Requirements: 12.6, 12.20_

### Wave 4 — controller, navigation and percentage regression

- [ ] 25. B14: The controller
  - [ ] 25.1 Implement `attendance/controller/AttendanceController.kt`
    - `@RestController @RequestMapping("/attendance")`; with `server.servlet.context-path: /api` the deployed routes resolve under `/api/attendance` (10.9)
    - Six methods, each three lines: `accessGuard.requireCaller`, `accessGuard.requireSessionAccess`, delegate to the facade, wrapped in `ApiResponse.success(...)` (10.10)
    - `POST` for `activate`, `confirm`, `cancel` and `amend`; `GET` for `roster` and `audit-history`
    - The session identifier appears **only in the path**, never in a request body
    - No repository query beyond resolving the caller (10.12). `GlobalExceptionHandler` is not modified
    - **Blocked by: 5.1 (B4), 6.1 (B5), 22.1 (B13).** The design lists B13 and B14 in the same wave, but its dependency table blocks B14 on B13, so this task runs after 22.1
    - _Requirements: 10.8, 10.9, 10.10, 10.11, 10.12_
  - [ ]* 25.2 Write MockMvc authorization and status-code tests
    - `AttendanceControllerAuthorizationTest.kt` with a MockK'd `AttendanceService`: parameterised across all six routes for 401 (no principal), 401 (unknown uid), 403 (STUDENT), 403 (non-owning TEACHER) and success (ADMIN), plus the full condition→status→code table from the design's Error Handling section
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 25.1 (B14)**
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.6, 10.7, 10.8, 10.10, 10.11, 1.8, 2.4, 2.9, 3.2, 5.2, 5.6, 5.7, 5.8, 6.2, 6.7, 6.8, 6.10, 9.2, 9.5, 9.11, 9.12_
  - [ ]* 25.3 Write routing and delegation tests
    - `AttendanceControllerRoutingTest.kt`: the six routes resolve under `/api/attendance/...` (10.9) and `confirmVerified` proves the controller issues no repository query beyond resolving the caller (10.12)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 25.1 (B14)**
    - _Requirements: 10.9, 10.12_

- [ ] 26. C8: Navigation wiring
  - [ ] 26.1 Add the route and the dashboard click handler
    - One route for `MarkAttendanceScreen` in `MainNavigation.kt`, carrying the lecture session identifier, plus the teacher dashboard click handler that navigates to it (12.1)
    - **Blocked by: 24.1 (C7)**
    - **Sole owner of `androidApp/.../navigation/MainNavigation.kt`** and of the teacher dashboard click handler edit. No other task may open them
    - _Requirements: 12.1_
  - [ ]* 26.2 Write the navigation test
    - `MarkAttendanceNavigationTest.kt`: selecting a lecture session produces the mark-attendance route with the correct session identifier argument (12.1)
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 26.1 (C8)**
    - _Requirements: 12.1_

- [ ] 27. Cross-track verification
  - [ ]* 27.1 Write the attendance percentage property test
    - `AttendancePercentagePropertyTest.kt` in the dashboard test package
    - **Property 23: The percentage equals the reference formula, scoped by semester**
    - Tag: `// Feature: attendance-marking, Property 23: The percentage equals the reference formula, scoped by semester`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over records spread across two or more semesters; assert the reference formula, two-decimal rounding, 0.0 on a zero denominator, and that other semesters affect neither value. `semesterId` is a `String` UUID, never a `Long`
    - `DashboardService` is **not** modified by this task or any other
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 17.1 (B11), 18.1 (B12)**
    - **Validates: Requirements 8.1, 8.5, 8.8, 8.9**
  - [ ]* 27.2 Write the cancellation-preserves-percentage property test
    - `CancellationPreservesPercentagePropertyTest.kt`
    - **Property 24: Cancelling a session preserves every percentage**
    - Tag: `// Feature: attendance-marking, Property 24: Cancelling a session preserves every percentage`
    - `checkAll(PropTestConfig(iterations = 100), ...)`; assert every student's percentage is identical before and after the CANCELLED transition
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 14.1 (B10), 17.1 (B11)**
    - **Validates: Requirements 8.6**
  - [ ]* 27.3 Write the neutral-status property test
    - `NeutralStatusPreservesPercentagePropertyTest.kt`
    - **Property 25: A neutral status preserves every percentage**
    - Tag: `// Feature: attendance-marking, Property 25: A neutral status preserves every percentage`
    - `checkAll(PropTestConfig(iterations = 100), ...)` over prior histories including the empty one where both values are 0.0
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 17.1 (B11)**
    - **Validates: Requirements 8.4, 8.7**
  - [ ]* 27.4 Extend `DashboardServiceTest` with the counter-independence example
    - Add a test asserting that corrupted Lecture_Session counters do not move a student's percentage, because the percentage derives from `countAttended`/`countCountable` (8.1)
    - **Extend, do not replace.** **Sole owner of the existing `DashboardServiceTest.kt`;** no other task may open it. Verify the existing assertions still pass unchanged
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 1.1 (T0), 17.1 (B11), 18.1 (B12)**
    - _Requirements: 8.1_
  - [ ]* 27.5 Write the confirmation concurrency test
    - `backend/src/test/.../attendance/integration/ConfirmationConcurrencyTest.kt`, **`@SpringBootTest(webEnvironment = NONE)`** on H2
    - **It must NOT be `@DataJpaTest`:** `@DataJpaTest` wraps each test in a single rolled-back transaction, so two transactions can never race and the test would prove nothing
    - One ACTIVE session with a known ACTIVE enrollment count; an `ExecutorService` of two threads each calling `attendanceService.confirm(...)` with a **different** `submissionId` and the same complete entry list; a `CountDownLatch(1)` released after both threads start so they contend
    - Assert: exactly one call returns a confirmation and the other throws `ConflictException`; `attendance_records` holds exactly `activeEnrollmentCount` rows; `attendance_submissions` holds exactly one row for the session; `present + absent + on_leave == record count`
    - The invariant assertions carry the weight, not the exception type — H2's `SELECT ... FOR UPDATE` is not PostgreSQL MVCC, so the test proves single-winner behaviour rather than PostgreSQL isolation semantics
    - This criterion is deliberately not a property test: no property harness provides two real transactions
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 22.1 (B13), 25.1 (B14)**
    - _Requirements: 6.11_
  - [ ]* 27.6 Write the transaction rollback integration tests
    - `backend/src/test/.../attendance/integration/AttendanceTransactionRollbackTest.kt`, `@SpringBootTest`
    - Induce a failure mid-confirmation and assert nothing is left behind: no attendance records, no submission row, unchanged counters and unchanged session status (5.15, 6.9); the same for an amendment (9.10)
    - Example-based, not property-based: this needs an induced failure, not input variation
    - Mocking: MockK only — no Mockito/`mockito-kotlin`/`@Mock`/`when(...)`/`thenReturn`
    - **Blocked by: 22.1 (B13), 25.1 (B14)**
    - _Requirements: 5.15, 6.9, 9.10_

- [ ] 28. Final checkpoint — regression guarantee
  - Run `./gradlew :backend:test :androidApp:testDebugUnitTest :shared:allTests`
  - The 87 pre-existing tests (55 backend, 32 Android) must still pass alongside every new test. If any of them fails, the cause is a contract change the design expected to be additive — report it rather than editing the existing test
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional test sub-tasks and can be skipped for a faster MVP. Two of them are load-bearing for the others: **5.2** creates the shared backend `Arb` generators and **20.1** creates `FakeAttendanceRepository`. If the optional tests are skipped, skip them as a group rather than individually.
- Unit IDs (`B1`–`B14`, `C1`–`C8`, `T0`) are the design's. Every task title carries its unit ID so the task list and the design stay cross-referenceable.
- The backend track and the client track have **no code dependency on each other**. They implement the same normative Wire Contract and meet only in the wave 4–5 verification tasks. A backend agent and an Android agent can start at minute zero.
- The ten contested files have exactly one owner each, listed in the table above and restated on the owning task. This is the highest-risk failure mode for concurrent execution: two agents editing one file.
- Property tests use `kotest-property` 5.8.0 as a library with `checkAll` inside `runTest { }`, never as a runner, so the existing JUnit suites are unaffected. Minimum 100 iterations, one test per design property, each tagged `// Feature: attendance-marking, Property N: ...`.
- `GlobalExceptionHandler`, `JacksonConfig` and `DashboardService` are not modified by any task in this plan.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "3.1", "4.1", "5.1", "6.1", "7.1", "8.1", "9.1"] },
    { "id": 1, "tasks": ["2.2", "3.2", "4.2", "5.2", "6.2", "6.3", "7.2", "7.3", "8.2", "8.3", "8.4", "8.5", "8.6", "8.7", "9.2", "11.1", "12.1", "13.1", "14.1", "15.1"] },
    { "id": 2, "tasks": ["11.2", "12.2", "12.3", "12.4", "13.2", "13.3", "13.4", "13.5", "13.6", "14.2", "14.3", "14.4", "14.5", "17.1", "18.1", "19.1", "20.1"] },
    { "id": 3, "tasks": ["17.2", "17.3", "17.4", "17.5", "17.6", "17.7", "17.8", "17.9", "17.10", "17.11", "17.12", "17.13", "18.2", "18.3", "18.4", "18.5", "18.6", "18.7", "18.8", "18.9", "19.2", "19.3", "19.4", "20.2", "20.3", "20.4", "20.5", "20.6", "20.7", "20.8", "20.9", "22.1", "23.1", "24.1"] },
    { "id": 4, "tasks": ["22.2", "24.2", "24.3", "25.1", "26.1", "27.1", "27.2", "27.3", "27.4"] },
    { "id": 5, "tasks": ["25.2", "25.3", "26.2", "27.5", "27.6"] }
  ]
}
```
