package com.wellpaid.ui.register

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.auth.AuthScreenShell
import com.wellpaid.ui.auth.authOutlinedFieldColors

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisteredNavigateToVerify: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fieldColors = authOutlinedFieldColors()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RegisterEvent.NavigateVerify ->
                    onRegisteredNavigateToVerify(event.email)
            }
        }
    }

    AuthScreenShell(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.register_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.register_subtitle),
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.login_email_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            enabled = !state.isLoading,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = state.fullName,
            onValueChange = viewModel::onFullNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.register_full_name_optional)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            enabled = !state.isLoading,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = state.phone,
            onValueChange = viewModel::onPhoneChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.register_phone_optional)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
            enabled = !state.isLoading,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.login_password_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            enabled = !state.isLoading,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.register_confirm_password)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            enabled = !state.isLoading,
        )

        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.submit() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.register_submit))
            }
        }

        TextButton(onClick = onNavigateToLogin, enabled = !state.isLoading) {
            Text(stringResource(R.string.register_already_have_account))
        }
    }
}
