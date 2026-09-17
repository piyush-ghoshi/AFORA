# Build Verification Guide

Instructions for verifying Phase A1 foundation builds successfully.

## Prerequisites

### System Requirements

- **JDK 17+** (required for both Android and Backend)
- **Android SDK** (for Android builds)
- **PostgreSQL 15+** (for backend database)
- **Gradle 8.4+** (wrapper included, or install globally)

### Installation

#### JDK 17
```bash
# Download from: https://adoptium.net/
# Or use sdkman:
sdk install java 17.0.8-tem
```

#### Gradle (optional, wrapper is included)
```bash
# Download from: https://gradle.org/releases/
# Or use sdkman:
sdk install gradle 8.4
```

#### PostgreSQL
```bash
# Windows: Download from https://www.postgresql.org/download/windows/
# After installation, run:
psql -U postgres -f backend/db-setup.sql
```

## Build Verification Steps

### Step 1: Initialize Gradle Wrapper

If gradle wrapper jar is missing:

```bash
# If you have gradle installed globally:
gradle wrapper --gradle-version 8.4

# This creates:
# - gradle/wrapper/gradle-wrapper.jar
# - gradle/wrapper/gradle-wrapper.properties
# - gradlew (Unix)
# - gradlew.bat (Windows)
```

### Step 2: Build Shared KMP Module

```bash
# Build shared module
./gradlew :shared:build

# Run shared tests
./gradlew :shared:testDebugUnitTest

# Expected output:
# - BUILD SUCCESSFUL
# - 2 tests passed (ResultTest, DateTimeUtilTest)
```

**Success Criteria:**
- ✅ Kotlin compiles without errors
- ✅ Common code compiles
- ✅ Android actual implementations compile
- ✅ Tests pass

### Step 3: Build Android App

```bash
# Build debug APK
./gradlew :androidApp:assembleDebug

# Expected output:
# - BUILD SUCCESSFUL
# - APK at: androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

**Success Criteria:**
- ✅ Shared module compiles
- ✅ Android app compiles
- ✅ Jetpack Compose compiles
- ✅ Hilt processes annotations
- ✅ APK generated

### Step 4: Build Backend

```bash
# Navigate to backend
cd backend

# Build backend
./gradlew build

# Expected output:
# - BUILD SUCCESSFUL
# - JAR at: backend/build/libs/backend-1.0.0.jar
# - 2 tests passed (HealthControllerTest)
```

**Success Criteria:**
- ✅ Spring Boot application compiles
- ✅ Kotlin compiles
- ✅ Tests pass (with H2 in-memory DB)
- ✅ JAR generated

### Step 5: Verify Backend with Database

Requires PostgreSQL running with `afora_dev` database.

```bash
# Start backend
cd backend
./gradlew bootRun

# In another terminal, verify health endpoint:
curl http://localhost:8080/api/health

# Expected response:
# {
#   "success": true,
#   "data": {
#     "status": "UP",
#     "service": "afora-backend",
#     "version": "1.0.0-A1",
#     "timestamp": "..."
#   },
#   "timestamp": "..."
# }
```

**Success Criteria:**
- ✅ Backend starts without errors
- ✅ Flyway migrations run successfully
- ✅ Health endpoint responds
- ✅ Database tables created

### Step 6: Verify Android App (Optional)

Requires Android device or emulator.

```bash
# Install APK
adb install androidApp/build/outputs/apk/debug/androidApp-debug.apk

# Or run directly:
./gradlew :androidApp:installDebug

# Launch app on device
# Expected: Foundation screen showing "Phase A1: Foundation Complete"
```

**Success Criteria:**
- ✅ App installs
- ✅ App launches
- ✅ Foundation screen displays
- ✅ No crashes

## Automated Verification

### Local Verification Script

Create `verify-build.sh` (Unix) or `verify-build.bat` (Windows):

```bash
#!/bin/bash
set -e

echo "=== Phase A1 Build Verification ==="

echo "Step 1: Build shared module..."
./gradlew :shared:build

echo "Step 2: Build Android app..."
./gradlew :androidApp:assembleDebug

echo "Step 3: Build backend..."
cd backend
./gradlew build
cd ..

echo "=== ✅ All builds successful! ==="
```

Run:
```bash
chmod +x verify-build.sh
./verify-build.sh
```

### CI/CD Verification

GitHub Actions will automatically verify builds on push:

- **Android CI**: Triggered on changes to `shared/` or `androidApp/`
- **Backend CI**: Triggered on changes to `backend/`
- **Full Build**: Triggered on push to `main`

Check status: https://github.com/YOUR_ORG/AFORA/actions

## Troubleshooting

### "Could not find or load main class org.gradle.wrapper.GradleWrapperMain"

**Cause:** Gradle wrapper jar is missing

**Fix:**
```bash
gradle wrapper --gradle-version 8.4
```

### "Android SDK not found"

**Cause:** ANDROID_HOME not set

**Fix:**
```bash
# Windows:
setx ANDROID_HOME "C:\Users\YourUser\AppData\Local\Android\Sdk"

# Unix:
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### "Connection to database failed"

**Cause:** PostgreSQL not running or database not created

**Fix:**
```bash
# Start PostgreSQL
# Windows: Services -> PostgreSQL -> Start
# Unix: sudo systemctl start postgresql

# Create database
psql -U postgres -f backend/db-setup.sql
```

### "Hilt annotation processor failed"

**Cause:** Kapt or Hilt configuration issue

**Fix:**
```bash
# Clean and rebuild
./gradlew clean
./gradlew :androidApp:kaptDebugKotlin
./gradlew :androidApp:assembleDebug
```

### "Flyway migration failed"

**Cause:** Database schema mismatch or migration conflict

**Fix:**
```bash
# Check migration status
cd backend
./gradlew flywayInfo

# If needed, clean and recreate (WARNING: destroys data)
./gradlew flywayClean
./gradlew flywayMigrate
```

## Phase A1 Acceptance Criteria

All criteria must pass for Phase A1 completion:

### Foundation Structure
- ✅ Root project with Gradle configuration
- ✅ Shared KMP module with Android target
- ✅ Android app module with Jetpack Compose
- ✅ Backend module with Spring Boot
- ✅ CI/CD workflows configured

### Code Quality
- ✅ All modules compile without errors
- ✅ No critical lint warnings
- ✅ Foundation tests pass
- ✅ Code follows Kotlin conventions

### Architecture
- ✅ KMP expect/actual pattern established
- ✅ Repository pattern defined
- ✅ Domain models created
- ✅ Platform abstractions defined
- ✅ Modular backend structure

### Infrastructure
- ✅ Database schema created with Flyway
- ✅ API client configured with Ktor
- ✅ DI configured (Koin for shared, Hilt for Android)
- ✅ Security foundation (Spring Security)
- ✅ Theme system (Material 3)

### Documentation
- ✅ README.md with project overview
- ✅ DATABASE.md with schema documentation
- ✅ WORKFLOWS.md with CI/CD guide
- ✅ BUILD_VERIFICATION.md (this file)

### Verification
- ✅ `./gradlew :shared:build` succeeds
- ✅ `./gradlew :androidApp:assembleDebug` succeeds
- ✅ `./gradlew backend:build` succeeds
- ✅ Backend health endpoint responds
- ✅ Android app launches

## Next Steps (Phase A2)

After verifying Phase A1 foundation:

1. **Authentication Implementation**
   - JWT token generation/validation
   - Login/logout endpoints
   - User registration
   - Role-based access control

2. **User Management**
   - Student profiles
   - Teacher profiles
   - User CRUD operations
   - Profile screens in Android app

3. **Enhanced Testing**
   - Integration tests
   - UI tests (Espresso)
   - API tests
   - Code coverage reports

4. **Deployment Preparation**
   - Docker configuration
   - Environment-specific configs
   - Secrets management
   - Deployment scripts

## License

Proprietary - AFORA Team
