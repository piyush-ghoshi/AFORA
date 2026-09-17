# Database Design Document
## Smart Classroom Attendance Management System

**Version:** 1.0  
**Date:** August 25, 2026  
**Database:** PostgreSQL 14+  
**Status:** Draft - Awaiting Review

---

## 1. Database Overview

### 1.1 Database Principles

- **Normalized**: 3NF minimum, denormalization only where justified
- **Referential Integrity**: Foreign keys enforced
- **Constraints**: Check constraints, unique constraints
- **Audit Trail**: Created/updated timestamps, soft deletes where needed
- **Indexing**: Strategic indexes for performance
- **Migrations**: Flyway for versioned schema management

### 1.2 Naming Conventions

- Tables: `snake_case`, plural (e.g., `students`, `attendance_records`)
- Columns: `snake_case` (e.g., `first_name`, `roll_number`)
- Primary keys: `id` (BIGSERIAL)
- Foreign keys: `{table}_id` (e.g., `student_id`, `class_id`)
- Indexes: `idx_{table}_{column(s)}` (e.g., `idx_students_roll_number`)
- Constraints: `{constraint_type}_{table}_{column(s)}` (e.g., `chk_attendance_status`)

---

## 2. Entity Relationship Diagram

```
┌──────────┐
│  users   │
└────┬─────┘
     │
     ├───────────────────────────────────┐
     │                                   │
┌────▼──────┐                      ┌────▼────┐
│ students  │                      │teachers │
└─────┬─────┘                      └────┬────┘
      │                                 │
      │    ┌────────────────┐          │
      └────│  enrollments   │──────────┤
           └───────┬────────┘          │
                   │                    │
           ┌───────▼──────────┐        │
           │   class_sections │        │
           └───────┬──────────┘        │
                   │                    │
           ┌───────▼────────┐          │
           │    subjects    │◄─────────┤
           └───────┬────────┘          │
                   │                    │
           ┌───────▼──────────────┐    │
           │ teacher_assignments  │────┘
           └───────┬──────────────┘
                   │
           ┌───────▼────────────┐
           │ lecture_sessions   │
           └───────┬────────────┘
                   │
           ┌───────▼──────────────┐
           │ attendance_records   │
           └──────────────────────┘
```

---

## 3. Core Tables

### 3.1 User Management

#### users
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'TEACHER', 'ADMIN')),
    is_active BOOLEAN DEFAULT true,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}$')
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
```

#### students
```sql
CREATE TABLE students (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    roll_number VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    phone VARCHAR(20),
    emergency_contact VARCHAR(20),
    address TEXT,
    enrollment_date DATE NOT NULL,
    graduation_date DATE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_dob_valid CHECK (date_of_birth < CURRENT_DATE),
    CONSTRAINT chk_graduation_after_enrollment CHECK (graduation_date IS NULL OR graduation_date > enrollment_date)
);

CREATE INDEX idx_students_roll_number ON students(roll_number);
CREATE INDEX idx_students_user_id ON students(user_id);
CREATE INDEX idx_students_is_active ON students(is_active);
CREATE INDEX idx_students_name ON students(first_name, last_name);
```

#### teachers
```sql
CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    employee_id VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    designation VARCHAR(100),
    department_id BIGINT REFERENCES departments(id),
    phone VARCHAR(20),
    office_location VARCHAR(100),
    joining_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_teachers_employee_id ON teachers(employee_id);
CREATE INDEX idx_teachers_user_id ON teachers(user_id);
CREATE INDEX idx_teachers_department ON teachers(department_id);
CREATE INDEX idx_teachers_is_active ON teachers(is_active);
```

#### admins
```sql
CREATE TABLE admins (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    designation VARCHAR(100),
    is_super_admin BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admins_user_id ON admins(user_id);
```

---

### 3.2 Academic Structure

#### departments
```sql
CREATE TABLE departments (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    head_teacher_id BIGINT REFERENCES teachers(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_departments_code ON departments(code);
CREATE INDEX idx_departments_is_active ON departments(is_active);
```

#### programs
```sql
CREATE TABLE programs (
    id BIGSERIAL PRIMARY KEY,
    department_id BIGINT NOT NULL REFERENCES departments(id),
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    degree_type VARCHAR(50) CHECK (degree_type IN ('BACHELOR', 'MASTER', 'DOCTORAL', 'DIPLOMA')),
    duration_years INT NOT NULL CHECK (duration_years > 0),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_programs_department ON programs(department_id);
CREATE INDEX idx_programs_code ON programs(code);
CREATE INDEX idx_programs_is_active ON programs(is_active);
```

#### batches
```sql
CREATE TABLE batches (
    id BIGSERIAL PRIMARY KEY,
    program_id BIGINT NOT NULL REFERENCES programs(id),
    year INT NOT NULL,
    name VARCHAR(100) NOT NULL,  -- e.g., "2024 Batch", "Batch 2024-2028"
    start_year INT NOT NULL,
    end_year INT NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_batch_years CHECK (end_year > start_year),
    CONSTRAINT uq_program_year UNIQUE(program_id, year)
);

CREATE INDEX idx_batches_program ON batches(program_id);
CREATE INDEX idx_batches_year ON batches(year);
CREATE INDEX idx_batches_is_active ON batches(is_active);
```

#### class_sections
```sql
CREATE TABLE class_sections (
    id BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES batches(id),
    year_of_study INT NOT NULL CHECK (year_of_study > 0),  -- 1, 2, 3, 4
    section VARCHAR(10) NOT NULL,  -- A, B, C
    name VARCHAR(100) NOT NULL,  -- CSE-3A, CSE-3B
    capacity INT CHECK (capacity > 0),
    class_coordinator_id BIGINT REFERENCES teachers(id),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_batch_year_section UNIQUE(batch_id, year_of_study, section)
);

CREATE INDEX idx_class_sections_batch ON class_sections(batch_id);
CREATE INDEX idx_class_sections_year ON class_sections(year_of_study);
CREATE INDEX idx_class_sections_is_active ON class_sections(is_active);
```

#### subjects
```sql
CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    credits INT CHECK (credits > 0),
    subject_type VARCHAR(50) CHECK (subject_type IN ('THEORY', 'PRACTICAL', 'PROJECT', 'ELECTIVE')),
    department_id BIGINT REFERENCES departments(id),
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_subjects_code ON subjects(code);
CREATE INDEX idx_subjects_department ON subjects(department_id);
CREATE INDEX idx_subjects_is_active ON subjects(is_active);
```

#### academic_years
```sql
CREATE TABLE academic_years (
    id BIGSERIAL PRIMARY KEY,
    year VARCHAR(20) NOT NULL UNIQUE,  -- 2024-2025
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_academic_year_dates CHECK (end_date > start_date)
);

CREATE INDEX idx_academic_years_is_current ON academic_years(is_current);
```

#### semesters
```sql
CREATE TABLE semesters (
    id BIGSERIAL PRIMARY KEY,
    academic_year_id BIGINT NOT NULL REFERENCES academic_years(id),
    semester_number INT NOT NULL CHECK (semester_number IN (1, 2)),  -- 1=Odd, 2=Even
    name VARCHAR(50) NOT NULL,  -- Fall 2024, Spring 2025
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_semester_dates CHECK (end_date > start_date),
    CONSTRAINT uq_academic_year_semester UNIQUE(academic_year_id, semester_number)
);

CREATE INDEX idx_semesters_academic_year ON semesters(academic_year_id);
CREATE INDEX idx_semesters_is_current ON semesters(is_current);
```

---

### 3.3 Enrollment & Assignment

#### enrollments
```sql
CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    class_section_id BIGINT NOT NULL REFERENCES class_sections(id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    semester_id BIGINT NOT NULL REFERENCES semesters(id),
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DROPPED', 'COMPLETED')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_enrollment UNIQUE(student_id, subject_id, semester_id)
);

CREATE INDEX idx_enrollments_student ON enrollments(student_id);
CREATE INDEX idx_enrollments_class ON enrollments(class_section_id);
CREATE INDEX idx_enrollments_subject ON enrollments(subject_id);
CREATE INDEX idx_enrollments_semester ON enrollments(semester_id);
CREATE INDEX idx_enrollments_status ON enrollments(status);
```

#### teacher_assignments
```sql
CREATE TABLE teacher_assignments (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    class_section_id BIGINT NOT NULL REFERENCES class_sections(id),
    semester_id BIGINT NOT NULL REFERENCES semesters(id),
    assigned_date DATE NOT NULL DEFAULT CURRENT_DATE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_teacher_assignment UNIQUE(teacher_id, subject_id, class_section_id, semester_id)
);

CREATE INDEX idx_teacher_assignments_teacher ON teacher_assignments(teacher_id);
CREATE INDEX idx_teacher_assignments_subject ON teacher_assignments(subject_id);
CREATE INDEX idx_teacher_assignments_class ON teacher_assignments(class_section_id);
CREATE INDEX idx_teacher_assignments_semester ON teacher_assignments(semester_id);
CREATE INDEX idx_teacher_assignments_active ON teacher_assignments(is_active);
```

---

### 3.4 Timetable

#### timetable_slots
```sql
CREATE TABLE timetable_slots (
    id BIGSERIAL PRIMARY KEY,
    class_section_id BIGINT NOT NULL REFERENCES class_sections(id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    semester_id BIGINT NOT NULL REFERENCES semesters(id),
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),  -- 1=Monday, 7=Sunday
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room_number VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_time_valid CHECK (end_time > start_time),
    CONSTRAINT uq_class_time UNIQUE(class_section_id, day_of_week, start_time, semester_id),
    CONSTRAINT uq_teacher_time UNIQUE(teacher_id, day_of_week, start_time, semester_id),
    CONSTRAINT uq_room_time UNIQUE(room_number, day_of_week, start_time, semester_id)
);

CREATE INDEX idx_timetable_class ON timetable_slots(class_section_id);
CREATE INDEX idx_timetable_teacher ON timetable_slots(teacher_id);
CREATE INDEX idx_timetable_subject ON timetable_slots(subject_id);
CREATE INDEX idx_timetable_day ON timetable_slots(day_of_week);
CREATE INDEX idx_timetable_semester ON timetable_slots(semester_id);
```

---

### 3.5 Attendance

#### lecture_sessions
```sql
CREATE TABLE lecture_sessions (
    id BIGSERIAL PRIMARY KEY,
    timetable_slot_id BIGINT REFERENCES timetable_slots(id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    class_section_id BIGINT NOT NULL REFERENCES class_sections(id),
    teacher_id BIGINT NOT NULL REFERENCES teachers(id),
    semester_id BIGINT NOT NULL REFERENCES semesters(id),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    room_number VARCHAR(50),
    status VARCHAR(20) DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    attendance_method VARCHAR(20) CHECK (attendance_method IN ('CAMERA', 'MANUAL', 'HYBRID')),
    total_students INT,
    present_count INT DEFAULT 0,
    absent_count INT DEFAULT 0,
    on_leave_count INT DEFAULT 0,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_lecture_time CHECK (end_time > start_time),
    CONSTRAINT chk_counts_valid CHECK (
        (total_students IS NULL) OR 
        (present_count + absent_count + on_leave_count <= total_students)
    )
);

CREATE INDEX idx_lecture_sessions_class ON lecture_sessions(class_section_id);
CREATE INDEX idx_lecture_sessions_subject ON lecture_sessions(subject_id);
CREATE INDEX idx_lecture_sessions_teacher ON lecture_sessions(teacher_id);
CREATE INDEX idx_lecture_sessions_date ON lecture_sessions(date);
CREATE INDEX idx_lecture_sessions_semester ON lecture_sessions(semester_id);
CREATE INDEX idx_lecture_sessions_status ON lecture_sessions(status);
CREATE INDEX idx_lecture_sessions_class_date ON lecture_sessions(class_section_id, date);
```

#### attendance_records
```sql
CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    lecture_session_id BIGINT NOT NULL REFERENCES lecture_sessions(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'ON_LEAVE', 'LATE')),
    attendance_method VARCHAR(20) CHECK (attendance_method IN ('CAMERA', 'MANUAL', 'CORRECTION')),
    confidence_score DECIMAL(5,4) CHECK (confidence_score BETWEEN 0 AND 1),  -- For camera attendance
    marked_by_teacher_id BIGINT REFERENCES teachers(id),
    marked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    remarks TEXT,
    is_corrected BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_attendance UNIQUE(lecture_session_id, student_id)
);

CREATE INDEX idx_attendance_student ON attendance_records(student_id);
CREATE INDEX idx_attendance_lecture ON attendance_records(lecture_session_id);
CREATE INDEX idx_attendance_status ON attendance_records(status);
CREATE INDEX idx_attendance_date ON attendance_records(marked_at);
CREATE INDEX idx_attendance_student_date ON attendance_records(student_id, marked_at);
CREATE INDEX idx_attendance_corrected ON attendance_records(is_corrected);
```

#### attendance_corrections
```sql
CREATE TABLE attendance_corrections (
    id BIGSERIAL PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL REFERENCES attendance_records(id) ON DELETE CASCADE,
    previous_status VARCHAR(20) NOT NULL,
    new_status VARCHAR(20) NOT NULL,
    reason TEXT NOT NULL,
    corrected_by_user_id BIGINT NOT NULL REFERENCES users(id),
    corrected_by_role VARCHAR(20) NOT NULL,  -- TEACHER, ADMIN
    related_query_id BIGINT REFERENCES attendance_queries(id),
    corrected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_corrections_record ON attendance_corrections(attendance_record_id);
CREATE INDEX idx_corrections_user ON attendance_corrections(corrected_by_user_id);
CREATE INDEX idx_corrections_date ON attendance_corrections(corrected_at);
```

---

### 3.6 Face Recognition

#### face_profiles
```sql
CREATE TABLE face_profiles (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL UNIQUE REFERENCES students(id) ON DELETE CASCADE,
    enrollment_date DATE NOT NULL DEFAULT CURRENT_DATE,
    total_images INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'EXPIRED')),
    last_updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_face_profiles_student ON face_profiles(student_id);
CREATE INDEX idx_face_profiles_status ON face_profiles(status);
```

#### face_images
```sql
CREATE TABLE face_images (
    id BIGSERIAL PRIMARY KEY,
    face_profile_id BIGINT NOT NULL REFERENCES face_profiles(id) ON DELETE CASCADE,
    storage_provider VARCHAR(50) NOT NULL,  -- S3, MINIO, etc.
    storage_bucket VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,  -- Object key/path
    file_size_bytes BIGINT,
    mime_type VARCHAR(50),
    width INT,
    height INT,
    quality_score DECIMAL(5,4),  -- Face quality assessment
    is_primary BOOLEAN DEFAULT false,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_storage_reference UNIQUE(storage_provider, storage_bucket, storage_key)
);

CREATE INDEX idx_face_images_profile ON face_images(face_profile_id);
CREATE INDEX idx_face_images_primary ON face_images(is_primary);
```

#### face_embeddings
```sql
CREATE TABLE face_embeddings (
    id BIGSERIAL PRIMARY KEY,
    face_profile_id BIGINT NOT NULL REFERENCES face_profiles(id) ON DELETE CASCADE,
    face_image_id BIGINT REFERENCES face_images(id) ON DELETE SET NULL,
    embedding_vector VECTOR(512),  -- pgvector extension, size depends on model
    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    embedding_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_image_embedding UNIQUE(face_image_id, model_name, model_version)
);

CREATE INDEX idx_face_embeddings_profile ON face_embeddings(face_profile_id);
-- Vector similarity index (requires pgvector extension)
CREATE INDEX idx_face_embeddings_vector ON face_embeddings 
    USING ivfflat (embedding_vector vector_cosine_ops) 
    WITH (lists = 100);
```

---

### 3.7 Leave Management

#### leave_requests
```sql
CREATE TABLE leave_requests (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason TEXT NOT NULL,
    leave_type VARCHAR(50) CHECK (leave_type IN ('MEDICAL', 'PERSONAL', 'EMERGENCY', 'OTHER')),
    attachment_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    approved_by_user_id BIGINT REFERENCES users(id),
    approval_date TIMESTAMP,
    approval_comments TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_approval_complete CHECK (
        (status IN ('APPROVED', 'REJECTED') AND approved_by_user_id IS NOT NULL) OR
        (status IN ('PENDING', 'CANCELLED'))
    )
);

CREATE INDEX idx_leave_requests_student ON leave_requests(student_id);
CREATE INDEX idx_leave_requests_status ON leave_requests(status);
CREATE INDEX idx_leave_requests_dates ON leave_requests(start_date, end_date);
CREATE INDEX idx_leave_requests_approver ON leave_requests(approved_by_user_id);
```

#### leave_request_subjects
```sql
CREATE TABLE leave_request_subjects (
    id BIGSERIAL PRIMARY KEY,
    leave_request_id BIGINT NOT NULL REFERENCES leave_requests(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    lecture_session_id BIGINT REFERENCES lecture_sessions(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_leave_subject UNIQUE(leave_request_id, subject_id, lecture_session_id)
);

CREATE INDEX idx_leave_subjects_request ON leave_request_subjects(leave_request_id);
CREATE INDEX idx_leave_subjects_subject ON leave_request_subjects(subject_id);
CREATE INDEX idx_leave_subjects_lecture ON leave_request_subjects(lecture_session_id);
```

---

### 3.8 Attendance Queries

#### attendance_queries
```sql
CREATE TABLE attendance_queries (
    id BIGSERIAL PRIMARY KEY,
    attendance_record_id BIGINT NOT NULL REFERENCES attendance_records(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    current_status VARCHAR(20) NOT NULL,
    requested_status VARCHAR(20) NOT NULL,
    reason TEXT NOT NULL,
    evidence_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by_user_id BIGINT REFERENCES users(id),
    review_date TIMESTAMP,
    review_comments TEXT,
    raised_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_query_review CHECK (
        (status IN ('APPROVED', 'REJECTED') AND reviewed_by_user_id IS NOT NULL) OR
        (status = 'PENDING')
    ),
    CONSTRAINT uq_active_query UNIQUE(attendance_record_id, student_id, status) 
        WHERE (status = 'PENDING')
);

CREATE INDEX idx_queries_student ON attendance_queries(student_id);
CREATE INDEX idx_queries_attendance ON attendance_queries(attendance_record_id);
CREATE INDEX idx_queries_status ON attendance_queries(status);
CREATE INDEX idx_queries_reviewer ON attendance_queries(reviewed_by_user_id);
CREATE INDEX idx_queries_raised_at ON attendance_queries(raised_at);
```

---

### 3.9 Notifications

#### notifications
```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL CHECK (notification_type IN (
        'ATTENDANCE_MARKED', 'LEAVE_APPROVED', 'LEAVE_REJECTED', 
        'QUERY_APPROVED', 'QUERY_REJECTED', 'LOW_ATTENDANCE', 
        'LECTURE_SCHEDULED', 'LECTURE_CANCELLED', 'SYSTEM'
    )),
    related_entity_type VARCHAR(50),  -- lecture_session, leave_request, attendance_query
    related_entity_id BIGINT,
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMP,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_type ON notifications(notification_type);
CREATE INDEX idx_notifications_sent_at ON notifications(sent_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) 
    WHERE is_read = false;
```

---

### 3.10 Audit Logging

#### audit_logs
```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    action VARCHAR(100) NOT NULL,  -- LOGIN, LOGOUT, CREATE_ATTENDANCE, UPDATE_ATTENDANCE, etc.
    entity_type VARCHAR(50),
    entity_id BIGINT,
    old_values JSONB,
    new_values JSONB,
    ip_address INET,
    user_agent VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Immutable table (no updates or deletes)
    CHECK (false) NO INHERIT
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp DESC);

-- Prevent updates and deletes
CREATE RULE no_update AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE no_delete AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
```

---

## 4. Database Views

### 4.1 Student Attendance Summary View

```sql
CREATE OR REPLACE VIEW v_student_attendance_summary AS
SELECT 
    s.id AS student_id,
    s.roll_number,
    s.first_name || ' ' || s.last_name AS student_name,
    subj.id AS subject_id,
    subj.code AS subject_code,
    subj.name AS subject_name,
    sem.id AS semester_id,
    sem.name AS semester_name,
    COUNT(ar.id) AS total_lectures,
    COUNT(CASE WHEN ar.status = 'PRESENT' THEN 1 END) AS present_count,
    COUNT(CASE WHEN ar.status = 'ABSENT' THEN 1 END) AS absent_count,
    COUNT(CASE WHEN ar.status = 'ON_LEAVE' THEN 1 END) AS on_leave_count,
    COUNT(CASE WHEN ar.status = 'LATE' THEN 1 END) AS late_count,
    ROUND(
        (COUNT(CASE WHEN ar.status = 'PRESENT' THEN 1 END)::NUMERIC / 
        NULLIF(COUNT(ar.id), 0)) * 100, 
        2
    ) AS attendance_percentage
FROM students s
INNER JOIN enrollments e ON s.id = e.student_id
INNER JOIN subjects subj ON e.subject_id = subj.id
INNER JOIN semesters sem ON e.semester_id = sem.id
LEFT JOIN attendance_records ar ON ar.student_id = s.id
LEFT JOIN lecture_sessions ls ON ar.lecture_session_id = ls.id AND ls.subject_id = subj.id
WHERE e.status = 'ACTIVE'
GROUP BY s.id, s.roll_number, student_name, subj.id, subj.code, subj.name, sem.id, sem.name;
```

### 4.2 Teacher Schedule View

```sql
CREATE OR REPLACE VIEW v_teacher_schedule AS
SELECT 
    t.id AS teacher_id,
    t.employee_id,
    t.first_name || ' ' || t.last_name AS teacher_name,
    ts.id AS timetable_slot_id,
    ts.day_of_week,
    ts.start_time,
    ts.end_time,
    ts.room_number,
    subj.id AS subject_id,
    subj.code AS subject_code,
    subj.name AS subject_name,
    cs.id AS class_id,
    cs.name AS class_name,
    sem.id AS semester_id,
    sem.name AS semester_name
FROM teachers t
INNER JOIN teacher_assignments ta ON t.id = ta.teacher_id
INNER JOIN timetable_slots ts ON ta.teacher_id = ts.teacher_id 
    AND ta.subject_id = ts.subject_id 
    AND ta.class_section_id = ts.class_section_id
INNER JOIN subjects subj ON ts.subject_id = subj.id
INNER JOIN class_sections cs ON ts.class_section_id = cs.id
INNER JOIN semesters sem ON ts.semester_id = sem.id
WHERE ta.is_active = true AND ts.is_active = true;
```

### 4.3 Low Attendance Students View

```sql
CREATE OR REPLACE VIEW v_low_attendance_students AS
SELECT 
    s.id AS student_id,
    s.roll_number,
    s.first_name || ' ' || s.last_name AS student_name,
    subj.id AS subject_id,
    subj.code AS subject_code,
    subj.name AS subject_name,
    vsa.attendance_percentage,
    CASE 
        WHEN vsa.attendance_percentage >= 75 THEN 'SAFE'
        WHEN vsa.attendance_percentage >= 65 THEN 'WARNING'
        ELSE 'CRITICAL'
    END AS attendance_status
FROM students s
INNER JOIN v_student_attendance_summary vsa ON s.id = vsa.student_id
INNER JOIN subjects subj ON vsa.subject_id = subj.id
WHERE vsa.attendance_percentage < 75
ORDER BY vsa.attendance_percentage ASC;
```

---

## 5. Database Functions & Triggers

### 5.1 Updated At Trigger

```sql
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables with updated_at column
CREATE TRIGGER trigger_update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Repeat for all relevant tables
```

### 5.2 Attendance Statistics Update Function

```sql
CREATE OR REPLACE FUNCTION update_lecture_session_stats()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE lecture_sessions
    SET 
        present_count = (
            SELECT COUNT(*) FROM attendance_records 
            WHERE lecture_session_id = NEW.lecture_session_id AND status = 'PRESENT'
        ),
        absent_count = (
            SELECT COUNT(*) FROM attendance_records 
            WHERE lecture_session_id = NEW.lecture_session_id AND status = 'ABSENT'
        ),
        on_leave_count = (
            SELECT COUNT(*) FROM attendance_records 
            WHERE lecture_session_id = NEW.lecture_session_id AND status = 'ON_LEAVE'
        ),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.lecture_session_id;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_lecture_stats
    AFTER INSERT OR UPDATE ON attendance_records
    FOR EACH ROW
    EXECUTE FUNCTION update_lecture_session_stats();
```

### 5.3 Prevent Duplicate Active Leave Request

```sql
CREATE OR REPLACE FUNCTION check_duplicate_leave_request()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM leave_requests
        WHERE student_id = NEW.student_id
        AND status = 'PENDING'
        AND (
            (NEW.start_date BETWEEN start_date AND end_date) OR
            (NEW.end_date BETWEEN start_date AND end_date) OR
            (start_date BETWEEN NEW.start_date AND NEW.end_date)
        )
    ) THEN
        RAISE EXCEPTION 'Overlapping leave request already exists';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_check_duplicate_leave
    BEFORE INSERT ON leave_requests
    FOR EACH ROW
    EXECUTE FUNCTION check_duplicate_leave_request();
```

---

## 6. Database Optimization

### 6.1 Partitioning Strategy

**Attendance Records Partitioning (Future):**

```sql
-- Partition by academic year for historical data management
CREATE TABLE attendance_records_2024 PARTITION OF attendance_records
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
    
CREATE TABLE attendance_records_2025 PARTITION OF attendance_records
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
```

### 6.2 Caching Strategy

**Redis Cache Keys:**
- `timetable:teacher:{teacher_id}:{date}` - Teacher's daily schedule
- `enrollment:class:{class_id}:subject:{subject_id}` - Enrolled students
- `face:embeddings:class:{class_id}` - Face embeddings for class
- `session:lecture:{lecture_id}` - Active lecture session state

---

## 7. Data Integrity Rules

### 7.1 Business Rules Enforced by Database

1. **Unique Attendance**: One attendance record per student per lecture
2. **Enrollment Required**: Cannot take attendance for non-enrolled students
3. **Teacher Authorization**: Only assigned teacher can mark attendance
4. **Leave Overlap**: No overlapping pending leave requests
5. **Query Uniqueness**: One pending query per attendance record per student
6. **Time Constraints**: End time must be after start time
7. **Date Validity**: Future dates validated where appropriate
8. **Cascading Deletes**: Proper cascade rules to maintain referential integrity

### 7.2 Data Validation

- Email format validation
- Date range validation
- Status enum validation
- Confidence score range (0-1)
- Phone number format (optional, application-level)

---

## 8. Migration Strategy

### 8.1 Flyway Migration Structure

```
db/
├── migration/
│   ├── V1__create_user_tables.sql
│   ├── V2__create_academic_tables.sql
│   ├── V3__create_enrollment_tables.sql
│   ├── V4__create_timetable_tables.sql
│   ├── V5__create_attendance_tables.sql
│   ├── V6__create_face_recognition_tables.sql
│   ├── V7__create_leave_tables.sql
│   ├── V8__create_query_tables.sql
│   ├── V9__create_notification_tables.sql
│   ├── V10__create_audit_tables.sql
│   ├── V11__create_indexes.sql
│   ├── V12__create_views.sql
│   ├── V13__create_functions_triggers.sql
│   └── V14__insert_initial_data.sql
```

### 8.2 Rollback Strategy

- Each migration must be reversible where possible
- Separate rollback scripts for critical changes
- Test migrations on staging before production
- Database backups before each migration

---

## 9. Performance Considerations

### 9.1 Query Optimization

- Use prepared statements
- Avoid N+1 queries
- Use batch inserts for attendance records
- Leverage materialized views for complex aggregations (if needed)
- Index foreign keys and frequently filtered columns

### 9.2 Connection Pooling

```properties
# HikariCP configuration
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

---

## 10. Backup & Recovery

### 10.1 Backup Schedule

- **Full Backup**: Daily at 2:00 AM
- **Incremental Backup**: Every 6 hours
- **WAL Archiving**: Continuous
- **Retention**: 30 days

### 10.2 Recovery Procedures

```bash
# Point-in-time recovery
pg_restore -d attendance_db -t "2026-08-25 10:00:00" backup_file.dump

# Full database restore
pg_restore -d attendance_db -c backup_file.dump
```

---

## 11. Security

### 11.1 Database User Roles

- **app_user**: Application connection (CRUD on application tables)
- **readonly_user**: Read-only access for reporting
- **admin_user**: Full access for DBA tasks
- **backup_user**: Backup and restore only

### 11.2 Row-Level Security (Future Enhancement)

```sql
-- Example: Students can only see their own data
CREATE POLICY student_isolation ON attendance_records
    FOR SELECT
    TO app_user
    USING (student_id = current_setting('app.current_user_id')::BIGINT);
```

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | Database Architect | Initial draft |

---

**Status**: DRAFT - Awaiting review
