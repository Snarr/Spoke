package com.jacobsnarr.spoke.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jacobsnarr.spoke.ui.components.SpokeTopBar
import com.jacobsnarr.spoke.ui.rememberAppContainer
import com.mudita.mmd.components.buttons.ButtonMMD
import com.mudita.mmd.components.buttons.OutlinedButtonMMD
import com.mudita.mmd.components.progress_indicator.CircularProgressIndicatorMMD
import com.mudita.mmd.components.text.TextMMD
import com.mudita.mmd.components.text_field.TextFieldMMD
import com.mudita.mmd.eInkTypography

@Composable
fun AccountScreen(onBack: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.provideFactory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        SpokeTopBar(
            title = "Account",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(28.dp))
                }
            },
        )
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            TextMMD(text = "Bikeshare system", style = eInkTypography.bodySmall)
            TextMMD(text = state.system.displayName, style = eInkTypography.bodyLarge)
            Spacer(Modifier.height(24.dp))

            if (state.isLoggedIn) {
                OutlinedButtonMMD(
                    onClick = viewModel::logout,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextMMD("Sign out")
                }
            } else if (state.canSignIn) {
                LoginSection(
                    email = state.email,
                    password = state.password,
                    isLoading = state.isLoading,
                    error = state.error,
                    canSubmit = state.canSubmit,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onSubmit = viewModel::login,
                )
            } else {
                TextMMD(
                    text = "Sign-in isn't available for this system yet.",
                    style = eInkTypography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun LoginSection(
    email: String,
    password: String,
    isLoading: Boolean,
    error: String?,
    canSubmit: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        TextMMD(text = "Sign in", style = eInkTypography.titleMedium)
        Spacer(Modifier.height(12.dp))

        TextFieldMMD(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            isError = error != null,
            placeholder = { TextMMD("Email") },
            keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
            ),
        )
        Spacer(Modifier.height(12.dp))

        TextFieldMMD(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            singleLine = true,
            isError = error != null,
            placeholder = { TextMMD("Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false,
                capitalization = KeyboardCapitalization.None,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )

        if (error != null) {
            Spacer(Modifier.height(12.dp))
            TextMMD(text = error, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(20.dp))

        ButtonMMD(
            onClick = onSubmit,
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) {
                CircularProgressIndicatorMMD(size = 20.dp)
            } else {
                TextMMD("Log in")
            }
        }
    }
}
