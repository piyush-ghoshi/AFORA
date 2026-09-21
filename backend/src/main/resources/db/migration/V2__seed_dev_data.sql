-- AFORA Database - Phase A1 Development Seed Data
-- Institutions and academic structure only (user seeds moved to V4)

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
