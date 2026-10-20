package com.afora.android.ui.faceprofile

import com.afora.shared.data.api.FaceProfileResponse
import com.afora.shared.util.AppError
import com.afora.shared.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [FaceRegistrationViewModel] (`.kiro/specs/face-recognition/tasks.md` task 17.2).
 *
 * Validates: Requirements 1.6, 13.2, 13.3, 13.4, 13.5.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FaceRegistrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fakeRepository = FakeFaceProfileRepository()
    private lateinit var viewModel: FaceRegistrationViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = FaceRegistrationViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is AwaitingConsent and captureShot is a no-op before consent (R1_6, R13_2)`() = runTest {
        assertTrue(viewModel.uiState.value is FaceRegistrationUiState.AwaitingConsent)

        // Consent-before-capture ordering: capturing before consent must not transition state.
        viewModel.captureShot()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is FaceRegistrationUiState.AwaitingConsent)
        assertEquals(0, fakeRepository.recordConsentCallCount)
    }

    @Test
    fun `affirmConsent records consent before capture becomes available (R1_6, R13_2)`() = runTest {
        viewModel.affirmConsent()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, fakeRepository.recordConsentCallCount)
        val state = viewModel.uiState.value
        assertTrue(state is FaceRegistrationUiState.Capturing)
        assertEquals(emptyList<List<Double>>(), (state as FaceRegistrationUiState.Capturing).capturedEmbeddings)

        // Capture is now reachable, and only after recordConsent() succeeded.
        viewModel.captureShot()
        val afterCapture = viewModel.uiState.value as FaceRegistrationUiState.Capturing
        assertEquals(1, afterCapture.capturedEmbeddings.size)
    }

    @Test
    fun `register transmits only the captured embeddings, never image data (R13_3)`() = runTest {
        viewModel.affirmConsent()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.captureShot()
        viewModel.captureShot()

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        // Captured compile-time as List<List<Double>> -- exactly the on-device embedding
        // shape, with no bitmap/image type anywhere in the call chain -- and matching exactly
        // what two captureShot() calls produced.
        val transmitted: List<List<Double>>? = fakeRepository.lastRegisteredEmbeddings
        val expectedEmbedding = List(128) { 0.0 }
        assertEquals(listOf(expectedEmbedding, expectedEmbedding), transmitted)
        assertTrue(viewModel.uiState.value is FaceRegistrationUiState.Registered)
    }

    @Test
    fun `register success exposes the server-reported embedding count (R13_4)`() = runTest {
        fakeRepository.registerFaceProfileResult = Result.Success(
            FaceProfileResponse(
                studentId = 1,
                embeddingCount = 3,
                dimensions = 128,
                registeredAt = "2026-01-01T00:00:00"
            )
        )

        viewModel.affirmConsent()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.captureShot()

        viewModel.register()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FaceRegistrationUiState.Registered)
        assertEquals(3, (state as FaceRegistrationUiState.Registered).embeddingCount)
    }

    @Test
    fun `register failure retains captured embeddings and permits retry without re-affirming consent (R13_5)`() =
        runTest {
            viewModel.affirmConsent()
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.captureShot()

            fakeRepository.registerFaceProfileResult =
                Result.Error(AppError.NetworkError("registration failed"))

            viewModel.register()
            testDispatcher.scheduler.advanceUntilIdle()

            val afterFailure = viewModel.uiState.value
            assertTrue(afterFailure is FaceRegistrationUiState.Capturing)
            afterFailure as FaceRegistrationUiState.Capturing
            assertEquals(1, afterFailure.capturedEmbeddings.size)
            assertEquals("registration failed", afterFailure.error)
            assertEquals(1, fakeRepository.recordConsentCallCount)

            // Retry: fix the backend response and call register() again without ever calling
            // affirmConsent() a second time.
            fakeRepository.registerFaceProfileResult = Result.Success(
                FaceProfileResponse(
                    studentId = 1,
                    embeddingCount = 1,
                    dimensions = 128,
                    registeredAt = "2026-01-01T00:00:00"
                )
            )
            viewModel.register()
            testDispatcher.scheduler.advanceUntilIdle()

            val afterRetry = viewModel.uiState.value
            assertTrue(afterRetry is FaceRegistrationUiState.Registered)
            assertEquals(1, fakeRepository.recordConsentCallCount)
        }

    @Test
    fun `affirmConsent failure stays in AwaitingConsent with the error surfaced`() = runTest {
        fakeRepository.recordConsentResult = Result.Error(AppError.NetworkError("consent failed"))

        viewModel.affirmConsent()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is FaceRegistrationUiState.AwaitingConsent)
        assertEquals("consent failed", (state as FaceRegistrationUiState.AwaitingConsent).error)
    }
}
