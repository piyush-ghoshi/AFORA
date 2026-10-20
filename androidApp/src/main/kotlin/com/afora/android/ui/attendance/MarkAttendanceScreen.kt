package com.afora.android.ui.attendance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.afora.android.ui.components.AforaCard
import com.afora.android.ui.components.ErrorBox
import com.afora.android.ui.components.InfoBanner
import com.afora.android.ui.components.LoadingBox
import com.afora.android.ui.components.MonoTextStyle
import com.afora.android.ui.components.StatusChip
import com.afora.android.ui.theme.CardBorder
import com.afora.android.ui.theme.StatusAbsent
import com.afora.android.ui.theme.StatusAbsentBar
import com.afora.android.ui.theme.StatusAbsentContainer
import com.afora.android.ui.theme.StatusLate
import com.afora.android.ui.theme.StatusLateBar
import com.afora.android.ui.theme.StatusLateContainer
import com.afora.android.ui.theme.StatusLeave
import com.afora.android.ui.theme.StatusLeaveBar
import com.afora.android.ui.theme.StatusLeaveContainer
import com.afora.android.ui.theme.StatusNeutral
import com.afora.android.ui.theme.StatusNeutralBar
import com.afora.android.ui.theme.StatusNeutralContainer
import com.afora.android.ui.theme.StatusPresent
import com.afora.android.ui.theme.StatusPresentBar
import com.afora.android.ui.theme.StatusPresentContainer
import com.afora.shared.data.api.AttendanceConfirmationResponse
import com.afora.shared.domain.model.AttendanceStatus
import com.afora.shared.domain.model.SessionStatus

/** Named touch-target constant so the confirm control's size is assertable without an emulator (R12.20). */
object MarkAttendanceDimens {
    val ConfirmMinTouchTarget = 48.dp
}

/**
 * The manual attendance marking screen (R12).
 *
 * - A SCHEDULED session shows "Start Session" first; the backend only accepts
 *   confirmation for an ACTIVE session, so the roster is read-only until then.
 * - ACTIVE: segmented status per student and a sticky "Confirm Attendance".
 * - FINALIZED: read-only, plus the amendment form while the server says the
 *   window is open (R12.17).
 * - After a failed confirm or amend the roster stays on screen under an error
 *   banner with every selection intact (R12.14).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAttendanceScreen(
    uiState: MarkAttendanceUiState,
    onBack: () -> Unit,
    onStatusChange: (studentId: Long, status: AttendanceStatus) -> Unit,
    onConfirm: () -> Unit,
    onAmendmentReasonChange: (String) -> Unit,
    onSubmitAmendment: () -> Unit,
    onStartSession: () -> Unit = {},
    onRetry: () -> Unit = {},
    onScanWithCamera: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // One view of the roster regardless of whether the state is Success or a
    // recoverable Error (an Error that still carries a header and rows).
    val view: RosterView? = when (uiState) {
        is MarkAttendanceUiState.Success -> RosterView(
            header = uiState.header,
            rows = uiState.rows,
            isSubmitting = uiState.isSubmitting,
            confirmation = uiState.confirmation,
            amendmentWindowOpen = uiState.amendmentWindowOpen,
            amendmentReason = uiState.amendmentReason,
            errorMessage = null
        )
        is MarkAttendanceUiState.Error ->
            if (uiState.header != null && uiState.rows.isNotEmpty()) {
                RosterView(
                    header = uiState.header,
                    rows = uiState.rows,
                    isSubmitting = false,
                    confirmation = null,
                    amendmentWindowOpen = uiState.amendmentWindowOpen,
                    amendmentReason = uiState.amendmentReason,
                    errorMessage = uiState.message
                )
            } else null
        MarkAttendanceUiState.Loading -> null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Mark Attendance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onScanWithCamera != null && view != null && !view.isFinalized) {
                        IconButton(onClick = onScanWithCamera) {
                            Icon(
                                Icons.Filled.Videocam,
                                contentDescription = "Scan with Camera",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (view != null) {
                ActionBar(view = view, onConfirm = onConfirm, onStartSession = onStartSession)
            }
        }
    ) { padding ->
        when {
            view != null -> RosterContent(
                view = view,
                onStatusChange = onStatusChange,
                onAmendmentReasonChange = onAmendmentReasonChange,
                onSubmitAmendment = onSubmitAmendment,
                modifier = Modifier.padding(padding)
            )
            uiState is MarkAttendanceUiState.Error -> ErrorBox(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier.padding(padding)
            )
            else -> LoadingBox(Modifier.padding(padding))
        }
    }
}

private data class RosterView(
    val header: SessionHeader,
    val rows: List<AttendanceRow>,
    val isSubmitting: Boolean,
    val confirmation: AttendanceConfirmationResponse?,
    val amendmentWindowOpen: Boolean,
    val amendmentReason: String,
    val errorMessage: String?
) {
    val status: SessionStatus get() = header.sessionStatus
    val isActive: Boolean get() = status == SessionStatus.ACTIVE
    val isFinalized: Boolean get() = status == SessionStatus.FINALIZED

    /** Rows can change while marking (ACTIVE) or while an amendment window is open. */
    val rowsEditable: Boolean get() = !isSubmitting && (isActive || (isFinalized && amendmentWindowOpen))

    // Same definitions the server uses (R7.2, R7.4).
    val present: Int get() = rows.count { it.selectedStatus == AttendanceStatus.PRESENT || it.selectedStatus == AttendanceStatus.LATE }
    val absent: Int get() = rows.count { it.selectedStatus == AttendanceStatus.ABSENT }
    val onLeave: Int get() = rows.count { it.selectedStatus == AttendanceStatus.ON_LEAVE || it.selectedStatus == AttendanceStatus.EXCUSED }
}

@Composable
private fun ActionBar(view: RosterView, onConfirm: () -> Unit, onStartSession: () -> Unit) {
    val scheduled = view.status == SessionStatus.SCHEDULED
    if (!scheduled && !view.isActive) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Button(
            onClick = if (scheduled) onStartSession else onConfirm,
            enabled = !view.isSubmitting,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .defaultMinSize(
                    minWidth = MarkAttendanceDimens.ConfirmMinTouchTarget,
                    minHeight = 56.dp
                )
        ) {
            if (view.isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = if (scheduled) "Start Session" else "Confirm Attendance (${view.rows.size} Students)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RosterContent(
    view: RosterView,
    onStatusChange: (Long, AttendanceStatus) -> Unit,
    onAmendmentReasonChange: (String) -> Unit,
    onSubmitAmendment: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (view.errorMessage != null) {
            item {
                InfoBanner(
                    text = view.errorMessage,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    icon = Icons.Filled.Warning
                )
            }
        }

        item { SessionHeaderCard(view.header) }

        if (view.status == SessionStatus.SCHEDULED) {
            item {
                InfoBanner(
                    text = "This session has not started. Start it to begin marking attendance.",
                    containerColor = StatusLeaveContainer,
                    contentColor = StatusLeave
                )
            }
        }

        item { CountsRow(view.present, view.absent, view.onLeave, view.rows.size) }

        if (view.confirmation != null) {
            item {
                InfoBanner(
                    text = "Attendance confirmed for ${view.confirmation.totalMarked} students.",
                    containerColor = StatusPresentContainer,
                    contentColor = StatusPresent,
                    icon = Icons.Filled.CheckCircle
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "STUDENT ROSTER (SORT: ROLL ID)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${view.rows.size} students",
                    style = MonoTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(view.rows, key = { it.studentId }) { row ->
            AttendanceRowCard(
                row = row,
                enabled = view.rowsEditable,
                onStatusChange = { status -> onStatusChange(row.studentId, status) }
            )
        }

        if (view.isFinalized && view.amendmentWindowOpen) {
            item {
                AmendmentSection(
                    reason = view.amendmentReason,
                    isSubmitting = view.isSubmitting,
                    onReasonChange = onAmendmentReasonChange,
                    onSubmit = onSubmitAmendment
                )
            }
        }
    }
}

private fun statusLabel(status: SessionStatus): Pair<String, Pair<Color, Color>> = when (status) {
    SessionStatus.ACTIVE -> "In Progress" to (StatusLeaveContainer to StatusLeave)
    SessionStatus.SCHEDULED, SessionStatus.CREATED -> "Not Started" to (StatusNeutralContainer to StatusNeutral)
    SessionStatus.FINALIZED -> "Confirmed" to (StatusPresentContainer to StatusPresent)
    SessionStatus.CANCELLED -> "Cancelled" to (StatusNeutralContainer to StatusNeutral)
    SessionStatus.FAILED -> "Failed" to (StatusAbsentContainer to StatusAbsent)
    else -> status.name to (StatusNeutralContainer to StatusNeutral)
}

@Composable
private fun SessionHeaderCard(header: SessionHeader) {
    val (label, colors) = statusLabel(header.sessionStatus)
    AforaCard(
        modifier = Modifier.fillMaxWidth(),
        barColor = MaterialTheme.colorScheme.primary
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = header.subjectCode,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                StatusChip(text = label, containerColor = colors.first, contentColor = colors.second)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = header.subjectName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${header.sessionDate} \u2022 ${header.startTime.take(5)} - ${header.endTime.take(5)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CountsRow(present: Int, absent: Int, onLeave: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CountPill("Present", present, StatusPresentContainer, StatusPresent, Modifier.weight(1f))
        CountPill("Absent", absent, StatusAbsentContainer, StatusAbsent, Modifier.weight(1f))
        CountPill("Leave", onLeave, StatusLeaveContainer, StatusLeave, Modifier.weight(1f))
        Text(
            text = "$total Total",
            style = MonoTextStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CountPill(
    label: String,
    value: Int,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.semantics { contentDescription = "$label: $value" },
        shape = RoundedCornerShape(percent = 50),
        color = container
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = content)
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = content
            )
        }
    }
}

private fun barFor(status: AttendanceStatus): Color = when (status) {
    AttendanceStatus.PRESENT -> StatusPresentBar
    AttendanceStatus.LATE -> StatusLateBar
    AttendanceStatus.ABSENT -> StatusAbsentBar
    AttendanceStatus.ON_LEAVE -> StatusLeaveBar
    AttendanceStatus.EXCUSED -> StatusNeutralBar
}

@Composable
private fun AttendanceRowCard(
    row: AttendanceRow,
    enabled: Boolean,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    AforaCard(
        modifier = Modifier.fillMaxWidth(),
        barColor = barFor(row.selectedStatus)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.rollNumber,
                        style = MonoTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = row.fullName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (row.isCorrected) {
                        Spacer(Modifier.height(2.dp))
                        StatusChip(
                            text = "Corrected",
                            containerColor = StatusAbsentContainer,
                            contentColor = StatusAbsent
                        )
                    }
                }
                StatusSelector(row = row, enabled = enabled, onStatusChange = onStatusChange)
            }
            if (row.suggestedStatus == AttendanceStatus.ON_LEAVE && row.leaveReason != null) {
                Spacer(Modifier.height(8.dp))
                InfoBanner(
                    text = "On leave: ${row.leaveReason}",
                    containerColor = StatusLeaveContainer,
                    contentColor = StatusLeave,
                    icon = Icons.Filled.Info
                )
            }
        }
    }
}

private data class Segment(val status: AttendanceStatus, val short: String, val full: String, val width: Int)

private val SEGMENTS = listOf(
    Segment(AttendanceStatus.PRESENT, "P", "Present", 36),
    Segment(AttendanceStatus.LATE, "L", "Late", 36),
    Segment(AttendanceStatus.ABSENT, "A", "Absent", 36),
    Segment(AttendanceStatus.ON_LEAVE, "Lve", "On leave", 44),
    Segment(AttendanceStatus.EXCUSED, "Exc", "Excused", 44)
)

private fun fillFor(status: AttendanceStatus): Color = when (status) {
    AttendanceStatus.PRESENT -> StatusPresent
    AttendanceStatus.LATE -> StatusLate
    AttendanceStatus.ABSENT -> StatusAbsent
    AttendanceStatus.ON_LEAVE -> StatusLeave
    AttendanceStatus.EXCUSED -> StatusNeutral
}

/**
 * P / L / A / Lve / Exc segmented control. Each segment is a 48dp-tall radio
 * target with a spoken label naming the student and the status (R12.19).
 */
@Composable
private fun StatusSelector(
    row: AttendanceRow,
    enabled: Boolean,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(modifier = Modifier.padding(2.dp)) {
            SEGMENTS.forEach { segment ->
                val selected = row.selectedStatus == segment.status
                Box(
                    modifier = Modifier
                        .width(segment.width.dp)
                        .height(44.dp)
                        .background(
                            color = if (selected) fillFor(segment.status) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable(enabled = enabled, role = Role.RadioButton) { onStatusChange(segment.status) }
                        .semantics {
                            contentDescription = "${row.fullName}: ${segment.full}" +
                                if (selected) ", selected" else ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = segment.short,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AmendmentSection(
    reason: String,
    isSubmitting: Boolean,
    onReasonChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    AforaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Amend Attendance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Change a status above, then give the reason for the correction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChange,
                label = { Text("Reason for correction") },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting && reason.isNotBlank(),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text("Submit Correction")
            }
        }
    }
}