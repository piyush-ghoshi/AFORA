package com.afora.android.ui.attendance.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.RectF
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.afora.android.ui.components.ErrorBox
import com.afora.android.ui.components.LoadingBox
import com.afora.android.ui.components.MonoTextStyle
import com.afora.android.ui.components.StatusChip
import com.afora.android.ui.theme.StatusAbsent
import com.afora.android.ui.theme.StatusAbsentContainer
import com.afora.android.ui.theme.StatusLate
import com.afora.android.ui.theme.StatusLateContainer
import com.afora.android.ui.theme.StatusLeave
import com.afora.android.ui.theme.StatusLeaveContainer
import com.afora.android.ui.theme.StatusPresent
import com.afora.android.ui.theme.StatusPresentContainer
import com.afora.shared.data.api.RosterEntry
import com.afora.shared.domain.model.AttendanceStatus
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraAttendanceScreen(
    uiState: CameraAttendanceUiState,
    onBack: () -> Unit,
    onSwitchToManual: () -> Unit,
    onFaceDetections: (List<FaceOverlayItem>, List<DetectedFaceCandidate>) -> Unit,
    onToggleTorch: () -> Unit,
    onToggleScanning: () -> Unit,
    onSetReviewMode: (Boolean) -> Unit,
    onUpdateCandidateStatus: (Long, AttendanceStatus) -> Unit,
    onConfirmAttendance: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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

    when (uiState) {
        is CameraAttendanceUiState.Loading -> LoadingBox(modifier = modifier)
        is CameraAttendanceUiState.Error -> ErrorBox(
            message = uiState.message,
            onRetry = onRetry,
            modifier = modifier
        )
        is CameraAttendanceUiState.Ready -> {
            if (!hasCameraPermission) {
                CameraPermissionDeniedView(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onSwitchToManual = onSwitchToManual,
                    modifier = modifier
                )
            } else {
                CameraScannerContent(
                    state = uiState,
                    onBack = onBack,
                    onSwitchToManual = onSwitchToManual,
                    onFaceDetections = onFaceDetections,
                    onToggleTorch = onToggleTorch,
                    onToggleScanning = onToggleScanning,
                    onOpenReview = { onSetReviewMode(true) },
                    modifier = modifier
                )

                if (uiState.isReviewing) {
                    CameraAttendanceReviewSheet(
                        state = uiState,
                        onDismiss = { onSetReviewMode(false) },
                        onUpdateStatus = onUpdateCandidateStatus,
                        onConfirm = onConfirmAttendance,
                        onBackToDashboard = onBack
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionDeniedView(
    onRequestPermission: () -> Unit,
    onSwitchToManual: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Camera Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "AFORA needs camera access to perform real-time classroom attendance scanning and multi-frame face recognition.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Camera Permission")
                }
                OutlinedButton(
                    onClick = onSwitchToManual,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use Manual Roster Instead", color = Color(0xFF38BDF8))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraScannerContent(
    state: CameraAttendanceUiState.Ready,
    onBack: () -> Unit,
    onSwitchToManual: () -> Unit,
    onFaceDetections: (List<FaceOverlayItem>, List<DetectedFaceCandidate>) -> Unit,
    onToggleTorch: () -> Unit,
    onToggleScanning: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraControl: Camera? by remember { mutableStateOf(null) }

    LaunchedEffect(state.isTorchOn) {
        cameraControl?.cameraControl?.enableTorch(state.isTorchOn)
    }

    val analyzer = remember {
        FaceDetectionAnalyzer(
            enrolledStudents = state.enrolledStudents,
            onDetections = onFaceDetections
        )
    }

    LaunchedEffect(state.enrolledStudents) {
        analyzer.updateRoster(state.enrolledStudents)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                val executor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor, analyzer)

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraControl = cameraProvider.bindToLifecycle(
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
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bounding Box Overlay
        LiveFaceBoundingBoxOverlay(
            faces = state.liveOverlayFaces,
            modifier = Modifier.fillMaxSize()
        )

        // Top Navigation & Stats Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xCC0F172A))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column {
                        Text(
                            text = "${state.roster.subjectCode} • ${state.roster.subjectName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Live Classroom Scanner",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Live Pulsing Indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.isScanning) Color(0xFFDC2626) else Color(0xFF64748B),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        Text(
                            text = if (state.isScanning) "LIVE" else "PAUSED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.detectedCount} of ${state.totalEnrolled} Enrolled Detected",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF38BDF8)
                )
                Text(
                    text = "${(state.detectedCount.toFloat() / state.totalEnrolled.coerceAtLeast(1) * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = (state.detectedCount.toFloat() / state.totalEnrolled.coerceAtLeast(1)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF10B981),
                trackColor = Color(0xFF334155)
            )
        }

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC0F172A))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Torch toggle
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (state.isTorchOn) Color(0xFFF59E0B) else Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = if (state.isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Torch",
                        tint = Color.White
                    )
                }

                // Pause/Resume toggle
                IconButton(
                    onClick = onToggleScanning,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = if (state.isScanning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Pause",
                        tint = Color.White
                    )
                }

                // Manual roster button
                IconButton(
                    onClick = onSwitchToManual,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = "Manual Roster",
                        tint = Color(0xFF38BDF8)
                    )
                }
            }

            // Review Attendance Button
            Button(
                onClick = onOpenReview,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Filled.RateReview, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Review (${state.detectedCount})",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Draws real-time bounding boxes around detected faces on canvas.
 */
@Composable
private fun LiveFaceBoundingBoxOverlay(
    faces: List<FaceOverlayItem>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (face in faces) {
            val box = face.boundingBox
            val left = box.left * canvasWidth
            val top = box.top * canvasHeight
            val right = box.right * canvasWidth
            val bottom = box.bottom * canvasHeight

            val strokeColor = when (face.category) {
                ConfidenceCategory.HIGH -> Color(0xFF10B981)   // Green
                ConfidenceCategory.MEDIUM -> Color(0xFFF59E0B) // Amber
                ConfidenceCategory.LOW -> Color(0xFFF97316)    // Orange
                ConfidenceCategory.UNKNOWN -> Color(0xFF38BDF8)// Cyan
            }

            // Draw bounding rectangle
            drawRect(
                color = strokeColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )

            // Draw text tag
            val labelText = if (face.matchedStudentName != null) {
                "${face.matchedStudentName} (${(face.confidence * 100).toInt()}%)"
            } else {
                "Unknown (${(face.confidence * 100).toInt()}%)"
            }

            drawContext.canvas.nativeCanvas.apply {
                val paint = Paint().apply {
                    color = strokeColor.toArgb()
                    textSize = 34f
                    isFakeBoldText = true
                }
                drawText(labelText, left, (top - 12f).coerceAtLeast(36f), paint)
            }
        }
    }
}

/**
 * Bottom Sheet for reviewing and confirming camera attendance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraAttendanceReviewSheet(
    state: CameraAttendanceUiState.Ready,
    onDismiss: () -> Unit,
    onUpdateStatus: (Long, AttendanceStatus) -> Unit,
    onConfirm: () -> Unit,
    onBackToDashboard: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Attendance Review",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${state.roster.subjectCode} • ${state.totalEnrolled} Students Enrolled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Summary Tally
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReviewStatPill(
                    label = "Present",
                    count = state.candidates.values.count { it.attendanceStatus == AttendanceStatus.PRESENT },
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                ReviewStatPill(
                    label = "Review",
                    count = state.reviewNeededCount,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                ReviewStatPill(
                    label = "Absent",
                    count = state.totalEnrolled - state.candidates.values.count { it.attendanceStatus == AttendanceStatus.PRESENT || it.attendanceStatus == AttendanceStatus.ON_LEAVE },
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }

            // Success or Error banner
            if (state.confirmation != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusPresentContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusPresent)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Camera Attendance Confirmed!",
                                fontWeight = FontWeight.Bold,
                                color = StatusPresent
                            )
                        }
                        Text(
                            text = "${state.confirmation.totalMarked} students recorded (${state.confirmation.presentCount} Present, ${state.confirmation.absentCount} Absent).",
                            color = StatusPresent
                        )
                        Button(
                            onClick = onBackToDashboard,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusPresent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Return to Dashboard", color = Color.White)
                        }
                    }
                }
            } else if (state.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFCA5A5))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.errorMessage,
                            color = Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Student Review List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.enrolledStudents) { student ->
                    val candidate = state.candidates[student.studentId]
                    ReviewStudentCard(
                        student = student,
                        candidate = candidate,
                        onStatusSelected = { status ->
                            onUpdateStatus(student.studentId, status)
                        }
                    )
                }
            }

            // Confirmation Button
            if (state.confirmation == null) {
                Button(
                    onClick = onConfirm,
                    enabled = !state.isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Confirm Camera Attendance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStatPill(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
private fun ReviewStudentCard(
    student: RosterEntry,
    candidate: DetectedFaceCandidate?,
    onStatusSelected: (AttendanceStatus) -> Unit
) {
    val currentStatus = candidate?.attendanceStatus ?: AttendanceStatus.ABSENT

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = student.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (candidate != null) {
                        Surface(
                            color = if (candidate.category == ConfidenceCategory.HIGH) Color(0xFF065F46) else Color(0xFF92400E),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${(candidate.confidence * 100).toInt()}% match",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (candidate.category == ConfidenceCategory.HIGH) Color(0xFF6EE7B7) else Color(0xFFFDE68A),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = student.rollNumber,
                    style = MonoTextStyle.copy(fontSize = 12.sp),
                    color = Color(0xFF94A3B8)
                )
            }

            // Quick Status Selector
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ReviewStatusChip(
                    text = "P",
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    color = StatusPresent,
                    onClick = { onStatusSelected(AttendanceStatus.PRESENT) }
                )
                ReviewStatusChip(
                    text = "A",
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    color = StatusAbsent,
                    onClick = { onStatusSelected(AttendanceStatus.ABSENT) }
                )
                ReviewStatusChip(
                    text = "L",
                    isSelected = currentStatus == AttendanceStatus.LATE,
                    color = StatusLate,
                    onClick = { onStatusSelected(AttendanceStatus.LATE) }
                )
            }
        }
    }
}

@Composable
private fun ReviewStatusChip(
    text: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) color else Color(0xFF334155),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .size(32.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontSize = 13.sp
            )
        }
    }
}
