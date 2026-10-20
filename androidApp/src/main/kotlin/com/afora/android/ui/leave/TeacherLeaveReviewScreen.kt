package com.afora.android.ui.leave

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afora.android.ui.components.AforaBottomBar
import com.afora.android.ui.components.AforaCard
import com.afora.android.ui.components.BottomBarItem
import com.afora.android.ui.components.ErrorBox
import com.afora.android.ui.components.InfoBanner
import com.afora.android.ui.components.InitialsAvatar
import com.afora.android.ui.components.LoadingBox
import com.afora.android.ui.components.MonoTextStyle
import com.afora.android.ui.components.StatusChip
import com.afora.android.ui.theme.StatusLate
import com.afora.android.ui.theme.StatusLateBar
import com.afora.android.ui.theme.StatusLateContainer
import com.afora.shared.data.api.LeaveRequestResponse

/**
 * A teacher's leave review queue: every PENDING request against a class they
 * teach, oldest first, each with Approve and Reject. Reject needs a reason,
 * which the student sees.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherLeaveReviewScreen(
    uiState: TeacherLeaveReviewUiState,
    onBack: () -> Unit,
    onApprove: (leaveRequestId: Long) -> Unit,
    onReject: (leaveRequestId: Long, notes: String) -> Unit,
    onRetry: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Leave Review", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            AforaBottomBar(
                items = listOf(
                    BottomBarItem("Home", Icons.Filled.Home, selected = false, onClick = onNavigateHome),
                    BottomBarItem("Leave Review", Icons.Filled.RateReview, selected = true, onClick = {}),
                    BottomBarItem("Profile", Icons.Filled.Person, selected = false, onClick = onNavigateProfile)
                )
            )
        }
    ) { padding ->
        when (uiState) {
            is TeacherLeaveReviewUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is TeacherLeaveReviewUiState.Error -> ErrorBox(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier.padding(padding)
            )
            is TeacherLeaveReviewUiState.Success -> SuccessContent(
                state = uiState,
                onApprove = onApprove,
                onReject = onReject,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun SuccessContent(
    state: TeacherLeaveReviewUiState.Success,
    onApprove: (Long) -> Unit,
    onReject: (Long, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (state.actionError != null) {
            item {
                InfoBanner(
                    text = state.actionError,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    icon = Icons.Filled.Warning
                )
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(StatusLateBar, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${state.requests.size} pending \u2022 oldest requests first",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.requests.isEmpty()) {
            item { EmptyQueueCard() }
        } else {
            items(state.requests, key = { it.id }) { request ->
                PendingRequestCard(
                    request = request,
                    isActioning = state.actioningRequestId == request.id,
                    anyActioning = state.actioningRequestId != null,
                    onApprove = { onApprove(request.id) },
                    onReject = { notes -> onReject(request.id, notes) }
                )
            }
        }
    }
}

@Composable
private fun PendingRequestCard(
    request: LeaveRequestResponse,
    isActioning: Boolean,
    anyActioning: Boolean,
    onApprove: () -> Unit,
    onReject: (String) -> Unit
) {
    var showRejectForm by remember { mutableStateOf(false) }
    var rejectNotes by remember { mutableStateOf("") }

    AforaCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Leave request from ${request.studentName}" },
        barColor = StatusLateBar
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                InitialsAvatar(name = request.studentName, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = request.studentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = request.rollNumber,
                        style = MonoTextStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusChip(
                    text = LeaveDates.submittedLabel(request.createdAt),
                    containerColor = StatusLateContainer,
                    contentColor = StatusLate
                )
            }

            Spacer(Modifier.height(12.dp))
            DetailRow(
                icon = { Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = "Scope:",
                value = request.classSectionName ?: "All classes"
            )
            Spacer(Modifier.height(6.dp))
            val days = LeaveDates.daysLabel(request.fromDate, request.toDate)
            DetailRow(
                icon = { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = "Window:",
                value = LeaveDates.displayRange(request.fromDate, request.toDate) +
                    if (days.isNotEmpty()) " ($days)" else ""
            )

            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "REASON",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = request.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            if (showRejectForm) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = rejectNotes,
                    onValueChange = { rejectNotes = it },
                    label = { Text("Reason for rejection (required)") },
                    enabled = !isActioning,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onApprove,
                    enabled = !anyActioning,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                ) {
                    if (isActioning && !showRejectForm) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Approve", fontWeight = FontWeight.SemiBold)
                    }
                }
                if (showRejectForm) {
                    Button(
                        onClick = { onReject(rejectNotes.trim()) },
                        enabled = !anyActioning && rejectNotes.isNotBlank(),
                        shape = RoundedCornerShape(4.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        if (isActioning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            Text("Confirm Reject", fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showRejectForm = true },
                        enabled = !anyActioning,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Reject", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (showRejectForm && !isActioning) {
                TextButton(onClick = { showRejectForm = false; rejectNotes = "" }) { Text("Back") }
            }
        }
    }
}

@Composable
private fun DetailRow(icon: @Composable () -> Unit, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        icon()
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyQueueCard() {
    AforaCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No pending leave requests",
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}