package com.afora.android.ui.attendance

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afora.android.ui.components.AforaCard
import com.afora.android.ui.components.AttendanceMath
import com.afora.android.ui.components.ErrorBox
import com.afora.android.ui.components.InfoBanner
import com.afora.android.ui.components.LoadingBox
import com.afora.android.ui.components.MonoTextStyle
import com.afora.android.ui.components.StatusChip
import com.afora.android.ui.theme.CardBorder
import com.afora.android.ui.theme.StatusAbsent
import com.afora.android.ui.theme.StatusAbsentContainer
import com.afora.android.ui.theme.StatusLate
import com.afora.android.ui.theme.StatusLateContainer
import com.afora.android.ui.theme.StatusLeave
import com.afora.android.ui.theme.StatusLeaveContainer
import com.afora.android.ui.theme.StatusNeutral
import com.afora.android.ui.theme.StatusNeutralContainer
import com.afora.android.ui.theme.StatusPresent
import com.afora.android.ui.theme.StatusPresentContainer
import com.afora.shared.data.api.SessionAttendanceEntry
import com.afora.shared.data.api.StudentAttendanceResponse
import com.afora.shared.data.api.SubjectAttendanceSummary
import com.afora.shared.domain.model.AttendanceStatus

/**
 * Student attendance detail screen (Phase 4).
 *
 * Shows an overall attendance ring, then an accordion list of subjects.
 * Expanding a subject reveals the per-session attendance records.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAttendanceScreen(
    uiState: StudentAttendanceUiState,
    onBack: () -> Unit,
    onToggleSubject: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("My Attendance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (uiState) {
            is StudentAttendanceUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is StudentAttendanceUiState.Error   -> ErrorBox(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier.padding(padding)
            )
            is StudentAttendanceUiState.Success -> AttendanceContent(
                response = uiState.response,
                expandedSubjectId = uiState.expandedSubjectId,
                onToggleSubject = onToggleSubject,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun AttendanceContent(
    response: StudentAttendanceResponse,
    expandedSubjectId: Long?,
    onToggleSubject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { OverallCard(response) }

        item {
            Text(
                text = "SUBJECT BREAKDOWN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (response.subjects.isEmpty()) {
            item {
                AforaCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No attendance records for this semester.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(response.subjects, key = { it.subjectId }) { subject ->
                SubjectAccordion(
                    subject = subject,
                    expanded = subject.subjectId == expandedSubjectId,
                    onToggle = { onToggleSubject(subject.subjectId) }
                )
            }
        }
    }
}

@Composable
private fun OverallCard(response: StudentAttendanceResponse) {
    val good = AttendanceMath.isGoodStanding(response.overallPercentage)
    val color = when {
        response.totalLectures == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
        good                        -> StatusPresent
        response.overallPercentage >= 60 -> StatusLate
        else                        -> StatusAbsent
    }

    AforaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Overall Attendance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Big percentage text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (response.totalLectures > 0)
                            "${"%.1f".format(response.overallPercentage)}%"
                        else "--",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = when {
                            response.totalLectures == 0 -> "No classes yet"
                            good                        -> "Good Standing"
                            else                        -> "Below Minimum"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = color,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Stats column
                Column(horizontalAlignment = Alignment.End) {
                    OverallStat("Present",  response.presentCount,  StatusPresent)
                    OverallStat("Absent",   response.absentCount,   StatusAbsent)
                    OverallStat("On Leave", response.onLeaveCount,  StatusLeave)
                    OverallStat("Total",    response.totalLectures, MaterialTheme.colorScheme.onSurface)
                }
            }
            if (response.totalLectures > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = (response.overallPercentage / 100.0).toFloat().coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(percent = 50)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(Modifier.height(8.dp))
                val minimum = "%.1f".format(AttendanceMath.THRESHOLD_PERCENT)
                if (good) {
                    val spare = AttendanceMath.absencesRemaining(response.presentCount, response.totalLectures)
                    InfoBanner(
                        text = "Minimum: $minimum%. You can miss $spare more ${if (spare == 1) "class" else "classes"}.",
                        containerColor = StatusPresentContainer,
                        contentColor = StatusPresent,
                        icon = Icons.Filled.CheckCircle
                    )
                } else {
                    val needed = AttendanceMath.classesNeeded(response.presentCount, response.totalLectures)
                    InfoBanner(
                        text = "Minimum: $minimum%. Attend the next $needed ${if (needed == 1) "class" else "classes"} in a row to recover.",
                        containerColor = StatusAbsentContainer,
                        contentColor = StatusAbsent,
                        icon = Icons.Filled.Info
                    )
                }
            }
        }
    }
}

@Composable
private fun OverallStat(label: String, value: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            style = MonoTextStyle,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun SubjectAccordion(
    subject: SubjectAttendanceSummary,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val pct = subject.attendancePercentage
    val good = AttendanceMath.isGoodStanding(pct)
    val barColor = when {
        subject.totalLectures == 0 -> MaterialTheme.colorScheme.primary
        good                       -> StatusPresent
        pct >= 60                  -> StatusLate
        else                       -> StatusAbsent
    }

    AforaCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${subject.subjectName}: ${
                    if (subject.totalLectures > 0) "${"%.1f".format(pct)}%" else "no data"
                }, ${if (expanded) "expanded" else "collapsed"}"
            },
        barColor = barColor,
        onClick = onToggle
    ) {
        Column {
            // Header row (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.subjectCode,
                        style = MonoTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subject.subjectName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniChip("P ${subject.presentCount}", StatusPresentContainer, StatusPresent)
                        MiniChip("A ${subject.absentCount}", StatusAbsentContainer, StatusAbsent)
                        if (subject.onLeaveCount > 0) {
                            MiniChip("L ${subject.onLeaveCount}", StatusLeaveContainer, StatusLeave)
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (subject.totalLectures > 0) "${"%.1f".format(pct)}%" else "--",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Session list (collapsible)
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp),
                        color = CardBorder
                    ) {}
                    Spacer(Modifier.height(4.dp))
                    if (subject.sessions.isEmpty()) {
                        Text(
                            text = "No session records yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        subject.sessions.forEach { session ->
                            SessionRow(session)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionAttendanceEntry) {
    val (chipContainer, chipContent) = statusColors(session.status)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.sessionDate,
                style = MonoTextStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${session.startTime.take(5)} – ${session.endTime.take(5)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (session.isCorrected) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "Corrected",
                modifier = Modifier.size(14.dp),
                tint = StatusAbsent
            )
        }
        StatusChip(
            text = statusLabel(session.status),
            containerColor = chipContainer,
            contentColor = chipContent
        )
    }
}

private fun statusLabel(status: AttendanceStatus): String = when (status) {
    AttendanceStatus.PRESENT  -> "Present"
    AttendanceStatus.LATE     -> "Late"
    AttendanceStatus.ABSENT   -> "Absent"
    AttendanceStatus.ON_LEAVE -> "On Leave"
    AttendanceStatus.EXCUSED  -> "Excused"
}

@Composable
private fun statusColors(status: AttendanceStatus): Pair<Color, Color> = when (status) {
    AttendanceStatus.PRESENT  -> StatusPresentContainer to StatusPresent
    AttendanceStatus.LATE     -> StatusLateContainer to StatusLate
    AttendanceStatus.ABSENT   -> StatusAbsentContainer to StatusAbsent
    AttendanceStatus.ON_LEAVE -> StatusLeaveContainer to StatusLeave
    AttendanceStatus.EXCUSED  -> StatusNeutralContainer to StatusNeutral
}

@Composable
private fun MiniChip(text: String, container: Color, content: Color) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = container,
        border = BorderStroke(0.5.dp, content.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            fontWeight = FontWeight.SemiBold
        )
    }
}
