-- Migration V7: Attendance records and leave requests
-- Phase A8.3: Attendance tracking and leave management

CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    lecture_session_id BIGINT NOT NULL REFERENCES lecture_sessions(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('PRESENT','ABSENT','LATE','ON_LEAVE','EXCUSED')),
    method VARCHAR(20) CHECK (method IN ('CAMERA','MANUAL','HYBRID')),
    confidence_score DOUBLE PRECISION,
    marked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    marked_by_teacher_id BIGINT REFERENCES teachers(id) ON DELETE SET NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance_session_student UNIQUE (lecture_session_id, student_id)
);

CREATE INDEX idx_attendance_session ON attendance_records(lecture_session_id);
CREATE INDEX idx_attendance_student ON attendance_records(student_id);
CREATE INDEX idx_attendance_status ON attendance_records(status);

COMMENT ON TABLE attendance_records IS 'One row per student per lecture session';
COMMENT ON COLUMN attendance_records.confidence_score IS 'Face-recognition confidence when method = CAMERA';

CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    class_section_id BIGINT REFERENCES class_sections(id) ON DELETE CASCADE,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    reviewed_by_teacher_id BIGINT REFERENCES teachers(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    review_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_leave_date_order CHECK (to_date >= from_date)
);

CREATE INDEX idx_leave_student ON leave_requests(student_id);
CREATE INDEX idx_leave_class_section ON leave_requests(class_section_id);
CREATE INDEX idx_leave_status ON leave_requests(status);

COMMENT ON TABLE leave_requests IS 'Student leave applications awaiting teacher review';
COMMENT ON COLUMN leave_requests.class_section_id IS 'Null means the request spans all of the students classes';

CREATE TRIGGER update_attendance_records_updated_at
    BEFORE UPDATE ON attendance_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_leave_requests_updated_at
    BEFORE UPDATE ON leave_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ---------------------------------------------------------------------------
-- Seed attendance for every finalized session, so the dashboard has real
-- history. Roughly a 6-in-7 present rate, varied per student and session.
-- ---------------------------------------------------------------------------

INSERT INTO attendance_records (lecture_session_id, student_id, status, method, marked_by_teacher_id)
SELECT
    ls.id,
    e.student_id,
    CASE WHEN ((ls.id + e.student_id) % 7) = 0 THEN 'ABSENT' ELSE 'PRESENT' END,
    'MANUAL',
    cs.teacher_id
FROM lecture_sessions ls
JOIN class_sections cs ON cs.id = ls.class_section_id
JOIN enrollments e ON e.class_section_id = ls.class_section_id AND e.status = 'ACTIVE'
WHERE ls.status = 'FINALIZED';

-- Roll the per-session counters up to match the records just inserted.
UPDATE lecture_sessions ls
SET present_count = agg.present_count,
    absent_count  = agg.absent_count,
    on_leave_count = agg.on_leave_count
FROM (
    SELECT lecture_session_id,
           COUNT(*) FILTER (WHERE status IN ('PRESENT','LATE'))  AS present_count,
           COUNT(*) FILTER (WHERE status = 'ABSENT')             AS absent_count,
           COUNT(*) FILTER (WHERE status = 'ON_LEAVE')           AS on_leave_count
    FROM attendance_records
    GROUP BY lecture_session_id
) AS agg
WHERE agg.lecture_session_id = ls.id;

-- A couple of pending leave requests for the teacher dashboard counter.
INSERT INTO leave_requests (student_id, class_section_id, from_date, to_date, reason, status)
VALUES
    ((SELECT id FROM students WHERE roll_number = '2024CS001'),
     (SELECT id FROM class_sections WHERE name = 'CS101 Section A'),
     CURRENT_DATE + 1, CURRENT_DATE + 2, 'Medical appointment', 'PENDING'),
    ((SELECT id FROM students WHERE roll_number = '2024CS002'),
     (SELECT id FROM class_sections WHERE name = 'CS101 Section A'),
     CURRENT_DATE + 3, CURRENT_DATE + 3, 'Family function', 'PENDING'),
    ((SELECT id FROM students WHERE roll_number = '2024CS002'),
     (SELECT id FROM class_sections WHERE name = 'CS103 Section A'),
     CURRENT_DATE - 5, CURRENT_DATE - 4, 'Illness', 'APPROVED');