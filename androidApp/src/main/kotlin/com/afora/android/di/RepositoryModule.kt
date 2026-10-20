package com.afora.android.di
import android.content.Context
import com.afora.android.data.UserPreferences
import com.afora.android.data.UserPreferencesStore
import com.afora.shared.data.api.ApiClient
import com.afora.shared.data.repository.AttendanceRepository
import com.afora.shared.data.repository.AuthRepository
import com.afora.shared.data.repository.DashboardRepository
import com.afora.shared.data.repository.FaceProfileRepository
import com.afora.shared.data.repository.FirebaseAuthRepository
import com.afora.shared.data.repository.HttpAttendanceRepository
import com.afora.shared.data.repository.HttpDashboardRepository
import com.afora.shared.data.repository.HttpFaceProfileRepository
import com.afora.shared.data.repository.HttpLeaveRepository
import com.afora.shared.data.repository.HttpStudentRepository
import com.afora.shared.data.repository.HttpTeacherRepository
import com.afora.shared.data.repository.HttpTimetableRepository
import com.afora.shared.data.repository.HttpUserRepository
import com.afora.shared.data.repository.LeaveRepository
import com.afora.shared.data.repository.StudentRepository
import com.afora.shared.data.repository.TeacherRepository
import com.afora.shared.data.repository.TimetableRepository
import com.afora.shared.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton

/**
 * Hilt module that provides all repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // -- Infrastructure -----------------------------------------------------

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferencesStore = UserPreferences(context)

    /**
     * ApiClient with Firebase token auto-injection.
     * Token is retrieved fresh on every request so it's always valid.
     */
    @Provides
    @Singleton
    fun provideApiClient(
        firebaseAuth: FirebaseAuth
    ): ApiClient = ApiClient(
        baseUrl = "http://localhost:8080",   // localhost:8080 with adb reverse for USB physical devices & emulators
        tokenProvider = {
            firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
        }
    )

    // -- Authentication -------------------------------------------------------

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth
    ): AuthRepository = FirebaseAuthRepository(firebaseAuth)

    // -- User -------------------------------------------------------------------

    @Provides
    @Singleton
    fun provideUserRepository(
        apiClient: ApiClient
    ): UserRepository = HttpUserRepository(apiClient)

    // -- Student ------------------------------------------------------------------

    @Provides
    @Singleton
    fun provideStudentRepository(
        apiClient: ApiClient
    ): StudentRepository = HttpStudentRepository(apiClient)

    // -- Teacher ------------------------------------------------------------------

    @Provides
    @Singleton
    fun provideTeacherRepository(
        apiClient: ApiClient
    ): TeacherRepository = HttpTeacherRepository(apiClient)

    // -- Timetable ------------------------------------------------------------------

    @Provides
    @Singleton
    fun provideTimetableRepository(
        apiClient: ApiClient
    ): TimetableRepository = HttpTimetableRepository(apiClient)

    // -- Dashboard ------------------------------------------------------------------

    @Provides
    @Singleton
    fun provideDashboardRepository(
        apiClient: ApiClient
    ): DashboardRepository = HttpDashboardRepository(apiClient)

    // -- Attendance (manual marking flow) ----------------------------------------

    @Provides
    @Singleton
    fun provideHttpAttendanceRepository(
        apiClient: ApiClient
    ): HttpAttendanceRepository = HttpAttendanceRepository(apiClient)

    @Provides
    @Singleton
    fun provideAttendanceRepository(
        impl: HttpAttendanceRepository
    ): AttendanceRepository = impl

    // -- Leave (leave requests feature) ------------------------------------------

    @Provides
    @Singleton
    fun provideLeaveRepository(
        apiClient: ApiClient
    ): LeaveRepository = HttpLeaveRepository(apiClient)

    // -- Face recognition ---------------------------------------------------------

    @Provides
    @Singleton
    fun provideHttpFaceProfileRepository(
        apiClient: ApiClient
    ): HttpFaceProfileRepository = HttpFaceProfileRepository(apiClient)

    @Provides
    @Singleton
    fun provideFaceProfileRepository(
        impl: HttpFaceProfileRepository
    ): FaceProfileRepository = impl
}