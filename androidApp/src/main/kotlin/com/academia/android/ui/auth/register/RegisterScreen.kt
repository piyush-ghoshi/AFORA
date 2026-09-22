package com.academia.android.ui.auth.register

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.academia.android.R
import com.academia.android.ui.auth.AuthUiState
import com.academia.android.ui.auth.RegisterFormState
import com.academia.android.ui.auth.UserRole
import com.academia.android.ui.auth.components.EmailTextField
import com.academia.android.ui.auth.components.GoogleSignInButton
import com.academia.android.ui.auth.components.NameTextField
import com.academia.android.ui.auth.components.PasswordTextField
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Registration screen with role selection and Google Sign-In.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    formState: RegisterFormState,
    onRoleChange: (UserRole) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
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

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                onGoogleSignInClick(idToken)
            }
        } catch (_: ApiException) {
            // Cancelled or error
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Join AFORA today",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error message
            if (uiState is AuthUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Role selection
            Text(
                text = "I am a",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserRole.values().forEach { role ->
                    RoleCard(
                        role = role,
                        selected = formState.role == role,
                        onSelect = { onRoleChange(role) },
                        enabled = uiState !is AuthUiState.Loading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Name fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NameTextField(
                    value = formState.firstName,
                    onValueChange = onFirstNameChange,
                    label = "First Name",
                    modifier = Modifier.weight(1f),
                    enabled = uiState !is AuthUiState.Loading,
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Right) }
                    )
                )
                
                NameTextField(
                    value = formState.lastName,
                    onValueChange = onLastNameChange,
                    label = "Last Name",
                    modifier = Modifier.weight(1f),
                    enabled = uiState !is AuthUiState.Loading,
                    imeAction = ImeAction.Next,
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Email field
            EmailTextField(
                value = formState.email,
                onValueChange = onEmailChange,
                enabled = uiState !is AuthUiState.Loading,
                isError = formState.validation.emailError != null,
                errorMessage = formState.validation.emailError,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Password field
            PasswordTextField(
                value = formState.password,
                onValueChange = onPasswordChange,
                label = "Password",
                enabled = uiState !is AuthUiState.Loading,
                isError = formState.validation.passwordError != null,
                errorMessage = formState.validation.passwordError,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm password field
            PasswordTextField(
                value = formState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm Password",
                enabled = uiState !is AuthUiState.Loading,
                isError = formState.confirmPassword.isNotEmpty() && 
                         formState.password != formState.confirmPassword,
                errorMessage = if (formState.confirmPassword.isNotEmpty() && 
                                 formState.password != formState.confirmPassword) {
                    "Passwords do not match"
                } else null,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (formState.validation.isValid) {
                            onRegisterClick()
                        }
                    }
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Terms and conditions note
            Text(
                text = "By creating an account, you agree to our Terms of Service and Privacy Policy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Register button
            Button(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = formState.validation.isValid && uiState !is AuthUiState.Loading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Divider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Google Sign-Up button
            GoogleSignInButton(
                onClick = {
                    val webClientId = context.getString(R.string.default_web_client_id)
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    googleLauncher.launch(googleSignInClient.signInIntent)
                },
                enabled = uiState !is AuthUiState.Loading,
                text = "Sign up with Google"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Sign in link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                TextButton(
                    onClick = onSignInClick,
                    enabled = uiState !is AuthUiState.Loading
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Role selection card (Student/Teacher).
 */
@Composable
private fun RoleCard(
    role: UserRole,
    selected: Boolean,
    onSelect: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onSelect
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (selected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.primary
                )
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = role.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
