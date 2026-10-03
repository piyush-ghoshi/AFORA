-- Migration V5: Core Academic Tables
-- Phase A8.1: Foundation for attendance system

-- Subjects table
CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    credits INT NOT NULL DEFAULT 3,
    department VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subjects_code ON subjects(code);
CREATE INDEX idx_subjects_department ON subjects(department);
CREATE INDEX idx_subjects_active ON subjects(is_active);

COMMENT ON TABLE subjects IS 'Academic subjects/courses offered';
COMMENT ON COLUMN subjects.code IS 'Unique subject code (e.g., CS101)';
COMMENT ON COLUMN subjects.credits IS 'Credit hours for the subject';

-- Class Sections table
CREATE TABLE class_sections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE RESTRICT,
    semester_id UUID NOT NULL REFERENCES semesters(id) ON DELETE CASCADE,
    batch VARCHAR(20) NOT NULL,
    section VARCHAR(10) NOT NULL,
    room_number VARCHAR(50),
    max_students INT NOT NULL DEFAULT 60,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(subject_id, semester_id, section)
);

CREATE INDEX idx_class_sections_subject ON class_sections(subject_id);
CREATE INDEX idx_class_sections_teacher ON class_sections(teacher_id);
CREATE INDEX idx_class_sections_semester ON class_sections(semester_id);
CREATE INDEX idx_class_sections_batch ON class_sections(batch);
CREATE INDEX idx_class_sections_active ON class_sections(is_active);

COMMENT ON TABLE class_sections IS 'Class sections for subjects (e.g., CS101 Section A)';
COMMENT ON COLUMN class_sections.batch IS 'Student batch year (e.g., 2024)';
COMMENT ON COLUMN class_sections.section IS 'Section identifier (e.g., A, B, C)';

-- Enrollments table (student-class mapping)
CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    class_section_id BIGINT NOT NULL REFERENCES class_sections(id) ON DELETE CASCADE,
    enrollment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DROPPED', 'COMPLETED')),
    grade VARCHAR(5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, class_section_id)
);

CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_class_section ON enrollments(class_section_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);

COMMENT ON TABLE enrollments IS 'Student enrollments in class sections';
COMMENT ON COLUMN enrollments.status IS 'Enrollment status: ACTIVE, DROPPED, or COMPLETED';
COMMENT ON COLUMN enrollments.grade IS 'Final grade (filled at end of semester)';

-- Triggers for updated_at
CREATE TRIGGER update_subjects_updated_at
    BEFORE UPDATE ON subjects
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_class_sections_updated_at
    BEFORE UPDATE ON class_sections
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_enrollments_updated_at
    BEFORE UPDATE ON enrollments
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Seed data: Subjects
INSERT INTO subjects (code, name, description, credits, department) VALUES
('CS101', 'Data Structures', 'Introduction to fundamental data structures and algorithms', 4, 'Computer Science'),
('CS102', 'Algorithms', 'Design and analysis of algorithms', 4, 'Computer Science'),
('CS103', 'Database Systems', 'Relational database design and SQL', 3, 'Computer Science'),
('MATH201', 'Linear Algebra', 'Matrices, vector spaces, and linear transformations', 3, 'Mathematics'),
('PHY101', 'Physics I', 'Mechanics and thermodynamics', 4, 'Physics');

-- Note: class_sections and enrollments seed data will be added after we have real users/teachers
-- This will be done in V5.1 migration after Phase A8.1 implementation

