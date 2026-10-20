package com.afora.android.ui.dashboard.student

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afora.android.ui.components.AforaBottomBar
import com.afora.android.ui.components.AforaCard
import com.afora.android.ui.components.AforaTopBar
import com.afora.android.ui.components.AttendanceMath
import com.afora.android.ui.components.BottomBarItem
import com.afora.android.ui.components.ErrorBox
import com.afora.android.ui.components.InfoBanner
import com.afora.android.ui.components.LoadingBox
import com.afora.android.ui.components.MonoTextStyle
import com.afora.android.ui.components.StatusChip
import com.afora.android.ui.dashboard.StudentDashboardData
import com.afora.android.ui.dashboard.StudentDashboardUiState
import com.afora.android.ui.theme.CardBorder
import com.afora.android.ui.theme.StatusAbsent
import com.afora.android.ui.theme.StatusAbsentContainer
import com.afora.android.ui.theme.StatusLate
import com.afora.android.ui.theme.StatusLateBar
import com.afora.android.ui.theme.StatusNeutralBar
import com.afora.android.ui.theme.StatusPresent
import com.afora.android.ui.theme.StatusPresentContainer
import com.afora.shared.domain.model.TimetableSlot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Student dashboard.
 *
 * Welcome row, overall attendance ring with threshold guidance, stat tiles
 * (the Pending tile opens leave requests) and upcoming classes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    uiState: StudentDashboardUiState,
    onNavigateToProfile: () -> Unit,
    onRetry: () -> Unit,
    onNavigateToLeave: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val displayName = (uiState as? StudentDashboardUiState.Success)?.data?.user?.fullName ?: "Student"

    Scaffold(
        modifier = modifier,
        topBar = {
            AforaTopBar(
                roleTitle = "Student Portal",
                displayName = displayName,
                onProfileClick = onNavigateToProfile
            )
        },
        bottomBar = {
            AforaBottomBar(
                items = listOf(
                    BottomBarItem("Home", Icons.Filled.Home, selected = true, onClick = {}),
                    BottomBarItem("Leave", Icons.Filled.EventAvailable, selected = false, onClick = onNavigateToLeave),
                    BottomBarItem("Profile", Icons.Filled.Person, selected = false, onClick = onNavigateToProfile)
                )
            )
        }
    ) { padding ->
        when (uiState) {
            is StudentDashboardUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is StudentDashboardUiState.Error -> ErrorBox(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier.padding(padding)
            )
            is StudentDashboardUiState.Success -> DashboardContent(
                data = uiState.data,
                onNavigateToLeave = onNavigateToLeave,
                onNavigateToAttendance = onNavigateToAttendance,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DashboardContent(
    data: StudentDashboardData,
    onNavigateToLeave: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item { WelcomeRow(firstName = data.user.firstName) }
        item { AttendanceCard(data = data, onClick = onNavigateToAttendance) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatTile(
                    label = "PRESENT",
                    value = data.presentCount.toString(),
                    dotColor = StatusPresent,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "CLASSES",
                    value = data.totalCount.toString(),
                    dotColor = StatusNeutralBar,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = "PENDING",
                    value = data.pendingLeaveRequests.toString(),
                    dotColor = if (data.pendingLeaveRequests > 0) StatusLateBar else StatusNeutralBar,
                    showArrow = true,
                    onClick = onNavigateToLeave,
                    clickDescription = "View leave requests, ${data.pendingLeaveRequests} pending",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Upcoming Classes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Today, " + SimpleDateFormat("EEE MMM d", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (data.upcomingClasses.isEmpty()) {
            item { EmptyClassesCard() }
        } else {
            items(data.upcomingClasses) { slot -> ClassCard(slot) }
        }
    }
}

@Composable
private fun WelcomeRow(firstName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Welcome back, $firstName",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        StatusChip(
            text = "Student",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun AttendanceCard(data: StudentDashboardData, onClick: () -> Unit = {}) {
    val hasData = data.totalCount > 0
    val good = AttendanceMath.isGoodStanding(data.attendancePercentage)
    val ringColor = when {
        good -> StatusPresent
        data.attendancePercentage >= 60.0 -> StatusLate
        else -> StatusAbsent
    }

    AforaCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Overall Attendance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(176.dp)
                    .semantics {
                        contentDescription = if (hasData) {
                            "Overall attendance ${"%.1f".format(data.attendancePercentage)} percent"
                        } else {
                            "No attendance data yet"
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = if (hasData) (data.attendancePercentage / 100.0).toFloat().coerceIn(0f, 1f) else 0f,
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 14.dp,
                    color = ringColor,
                    trackColor = MaterialTheme.colorScheme.primaryContainer,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (hasData) "${"%.1f".format(data.attendancePercentage)}%" else "--",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            !hasData -> "No data yet"
                            good -> "Good Standing"
                            else -> "Below Minimum"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasData) ringColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "${data.presentCount} of ${data.totalCount} classes attended",
                style = MaterialTheme.typography.bodyMedium
            )

            if (hasData) {
                Spacer(Modifier.height(12.dp))
                ThresholdBanner(data = data, good = good)
            }
        }
    }
}

@Composable
private fun ThresholdBanner(data: StudentDashboardData, good: Boolean) {
    val minimum = "%.1f".format(AttendanceMath.THRESHOLD_PERCENT)
    if (good) {
        val spare = AttendanceMath.absencesRemaining(data.presentCount, data.totalCount)
        val text = if (spare == 0) {
            "Minimum required: $minimum%. You are right at the limit, so avoid missing the next class."
        } else {
            "Minimum required: $minimum%. You can miss $spare more ${if (spare == 1) "class" else "classes"} and stay on track."
        }
        InfoBanner(
            text = text,
            containerColor = StatusPresentContainer,
            contentColor = StatusPresent,
            icon = Icons.Filled.CheckCircle
        )
    } else {
        val needed = AttendanceMath.classesNeeded(data.presentCount, data.totalCount)
        InfoBanner(
            text = "Minimum required: $minimum%. Attending the next $needed ${if (needed == 1) "class" else "classes"} in a row brings you back to the minimum.",
            containerColor = StatusAbsentContainer,
            contentColor = StatusAbsent,
            icon = Icons.Filled.Info
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    dotColor: Color,
    modifier: Modifier = Modifier,
    showArrow: Boolean = false,
    onClick: (() -> Unit)? = null,
    clickDescription: String? = null
) {
    val semanticsModifier = if (clickDescription != null) {
        Modifier.semantics { contentDescription = clickDescription }
    } else {
        Modifier
    }
    AforaCard(modifier = modifier.then(semanticsModifier), onClick = onClick) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (showArrow) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassCard(slot: TimetableSlot) {
    val days = listOf("", "MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    AforaCard(
        modifier = Modifier.fillMaxWidth(),
        barColor = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = days.getOrElse(slot.dayOfWeek) { "?" },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    style = MonoTextStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${slot.subjectCode} - ${slot.subjectName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${slot.startTime} - ${slot.endTime} \u2022 ${slot.teacherName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            slot.roomNumber?.let { room ->
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = room,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        style = MonoTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyClassesCard() {
    AforaCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No upcoming classes",
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}