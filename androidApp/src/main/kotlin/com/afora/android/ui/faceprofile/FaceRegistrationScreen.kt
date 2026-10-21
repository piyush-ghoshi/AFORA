package com.afora.android.ui.faceprofile

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.afora.android.ui.components.AforaCard
import com.afora.android.ui.components.InfoBanner
import com.afora.android.ui.theme.StatusAbsent
import com.afora.android.ui.theme.StatusAbsentContainer
import com.afora.android.ui.theme.StatusLeave
import com.afora.android.ui.theme.StatusLeaveContainer
import com.afora.android.ui.theme.StatusPresent
import com.afora.android.ui.theme.StatusPresentContainer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

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
    modifier: Modifier = Modifier,
    onResetShots: () -> Unit = {}
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
                .verticalScroll(rememberScrollState())
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
                    onRegister = onRegister,
                    onResetShots = onResetShots
                )
                is FaceRegistrationUiState.Registered -> RegisteredSection(
                    state = uiState,
                    onDone = onBack
                )
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
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
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
 * R13.3-R13.5: live CameraX front camera viewfinder with ML Kit face detection,
 * capture controls with visual flash feedback, shot progress indicator, and register action.
 */
@Composable
private fun CaptureSection(
    state: FaceRegistrationUiState.Capturing,
    onCaptureShot: () -> Unit,
    onRegister: () -> Unit,
    onResetShots: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val shotCount = state.capturedEmbeddings.size

    AforaCard(modifier = Modifier.fillMaxWidth(), barColor = MaterialTheme.colorScheme.primary) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Capture Face Biometrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Align your face inside the frame. Take up to 4 shots at slight angle variations for maximum recognition accuracy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (!hasCameraPermission) {
        AforaCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "AFORA needs camera access to capture your face for biometric attendance. " +
                        "Images are processed directly on your device — no raw photos are ever saved or uploaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Grant Camera Permission", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onCaptureShot,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Capture Without Camera (Test Mode)")
                }
            }
        }
    } else {
        var isFaceDetected by remember { mutableStateOf(false) }
        var isFlashing by remember { mutableStateOf(false) }

        val detector = remember {
            FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .build()
            )
        }
        val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

        DisposableEffect(Unit) {
            onDispose {
                detector.close()
                cameraExecutor.shutdown()
            }
        }

        // CameraX Front Viewfinder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                                detector.process(inputImage)
                                    .addOnSuccessListener { faces ->
                                        isFaceDetected = faces.isNotEmpty()
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Oval face guide frame
            Box(
                modifier = Modifier
                    .size(width = 210.dp, height = 270.dp)
                    .border(
                        width = 3.dp,
                        color = if (isFaceDetected) Color(0xFF22C55E) else Color.White.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(110.dp)
                    )
            )

            // Floating status chip at the top of viewfinder
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFaceDetected) Color(0xCC15803D) else Color(0x99000000)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isFaceDetected) Color(0xFF4ADE80) else Color(0xFFFBBF24))
                    )
                    Text(
                        text = if (isFaceDetected) "Face Detected - Hold Steady" else "Position Face in Oval",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Shutter flash feedback
            AnimatedVisibility(
                visible = isFlashing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.8f))
                )
            }
        }

        // Dynamic Tip text based on shots captured
        val tipText = when (shotCount) {
            0 -> "Shot 1 of 4: Look straight ahead with neutral expression"
            1 -> "Shot 2 of 4: Turn head slightly to the left"
            2 -> "Shot 3 of 4: Turn head slightly to the right"
            3 -> "Shot 4 of 4: Tilt head slightly up or smile"
            else -> "4/4 shots captured! Ready to register face profile."
        }

        Text(
            text = tipText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )

        // 4 Shot indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..4) {
                val isCaptured = i <= shotCount
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCaptured) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCaptured) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Shot $i captured",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "$i",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Circular Shutter Capture Button
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        isFlashing = true
                        delay(120)
                        isFlashing = false
                    }
                    onCaptureShot()
                },
                enabled = !state.isSubmitting && shotCount < 4,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (!state.isSubmitting && shotCount < 4) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Capture Shot",
                    tint = if (!state.isSubmitting && shotCount < 4) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }

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

    if (shotCount > 0) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(
                onClick = onResetShots,
                enabled = !state.isSubmitting
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Retake All Shots")
            }
        }
    }

    Button(
        onClick = onRegister,
        enabled = !state.isSubmitting && shotCount > 0,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        if (state.isSubmitting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(
                text = "Register Face Profile ($shotCount ${if (shotCount == 1) "shot" else "shots"})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/** R13.4: confirmation naming the embedding count the server actually stored. */
@Composable
private fun RegisteredSection(
    state: FaceRegistrationUiState.Registered,
    onDone: () -> Unit = {}
) {
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

    Spacer(Modifier.height(8.dp))

    Button(
        onClick = onDone,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text("Done", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}
