package com.wellpaid.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.BuildConfig
import com.wellpaid.R
import com.wellpaid.ui.theme.LoginBg
import com.wellpaid.ui.theme.LoginButtonGradient
import com.wellpaid.ui.theme.LoginCard
import com.wellpaid.ui.theme.LoginCardBorder
import com.wellpaid.ui.theme.LoginFieldFill
import com.wellpaid.ui.theme.LoginFooter
import com.wellpaid.ui.theme.LoginGold
import com.wellpaid.ui.theme.LoginGoldDeep
import com.wellpaid.ui.theme.LoginGoldMuted
import com.wellpaid.ui.theme.LoginOnCard
import com.wellpaid.ui.theme.LoginOnCardMuted
import com.wellpaid.ui.theme.WellPaidLoginTheme
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding

@Composable
fun LoginScreen(
    onNavigateToMain: () -> Unit,
    onExploreWithoutAccount: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    WellPaidLoginTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        val activity = LocalContext.current as FragmentActivity

        LaunchedEffect(Unit) {
            viewModel.refreshQuickLoginAvailability()
        }

        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    LoginEvent.NavigateToMain -> onNavigateToMain()
                }
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(LoginBg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .wellPaidScreenHorizontalPadding()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wellPaidMaxContentWidth(WellPaidMaxContentWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                Image(
                    painter = painterResource(R.drawable.logo_well_paid),
                    contentDescription = stringResource(R.string.logo_content_description),
                    modifier = Modifier
                        .height(128.dp)
                        .fillMaxWidth(0.85f),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.height(4.dp))
                WellPaidBrandTitle()
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 400.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = LoginCard),
                    border = BorderStroke(1.dp, LoginCardBorder.copy(alpha = 0.9f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.login_card_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = LoginOnCard,
                        )
                        Spacer(Modifier.height(10.dp))

                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LoginOnCard,
                            unfocusedTextColor = LoginOnCard,
                            focusedLabelColor = LoginGold,
                            unfocusedLabelColor = LoginGold.copy(alpha = 0.85f),
                            focusedBorderColor = LoginGold,
                            unfocusedBorderColor = LoginCardBorder.copy(alpha = 0.55f),
                            cursorColor = LoginGold,
                            focusedContainerColor = LoginFieldFill,
                            unfocusedContainerColor = LoginFieldFill,
                            selectionColors = TextSelectionColors(
                                handleColor = LoginGold,
                                backgroundColor = LoginGold.copy(alpha = 0.35f),
                            ),
                        )

                        var passwordVisible by remember { mutableStateOf(false) }

                        OutlinedTextField(
                            value = state.email,
                            onValueChange = viewModel::onEmailChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.login_email_label)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            enabled = !state.isLoading,
                            colors = fieldColors,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = viewModel::onPasswordChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.login_password_label)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    enabled = !state.isLoading,
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) {
                                            Icons.Filled.VisibilityOff
                                        } else {
                                            Icons.Filled.Visibility
                                        },
                                        contentDescription = stringResource(
                                            if (passwordVisible) {
                                                R.string.login_password_hide
                                            } else {
                                                R.string.login_password_show
                                            },
                                        ),
                                        tint = LoginGold,
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { viewModel.submit() },
                            ),
                            enabled = !state.isLoading,
                            colors = fieldColors,
                        )

                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = state.rememberCredentials,
                                onCheckedChange = viewModel::onRememberChange,
                                enabled = !state.isLoading,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = LoginGold,
                                    uncheckedColor = LoginGoldMuted,
                                    checkmarkColor = LoginCard,
                                ),
                            )
                            Text(
                                text = stringResource(R.string.login_remember),
                                style = MaterialTheme.typography.bodySmall,
                                color = LoginOnCardMuted,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = onNavigateToForgotPassword,
                                enabled = !state.isLoading,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.login_forgot_password),
                                    color = LoginGold,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        }

                        state.errorMessage?.let { msg ->
                            Text(
                                text = msg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        if (state.quickLoginAvailable) {
                            OutlinedButton(
                                onClick = { viewModel.loginWithBiometric(activity) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                enabled = !state.isLoading,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, LoginGold.copy(alpha = 0.75f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = LoginGold,
                                ),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Fingerprint,
                                        contentDescription = null,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.login_with_biometric),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                        ),
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        val buttonShape = MaterialTheme.shapes.medium
                        Button(
                            onClick = { viewModel.submit() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = !state.isLoading,
                            shape = buttonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                            ),
                            contentPadding = PaddingValues(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(buttonShape)
                                    .background(
                                        if (state.isLoading) {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    LoginFieldFill,
                                                    LoginGoldMuted.copy(alpha = 0.4f),
                                                ),
                                            )
                                        } else {
                                            LoginButtonGradient
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(26.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White,
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.login_submit),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                        ),
                                        color = Color.White,
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onExploreWithoutAccount,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            enabled = !state.isLoading,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, LoginGoldMuted.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = LoginOnCard,
                            ),
                        ) {
                            Text(stringResource(R.string.login_explore_shell))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.login_no_account_prefix),
                                style = MaterialTheme.typography.bodyMedium,
                                color = LoginFooter,
                            )
                            TextButton(
                                onClick = onNavigateToRegister,
                                enabled = !state.isLoading,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.login_create_account_highlight),
                                    color = LoginGold,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.login_copyright),
                            style = MaterialTheme.typography.labelSmall,
                            color = LoginFooter,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.login_version, BuildConfig.VERSION_DISPLAY_LINE),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                            lineHeight = 8.sp,
                            color = LoginFooter.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WellPaidBrandTitle() {
    val paidBrush = Brush.linearGradient(
        colors = listOf(
            LoginGoldDeep,
            LoginGold,
            Color(0xFFFFF2C4),
        ),
    )
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                append("Well ")
            }
            withStyle(
                SpanStyle(
                    brush = paidBrush,
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                append("Paid")
            }
        },
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
        ),
    )
}
