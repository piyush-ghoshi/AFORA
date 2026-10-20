package com.afora.android.ui.faceprofile

/**
 * UI state for the Face_Registration_Screen (requirements.md Requirement 1, 13).
 *
 * The three states model the required flow in order: consent must be
 * affirmed and recorded (R1.6, R13.1-13.2) before capture is reachable at
 * all, capture accumulates on-device-generated embeddings for one or more
 * "shots" (R13.3), and a successful registration surfaces the embedding
 * count the server actually stored (R13.4).
 *
 * There is deliberately no separate "consent already recorded" flag on
 * [Capturing]/its failure path: once the state machine has moved past
 * [AwaitingConsent] it never returns to it, so a registration failure's
 * retry (R13.5) simply stays in [Capturing] and never re-affirms consent.
 */
sealed interface FaceRegistrationUiState {

    /**
     * R1.6, R13.1: the Student must affirm Face_Consent here before the
     * camera capture control becomes available anywhere in this flow.
     */
    data class AwaitingConsent(
        val isSubmitting: Boolean = false,
        val error: String? = null
    ) : FaceRegistrationUiState

    /**
     * Reached only after [FaceRegistrationViewModel.affirmConsent] has
     * successfully called `recordConsent()` (R13.2). [capturedEmbeddings] is
     * one `List<Double>` per captured face "shot", generated entirely
     * on-device (R13.3) -- no image data is ever held here.
     *
     * [error] carries a failed registration attempt's `AppError` message
     * (R13.5); staying in this same state on failure is what lets the
     * Student retry without being routed back through [AwaitingConsent].
     * [captureError] is a separate, transient message for a capture "shot"
     * that found no face at all.
     */
    data class Capturing(
        val capturedEmbeddings: List<List<Double>> = emptyList(),
        val isSubmitting: Boolean = false,
        val error: String? = null,
        val captureError: String? = null
    ) : FaceRegistrationUiState

    /**
     * R13.4: a confirmation state naming the number of embeddings the
     * Face_Profile_Service actually registered.
     */
    data class Registered(
        val embeddingCount: Int
    ) : FaceRegistrationUiState
}
