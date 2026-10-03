-- Migration V6: Timetable slots and lecture sessions
-- Phase A8.2: Schedule management

-- Recurring weekly timetable entries for a class section
CREATE TABLE timetable_slots (
    id BIGSERIAL PRIMARY KEY,
    class_section_id BIGINT NOT NULL REFERENCES class_sections(id) ON DELETE CASCADE,
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room_number VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_timetable_time_order CHECK (end_time > start_time)
);

CREATE INDEX idx_timetable_slots_class_section ON timetable_slots(class_section_id);
CREATE INDEX idx_timetable_slots_day ON timetable_slots(day_of_week);
CREATE INDEX idx_timetable_slots_active ON timetable_slots(is_active);

COMMENT ON TABLE timetable_slots IS 'Recurring weekly schedule entries per class section';
COMMENT ON COLUMN timetable_slots.day_of_week IS 'ISO day of week: 1=Monday .. 7=Sunday';

-- A concrete occurrence of a class on a specific date
CREATE TABLE lecture_sessions (
    id BIGSERIAL PRIMARY KEY,
    class_section_id BIGINT NOT NULL REFERENCES class_sections(id) ON DELETE CASCADE,
    timetable_slot_id BIGINT REFERENCES timetable_slots(id) ON DELETE SET NULL,
    semester_id UUID NOT NULL REFERENCES semesters(id) ON DELETE CASCADE,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room_number VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED','CREATED','ACTIVE','SCANNING','REVIEW','FINALIZING','FINALIZED','CANCELLED','FAILED')),
    attendance_method VARCHAR(20) CHECK (attendance_method IN ('CAMERA','MANUAL','HYBRID')),
    total_students INT,
    present_count INT NOT NULL DEFAULT 0,
    absent_count INT NOT NULL DEFAULT 0,
    on_leave_count INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lecture_session_slot_date UNIQUE (class_section_id, session_date, start_time)
);

CREATE INDEX idx_lecture_sessions_class_section ON lecture_sessions(class_section_id);
CREATE INDEX idx_lecture_sessions_date ON lecture_sessions(session_date);
CREATE INDEX idx_lecture_sessions_semester ON lecture_sessions(semester_id);
CREATE INDEX idx_lecture_sessions_status ON lecture_sessions(status);

COMMENT ON TABLE lecture_sessions IS 'Concrete dated occurrence of a class, the unit attendance is taken against';
COMMENT ON COLUMN lecture_sessions.session_date IS 'Named session_date rather than date to avoid reserved-word friction';

-- Triggers
CREATE TRIGGER update_timetable_slots_updated_at
    BEFORE UPDATE ON timetable_slots
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_lecture_sessions_updated_at
    BEFORE UPDATE ON lecture_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ---------------------------------------------------------------------------
-- Seed: class sections, enrollments, timetable, sessions
-- V5 deferred these because they depend on the users seeded in V4.
-- ---------------------------------------------------------------------------

INSERT INTO class_sections (name, subject_id, teacher_id, semester_id, batch, section, room_number, max_students)
VALUES
    ('CS101 Section A',
     (SELECT id FROM subjects WHERE code = 'CS101'),
     (SELECT id FROM teachers WHERE employee_id = 'EMP001'),
     '123e4567-e89b-12d3-a456-426614174002', '2024', 'A', 'A-101', 60),
    ('CS102 Section A',
     (SELECT id FROM subjects WHERE code = 'CS102'),
     (SELECT id FROM teachers WHERE employee_id = 'EMP001'),
     '123e4567-e89b-12d3-a456-426614174002', '2024', 'A', 'B-205', 60),
    ('CS103 Section A',
     (SELECT id FROM subjects WHERE code = 'CS103'),
     (SELECT id FROM teachers WHERE employee_id = 'EMP002'),
     '123e4567-e89b-12d3-a456-426614174002', '2024', 'A', 'C-301', 55);

INSERT INTO enrollments (student_id, class_section_id, status)
SELECT s.id, cs.id, 'ACTIVE'
FROM students s
CROSS JOIN class_sections cs
WHERE s.roll_number IN ('2024CS001', '2024CS002')
  AND cs.batch = '2024';

INSERT INTO timetable_slots (class_section_id, day_of_week, start_time, end_time, room_number)
SELECT cs.id, d.day_of_week, d.start_time, d.end_time, cs.room_number
FROM class_sections cs
JOIN (VALUES
    ('CS101 Section A', 1, TIME '09:00', TIME '10:30'),
    ('CS101 Section A', 3, TIME '09:00', TIME '10:30'),
    ('CS102 Section A', 1, TIME '11:00', TIME '12:30'),
    ('CS102 Section A', 4, TIME '11:00', TIME '12:30'),
    ('CS103 Section A', 2, TIME '14:00', TIME '15:30'),
    ('CS103 Section A', 5, TIME '14:00', TIME '15:30')
) AS d(section_name, day_of_week, start_time, end_time)
  ON d.section_name = cs.name;

-- Generate the last 14 days of sessions from the weekly timetable so the
-- dashboard has attendance history to aggregate.
INSERT INTO lecture_sessions (
    class_section_id, timetable_slot_id, semester_id, session_date,
    start_time, end_time, room_number, status, total_students
)
SELECT
    ts.class_section_id,
    ts.id,
    cs.semester_id,
    d.session_date,
    ts.start_time,
    ts.end_time,
    ts.room_number,
    CASE WHEN d.session_date < CURRENT_DATE THEN 'FINALIZED' ELSE 'SCHEDULED' END,
    (SELECT COUNT(*) FROM enrollments e WHERE e.class_section_id = ts.class_section_id AND e.status = 'ACTIVE')
FROM timetable_slots ts
JOIN class_sections cs ON cs.id = ts.class_section_id
CROSS JOIN (
    SELECT (CURRENT_DATE - offs)::date AS session_date
    FROM generate_series(0, 13) AS offs
) AS d
WHERE EXTRACT(ISODOW FROM d.session_date) = ts.day_of_week;