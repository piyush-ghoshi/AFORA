# AFORA Project Structure
## Repository & Module Organization

**Version:** 1.0  
**Date:** August 25, 2026  
**Status:** Phase A1 Implementation Guide

---

## 1. Repository Root Structure

```
afora/
├── .github/
│   └── workflows/
│       ├── android-ci.yml
│       ├── backend-ci.yml
│       └── lint.yml
├── shared/                          # Kotlin Multiplatform Module
├── androidApp/                      # Android Application Module
├── backend/                         # Spring Boot Backend
├── docs/                            # Documentation
├── uidesign/                        # UI Designs (existing)
├── .gitignore
├── .editorconfig
├── gradle/
│   └── wrapper/
├── gradlew
├── gradlew.bat
├── settings.gradle.kts              # Root project settings
├── build.gradle.kts                 # Root build configuration
└── README.md
```

---

## 2. Shared Module (KMP)

**Path**: `shared/`

### 2.1 Directory Structure

```
shared/
├── build.gradle.kts
├── src/
│   ├── commonMain/                   # Platform-independent code
│   │   ├── kotlin/
│   │   │   └── com/academia/shared/
│   │   │       ├── domain/
│   │   │       │   ├── model/        # Domain models
│   │   │       │   │   ├── Student.kt
│   │   │       │   │   ├── Teacher.kt
│   │   │       │   │   ├── Lecture.kt
│   │   │       │   │   ├── LectureSession.kt
│   │   │       │   │   ├── AttendanceRecord.kt
│   │   │       │   │   ├── AttendanceCandidate.kt
│   │   │       │   │   ├── AttendanceStatus.kt
│   │   │       │   │   ├── AttendanceMethod.kt
│   │   │       │   │   ├── LeaveRequest.kt
│   │   │       │   │   ├── AttendanceQuery.kt
│   │   │       │   │   ├── Class.kt
│   │   │       │   │   ├── Subject.kt
│   │   │       │   │   ├── Department.kt
│   │   │       │   │   ├── Program.kt
│   │   │       │   │   ├── Batch.kt
│   │   │       │   │   ├── AcademicYear.kt
│   │   │       │   │   ├── Semester.kt
│   │   │       │   │   ├── Enrollment.kt
│   │   │       │   │   ├── FaceProfile.kt
│   │   │       │   │   └── Notification.kt
│   │   │       │   └── usecase/      # Business logic
│   │   │       │       ├── AttendanceUseCase.kt
│   │   │       │       ├── AttendanceCandidateGenerator.kt
│   │   │       │       ├── AttendanceCalculator.kt
│   │   │       │       ├── LeaveUseCase.kt
│   │   │       │       ├── QueryUseCase.kt
│   │   │       │       └── FaceRecognitionUseCase.kt
│   │   │       ├── data/
│   │   │       │   ├── repository/   # Repository interfaces
│   │   │       │   │   ├── AttendanceRepository.kt
│   │   │       │   │   ├── StudentRepository.kt
│   │   │       │   │   ├── TeacherRepository.kt
│   │   │       │   │   ├── LectureRepository.kt
│   │   │       │   │   ├── LeaveRepository.kt
│   │   │       │   │   ├── QueryRepository.kt
│   │   │       │   │   └── FaceRepository.kt
│   │   │       │   ├── api/          # API client & models
│   │   │       │   │   ├── ApiClient.kt
│   │   │       │   │   ├── request/
│   │   │       │   │   │   ├── LoginRequest.kt
│   │   │       │   │   │   ├── AttendanceSubmission.kt
│   │   │       │   │   │   ├── LeaveRequest.kt
│   │   │       │   │   │   └── QueryRequest.kt
│   │   │       │   │   └── response/
│   │   │       │   │       ├── AuthResponse.kt
│   │   │       │   │       ├── AttendanceConfirmation.kt
│   │   │       │   │       ├── StudentResponse.kt
│   │   │       │   │       └── ErrorResponse.kt
│   │   │       │   └── mapper/       # DTO ↔ Domain mappers
│   │   │       │       └── ModelMapper.kt
│   │   │       ├── platform/         # Platform abstractions (expect)
│   │   │       │   ├── CameraController.kt
│   │   │       │   ├── FaceDetector.kt
│   │   │       │   ├── FaceRecognizer.kt
│   │   │       │   ├── ImageProcessor.kt
│   │   │       │   ├── PlatformImage.kt
│   │   │       │   ├── FileStorage.kt
│   │   │       │   └── NetworkMonitor.kt
│   │   │       ├── validation/       # Validation logic
│   │   │       │   ├── AttendanceValidator.kt
│   │   │       │   ├── LeaveValidator.kt
│   │   │       │   ├── EmailValidator.kt
│   │   │       │   └── ValidationResult.kt
│   │   │       ├── policy/           # Configurable policies
│   │   │       │   ├── AttendanceCalculationPolicy.kt
│   │   │       │   ├── AttendanceThresholdPolicy.kt
│   │   │       │   └── RecognitionThresholdPolicy.kt
│   │   │       ├── util/             # Common utilities
│   │   │       │   ├── DateTimeUtil.kt
│   │   │       │   ├── Result.kt
│   │   │       │   ├── Error.kt
│   │   │       │   └── Logger.kt
│   │   │       └── di/               # Dependency injection
│   │   │           └── SharedModule.kt
│   │   └── resources/
│   │
│   ├── androidMain/                  # Android-specific implementations
│   │   ├── kotlin/
│   │   │   └── com/academia/shared/
│   │   │       ├── platform/         # Actual implementations
│   │   │       │   ├── AndroidCameraController.kt
│   │   │       │   ├── AndroidFaceDetector.kt
│   │   │       │   ├── AndroidFaceRecognizer.kt
│   │   │       │   ├── AndroidImageProcessor.kt
│   │   │       │   ├── AndroidPlatformImage.kt
│   │   │       │   ├── AndroidFileStorage.kt
│   │   │       │   └── AndroidNetworkMonitor.kt
│   │   │       ├── data/
│   │   │       │   └── repository/   # Android-specific repo implementations
│   │   │       │       └── AndroidAttendanceRepository.kt
│   │   │       └── di/
│   │   │           └── AndroidModule.kt
│   │   └── AndroidManifest.xml
│   │
│   ├── iosMain/                      # iOS-specific (future)
│   │   └── kotlin/
│   │       └── com/academia/shared/
│   │           └── platform/
│   │               ├── IOSCameraController.kt
│   │               ├── IOSFaceDetector.kt
│   │               ├── IOSFaceRecognizer.kt
│   │               ├── IOSImageProcessor.kt
│   │               ├── IOSPlatformImage.kt
│   │               ├── IOSFileStorage.kt
│   │               └── IOSNetworkMonitor.kt
│   │
│   ├── commonTest/                   # Shared tests
│   │   └── kotlin/
│   │       └── com/academia/shared/
│   │           ├── domain/
│   │           │   └── usecase/
│   │           │       ├── AttendanceUseCaseTest.kt
│   │           │       └── AttendanceCalculatorTest.kt
│   │           └── validation/
│   │               └── AttendanceValidatorTest.kt
│   │
│   ├── androidUnitTest/              # Android-specific tests
│   │   └── kotlin/
│   │
│   └── iosTest/                      # iOS-specific tests (future)
│       └── kotlin/
```

### 2.2 Shared Module build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.10"
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    // Future iOS targets
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
                
                // Networking (Ktor)
                implementation("io.ktor:ktor-client-core:2.3.5")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.5")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
                
                // DI (Koin)
                implementation("io.insert-koin:koin-core:3.5.0")
            }
        }
        
        val androidMain by getting {
            dependencies {
                // Android-specific
                implementation("androidx.core:core-ktx:1.12.0")
                
                // Camera
                implementation("androidx.camera:camera-core:1.3.0")
                implementation("androidx.camera:camera-camera2:1.3.0")
                implementation("androidx.camera:camera-lifecycle:1.3.0")
                implementation("androidx.camera:camera-view:1.3.0")
                
                // ML Kit (detection)
                implementation("com.google.mlkit:face-detection:16.1.5")
                
                // TensorFlow Lite (recognition)
                implementation("org.tensorflow:tensorflow-lite:2.14.0")
                implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
                
                // Ktor Android
                implementation("io.ktor:ktor-client-okhttp:2.3.5")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("io.mockk:mockk:1.13.8")
            }
        }
    }
}

android {
    namespace = "com.academia.shared"
    compileSdk = 34
    
    defaultConfig {
        minSdk = 24
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

---

## 3. Android App Module

**Path**: `androidApp/`

### 3.1 Directory Structure

```
androidApp/
├── build.gradle.kts
├── proguard-rules.pro
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/academia/android/
│   │   │       ├── AcademiaApplication.kt
│   │   │       ├── MainActivity.kt
│   │   │       ├── ui/
│   │   │       │   ├── theme/
│   │   │       │   │   ├── Color.kt
│   │   │       │   │   ├── Type.kt
│   │   │       │   │   ├── Shape.kt
│   │   │       │   │   └── Theme.kt
│   │   │       │   ├── components/      # Reusable components
│   │   │       │   │   ├── AttendanceCard.kt
│   │   │       │   │   ├── StudentListItem.kt
│   │   │       │   │   ├── StatusBadge.kt
│   │   │       │   │   ├── LoadingIndicator.kt
│   │   │       │   │   └── ErrorScreen.kt
│   │   │       │   ├── navigation/
│   │   │       │   │   ├── NavGraph.kt
│   │   │       │   │   ├── Screen.kt
│   │   │       │   │   └── NavigationActions.kt
│   │   │       │   └── screen/
│   │   │       │       ├── auth/
│   │   │       │       │   ├── LoginScreen.kt
│   │   │       │       │   └── LoginViewModel.kt
│   │   │       │       ├── student/
│   │   │       │       │   ├── StudentDashboardScreen.kt
│   │   │       │       │   ├── StudentDashboardViewModel.kt
│   │   │       │       │   ├── AttendanceDetailScreen.kt
│   │   │       │       │   ├── AttendanceDetailViewModel.kt
│   │   │       │       │   ├── ApplyLeaveScreen.kt
│   │   │       │       │   ├── LeaveListScreen.kt
│   │   │       │       │   ├── RaiseQueryScreen.kt
│   │   │       │       │   ├── QueryListScreen.kt
│   │   │       │       │   └── LeaderboardScreen.kt
│   │   │       │       ├── teacher/
│   │   │       │       │   ├── TeacherDashboardScreen.kt
│   │   │       │       │   ├── TeacherDashboardViewModel.kt
│   │   │       │       │   ├── StartAttendanceScreen.kt
│   │   │       │       │   ├── CameraAttendanceScreen.kt
│   │   │       │       │   ├── CameraAttendanceViewModel.kt
│   │   │       │       │   ├── ManualAttendanceScreen.kt
│   │   │       │       │   ├── ManualAttendanceViewModel.kt
│   │   │       │       │   ├── ReviewAttendanceScreen.kt
│   │   │       │       │   ├── ReviewAttendanceViewModel.kt
│   │   │       │       │   ├── AttendanceHistoryScreen.kt
│   │   │       │       │   ├── LeaveApprovalScreen.kt
│   │   │       │       │   └── QueryApprovalScreen.kt
│   │   │       │       └── admin/
│   │   │       │           ├── AdminDashboardScreen.kt
│   │   │       │           ├── StudentManagementScreen.kt
│   │   │       │           ├── TeacherManagementScreen.kt
│   │   │       │           ├── ClassManagementScreen.kt
│   │   │       │           ├── SubjectManagementScreen.kt
│   │   │       │           ├── EnrollmentScreen.kt
│   │   │       │           ├── TimetableManagementScreen.kt
│   │   │       │           └── AnalyticsScreen.kt
│   │   │       ├── viewmodel/       # ViewModels (if not in screen/)
│   │   │       ├── di/              # Hilt modules
│   │   │       │   ├── AppModule.kt
│   │   │       │   ├── NetworkModule.kt
│   │   │       │   ├── RepositoryModule.kt
│   │   │       │   └── CameraModule.kt
│   │   │       └── util/
│   │   │           ├── PermissionUtil.kt
│   │   │           └── DateFormatter.kt
│   │   ├── res/
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   ├── drawable/
│   │   │   ├── mipmap/
│   │   │   └── xml/
│   │   │       └── network_security_config.xml
│   │   └── AndroidManifest.xml
│   ├── debug/
│   │   └── AndroidManifest.xml
│   ├── release/
│   │   └── AndroidManifest.xml
│   └── androidTest/
│       └── kotlin/
│           └── com/academia/android/
│               ├── ExampleInstrumentedTest.kt
│               └── ui/
│                   └── LoginScreenTest.kt
```

### 3.2 Android App build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.academia.android"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.academia.attendance"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Shared KMP module
    implementation(project(":shared"))
    
    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Datastore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coil (image loading)
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}
```

---

## 4. Backend Module (Spring Boot)

**Path**: `backend/`

### 4.1 Directory Structure

```
backend/
├── build.gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/academia/backend/
│   │   │       ├── AcademiaBackendApplication.java
│   │   │       ├── config/
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── WebConfig.java
│   │   │       │   ├── DatabaseConfig.java
│   │   │       │   ├── RedisConfig.java
│   │   │       │   └── OpenApiConfig.java
│   │   │       ├── security/
│   │   │       │   ├── JwtTokenProvider.java
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   ├── UserDetailsServiceImpl.java
│   │   │       │   └── SecurityUtils.java
│   │   │       ├── auth/
│   │   │       │   ├── controller/
│   │   │       │   │   └── AuthController.java
│   │   │       │   ├── service/
│   │   │       │   │   └── AuthService.java
│   │   │       │   ├── dto/
│   │   │       │   │   ├── LoginRequest.java
│   │   │       │   │   ├── LoginResponse.java
│   │   │       │   │   └── RefreshTokenRequest.java
│   │   │       │   └── exception/
│   │   │       │       └── AuthenticationException.java
│   │   │       ├── user/
│   │   │       │   ├── model/
│   │   │       │   │   └── User.java
│   │   │       │   ├── repository/
│   │   │       │   │   └── UserRepository.java
│   │   │       │   ├── service/
│   │   │       │   │   └── UserService.java
│   │   │       │   └── controller/
│   │   │       │       └── UserController.java
│   │   │       ├── student/
│   │   │       │   ├── model/
│   │   │       │   │   └── Student.java
│   │   │       │   ├── repository/
│   │   │       │   │   └── StudentRepository.java
│   │   │       │   ├── service/
│   │   │       │   │   └── StudentService.java
│   │   │       │   ├── controller/
│   │   │       │   │   └── StudentController.java
│   │   │       │   └── dto/
│   │   │       │       ├── StudentRequest.java
│   │   │       │       └── StudentResponse.java
│   │   │       ├── teacher/
│   │   │       │   ├── model/
│   │   │       │   │   └── Teacher.java
│   │   │       │   ├── repository/
│   │   │       │   │   └── TeacherRepository.java
│   │   │       │   ├── service/
│   │   │       │   │   └── TeacherService.java
│   │   │       │   ├── controller/
│   │   │       │   │   └── TeacherController.java
│   │   │       │   └── dto/
│   │   │       ├── academic/
│   │   │       │   ├── model/
│   │   │       │   │   ├── Department.java
│   │   │       │   │   ├── Program.java
│   │   │       │   │   ├── Batch.java
│   │   │       │   │   ├── ClassSection.java
│   │   │       │   │   ├── Subject.java
│   │   │       │   │   ├── AcademicYear.java
│   │   │       │   │   └── Semester.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   ├── controller/
│   │   │       │   └── dto/
│   │   │       ├── enrollment/
│   │   │       │   ├── model/
│   │   │       │   │   ├── Enrollment.java
│   │   │       │   │   └── TeacherAssignment.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   ├── controller/
│   │   │       │   └── dto/
│   │   │       ├── timetable/
│   │   │       │   ├── model/
│   │   │       │   │   └── TimetableSlot.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   ├── controller/
│   │   │       │   └── dto/
│   │   │       ├── attendance/
│   │   │       │   ├── model/
│   │   │       │   │   ├── LectureSession.java
│   │   │       │   │   ├── AttendanceRecord.java
│   │   │       │   │   ├── AttendanceCorrection.java
│   │   │       │   │   ├── SessionStatus.java (enum)
│   │   │       │   │   ├── AttendanceStatus.java (enum)
│   │   │       │   │   └── AttendanceMethod.java (enum)
│   │   │       │   ├── repository/
│   │   │       │   │   ├── LectureSessionRepository.java
│   │   │       │   │   ├── AttendanceRecordRepository.java
│   │   │       │   │   └── AttendanceCorrectionRepository.java
│   │   │       │   ├── service/
│   │   │       │   │   ├── LectureSessionService.java
│   │   │       │   │   ├── AttendanceService.java
│   │   │       │   │   ├── AttendanceValidationService.java
│   │   │       │   │   └── AttendanceCalculationService.java
│   │   │       │   ├── controller/
│   │   │       │   │   └── AttendanceController.java
│   │   │       │   └── dto/
│   │   │       │       ├── AttendanceSubmission.java
│   │   │       │       ├── AttendanceConfirmation.java
│   │   │       │       └── AttendanceStats.java
│   │   │       ├── face/
│   │   │       │   ├── model/
│   │   │       │   │   ├── FaceProfile.java
│   │   │       │   │   ├── FaceImage.java
│   │   │       │   │   └── FaceEmbedding.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   │   ├── FaceRegistrationService.java
│   │   │       │   │   └── FaceStorageService.java
│   │   │       │   ├── controller/
│   │   │       │   └── dto/
│   │   │       ├── leave/
│   │   │       │   ├── model/
│   │   │       │   │   ├── LeaveRequest.java
│   │   │       │   │   └── LeaveRequestSubject.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   ├── controller/
│   │   │       │   └── dto/
│   │   │       ├── query/
│   │   │       │   ├── model/
│   │   │       │   │   └── AttendanceQuery.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   ├── controller/
│   │   │       │   └── dto/
│   │   │       ├── notification/
│   │   │       │   ├── model/
│   │   │       │   │   └── Notification.java
│   │   │       │   ├── repository/
│   │   │       │   ├── service/
│   │   │       │   ├── controller/
│   │   │       │   └── provider/
│   │   │       │       ├── EmailNotificationProvider.java
│   │   │       │       └── PushNotificationProvider.java
│   │   │       ├── leaderboard/
│   │   │       │   ├── service/
│   │   │       │   ├── controller/
│   │   │       │   └── dto/
│   │   │       ├── admin/
│   │   │       │   ├── controller/
│   │   │       │   ├── service/
│   │   │       │   └── dto/
│   │   │       ├── common/
│   │   │       │   ├── exception/
│   │   │       │   │   ├── GlobalExceptionHandler.java
│   │   │       │   │   ├── ResourceNotFoundException.java
│   │   │       │   │   ├── ValidationException.java
│   │   │       │   │   ├── UnauthorizedException.java
│   │   │       │   │   └── DuplicateException.java
│   │   │       │   ├── validation/
│   │   │       │   │   └── Validators.java
│   │   │       │   ├── audit/
│   │   │       │   │   ├── AuditLog.java
│   │   │       │   │   ├── AuditLogRepository.java
│   │   │       │   │   └── AuditService.java
│   │   │       │   ├── dto/
│   │   │       │   │   ├── ApiResponse.java
│   │   │       │   │   ├── ErrorResponse.java
│   │   │       │   │   └── PagedResponse.java
│   │   │       │   └── util/
│   │   │       │       ├── DateTimeUtil.java
│   │   │       │       └── SecurityUtil.java
│   │   │       └── policy/
│   │   │           ├── AttendanceCalculationPolicy.java
│   │   │           └── AttendanceThresholdPolicy.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/
│   │           └── migration/
│   │               ├── V1__create_users_tables.sql
│   │               ├── V2__create_academic_tables.sql
│   │               ├── V3__create_enrollment_tables.sql
│   │               ├── V4__create_timetable_tables.sql
│   │               ├── V5__create_attendance_tables.sql
│   │               ├── V6__create_face_tables.sql
│   │               ├── V7__create_leave_tables.sql
│   │               ├── V8__create_query_tables.sql
│   │               ├── V9__create_notification_tables.sql
│   │               └── V10__create_audit_tables.sql
│   └── test/
│       └── java/
│           └── com/academia/backend/
│               ├── attendance/
│               │   └── service/
│               │       └── AttendanceServiceTest.java
│               └── auth/
│                   └── service/
│                       └── AuthServiceTest.java
```

### 4.2 Backend build.gradle

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.1.5'
    id 'io.spring.dependency-management' version '1.1.3'
}

group = 'com.academia'
version = '1.0.0'
sourceCompatibility = '17'

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    
    // Database
    implementation 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
    implementation 'com.pgvector:pgvector:0.1.4'
    
    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
    
    // OpenAPI/Swagger
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'
    
    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    // Object Storage (AWS SDK for S3)
    implementation 'software.amazon.awssdk:s3:2.20.160'
    
    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:testcontainers:1.19.1'
    testImplementation 'org.testcontainers:postgresql:1.19.1'
    testImplementation 'org.testcontainers:junit-jupiter:1.19.1'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

## 5. Root Configuration Files

### 5.1 settings.gradle.kts

```kotlin
rootProject.name = "AFORA"

include(":shared")
include(":androidApp")

// Backend uses separate Gradle project
// (Java/Gradle vs Kotlin/KGP build systems)
```

### 5.2 Root build.gradle.kts

```kotlin
plugins {
    // Kotlin
    kotlin("jvm") version "1.9.10" apply false
    kotlin("multiplatform") version "1.9.10" apply false
    kotlin("android") version "1.9.10" apply false
    kotlin("plugin.serialization") version "1.9.10" apply false
    
    // Android
    id("com.android.application") version "8.1.2" apply false
    id("com.android.library") version "8.1.2" apply false
    
    // Hilt
    id("com.google.dagger.hilt.android") version "2.48" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
```

---

## 6. Key Principles

### 6.1 Module Separation

✅ **Shared Module**: Platform-independent business logic  
✅ **Android Module**: UI, platform-specific implementations  
✅ **Backend Module**: Server-side logic, database, APIs  

### 6.2 Dependency Flow

```
androidApp → shared (commonMain + androidMain)
            ↓
         Backend APIs
```

### 6.3 Platform Boundaries

**NEVER**:
- Android APIs in `shared/commonMain`
- Business logic in `androidApp/ui`
- Direct database access from client

**ALWAYS**:
- Platform abstractions via `expect/actual`
- Repository interfaces in shared
- API contracts in shared

---

## 7. Next Steps (Phase A1)

**Implementation Order**:

1. ✅ Create root project structure
2. ✅ Configure `settings.gradle.kts` and root `build.gradle.kts`
3. ✅ Create `shared` module with KMP configuration
4. ✅ Define platform abstractions (expect interfaces)
5. ✅ Create `androidApp` module
6. ✅ Set up dependency injection (Koin + Hilt)
7. ✅ Create basic navigation structure
8. ✅ Implement Material 3 theme (Academic Precision design)
9. ✅ Create `backend` project (separate Gradle)
10. ✅ Test compilation of all modules

**Phase A1 Deliverable**: Compiling, foundational project structure with KMP boundaries defined.

---

## Document Control

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-08-25 | System Architect | Initial structure definition |

**Status**: Ready for Phase A1 Implementation
