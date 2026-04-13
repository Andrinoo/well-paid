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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.auth.AuthScreenShell
import com.wellpaid.ui.auth.authOutlinedFieldColors

@Composable
fun VerifyEmailScreen(
    onNavigateToMain: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VerifyEmailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fieldColors = authOutlinedFieldColors()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                VerifyEvent.NavigateToMain -> onNavigateToMain()
            }
        }
    }

    AuthScreenShell(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.verify_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.verify_subtitle),
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
            value = state.code,
            onValueChange = viewModel::onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.verify_code_label)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Next,
            ),
            enabled = !state.isLoading,
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = state.linkToken,
            onValueChange = viewModel::onTokenChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.verify_token_optional)) },
            singleLine = false,
            minLines = 2,
            shape = MaterialTheme.shapes.medium,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            enabled = !state.isLoading,
        )

        state.infoMessage?.let { msg ->
            Text(
                text = msg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
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
                Text(stringResource(R.string.verify_submit))
            }
        }

        OutlinedButton(
            onClick = { viewModel.resend() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            enabled = !state.isLoading && !state.isResending,
            shape = MaterialTheme.shapes.medium,
        ) {
            if (state.isResending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(stringResource(R.string.verify_resend))
            }
        }

        TextButton(onClick = onNavigateBack, enabled = !state.isLoading) {
            Text(stringResource(R.string.verify_back))
        }
    }
}
