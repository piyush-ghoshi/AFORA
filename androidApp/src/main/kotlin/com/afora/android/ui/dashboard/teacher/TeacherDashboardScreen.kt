package com.afora.android.ui.dashboard.teacher

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afora.android.ui.components.AforaBottomBar
import com.afora.android.ui.components.AforaCard
import com.afora.android.ui.components.AforaTopBar
import com.afora.android.ui.components.BottomBarItem
import com.afora.android.ui.components.ErrorBox
import com.afora.android.ui.components.LoadingBox
import com.afora.android.ui.components.MonoTextStyle
import com.afora.android.ui.components.StatusChip
import com.afora.android.ui.dashboard.TeacherDashboardData
import com.afora.android.ui.dashboard.TeacherDashboardUiState
import com.afora.android.ui.theme.StatusAbsent
import com.afora.android.ui.theme.StatusLeave
import com.afora.android.ui.theme.StatusLeaveBar
import com.afora.android.ui.theme.StatusLeaveContainer
import com.afora.android.ui.theme.StatusNeutral
import com.afora.android.ui.theme.StatusNeutralBar
import com.afora.android.ui.theme.StatusNeutralContainer
import com.afora.android.ui.theme.StatusAbsentContainer
import com.afora.android.ui.theme.StatusPresent
import com.afora.android.ui.theme.StatusPresentBar
import com.afora.android.ui.theme.StatusPresentContainer
import com.afora.shared.domain.model.LectureSession
import com.afora.shared.domain.model.SessionStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Teacher dashboard.
 *
 * Welcome card, a tappable pending-leaves tile that opens the review queue,
 * and today's sessions. Each session card reflects its lifecycle state. Open
 * Queries from the design is omitted until attendance queries exist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    uiState: TeacherDashboardUiState,
    onNavigateToProfile: () -> Unit,
    onRetry: () -> Unit,
    onSessionClick: (sessionId: Long) -> Unit,
    onNavigateToLeaveReview: () -> Unit = {},
    onScanCameraClick: ((sessionId: Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayName = (uiState as? TeacherDashboardUiState.Success)?.data?.user?.fullName ?: "Teacher"

    Scaffold(
        modifier = modifier,
        topBar = {
            AforaTopBar(
                roleTitle = "Teacher Portal",
                displayName = displayName,
                onProfileClick = onNavigateToProfile
            )
        },
        bottomBar = {
            AforaBottomBar(
                items = listOf(
                    BottomBarItem("Home", Icons.Filled.Home, selected = true, onClick = {}),
                    BottomBarItem("Leave Review", Icons.Filled.RateReview, selected = false, onClick = onNavigateToLeaveReview),
                    BottomBarItem("Profile", Icons.Filled.Person, selected = false, onClick = onNavigateToProfile)
                )
            )
        }
    ) { padding ->
        when (uiState) {
            is TeacherDashboardUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is TeacherDashboardUiState.Error -> ErrorBox(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier.padding(padding)
            )
            is TeacherDashboardUiState.Success -> DashboardContent(
                data = uiState.data,
                onSessionClick = onSessionClick,
                onNavigateToLeaveReview = onNavigateToLeaveReview,
                onScanCameraClick = onScanCameraClick,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DashboardContent(
    data: TeacherDashboardData,
    onSessionClick: (Long) -> Unit,
    onNavigateToLeaveReview: () -> Unit,
    onScanCameraClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { WelcomeCard(name = data.user.fullName) }
        item {
            PendingLeavesTile(
                count = data.pendingLeaveRequests,
                onClick = onNavigateToLeaveReview
            )
        }
        item {
            Column {
                Text(
                    text = "Today's Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                val count = data.todaySchedule.size
                Text(
                    text = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date()) +
                        " \u2022 $count ${if (count == 1) "Session" else "Sessions"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (data.todaySchedule.isEmpty()) {
            item { EmptyScheduleCard() }
        } else {
            items(data.todaySchedule) { session ->
                SessionCard(
                    session = session,
                    onClick = { onSessionClick(session.id) },
                    onScanCameraClick = onScanCameraClick
                )
            }
        }
    }
}

@Composable
private fun WelcomeCard(name: String) {
    AforaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            StatusChip(
                text = "FACULTY / TEACHER",
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Welcome back, $name",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PendingLeavesTile(count: Int, onClick: () -> Unit) {
    AforaCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Open leave review, $count pending" },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PENDING LEAVES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (count > 0) "Requires action" else "All caught up",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (count > 0) StatusAbsent else StatusPresent
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class SessionVisual(
    val label: String,
    val bar: Color,
    val chipContainer: Color,
    val chipContent: Color
)

private fun visualFor(status: SessionStatus, primary: Color): SessionVisual = when (status) {
    SessionStatus.ACTIVE -> SessionVisual("ACTIVE NOW", StatusPresentBar, StatusPresentContainer, StatusPresent)
    SessionStatus.SCHEDULED, SessionStatus.CREATED ->
        SessionVisual("SCHEDULED", primary, StatusLeaveContainer, StatusLeave)
    SessionStatus.SCANNING, SessionStatus.REVIEW, SessionStatus.FINALIZING ->
        SessionVisual("IN PROGRESS", StatusLeaveBar, StatusLeaveContainer, StatusLeave)
    SessionStatus.FINALIZED -> SessionVisual("FINALIZED", StatusNeutralBar, StatusPresentContainer, StatusPresent)
    SessionStatus.CANCELLED -> SessionVisual("CANCELLED", StatusNeutralBar, StatusNeutralContainer, StatusNeutral)
    SessionStatus.FAILED -> SessionVisual("FAILED", StatusAbsent, StatusAbsentContainer, StatusAbsent)
}

@Composable
private fun SessionCard(
    session: LectureSession,
    onClick: () -> Unit,
    onScanCameraClick: ((Long) -> Unit)? = null
) {
    val visual = visualFor(session.status, MaterialTheme.colorScheme.primary)
    val cancelled = session.status == SessionStatus.CANCELLED
    val title = if (session.subjectCode != null && session.subjectName != null) {
        "${session.subjectCode} - ${session.subjectName}"
    } else {
        "Subject #${session.subjectId}"
    }
    val time = "${session.startTime.take(5)} - ${session.endTime.take(5)}"

    AforaCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$title, $time, ${visual.label.lowercase()}" },
        barColor = visual.bar,
        onClick = if (cancelled) null else onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = time,
                    style = MonoTextStyle,
                    fontWeight = FontWeight.Bold,
                    color = if (cancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    textDecoration = if (cancelled) TextDecoration.LineThrough else null
                )
                StatusChip(
                    text = visual.label,
                    containerColor = visual.chipContainer,
                    contentColor = visual.chipContent
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (cancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val meta = buildList {
                session.roomNumber?.let { add(it) }
                session.totalStudents?.let { add("$it enrolled") }
            }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta.joinToString(" \u2022 "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            SessionFooter(
                session = session,
                cancelled = cancelled,
                onScanCameraClick = onScanCameraClick
            )
        }
    }
}

@Composable
private fun SessionFooter(
    session: LectureSession,
    cancelled: Boolean,
    onScanCameraClick: ((Long) -> Unit)? = null
) {
    when {
        cancelled -> {
            session.notes?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = "Note: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        session.status == SessionStatus.FINALIZED -> {
            val total = session.totalStudents
            val summary = if (total != null && total > 0) {
                val pct = session.presentCount * 100.0 / total
                "Attendance submitted \u2022 ${session.presentCount}/$total (${"%.1f".format(pct)}%)"
            } else {
                "Attendance submitted"
            }
            Text(
                text = "$summary \u2022 tap to view",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to mark attendance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onScanCameraClick != null) {
                        Surface(
                            onClick = { onScanCameraClick(session.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = "Scan with Camera",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Camera",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyScheduleCard() {
    AforaCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No classes scheduled for today",
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}