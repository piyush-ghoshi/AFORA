-- V11__create_face_recognition_tables.sql
-- Face_Consent, Face_Profile and Face_Embedding storage (Requirements 1-4).
-- No face image, no pgvector extension: embeddings are plain delimited TEXT,
-- parsed and serialized only by Face_Profile_Service, for the same cross-engine
-- (H2 test / PostgreSQL dev, both under ddl-auto: validate) reason the sibling
-- attendance-marking migration (V9) chose TEXT over jsonb for confirmation_json.

CREATE TABLE face_consents (
    student_id   BIGINT PRIMARY KEY REFERENCES students(id) ON DELETE CASCADE,
    consented_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE face_consents IS
    'One row per student: the single most recent Face_Consent (R1.3). Deleted whenever '
    'face_embeddings rows are purged under Requirement 4 (R4.3), so re-registration after '
    'deletion requires consenting again (R1.1, R1.5).';

CREATE TABLE face_profiles (
    student_id    BIGINT PRIMARY KEY REFERENCES students(id) ON DELETE CASCADE,
    dimensions    INT NOT NULL CHECK (dimensions > 0),
    registered_at TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL
);

COMMENT ON TABLE face_profiles IS
    'One row per student holding a registered Face_Profile. Exists so "does a profile '
    'already exist" (R2.4) and the registration timestamp (R2.5) are O(1) lookups that do '
    'not scan face_embeddings. dimensions caches the vector length so R2.7''s cross-request '
    'dimensionality check never has to parse embedding_vector.';

CREATE TABLE face_embeddings (
    id               BIGSERIAL PRIMARY KEY,
    student_id       BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    embedding_vector TEXT NOT NULL,
    dimensions       INT NOT NULL CHECK (dimensions > 0),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_face_embeddings_student ON face_embeddings(student_id);

COMMENT ON TABLE face_embeddings IS
    'Numeric Face_Embedding vectors only -- never a face image (R3.2). embedding_vector is '
    'a comma-delimited list of doubles, app-parsed. No vector-similarity index, no pgvector '
    'extension (R3.1). No backend process ever compares two of these rows (R3.3).';

CREATE TABLE face_profile_deletions (
    id         BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    reason     VARCHAR(30) NOT NULL
        CHECK (reason IN ('STUDENT_REQUEST', 'ADMIN_REQUEST', 'ENROLLMENT_ENDED')),
    deleted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_face_profile_deletions_student ON face_profile_deletions(student_id);

COMMENT ON TABLE face_profile_deletions IS
    'Append-only record of every Face_Profile purge (R4.4). Never updated, never deleted. '
    'The only durable trace that a student''s biometric data existed, once face_consents '
    'and face_embeddings rows for that student are gone.';
