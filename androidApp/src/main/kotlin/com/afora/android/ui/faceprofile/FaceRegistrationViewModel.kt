package com.afora.android.ui.faceprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afora.shared.data.repository.FaceProfileRepository
import com.afora.shared.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Face_Registration_Screen (`.kiro/specs/face-recognition/design.md`
 * -> "Registration sequence"; requirements.md Requirement 1, 2, 13).
 *
 * The backend resolves the caller's own student id from the Firebase token
 * (Settled Decision D), so no studentId parameter is needed here -- every
 * call just mirrors [StudentLeaveViewModel]'s shape against
 * [FaceProfileRepository] (R13.6).
 *
 * On-device embedding generation, Binding Convention 11 note (stated plainly
 * per task 17.1): `OnDeviceFaceMatcher.generateEmbedding(face: Face, frame:
 * InputImage)` requires an ML Kit `Face` instance. `Face` has no public
 * constructor -- decompiling the shipped `face-detection` AAR shows both of
 * its constructors take internal, non-public `zzf`/`zznt` types -- so a
 * `Face` can only ever be produced by a live CameraX + ML Kit detector
 * pipeline (see `FaceDetectionAnalyzer`). This screen's capture control
 * simulates one "shot" with no such live frame, so there is no `Face` to
 * pass. Per design.md task 17.1's own documented fallback ("simplify the
 * signature if constructing real ML Kit types is awkward... a reasonable
 * minimal approach is acceptable here"), [generatePlaceholderEmbedding]
 * stands in for that call this wave rather than fabricating an
 * unconstructable SDK type. Either way the result is the same stub-quality
 * placeholder vector task 15's `StubFaceMatcher` would produce: no image
 * data is ever generated, held, or transmitted -- only this numeric vector
 * is sent to [FaceProfileRepository.registerFaceProfile] (R13.3).
 */
@HiltViewModel
class FaceRegistrationViewModel @Inject constructor(
    private val faceProfileRepository: FaceProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FaceRegistrationUiState>(FaceRegistrationUiState.AwaitingConsent())
    val uiState: StateFlow<FaceRegistrationUiState> = _uiState.asStateFlow()

    /**
     * R1.6, R13.1-R13.2: affirm Face_Consent. Calls `recordConsent()` before
     * capture becomes reachable at all -- success transitions
     * [FaceRegistrationUiState.AwaitingConsent] to
     * [FaceRegistrationUiState.Capturing]; failure stays in
     * [FaceRegistrationUiState.AwaitingConsent] with [AppError][com.afora.shared.util.AppError]'s
     * message surfaced.
     */
    fun affirmConsent() {
        val state = _uiState.value as? FaceRegistrationUiState.AwaitingConsent ?: return
        if (state.isSubmitting) return
        _uiState.value = state.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            when (val result = faceProfileRepository.recordConsent()) {
                is Result.Success -> _uiState.value = FaceRegistrationUiState.Capturing()
                is Result.Error -> _uiState.value = state.copy(isSubmitting = false, error = result.error.message)
            }
        }
    }

    /**
     * R13.3: generate one Face_Embedding "shot" entirely on-device (see the
     * class KDoc for why [generatePlaceholderEmbedding] stands in for
     * `OnDeviceFaceMatcher.generateEmbedding` this wave) and append it to the
     * captured set. A no-op outside [FaceRegistrationUiState.Capturing] or
     * while a registration submission is in flight.
     */
    fun captureShot() {
        val state = _uiState.value as? FaceRegistrationUiState.Capturing ?: return
        if (state.isSubmitting) return

        _uiState.value = state.copy(
            capturedEmbeddings = state.capturedEmbeddings + listOf(generatePlaceholderEmbedding()),
            captureError = null
        )
    }

    /**
     * Clears any accumulated captured shots so the student can re-capture fresh shots.
     */
    fun resetShots() {
        val state = _uiState.value as? FaceRegistrationUiState.Capturing ?: return
        if (state.isSubmitting) return

        _uiState.value = state.copy(
            capturedEmbeddings = emptyList(),
            captureError = null,
            error = null
        )
    }

    /**
     * R13.3-R13.5: transmit only the accumulated on-device embeddings --
     * never image data -- to `registerFaceProfile(...)`. On success, exposes
     * [FaceRegistrationUiState.Registered] naming the embedding count the
     * server actually stored (R13.4). On failure, stays in
     * [FaceRegistrationUiState.Capturing] with `error` set, so a retry never
     * has to re-affirm already-recorded consent (R13.5).
     */
    fun register() {
        val state = _uiState.value as? FaceRegistrationUiState.Capturing ?: return
        if (state.isSubmitting || state.capturedEmbeddings.isEmpty()) return
        _uiState.value = state.copy(isSubmitting = true, error = null)

        viewModelScope.launch {
            when (val result = faceProfileRepository.registerFaceProfile(state.capturedEmbeddings)) {
                is Result.Success -> _uiState.value =
                    FaceRegistrationUiState.Registered(embeddingCount = result.data.embeddingCount)
                is Result.Error -> {
                    val current = _uiState.value as? FaceRegistrationUiState.Capturing ?: state
                    _uiState.value = current.copy(isSubmitting = false, error = result.error.message)
                }
            }
        }
    }

    /**
     * Wave-3 placeholder embedding (see the class KDoc). Fixed dimensionality
     * across every "shot" so a multi-shot registration satisfies the
     * server's within-request dimensionality-consistency check (R2.7) --
     * this is not real biometric data by any measure and exists only so the
     * consent -> capture -> transmit -> confirm flow can be exercised end to
     * end until a real `OnDeviceFaceMatcher` replaces task 15's stub.
     */
    private fun generatePlaceholderEmbedding(): List<Double> =
        List(PLACEHOLDER_EMBEDDING_DIMENSIONS) { 0.0 }

    private companion object {
        const val PLACEHOLDER_EMBEDDING_DIMENSIONS = 128
    }
}
