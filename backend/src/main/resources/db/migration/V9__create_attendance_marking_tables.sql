-- Migration V9: Submission records and attendance audit trail
-- Supports idempotent confirmation (R6) and amendment auditing (R9).
-- Both tables are append-only: no updated_at, no update_updated_at_column() trigger.

-- ---------------------------------------------------------------------------
-- Submission_Record. Primary key is the client-generated submissionId, so a
-- duplicate insert fails at the database level (R6.12).
-- Retention: 90 days from created_at. Purged by an operator, not a job.
-- ---------------------------------------------------------------------------
CREATE TABLE attendance_submissions (
    submission_id       UUID PRIMARY KEY,
    lecture_session_id  BIGINT NOT NULL REFERENCES lecture_sessions(id) ON DELETE CASCADE,
    acting_user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- VARCHAR, not CHAR: PostgreSQL renders CHAR(64) as bpchar, which fails
    -- ddl-auto=validate against Hibernate's varchar(64) mapping for a String.
    request_fingerprint VARCHAR(64) NOT NULL,
    confirmation_json   TEXT NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attendance_submissions_session ON attendance_submissions(lecture_session_id);
CREATE INDEX idx_attendance_submissions_created ON attendance_submissions(created_at);

COMMENT ON TABLE attendance_submissions IS
    'Idempotency keys for attendance confirmation. Append-only. Retention 90 days: DELETE FROM attendance_submissions WHERE created_at < now() - interval ''90 days'';';
COMMENT ON COLUMN attendance_submissions.request_fingerprint IS
    'Lowercase SHA-256 hex over the session id and the entries sorted by student id';
COMMENT ON COLUMN attendance_submissions.confirmation_json IS
    'The stored AttendanceConfirmation, replayed verbatim on a matching retry. TEXT rather than JSONB so the mapping is identical on PostgreSQL and H2.';

-- ---------------------------------------------------------------------------
-- Attendance_Audit_Entry. Append-only and never purged (R9.15).
-- lecture_session_id and student_id are denormalised so the audit-history
-- query (R9.16) and the isCorrected derivation (R9.14) need no entity join.
-- ---------------------------------------------------------------------------
CREATE TABLE attendance_audit_entries (
    id                   BIGSERIAL PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL REFERENCES attendance_records(id) ON DELETE CASCADE,
    lecture_session_id   BIGINT NOT NULL REFERENCES lecture_sessions(id) ON DELETE CASCADE,
    student_id           BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    previous_status      VARCHAR(20) NOT NULL
        CHECK (previous_status IN ('PRESENT','ABSENT','LATE','ON_LEAVE','EXCUSED')),
    new_status           VARCHAR(20) NOT NULL
        CHECK (new_status IN ('PRESENT','ABSENT','LATE','ON_LEAVE','EXCUSED')),
    changed_by_user_id   BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason               TEXT NOT NULL,
    changed_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- R9.13: a no-op amendment must write no audit entry. Enforced here so a
    -- service bug cannot persist one.
    CONSTRAINT chk_audit_status_changed CHECK (previous_status <> new_status)
);

CREATE INDEX idx_audit_session_changed ON attendance_audit_entries(lecture_session_id, changed_at);
CREATE INDEX idx_audit_record          ON attendance_audit_entries(attendance_record_id);

COMMENT ON TABLE attendance_audit_entries IS
    'Append-only record of attendance amendments. Never updated, never deleted, never purged (R9.15). Growth is bounded by amendment volume, not by time.';