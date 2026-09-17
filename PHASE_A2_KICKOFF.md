# Phase A2 Kickoff - Authentication & User Management

**Phase:** A2 - Authentication & User Management  
**Status:** 🚀 Ready to Start  
**Start Date:** September 17, 2026  
**Estimated Duration:** 2-3 weeks  
**Dependencies:** Phase A1 Complete ✅

---

## Overview

Phase A2 implements the complete authentication and user management system for AFORA, including JWT-based authentication, user registration, login/logout, and role-based access control (RBAC).

---

## Phase A1 Completion Status ✅

- ✅ KMP foundation with shared + androidApp + backend
- ✅ Domain models created (User, Student, Teacher, etc.)
- ✅ Repository interfaces defined
- ✅ Platform abstractions (expect/actual)
- ✅ Ktor API client with auth support
- ✅ DI configured (Koin + Hilt)
- ✅ Database schema with Flyway
- ✅ Material 3 theme
- ✅ Builds verified (shared + androidApp + backend)
- ✅ Git repository initialized and pushed to GitHub

---

## Phase A2 Objectives

### Backend (Spring Boot)
1. **JWT Authentication**
   - Token generation with user claims
   - Token validation middleware
   - Refresh token mechanism
   - Token expiry handling

2. **User Management**
   - User registration endpoint
   - Login/logout endpoints
   - Password hashing (BCrypt)
   - User CRUD operations
   - Profile management

3. **Role-Based Access Control**
   - Role assignment (STUDENT, TEACHER, ADMIN)
   - Permission-based endpoint security
   - Role hierarchy

4. **Security Enhancements**
   - Rate limiting for auth endpoints
   - Account lockout after failed attempts
   - Password strength validation
   - Secure session management

### Shared Module (KMP)
1. **Auth State Management**
   - AuthState sealed class
   - Token storage interface
   - Auth repository implementation
   - Auto token refresh logic

2. **User Management**
   - User repository implementation
   - Profile update logic
   - Password change logic

### Android App
1. **Authentication Screens**
   - Welcome/Splash screen
   - Login screen
   - Registration screen (Student/Teacher)
   - Forgot password screen

2. **Navigation**
   - Auth navigation graph
   - Protected routes
   - Auto-redirect logic

3. **State Management**
   - Login ViewModel
   - Registration ViewModel
   - Auth state observers

4. **Storage**
   - Encrypted token storage (Android Keystore)
   - User preferences
   - Session persistence

---

## Implementation Tasks

### Backend Tasks (8-10 days)

#### Task 1: JWT Infrastructure
**Files to create/modify:**
- `backend/src/main/kotlin/com/academia/backend/common/security/JwtTokenProvider.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/common/security/JwtAuthenticationFilter.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/common/config/JwtConfig.kt` (update)
- `backend/src/main/kotlin/com/academia/backend/common/config/SecurityConfig.kt` (update)

**Implementation:**
- JWT token generation with claims (userId, email, role)
- Token validation and parsing
- Custom authentication filter for protected endpoints
- Exception handling for expired/invalid tokens

**Estimated time:** 2 days

---

#### Task 2: User Entity & Repository
**Files to create/modify:**
- `backend/src/main/kotlin/com/academia/backend/user/entity/User.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/user/entity/Student.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/user/entity/Teacher.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/user/repository/UserRepository.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/user/repository/StudentRepository.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/user/repository/TeacherRepository.kt` (new)
- `backend/src/main/resources/db/migration/V3__auth_user_tables.sql` (new)

**Implementation:**
- JPA entities with proper relationships
- Password hashing with BCrypt
- Repository interfaces with Spring Data JPA
- Database migration for user tables

**Estimated time:** 1.5 days

---

#### Task 3: Authentication Endpoints
**Files to create/modify:**
- `backend/src/main/kotlin/com/academia/backend/auth/controller/AuthController.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/auth/service/AuthService.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/auth/dto/AuthDto.kt` (update)

**Endpoints:**
```
POST /api/auth/register - Register new user
POST /api/auth/login - Login and get JWT
POST /api/auth/refresh - Refresh access token
POST /api/auth/logout - Logout and invalidate token
POST /api/auth/forgot-password - Request password reset
POST /api/auth/reset-password - Reset password with token
```

**Estimated time:** 2 days

---

#### Task 4: User Management Endpoints
**Files to create/modify:**
- `backend/src/main/kotlin/com/academia/backend/user/controller/UserController.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/user/service/UserService.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/user/dto/UserDto.kt` (new)

**Endpoints:**
```
GET /api/users/me - Get current user profile
PUT /api/users/me - Update current user profile
PUT /api/users/me/password - Change password
GET /api/users/{id} - Get user by ID (admin only)
GET /api/users - List users (admin only)
```

**Estimated time:** 1.5 days

---

#### Task 5: Security & Validation
**Files to create/modify:**
- `backend/src/main/kotlin/com/academia/backend/common/security/RateLimiter.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/common/validation/PasswordValidator.kt` (new)
- `backend/src/main/kotlin/com/academia/backend/common/security/SecurityUtils.kt` (new)

**Implementation:**
- Rate limiting for auth endpoints (5 login attempts per minute)
- Password strength validation (min 8 chars, uppercase, lowercase, number)
- Account lockout after 5 failed attempts
- Secure password reset token generation

**Estimated time:** 1 day

---

#### Task 6: Backend Testing
**Files to create:**
- `backend/src/test/kotlin/com/academia/backend/auth/AuthControllerTest.kt` (new)
- `backend/src/test/kotlin/com/academia/backend/auth/JwtTokenProviderTest.kt` (new)
- `backend/src/test/kotlin/com/academia/backend/user/UserServiceTest.kt` (new)

**Test Coverage:**
- JWT token generation and validation
- Login success/failure scenarios
- Registration validation
- Password hashing
- Token refresh logic

**Estimated time:** 2 days

---

### Shared Module Tasks (3-4 days)

#### Task 7: Auth State Management
**Files to create/modify:**
- `shared/src/commonMain/kotlin/com/academia/shared/domain/model/AuthState.kt` (new)
- `shared/src/commonMain/kotlin/com/academia/shared/data/repository/AuthRepository.kt` (update)
- `shared/src/commonMain/kotlin/com/academia/shared/platform/SecureStorage.kt` (new - expect/actual)
- `shared/src/androidMain/kotlin/com/academia/shared/platform/AndroidSecureStorage.kt` (new)

**Implementation:**
```kotlin
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User, val token: String) : AuthState()
    data class Error(val error: AppError) : AuthState()
}
```

**Estimated time:** 1 day

---

#### Task 8: Auth Repository Implementation
**Files to create/modify:**
- `shared/src/commonMain/kotlin/com/academia/shared/data/repository/impl/AuthRepositoryImpl.kt` (new)
- `shared/src/commonMain/kotlin/com/academia/shared/data/api/AuthApi.kt` (new)
- `shared/src/commonMain/kotlin/com/academia/shared/data/api/request/AuthRequests.kt` (new)

**Methods:**
```kotlin
suspend fun login(email: String, password: String): Result<AuthResponse>
suspend fun register(request: RegisterRequest): Result<AuthResponse>
suspend fun refreshToken(): Result<AuthResponse>
suspend fun logout(): Result<Unit>
suspend fun getCurrentUser(): Result<User>
```

**Estimated time:** 1.5 days

---

#### Task 9: Token Management
**Files to create/modify:**
- `shared/src/commonMain/kotlin/com/academia/shared/util/TokenManager.kt` (new)
- `shared/src/commonMain/kotlin/com/academia/shared/data/api/ApiClient.kt` (update)

**Implementation:**
- Auto token refresh before expiry
- Token storage and retrieval
- Token expiry detection
- Auth interceptor for API client

**Estimated time:** 0.5 days

---

### Android App Tasks (5-6 days)

#### Task 10: Authentication Screens UI
**Files to create:**
- `androidApp/src/main/kotlin/com/academia/android/ui/auth/login/LoginScreen.kt` (new)
- `androidApp/src/main/kotlin/com/academia/android/ui/auth/register/RegisterScreen.kt` (new)
- `androidApp/src/main/kotlin/com/academia/android/ui/auth/welcome/WelcomeScreen.kt` (new)
- `androidApp/src/main/kotlin/com/academia/android/ui/auth/components/AuthTextField.kt` (new)
- `androidApp/src/main/kotlin/com/academia/android/ui/auth/components/PasswordTextField.kt` (new)

**Screens:**
1. Welcome screen with logo and "Get Started" button
2. Login screen (email + password)
3. Registration screen (role selection, email, password, name)
4. Forgot password screen

**Estimated time:** 2 days

---

#### Task 11: Authentication ViewModels
**Files to create:**
- `androidApp/src/main/kotlin/com/academia/android/ui/auth/login/LoginViewModel.kt` (new)
- `androidApp/src/main/kotlin/com/academia/android/ui/auth/register/RegisterViewModel.kt` (new)
- `androidApp/src/main/kotlin/com/academia/android/ui/auth/AuthUiState.kt` (new)

**Implementation:**
- Form validation
- API call handling
- Loading states
- Error handling
- Success navigation

**Estimated time:** 1.5 days

---

#### Task 12: Navigation Setup
**Files to create/modify:**
- `androidApp/src/main/kotlin/com/academia/android/navigation/NavGraph.kt` (new)
- `androidApp/src/main/kotlin/com/academia/android/navigation/AuthNavigation.kt` (new)
- `androidApp/src/main/kotlin/com/academia/android/navigation/MainNavigation.kt` (new)
- `androidApp/src/main/kotlin/com/academia/android/MainActivity.kt` (update)

**Routes:**
```kotlin
object AuthRoutes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
}

object MainRoutes {
    const val DASHBOARD = "dashboard"
    const val PROFILE = "profile"
}
```

**Estimated time:** 1 day

---

#### Task 13: Secure Storage Implementation
**Files to create:**
- `androidApp/src/main/kotlin/com/academia/android/data/storage/AndroidSecureStorageImpl.kt` (new)
- `androidApp/src/main/kotlin/com/academia/android/data/storage/EncryptedPreferences.kt` (new)

**Implementation:**
- Android Keystore integration
- Encrypted SharedPreferences
- Token storage/retrieval
- Auto-clear on logout

**Estimated time:** 1 day

---

### Testing & Polish (2-3 days)

#### Task 14: Integration Testing
- End-to-end auth flow testing
- Token refresh testing
- Session persistence testing
- Error handling verification

#### Task 15: UI Polish
- Loading indicators
- Error messages
- Form validation feedback
- Smooth transitions

---

## Acceptance Criteria

### Backend
- [ ] JWT tokens generated with correct claims
- [ ] Login endpoint authenticates users correctly
- [ ] Registration endpoint creates users with hashed passwords
- [ ] Token refresh mechanism works
- [ ] Protected endpoints require valid JWT
- [ ] Rate limiting prevents brute force attacks
- [ ] All endpoints return proper error messages
- [ ] Backend tests pass with >80% coverage

### Shared Module
- [ ] AuthRepository methods work correctly
- [ ] Token storage persists across app restarts
- [ ] Token auto-refresh works before expiry
- [ ] Auth state flows to UI correctly

### Android App
- [ ] Welcome screen displays correctly
- [ ] Login screen authenticates and navigates to dashboard
- [ ] Registration screen creates user and logs in
- [ ] Form validation works (email format, password strength)
- [ ] Loading states display during API calls
- [ ] Error messages are user-friendly
- [ ] Session persists across app restarts
- [ ] Logout clears session and redirects to welcome

---

## API Contracts

### POST /api/auth/register
**Request:**
```json
{
  "email": "student@university.edu",
  "password": "SecurePass123",
  "firstName": "John",
  "lastName": "Doe",
  "role": "STUDENT",
  "studentData": {
    "rollNumber": "2024CS001",
    "department": "Computer Science",
    "batch": "2024"
  }
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "user": {
      "id": 1,
      "email": "student@university.edu",
      "firstName": "John",
      "lastName": "Doe",
      "role": "STUDENT"
    }
  },
  "timestamp": "2026-09-17T10:30:00Z"
}
```

### POST /api/auth/login
**Request:**
```json
{
  "email": "student@university.edu",
  "password": "SecurePass123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc...",
    "user": {
      "id": 1,
      "email": "student@university.edu",
      "firstName": "John",
      "lastName": "Doe",
      "role": "STUDENT"
    }
  },
  "timestamp": "2026-09-17T10:30:00Z"
}
```

### POST /api/auth/refresh
**Request:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGc...",
    "refreshToken": "eyJhbGc..."
  },
  "timestamp": "2026-09-17T10:30:00Z"
}
```

### GET /api/users/me
**Headers:**
```
Authorization: Bearer eyJhbGc...
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "student@university.edu",
    "firstName": "John",
    "lastName": "Doe",
    "role": "STUDENT",
    "profilePicture": null,
    "createdAt": "2026-09-17T10:00:00Z",
    "studentProfile": {
      "rollNumber": "2024CS001",
      "department": "Computer Science",
      "batch": "2024"
    }
  },
  "timestamp": "2026-09-17T10:30:00Z"
}
```

---

## Technical Decisions

### JWT Token Structure
```json
{
  "sub": "1",
  "email": "student@university.edu",
  "role": "STUDENT",
  "iat": 1694945400,
  "exp": 1694948400
}
```

### Token Expiry
- **Access Token:** 1 hour
- **Refresh Token:** 7 days

### Password Requirements
- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 number
- Optional: 1 special character

### Rate Limiting
- Login: 5 attempts per minute per IP
- Register: 3 attempts per minute per IP
- Password reset: 3 attempts per hour per email

---

## Database Schema Updates

### V3__auth_user_tables.sql
```sql
-- Extend users table
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NOT NULL;
ALTER TABLE users ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Refresh tokens table
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(500) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- Password reset tokens
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
```

---

## Dependencies to Add

### Backend
```kotlin
// build.gradle.kts
dependencies {
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // Password hashing (already included in Spring Security)
    // implementation("org.springframework.security:spring-security-crypto")
    
    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
```

### Android
```kotlin
// androidApp/build.gradle.kts
dependencies {
    // Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.3")
    
    // DataStore (alternative to SharedPreferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}
```

---

## Risk Mitigation

### Security Risks
- **Risk:** Token theft via XSS
  - **Mitigation:** Secure storage (Android Keystore), HTTPS only

- **Risk:** Brute force attacks
  - **Mitigation:** Rate limiting, account lockout

- **Risk:** Password in plain text
  - **Mitigation:** BCrypt hashing with salt

### Technical Risks
- **Risk:** Token refresh failures
  - **Mitigation:** Retry logic, fallback to login

- **Risk:** Session persistence issues
  - **Mitigation:** Thorough testing, error recovery

---

## Success Metrics

- [ ] 100% of auth endpoints functional
- [ ] <200ms average response time for login
- [ ] Zero plaintext passwords in database
- [ ] >90% test coverage for auth logic
- [ ] Session persists correctly across restarts
- [ ] All acceptance criteria met

---

## Next Steps After Phase A2

**Phase A3:** Academic Structure Management
- Institution, Department, Course setup
- Class section management
- Semester configuration
- Timetable creation

**Phase A4:** Attendance Engine Core
- Lecture session management
- Manual attendance marking
- Attendance calculations
- Query/dispute handling

---

## Team Assignments (If Available)

- **Backend Developer:** Tasks 1-6
- **KMP/Shared Developer:** Tasks 7-9
- **Android Developer:** Tasks 10-13
- **QA Engineer:** Task 14-15

---

## Daily Standup Template

### What did I complete yesterday?
- Task completed
- Blockers resolved

### What will I work on today?
- Current task
- Expected completion

### Any blockers?
- Technical issues
- Dependencies needed

---

## Phase A2 Completion Checklist

### Code Complete
- [ ] All backend endpoints implemented
- [ ] All shared module repositories implemented
- [ ] All Android screens implemented
- [ ] All ViewModels implemented

### Testing Complete
- [ ] Unit tests pass (backend)
- [ ] Unit tests pass (shared)
- [ ] UI tests pass (Android)
- [ ] Integration tests pass

### Documentation Complete
- [ ] API documentation updated
- [ ] Code comments added
- [ ] README updated with Phase A2 info
- [ ] Phase A2 completion report written

### Deployment Ready
- [ ] Backend builds without errors
- [ ] Android app builds without errors
- [ ] All migrations tested
- [ ] Environment configs updated

---

**Let's build Phase A2!** 🚀

Ready to start with backend JWT implementation or prefer to start with UI screens first?
