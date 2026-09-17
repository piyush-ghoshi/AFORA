# CI/CD Workflows

GitHub Actions workflows for AFORA project.

## Workflows

### 1. Android CI (`android-ci.yml`)

Builds and tests Android app and shared KMP module.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Only when Android/shared code changes

**Steps:**
1. Checkout code
2. Set up JDK 17
3. Build shared KMP module
4. Build Android app (assembleDebug)
5. Run shared module tests
6. Run Android lint (continues on error)
7. Upload APK artifact (7-day retention)
8. Upload lint reports (7-day retention)

**Artifacts:**
- `android-apk`: Debug APK
- `lint-reports`: Lint analysis reports

### 2. Backend CI (`backend-ci.yml`)

Builds and tests Spring Boot backend.

**Triggers:**
- Push to `main` or `develop` branches
- Pull requests to `main` or `develop`
- Only when backend code changes

**Services:**
- PostgreSQL 15 (for tests)

**Steps:**
1. Checkout code
2. Set up JDK 17
3. Start PostgreSQL service
4. Build backend
5. Run tests with test database
6. Upload test results (7-day retention)
7. Upload JAR artifact (7-day retention)

**Artifacts:**
- `backend-test-results`: JUnit test reports
- `backend-jar`: Compiled JAR file

### 3. Full Build (`full-build.yml`)

Composite workflow that runs both Android and Backend builds.

**Triggers:**
- Push to `main`
- Pull requests to `main`
- Manual dispatch (workflow_dispatch)

**Jobs:**
1. Reuses `android-ci.yml`
2. Reuses `backend-ci.yml`
3. Reports combined status

## Running Workflows

### Automatic

Workflows run automatically on push/PR to specified branches.

### Manual

Trigger "Full Build" manually:
1. Go to Actions tab in GitHub
2. Select "Full Build" workflow
3. Click "Run workflow"
4. Select branch
5. Click "Run workflow"

## Build Status Badges

Add to README.md:

```markdown
![Android CI](https://github.com/YOUR_ORG/AFORA/workflows/Android%20CI/badge.svg)
![Backend CI](https://github.com/YOUR_ORG/AFORA/workflows/Backend%20CI/badge.svg)
```

## Local Verification

Before pushing, verify builds locally:

### Android

```bash
# Build shared module
./gradlew :shared:build

# Build Android app
./gradlew :androidApp:assembleDebug

# Run tests
./gradlew :shared:testDebugUnitTest

# Run lint
./gradlew :androidApp:lintDebug
```

### Backend

```bash
# Build
cd backend
./gradlew build

# Run tests
./gradlew test

# Check test reports
open build/reports/tests/test/index.html
```

## Optimization

Current workflows are basic for Phase A1. Future optimizations:

1. **Caching:**
   - Gradle dependencies ✅ (already enabled)
   - Build cache
   - Android SDK components

2. **Parallel Execution:**
   - Run Android and Backend builds in parallel ✅ (full-build.yml)
   - Matrix builds for multiple Android API levels

3. **Advanced Testing:**
   - UI tests (Espresso)
   - Integration tests
   - Code coverage reports (JaCoCo)

4. **Quality Checks:**
   - Detekt (Kotlin static analysis)
   - ktlint (Kotlin linting)
   - SonarQube integration

5. **Security:**
   - Dependency vulnerability scanning
   - Secret scanning
   - SAST (Static Application Security Testing)

6. **Deployment:**
   - Automatic deployment to staging
   - Release builds with signing
   - Docker image building (backend)

## Troubleshooting

### Workflow fails on "Grant execute permission"

**Cause:** gradlew not executable in repo

**Fix:**
```bash
git update-index --chmod=+x gradlew
git update-index --chmod=+x backend/gradlew
git commit -m "Make gradlew executable"
```

### Android build fails on "SDK not found"

**Cause:** GitHub Actions runner doesn't have Android SDK

**Fix:** Workflow uses `setup-java` which includes Android SDK for gradle builds. If issue persists, add:
```yaml
- name: Set up Android SDK
  uses: android-actions/setup-android@v2
```

### Backend tests fail on database connection

**Cause:** PostgreSQL service not ready or wrong credentials

**Fix:** Check:
1. Service configuration in workflow
2. Environment variables (DB_URL, DB_USERNAME, DB_PASSWORD)
3. Health check is passing before tests run

### Gradle build fails with "Out of memory"

**Cause:** Default heap size too small

**Fix:** Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

## Secrets Configuration

For deployment workflows (Phase A2+), configure these secrets in GitHub:

- `ANDROID_KEYSTORE`: Base64-encoded keystore
- `ANDROID_KEYSTORE_PASSWORD`: Keystore password
- `ANDROID_KEY_ALIAS`: Key alias
- `ANDROID_KEY_PASSWORD`: Key password
- `DB_URL_PROD`: Production database URL
- `DB_USERNAME_PROD`: Production database user
- `DB_PASSWORD_PROD`: Production database password
- `JWT_SECRET_PROD`: Production JWT secret

## Branch Protection

Recommended branch protection rules for `main`:

- ✅ Require pull request reviews (1 approval)
- ✅ Require status checks to pass (Android CI, Backend CI)
- ✅ Require branches to be up to date
- ✅ Require linear history
- ❌ Allow force pushes (disable)
- ❌ Allow deletions (disable)

## License

Proprietary - AFORA Team
