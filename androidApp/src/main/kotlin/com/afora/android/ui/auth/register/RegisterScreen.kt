package com.afora.android.ui.auth.register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.afora.android.R
import com.afora.android.ui.auth.AuthUiState
import com.afora.android.ui.auth.RegisterFormState
import com.afora.android.ui.auth.UserRole
import com.afora.android.ui.auth.components.AuthTextField
import com.afora.android.ui.auth.components.EmailTextField
import com.afora.android.ui.auth.components.GoogleSignInButton
import com.afora.android.ui.auth.components.NameTextField
import com.afora.android.ui.auth.components.PasswordTextField
import com.afora.android.ui.components.AforaCard
import com.afora.android.ui.components.InfoBanner
import com.afora.android.ui.theme.CardBorder
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Registration screen: role, name, roll number or employee ID, email and password.
 *
 * The roll number / employee ID is required because the backend uses it to create
 * the student or teacher record. Google sign-up reuses the role and ID entered here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    formState: RegisterFormState,
    onRoleChange: (UserRole) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onIdNumberChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onGoogleSignInClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val busy = uiState is AuthUiState.Loading
    val passwordsMismatch = formState.confirmPassword.isNotEmpty() && formState.password != formState.confirmPassword

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken -> onGoogleSignInClick(idToken) }
        } catch (_: ApiException) {
            // Cancelled or error
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AFORA ACADEMIC",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Register your university profile with AFORA",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            // Role toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    UserRole.values().forEach { role ->
                        RoleOption(
                            label = role.displayName,
                            icon = if (role == UserRole.STUDENT) Icons.Filled.School else Icons.Filled.Badge,
                            selected = formState.role == role,
                            enabled = !busy,
                            onClick = { onRoleChange(role) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (uiState is AuthUiState.Error) {
                InfoBanner(
                    text = uiState.message,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    icon = Icons.Filled.Warning
                )
                Spacer(Modifier.height(16.dp))
            }

            AforaCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NameTextField(
                            value = formState.firstName,
                            onValueChange = onFirstNameChange,
                            label = "First Name",
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            imeAction = ImeAction.Next,
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                        )
                        NameTextField(
                            value = formState.lastName,
                            onValueChange = onLastNameChange,
                            label = "Last Name",
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                            imeAction = ImeAction.Next,
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    AuthTextField(
                        value = formState.idNumber,
                        onValueChange = onIdNumberChange,
                        label = formState.role.idLabel,
                        enabled = !busy,
                        isError = formState.idNumber.isNotEmpty() && formState.validation.idNumberError != null,
                        errorMessage = formState.validation.idNumberError,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    Spacer(Modifier.height(16.dp))

                    EmailTextField(
                        value = formState.email,
                        onValueChange = onEmailChange,
                        enabled = !busy,
                        isError = formState.email.isNotEmpty() && formState.validation.emailError != null,
                        errorMessage = formState.validation.emailError,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    Spacer(Modifier.height(16.dp))

                    PasswordTextField(
                        value = formState.password,
                        onValueChange = onPasswordChange,
                        label = "Password",
                        enabled = !busy,
                        isError = formState.password.isNotEmpty() && formState.validation.passwordError != null,
                        errorMessage = formState.validation.passwordError,
                        imeAction = ImeAction.Next,
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )
                    Text(
                        text = "At least 8 characters, with upper and lower case letters and a number",
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    PasswordTextField(
                        value = formState.confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = "Confirm Password",
                        enabled = !busy,
                        isError = passwordsMismatch,
                        errorMessage = if (passwordsMismatch) "Passwords do not match" else null,
                        imeAction = ImeAction.Done,
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (formState.validation.isValid) onRegisterClick()
                            }
                        )
                    )

                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "By creating an account, you agree to the Terms of Service and Privacy Policy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onRegisterClick,
                        enabled = formState.validation.isValid && !busy,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Create Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))

            GoogleSignInButton(
                onClick = {
                    val webClientId = context.getString(R.string.default_web_client_id)
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    googleLauncher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                },
                enabled = !busy && formState.isIdValid,
                text = "Sign up with Google"
            )
            if (!formState.isIdValid) {
                Text(
                    text = "Enter your ${formState.role.idLabel.lowercase()} above to sign up with Google.",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already registered?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onSignInClick, enabled = !busy) {
                    Text("Log In", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** One half of the Student / Faculty toggle. */
@Composable
private fun RoleOption(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = if (selected) "$label, selected" else label },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}