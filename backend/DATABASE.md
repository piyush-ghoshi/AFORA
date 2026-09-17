# AFORA Database Documentation

## Overview

PostgreSQL database for AFORA attendance management system.

## Phase A1 Schema

Foundation tables only. Full schema will be expanded in Phase A2+.

### Tables

#### users
Core authentication table.

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| username | VARCHAR(100) | Unique username |
| email | VARCHAR(255) | Unique email |
| password_hash | VARCHAR(255) | BCrypt hashed password |
| role | VARCHAR(50) | STUDENT, TEACHER, ADMIN, SUPER_ADMIN |
| is_active | BOOLEAN | Account status |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |
| last_login_at | TIMESTAMP | Last login timestamp |

#### institutions
Multi-tenant support for educational institutions.

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| name | VARCHAR(255) | Institution name |
| code | VARCHAR(50) | Unique institution code |
| type | VARCHAR(50) | UNIVERSITY, COLLEGE, SCHOOL |
| address | TEXT | Full address |
| city | VARCHAR(100) | City |
| state | VARCHAR(100) | State/Province |
| country | VARCHAR(100) | Country (default: India) |
| timezone | VARCHAR(50) | Timezone (default: Asia/Kolkata) |
| is_active | BOOLEAN | Institution status |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

#### academic_years
Academic years per institution.

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| institution_id | UUID | FK to institutions |
| name | VARCHAR(100) | Year name (e.g., "2024-2025") |
| start_date | DATE | Start date |
| end_date | DATE | End date |
| is_current | BOOLEAN | Current academic year flag |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

#### semesters
Semesters within academic years.

| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| academic_year_id | UUID | FK to academic_years |
| name | VARCHAR(100) | Semester name |
| semester_number | INTEGER | Semester number (1, 2, etc.) |
| start_date | DATE | Start date |
| end_date | DATE | End date |
| is_current | BOOLEAN | Current semester flag |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

## Future Tables (Phase A2+)

Will be added in later migrations:
- `students` - Student profiles
- `teachers` - Teacher profiles  
- `departments` - Academic departments
- `courses` - Course catalog
- `class_sections` - Class sections
- `enrollments` - Student enrollments
- `subjects` - Subjects/courses
- `timetable_slots` - Timetable scheduling
- `lecture_sessions` - Individual lecture instances
- `attendance_records` - Attendance data
- `face_profiles` - Face recognition profiles
- `face_embeddings` - Face embedding vectors
- `leave_requests` - Student leave applications
- `attendance_queries` - Attendance dispute queries
- `notifications` - System notifications

## Setup

### Prerequisites

- PostgreSQL 15+
- psql client

### Development Setup

```bash
# Run setup script
psql -U postgres -f db-setup.sql

# Verify
psql -U afora_user -d afora_dev -c "\dt"
```

### Connection String

Development:
```
jdbc:postgresql://localhost:5432/afora_dev
Username: afora_user
Password: afora_pass
```

Production:
```
Use environment variables:
- DB_URL
- DB_USERNAME  
- DB_PASSWORD
```

## Flyway Migrations

Migrations are in `src/main/resources/db/migration/`.

### Naming Convention

```
V<VERSION>__<description>.sql
```

Examples:
- `V1__init_schema.sql` - Initial schema
- `V2__seed_dev_data.sql` - Dev seed data
- `V3__add_students_table.sql` - Add students (Phase A2)

### Running Migrations

Spring Boot runs Flyway automatically on startup.

Manual run:
```bash
./gradlew flywayMigrate
```

### Migration Commands

```bash
# Check migration status
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate

# Clean database (WARNING: destroys all data)
./gradlew flywayClean
```

## Seed Data (Development Only)

V2 migration includes test data:

**Admin User:**
- Username: `admin`
- Email: `admin@test.com`
- Password: `password123`
- Role: ADMIN

**Teacher User:**
- Username: `teacher1`
- Email: `teacher1@test.com`
- Password: `password123`
- Role: TEACHER

**Student User:**
- Username: `student1`
- Email: `student1@test.com`
- Password: `password123`
- Role: STUDENT

**⚠️ WARNING:** These are test credentials. Never use in production!

## Indexes

Indexes are created for:
- Username/email lookups
- Role-based queries
- Active status filtering
- Foreign key relationships
- Current academic year/semester queries

## Triggers

`update_updated_at_column()` trigger automatically updates `updated_at` timestamp on every UPDATE.

## Best Practices

1. **Never modify existing migrations** - Create new ones
2. **Use transactions** - Migrations run in transactions by default
3. **Test migrations** - Test on dev before production
4. **Backup before migrate** - Always backup production DB
5. **Use UUIDs** - For better distribution and security
6. **Timestamp everything** - created_at, updated_at standard
7. **Soft deletes** - Use is_active flags instead of DELETE

## Troubleshooting

### Connection refused
```bash
# Check PostgreSQL is running
sudo systemctl status postgresql

# Start if needed
sudo systemctl start postgresql
```

### Permission denied
```bash
# Re-run db-setup.sql with proper privileges
psql -U postgres -f db-setup.sql
```

### Migration failed
```bash
# Check migration status
./gradlew flywayInfo

# Fix the issue, then repair
./gradlew flywayRepair
```

## Monitoring

Monitor database performance:
```sql
-- Active connections
SELECT * FROM pg_stat_activity WHERE datname = 'afora_dev';

-- Table sizes
SELECT
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Index usage
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

## License

Proprietary - AFORA Team
