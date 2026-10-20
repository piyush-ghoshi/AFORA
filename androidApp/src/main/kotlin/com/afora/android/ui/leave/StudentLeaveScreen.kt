package com.afora.android.ui.leave

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afora.android.ui.components.AforaBottomBar
import com.afora.android.ui.components.AforaCard
import com.afora.android.ui.components.BottomBarItem
import com.afora.android.ui.components.ErrorBox
import com.afora.android.ui.components.InfoBanner
import com.afora.android.ui.components.LoadingBox
import com.afora.android.ui.components.MonoTextStyle
import com.afora.android.ui.components.StatusChip
import com.afora.android.ui.theme.StatusAbsent
import com.afora.android.ui.theme.StatusAbsentBar
import com.afora.android.ui.theme.StatusAbsentContainer
import com.afora.android.ui.theme.StatusLate
import com.afora.android.ui.theme.StatusLateBar
import com.afora.android.ui.theme.StatusLateContainer
import com.afora.android.ui.theme.StatusNeutral
import com.afora.android.ui.theme.StatusNeutralBar
import com.afora.android.ui.theme.StatusNeutralContainer
import com.afora.android.ui.theme.StatusPresent
import com.afora.android.ui.theme.StatusPresentBar
import com.afora.android.ui.theme.StatusPresentContainer
import com.afora.shared.data.api.LeaveClassOption
import com.afora.shared.data.api.LeaveRequestResponse
import com.afora.shared.domain.model.LeaveStatus

/**
 * A student's leave screen: submit form on top, the student's own requests
 * below. Dates come from a date picker, the scope from the student's real
 * enrolled classes, and a still-PENDING request can be cancelled after a
 * confirmation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentLeaveScreen(
    uiState: StudentLeaveUiState,
    onBack: () -> Unit,
    onSubmit: (fromDate: String, toDate: String, reason: String, classSectionId: Long?) -> Unit,
    onCancel: (leaveRequestId: Long) -> Unit,
    onRetry: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Leave Management", fontWeight = FontWeight.Bold) },
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
                    BottomBarItem("Leave", Icons.Filled.EventAvailable, selected = true, onClick = {}),
                    BottomBarItem("Profile", Icons.Filled.Person, selected = false, onClick = onNavigateProfile)
                )
            )
        }
    ) { padding ->
        when (uiState) {
            is StudentLeaveUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is StudentLeaveUiState.Error -> ErrorBox(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier.padding(padding)
            )
            is StudentLeaveUiState.Success -> SuccessContent(
                state = uiState,
                onSubmit = onSubmit,
                onCancel = onCancel,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun SuccessContent(
    state: StudentLeaveUiState.Success,
    onSubmit: (String, String, String, Long?) -> Unit,
    onCancel: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var cancelTarget by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Apply for absence verification and review request status",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            SubmitLeaveForm(
                classOptions = state.classOptions,
                isSubmitting = state.isSubmitting,
                submitError = state.submitError,
                submitCount = state.submitCount,
                onSubmit = onSubmit
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Requests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                StatusChip(
                    text = "${state.requests.size} total",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        if (state.requests.isEmpty()) {
            item { EmptyRequestsCard() }
        } else {
            items(state.requests, key = { it.id }) { request ->
                LeaveRequestCard(request = request, onCancel = { cancelTarget = request.id })
            }
        }
    }

    cancelTarget?.let { id ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel this request?") },
            text = { Text("The request will be withdrawn and your teacher will no longer see it.") },
            confirmButton = {
                TextButton(onClick = {
                    onCancel(id)
                    cancelTarget = null
                }) { Text("Cancel Request", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) { Text("Keep") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubmitLeaveForm(
    classOptions: List<LeaveClassOption>,
    isSubmitting: Boolean,
    submitError: String?,
    submitCount: Int,
    onSubmit: (String, String, String, Long?) -> Unit
) {
    // Keyed on submitCount: a successful submit resets the form, a failed one keeps the input.
    var fromDate by rememberSaveable(submitCount) { mutableStateOf("") }
    var toDate by rememberSaveable(submitCount) { mutableStateOf("") }
    var reason by rememberSaveable(submitCount) { mutableStateOf("") }
    var scopeId by rememberSaveable(submitCount) { mutableStateOf<Long?>(null) }
    var scopeExpanded by remember { mutableStateOf(false) }

    val rangeInvalid = fromDate.isNotBlank() && toDate.isNotBlank() &&
        LeaveDates.inclusiveDays(fromDate, toDate) == null
    val isValid = fromDate.isNotBlank() && toDate.isNotBlank() && reason.isNotBlank() && !rangeInvalid

    val scopeLabel = classOptions.firstOrNull { it.classSectionId == scopeId }
        ?.let { "${it.subjectCode} - ${it.subjectName}" } ?: "All my classes"

    AforaCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Submit Leave Request",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField("FROM DATE", fromDate, !isSubmitting, { fromDate = it }, Modifier.weight(1f))
                DateField("TO DATE", toDate, !isSubmitting, { toDate = it }, Modifier.weight(1f))
            }
            if (rangeInvalid) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "The end date cannot be before the start date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "COURSE SCOPE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = scopeExpanded,
                onExpandedChange = { if (!isSubmitting) scopeExpanded = it }
            ) {
                OutlinedTextField(
                    value = scopeLabel,
                    onValueChange = {},
                    readOnly = true,
                    enabled = !isSubmitting,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scopeExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = scopeExpanded, onDismissRequest = { scopeExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("All my classes") },
                        onClick = { scopeId = null; scopeExpanded = false }
                    )
                    classOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text("${option.subjectCode} - ${option.subjectName}") },
                            onClick = { scopeId = option.classSectionId; scopeExpanded = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Reason for Absence",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "REQUIRED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                placeholder = { Text("State clinical, emergency, or university sanctioned event details...") },
                enabled = !isSubmitting,
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            if (submitError != null) {
                Spacer(Modifier.height(8.dp))
                InfoBanner(
                    text = submitError,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    icon = Icons.Filled.Warning
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSubmit(fromDate, toDate, reason.trim(), scopeId) },
                enabled = !isSubmitting && isValid,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Submit Request", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: String,
    enabled: Boolean,
    onPicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val shown = if (value.isEmpty()) "Select date" else LeaveDates.display(value)

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(enabled = enabled, role = Role.Button) { showPicker = true }
                .semantics { contentDescription = "$label, ${if (value.isEmpty()) "not set" else shown}" },
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = shown,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = LeaveDates.millisFromIso(value))
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onPicked(LeaveDates.isoFromMillis(it)) }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private data class LeaveVisual(val label: String, val bar: Color, val chipBg: Color, val chipFg: Color)

private fun visualFor(status: LeaveStatus): LeaveVisual = when (status) {
    LeaveStatus.PENDING -> LeaveVisual("Pending Review", StatusLateBar, StatusLateContainer, StatusLate)
    LeaveStatus.APPROVED -> LeaveVisual("Approved", StatusPresentBar, StatusPresentContainer, StatusPresent)
    LeaveStatus.REJECTED -> LeaveVisual("Rejected", StatusAbsentBar, StatusAbsentContainer, StatusAbsent)
    LeaveStatus.CANCELLED -> LeaveVisual("Cancelled", StatusNeutralBar, StatusNeutralContainer, StatusNeutral)
}

@Composable
private fun LeaveRequestCard(request: LeaveRequestResponse, onCancel: () -> Unit) {
    val visual = visualFor(request.status)
    val range = LeaveDates.displayRange(request.fromDate, request.toDate)

    AforaCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Leave request $range, ${visual.label}" },
        barColor = visual.bar
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = range, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    val days = LeaveDates.daysLabel(request.fromDate, request.toDate)
                    if (days.isNotEmpty()) {
                        Text(
                            text = days,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusChip(text = visual.label, containerColor = visual.chipBg, contentColor = visual.chipFg)
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = request.classSectionName ?: "All my classes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(text = request.reason, style = MaterialTheme.typography.bodyMedium)

            val reviewed = request.status == LeaveStatus.APPROVED || request.status == LeaveStatus.REJECTED
            if (reviewed && (request.reviewedByTeacherName != null || !request.reviewNotes.isNullOrBlank())) {
                Spacer(Modifier.height(10.dp))
                val who = request.reviewedByTeacherName?.let { "Reviewed by $it" } ?: "Reviewed"
                val body = request.reviewNotes?.takeIf { it.isNotBlank() }
                InfoBanner(
                    text = if (body != null) "$who\n$body" else who,
                    containerColor = if (request.status == LeaveStatus.APPROVED) StatusPresentContainer else StatusAbsentContainer,
                    contentColor = if (request.status == LeaveStatus.APPROVED) StatusPresent else StatusAbsent,
                    icon = if (request.status == LeaveStatus.APPROVED) Icons.Filled.CheckCircle else Icons.Filled.Info
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ref: #LR-${request.id}",
                    style = MonoTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (request.status == LeaveStatus.PENDING) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel Request", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRequestsCard() {
    AforaCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No leave requests yet",
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}