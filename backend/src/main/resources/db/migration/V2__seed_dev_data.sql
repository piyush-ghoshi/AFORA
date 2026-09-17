-- AFORA Database - Phase A1 Development Seed Data
-- Only for development/testing - NOT for production

-- Insert test institution
INSERT INTO institutions (id, name, code, type, city, state, country, timezone) VALUES
    ('123e4567-e89b-12d3-a456-426614174000', 'Test University', 'TEST_UNIV', 'UNIVERSITY', 'Mumbai', 'Maharashtra', 'India', 'Asia/Kolkata');

-- Insert test academic year
INSERT INTO academic_years (id, institution_id, name, start_date, end_date, is_current) VALUES
    ('123e4567-e89b-12d3-a456-426614174001', '123e4567-e89b-12d3-a456-426614174000', '2024-2025', '2024-08-01', '2025-07-31', true);

-- Insert test semesters
INSERT INTO semesters (id, academic_year_id, name, semester_number, start_date, end_date, is_current) VALUES
    ('123e4567-e89b-12d3-a456-426614174002', '123e4567-e89b-12d3-a456-426614174001', 'Fall 2024', 1, '2024-08-01', '2024-12-31', true),
    ('123e4567-e89b-12d3-a456-426614174003', '123e4567-e89b-12d3-a456-426614174001', 'Spring 2025', 2, '2025-01-01', '2025-07-31', false);

-- Insert test users
-- Password: 'password123' (hashed with BCrypt)
INSERT INTO users (id, username, email, password_hash, role, is_active) VALUES
    ('123e4567-e89b-12d3-a456-426614174010', 'admin', 'admin@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', true),
    ('123e4567-e89b-12d3-a456-426614174011', 'teacher1', 'teacher1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'TEACHER', true),
    ('123e4567-e89b-12d3-a456-426614174012', 'student1', 'student1@test.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'STUDENT', true);

-- Note: These are test credentials for development only
-- Production will use proper user registration and secure password handling
