# AFORA - Smart Classroom Attendance Management System

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin)](https://kotlinlang.org/)
[![KMP](https://img.shields.io/badge/Architecture-KMP-7F52FF)](https://kotlinlang.org/docs/multiplatform.html)
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)

**Version:** 1.0.0  
**Status:** Phase A1 - Foundation Development  
**Date:** August 25, 2026

---

## Overview

AFORA is a production-grade Smart Classroom Attendance Management System built with:
- **Android-first** Kotlin Multiplatform (KMP) architecture
- **AI-assisted face recognition** with teacher validation
- **Spring Boot** modular backend
- **PostgreSQL** database with pgvector for face embeddings

### Key Features

- 🎥 Multi-frame camera-based attendance scanning
- 👨‍🏫 Teacher review and validation workflow
- 📱 Android-first with iOS-ready architecture
- 🔒 Role-based access control (Student, Teacher, Admin)
- 📊 Real-time attendance analytics
- 🏥 Leave management with approval workflow
- ❓ Attendance query/dispute resolution
- 🏆 Attendance leaderboards

---

## Architecture

```
┌─────────────────────────────────────────┐
│         Android App (Compose)           │
│     ┌───────────────────────┐           │
│     │  Shared KMP Module    │           │
│     │  (Business Logic)     │           │
│     └───────────────────────┘           │
└─────────────────┬───────────────────────┘
                  │ REST API
┌─────────────────▼───────────────────────┐
│       Spring Boot Backend               │
│  ┌──────────┬──────────┬──────────┐    │
│  │  Auth    │Academic  │Attendance│    │
│  │  Module  │ Module   │  Module  │    │
│  └──────────┴──────────┴──────────┘    │
└─────────────────┬───────────────────────┘
                  │
     ┌────────────┴────────────┐
     │                         │
┌────▼─────┐            ┌─────▼──────┐
│PostgreSQL│            │   Redis    │
│ + pgvector│            │  (Cache)   │
└──────────┘            └────────────┘
```

---

## Project Structure

```
afora/
├── shared/              # Kotlin Multiplatform (60-70% shared code)
│   ├── commonMain/      # Platform-independent business logic
│   ├── androidMain/     # Android-specific implementations
│   └── iosMain/         # iOS-specific (future)
├── androidApp/          # Android application (Jetpack Compose)
├── backend/             # Spring Boot backend (Java 17)
├── docs/                # Architecture & design documentation
└── uidesign/            # UI/UX design specifications
```

---

## Technology Stack

### Client (Android)
- **Language:** Kotlin
- **Architecture:** Kotlin Multiplatform (KMP)
- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt (Android) + Koin (shared)
- **Networking:** Ktor Client
- **Camera:** CameraX
- **ML:** TensorFlow Lite / ML Kit
- **State:** ViewModel + StateFlow

### Backend
- **Language:** Java 17
- **Framework:** Spring Boot 3.x
- **Database:** PostgreSQL 14+ with pgvector
- **Cache:** Redis
- **Migration:** Flyway
- **Security:** Spring Security + JWT
- **Storage:** S3-compatible object storage
- **API Docs:** OpenAPI 3.0 (Springdoc)

### DevOps
- **VCS:** Git
- **CI/CD:** GitHub Actions
- **Build:** Gradle (KGP) / Gradle (Java)

---

## Getting Started

### Prerequisites

- **JDK:** 17 or higher
- **Android Studio:** Hedgehog (2023.1.1) or newer
- **Gradle:** 8.1+ (via wrapper)
- **PostgreSQL:** 14+ with pgvector extension
- **Redis:** 7.0+

### Build Instructions

#### Clone Repository
```bash
git clone <repository-url>
cd afora
```

#### Build Shared KMP Module
```bash
./gradlew :shared:build
```

#### Build Android App
```bash
./gradlew :androidApp:assembleDebug
```

#### Build Backend
```bash
cd backend
./gradlew build
```

#### Run Backend
```bash
cd backend
./gradlew bootRun
```

### Configuration

#### Backend Configuration
Create `backend/src/main/resources/application-dev.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/afora_db
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
```

#### Android Configuration
Update base URL in shared module or via build config.

---

## Development Phases

- ✅ **Phase A0:** Documentation & Architecture Revision (Complete)
- 🚧 **Phase A1:** KMP Project Foundation (In Progress)
- ⏳ **Phase A2:** Authentication & User Management
- ⏳ **Phase A3:** Academic Structure Management
- ⏳ **Phase A4:** Attendance Engine Core
- ⏳ **Phase A5:** Face Recognition Benchmarking
- ⏳ **Phase A6:** Camera Attendance Implementation
- ⏳ **Phase A7:** Leave & Query Management
- ⏳ **Phase A8:** Analytics & Leaderboard
- ⏳ **Phase A9:** Notifications & Polish

---

## Key Architectural Principles

### ✅ DO
- Use configurable policy objects (not hard-coded thresholds)
- Maintain clean boundaries (CV ≠ Business ≠ Platform)
- Treat face recognition as candidate generator (teacher validates)
- Use explicit state machines for attendance sessions
- Implement idempotency for critical operations
- Provide manual fallback for all automated features

### ❌ DON'T
- Hard-code attendance percentages (75%, etc.)
- Hard-code face recognition thresholds (0.85, etc.)
- Let CV directly finalize attendance
- Skip Phase 5 benchmarking before model selection
- Block development on every institutional policy
- Mix platform-specific code in shared business logic

---

## Testing

### Run Unit Tests
```bash
# Shared module tests
./gradlew :shared:testDebugUnitTest

# Android tests
./gradlew :androidApp:testDebugUnitTest

# Backend tests
cd backend && ./gradlew test
```

### Run Android Instrumented Tests
```bash
./gradlew :androidApp:connectedDebugAndroidTest
```

---

## Documentation

- [Requirements](docs/requirements.md)
- [Architecture](docs/architecture.md)
- [KMP Architecture](docs/kmp-architecture.md)
- [Attendance Engine](docs/attendance-engine.md)
- [Database Design](docs/database.md)
- [Face Recognition](docs/face-recognition.md)
- [Security & Threat Model](docs/threat-model.md)
- [Decision Log](docs/decision-log.md)
- [Project Structure](docs/PROJECT_STRUCTURE.md)
- [Benchmark Plan](docs/benchmark-plan.md)

---

## Contributing

This is a production-oriented project. Please follow these guidelines:

1. Read architecture documentation before contributing
2. Follow Kotlin coding conventions
3. Write tests for new features
4. Update documentation for architectural changes
5. Ensure CI passes before submitting PRs

---

## Security

- **Authentication:** JWT-based with refresh tokens
- **Authorization:** Role-based access control (RBAC)
- **Biometric Data:** Encrypted at rest and in transit
- **Audit Trail:** Immutable logs for critical operations
- **API Security:** Rate limiting, input validation

See [Security Documentation](docs/security.md) and [Threat Model](docs/threat-model.md).

---

## License

[To be determined]

---

## Contact

**Project:** AFORA - Smart Classroom Attendance Management System  
**Documentation Date:** August 25, 2026  
**Phase:** A1 - Foundation Development

For questions or support, refer to the documentation in the `docs/` directory.
