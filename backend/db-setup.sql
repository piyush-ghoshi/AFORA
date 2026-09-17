-- AFORA Database Setup Script
-- Run this script to create the database and user for development

-- Connect to PostgreSQL as superuser (postgres) and run:

-- Create database
CREATE DATABASE afora_dev;

-- Create user
CREATE USER afora_user WITH PASSWORD 'afora_pass';

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE afora_dev TO afora_user;

-- Connect to afora_dev database
\c afora_dev

-- Grant schema privileges (PostgreSQL 15+)
GRANT ALL ON SCHEMA public TO afora_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO afora_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO afora_user;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO afora_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO afora_user;

-- Verify
\du afora_user
\l afora_dev
