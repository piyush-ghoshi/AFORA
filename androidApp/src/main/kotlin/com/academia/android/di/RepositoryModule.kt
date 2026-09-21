package com.academia.android.di

import com.academia.shared.data.api.ApiClient
import com.academia.shared.data.repository.AuthRepository
import com.academia.shared.data.repository.FirebaseAuthRepository
import com.academia.shared.data.repository.HttpStudentRepository
import com.academia.shared.data.repository.HttpTeacherRepository
import com.academia.shared.data.repository.HttpTimetableRepository
import com.academia.shared.data.repository.HttpUserRepository
import com.academia.shared.data.repository.StudentRepository
import com.academia.shared.data.repository.TeacherRepository
import com.academia.shared.data.repository.TimetableRepository
import com.academia.shared.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.tasks.await
import javax.inject.Singleton

/**
 * Hilt module that provides all repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    // ── Infrastructure ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * ApiClient with Firebase token auto-injection.
     * Token is retrieved fresh on every request so it's always valid.
     */
    @Provides
    @Singleton
    fun provideApiClient(
        firebaseAuth: FirebaseAuth
    ): ApiClient = ApiClient(
        baseUrl = "http://10.0.2.2:8080",   // 10.0.2.2 = host machine from Android emulator
        tokenProvider = {
            firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
        }
    )

    // ── Authentication ────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth
    ): AuthRepository = FirebaseAuthRepository(firebaseAuth)

    // ── User ──────────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideUserRepository(
        apiClient: ApiClient
    ): UserRepository = HttpUserRepository(apiClient)

    // ── Student ───────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideStudentRepository(
        apiClient: ApiClient
    ): StudentRepository = HttpStudentRepository(apiClient)

    // ── Teacher ───────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideTeacherRepository(
        apiClient: ApiClient
    ): TeacherRepository = HttpTeacherRepository(apiClient)

    // ── Timetable ─────────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideTimetableRepository(
        apiClient: ApiClient
    ): TimetableRepository = HttpTimetableRepository(apiClient)
}
