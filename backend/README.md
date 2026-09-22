# AFORA Backend

Spring Boot backend for Smart Classroom Attendance Management System.

## Phase A1 Status

**Foundation complete.** Backend compiles and runs with:
- ✅ Spring Boot 3.2.0 with Kotlin
- ✅ PostgreSQL + JPA + Flyway
- ✅ Security configuration (permits all for now)
- ✅ CORS configuration
- ✅ Global exception handling
- ✅ Standard API response format
- ✅ Health check endpoint
- ✅ Modular structure (auth, user modules)

**Not implemented yet:**
- JWT authentication (Phase A2)
- User management (Phase A2)
- Attendance features (Phase B)
- Face recognition integration (Phase C)

## Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Language**: Kotlin 1.9.10
- **Database**: PostgreSQL 15+
- **Migrations**: Flyway
- **Security**: Spring Security + JWT (future)
- **Build**: Gradle 8.4

## Getting Started

### Prerequisites

- JDK 17+
- PostgreSQL 15+
- Gradle 8.4+ (or use wrapper)

### Database Setup

```sql
CREATE DATABASE afora_dev;
CREATE USER afora_user WITH PASSWORD 'afora_pass';
GRANT ALL PRIVILEGES ON DATABASE afora_dev TO afora_user;
```

### Run

```bash
# Development
./gradlew bootRun

# Build
./gradlew build

# Test
./gradlew test
```

### Verify

```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "afora-backend",
    "version": "1.0.0-A1",
    "timestamp": "2024-..."
  },
  "timestamp": "2024-..."
}
```

## Project Structure

```
backend/
├── src/main/kotlin/com/academia/backend/
│   ├── AforaBackendApplication.kt
│   ├── common/              # Shared utilities
│   │   ├── config/          # WebConfig, SecurityConfig
│   │   ├── dto/             # ApiResponse, PagedResponse
│   │   ├── exception/       # GlobalExceptionHandler
│   │   └── controller/      # HealthController
│   ├── auth/                # Authentication module (Phase A2)
│   └── user/                # User management module (Phase A2)
└── src/main/resources/
    ├── application.yml      # Main config
    ├── application-dev.yml  # Dev profile
    ├── application-prod.yml # Prod profile
    └── db/migration/        # Flyway migrations (Phase A2)
```

## Configuration

Environment variables:
- `DB_URL`: Database connection string
- `DB_USERNAME`: Database user
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: JWT signing secret (future)
- `CORS_ORIGINS`: Allowed CORS origins

## API Documentation

Phase A1 endpoints:
- `GET /api/health` - Health check

Future endpoints (Phase A2+):
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh token
- `GET /api/users/me` - Current user profile

## Development

### Code Style

- Kotlin coding conventions
- Package-by-feature structure
- Repository pattern for data access
- Service layer for business logic
- DTOs for API contracts

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests "HealthControllerTest"
```

## License

Proprietary - AFORA Team
