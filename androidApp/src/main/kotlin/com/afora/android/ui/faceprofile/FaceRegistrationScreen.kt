package com.afora.android.ui.faceprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.afora.android.ui.components.AforaCard
import com.afora.android.ui.components.InfoBanner
import com.afora.android.ui.theme.StatusAbsent
import com.afora.android.ui.theme.StatusAbsentContainer
import com.afora.android.ui.theme.StatusLeave
import com.afora.android.ui.theme.StatusLeaveContainer
import com.afora.android.ui.theme.StatusPresent
import com.afora.android.ui.theme.StatusPresentContainer

/**
 * Face_Registration_Screen (`.kiro/specs/face-recognition/design.md` -> "Registration
 * sequence"; requirements.md R1.6, R2, R13).
 *
 * R1.6/R13.1: the consent affirmation is presented first and is the only
 * control available in [FaceRegistrationUiState.AwaitingConsent] -- the
 * camera capture control only exists once [uiState] has moved on to
 * [FaceRegistrationUiState.Capturing], which only happens after
 * `recordConsent()` has succeeded (R13.2).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRegistrationScreen(
    uiState: FaceRegistrationUiState,
    onBack: () -> Unit,
    onAffirmConsent: () -> Unit,
    onCaptureShot: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Face Registration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState) {
                is FaceRegistrationUiState.AwaitingConsent -> ConsentSection(
                    state = uiState,
                    onAffirmConsent = onAffirmConsent
                )
                is FaceRegistrationUiState.Capturing -> CaptureSection(
                    state = uiState,
                    onCaptureShot = onCaptureShot,
                    onRegister = onRegister
                )
                is FaceRegistrationUiState.Registered -> RegisteredSection(state = uiState)
            }
        }
    }
}

/** R1.6, R13.1-R13.2: consent must be affirmed before capture is reachable at all. */
@Composable
private fun ConsentSection(
    state: FaceRegistrationUiState.AwaitingConsent,
    onAffirmConsent: () -> Unit
) {
    AforaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Face Registration Consent",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "To use face recognition for attendance, you must consent to " +
                    "registering a face profile. Your device will generate a numeric " +
                    "representation of your face (an embedding) -- no photo or image is " +
                    "ever stored or sent to the server.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (state.error != null) {
        InfoBanner(
            text = state.error,
            containerColor = StatusAbsentContainer,
            contentColor = StatusAbsent,
            icon = Icons.Filled.Warning
        )
    }

    Button(
        onClick = onAffirmConsent,
        enabled = !state.isSubmitting,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        if (state.isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text("I Consent, Continue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * R13.3-R13.5: one capture control per "shot" plus a running count, and a
 * register action. A registration failure stays here with [state.error]
 * shown -- retry never re-affirms consent (R13.5).
 */
@Composable
private fun CaptureSection(
    state: FaceRegistrationUiState.Capturing,
    onCaptureShot: () -> Unit,
    onRegister: () -> Unit
) {
    AforaCard(modifier = Modifier.fillMaxWidth(), barColor = MaterialTheme.colorScheme.primary) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Capture Face",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Capture one or more shots, then register your face profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    InfoBanner(
        text = "${state.capturedEmbeddings.size} shot(s) captured",
        containerColor = StatusLeaveContainer,
        contentColor = StatusLeave
    )

    if (state.captureError != null) {
        InfoBanner(
            text = state.captureError,
            containerColor = StatusAbsentContainer,
            contentColor = StatusAbsent,
            icon = Icons.Filled.Warning
        )
    }

    if (state.error != null) {
        InfoBanner(
            text = state.error,
            containerColor = StatusAbsentContainer,
            contentColor = StatusAbsent,
            icon = Icons.Filled.Warning
        )
    }

    OutlinedButton(
        onClick = onCaptureShot,
        enabled = !state.isSubmitting,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Icon(Icons.Filled.CameraAlt, contentDescription = null)
        Text(
            text = "  Capture Shot",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }

    Button(
        onClick = onRegister,
        enabled = !state.isSubmitting && state.capturedEmbeddings.isNotEmpty(),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        if (state.isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = "Register Face Profile (${state.capturedEmbeddings.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** R13.4: confirmation naming the embedding count the server actually stored. */
@Composable
private fun RegisteredSection(state: FaceRegistrationUiState.Registered) {
    AforaCard(modifier = Modifier.fillMaxWidth(), barColor = StatusPresent) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = StatusPresent,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Face Profile Registered",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${state.embeddingCount} embedding(s) stored.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    InfoBanner(
        text = "You can now be recognized for camera-based attendance.",
        containerColor = StatusPresentContainer,
        contentColor = StatusPresent,
        icon = Icons.Filled.CheckCircle
    )
}
