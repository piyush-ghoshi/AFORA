-- Migration V4: Seed test users for development
-- Phase A2: Test data for Firebase authentication testing

-- Note: These are placeholder users with dummy Firebase UIDs
-- In real usage, Firebase UIDs will be generated when users sign up through Firebase

-- Insert test admin user
INSERT INTO users (firebase_uid, email, first_name, last_name, role, active)
VALUES 
    ('firebase_test_admin_uid_001', 'admin@afora.edu', 'Admin', 'User', 'ADMIN', TRUE);

-- Insert test teacher users
INSERT INTO users (firebase_uid, email, first_name, last_name, role, active)
VALUES 
    ('firebase_test_teacher_uid_001', 'teacher1@afora.edu', 'John', 'Doe', 'TEACHER', TRUE),
    ('firebase_test_teacher_uid_002', 'teacher2@afora.edu', 'Jane', 'Smith', 'TEACHER', TRUE);

-- Insert test student users
INSERT INTO users (firebase_uid, email, first_name, last_name, role, active)
VALUES 
    ('firebase_test_student_uid_001', 'student1@afora.edu', 'Alice', 'Johnson', 'STUDENT', TRUE),
    ('firebase_test_student_uid_002', 'student2@afora.edu', 'Bob', 'Williams', 'STUDENT', TRUE),
    ('firebase_test_student_uid_003', 'student3@afora.edu', 'Charlie', 'Brown', 'STUDENT', TRUE);

-- Insert teacher details
INSERT INTO teachers (user_id, employee_id, department, designation, specialization)
VALUES 
    ((SELECT id FROM users WHERE email = 'teacher1@afora.edu'), 'EMP001', 'Computer Science', 'Professor', 'Machine Learning'),
    ((SELECT id FROM users WHERE email = 'teacher2@afora.edu'), 'EMP002', 'Computer Science', 'Assistant Professor', 'Database Systems');

-- Insert student details
INSERT INTO students (user_id, roll_number, department, batch, semester)
VALUES 
    ((SELECT id FROM users WHERE email = 'student1@afora.edu'), '2024CS001', 'Computer Science', '2024', '1'),
    ((SELECT id FROM users WHERE email = 'student2@afora.edu'), '2024CS002', 'Computer Science', '2024', '1'),
    ((SELECT id FROM users WHERE email = 'student3@afora.edu'), '2023CS050', 'Computer Science', '2023', '3');

-- Add comments
COMMENT ON TABLE users IS 'Test users have dummy Firebase UIDs - replace with real ones from Firebase Auth';
