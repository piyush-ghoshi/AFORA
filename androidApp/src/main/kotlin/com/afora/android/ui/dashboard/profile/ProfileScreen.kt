package com.afora.android.ui.dashboard.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afora.android.ui.auth.components.NameTextField
import com.afora.android.ui.components.AforaBottomBar
import com.afora.android.ui.components.AforaCard
import com.afora.android.ui.components.BottomBarItem
import com.afora.android.ui.components.ErrorBox
import com.afora.android.ui.components.InfoBanner
import com.afora.android.ui.components.InitialsAvatar
import com.afora.android.ui.components.LoadingBox
import com.afora.android.ui.components.MonoTextStyle
import com.afora.android.ui.components.StatusChip
import com.afora.android.ui.dashboard.ProfileUiState
import com.afora.android.ui.theme.StatusPresent
import com.afora.android.ui.theme.StatusPresentContainer
import com.afora.shared.domain.model.User

/**
 * Profile screen, shared by students and teachers.
 *
 * Shows who you are and your academic identifiers (roll number or employee id,
 * department, batch), lets you edit your name, and signs you out. Department and
 * batch read "Not assigned yet" until an administrator sets them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit,
    onLogout: () -> Unit,
    onRetry: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    onNavigateToLeave: () -> Unit = {},
    onNavigateToLeaveReview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val role = (uiState as? ProfileUiState.Ready)?.user?.role

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (role != null) {
                val second = if (role.equals("TEACHER", ignoreCase = true)) {
                    BottomBarItem("Leave Review", Icons.Filled.RateReview, false, onNavigateToLeaveReview)
                } else {
                    BottomBarItem("Leave", Icons.Filled.EventAvailable, false, onNavigateToLeave)
                }
                AforaBottomBar(
                    items = listOf(
                        BottomBarItem("Home", Icons.Filled.Home, false, onNavigateHome),
                        second,
                        BottomBarItem("Profile", Icons.Filled.Person, true) {}
                    )
                )
            }
        }
    ) { padding ->
        when (uiState) {
            is ProfileUiState.Loading -> LoadingBox(Modifier.padding(padding))
            is ProfileUiState.Error -> ErrorBox(uiState.message, onRetry, Modifier.padding(padding))
            is ProfileUiState.Ready -> ReadyContent(
                uiState = uiState,
                onFirstNameChange = onFirstNameChange,
                onLastNameChange = onLastNameChange,
                onSave = onSave,
                onLogout = onLogout,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun ReadyContent(
    uiState: ProfileUiState.Ready,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user = uiState.user
    val isTeacher = user.role.equals("TEACHER", ignoreCase = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Identity
        AforaCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                InitialsAvatar(name = user.fullName, size = 80.dp)
                Spacer(Modifier.height(12.dp))
                Text(user.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                StatusChip(
                    text = if (isTeacher) "Teacher" else "Student",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Academic identifiers
        AforaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isTeacher) "FACULTY DETAILS" else "ACADEMIC CREDENTIALS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isTeacher) {
                    DetailRow("Employee ID", user.employeeId, mono = true)
                    DetailRow("Department", user.department)
                } else {
                    DetailRow("Roll Number", user.rollNumber, mono = true)
                    DetailRow("Department", user.department)
                    DetailRow("Batch", user.batch)
                    user.semester?.let { DetailRow("Semester", it) }
                }
            }
        }

        // Edit name
        AforaCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Edit Personal Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NameTextField(
                        value = uiState.firstName,
                        onValueChange = onFirstNameChange,
                        label = "First Name",
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1f)
                    )
                    NameTextField(
                        value = uiState.lastName,
                        onValueChange = onLastNameChange,
                        label = "Last Name",
                        enabled = !uiState.isSaving,
                        modifier = Modifier.weight(1f)
                    )
                }

                uiState.errorMessage?.let { msg ->
                    Spacer(Modifier.height(12.dp))
                    InfoBanner(
                        text = msg,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        icon = Icons.Filled.Warning
                    )
                }
                if (uiState.saveSuccess) {
                    Spacer(Modifier.height(12.dp))
                    InfoBanner(
                        text = "Profile updated.",
                        containerColor = StatusPresentContainer,
                        contentColor = StatusPresent,
                        icon = Icons.Filled.CheckCircle
                    )
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Changes", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
        ) {
            Icon(
                Icons.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Log Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A label/value row. A missing or "Unassigned" value reads "Not assigned yet" in a muted tone. */
@Composable
private fun DetailRow(label: String, value: String?, mono: Boolean = false) {
    val missing = value.isNullOrBlank() || value.equals("Unassigned", ignoreCase = true)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        if (mono && !missing) {
            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = value.orEmpty(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MonoTextStyle,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = if (missing) "Not assigned yet" else value.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (missing) FontWeight.Normal else FontWeight.Medium,
                color = if (missing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
    }
}