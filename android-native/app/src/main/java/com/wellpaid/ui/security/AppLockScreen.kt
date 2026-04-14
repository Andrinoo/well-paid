package com.wellpaid.ui.security

import android.app.Activity
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.security.AppLockMethod
import com.wellpaid.security.AppSecurityManager
import com.wellpaid.ui.theme.WellPaidGold

/**
 * Bloqueio da app: ecrã **só preto** quando a biometria está disponível (sem logo — evita “duas camadas”).
 * O diálogo de biometria é o do sistema (aparência depende do fabricante).
 */
@Composable
fun AppLockScreen(
    manager: AppSecurityManager,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val lockMethod by manager.lockMethod.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var showPinField by remember { mutableStateOf(false) }

    val canBio = remember(manager, context) {
        manager.biometricUnlockEnabled() &&
            BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG,
            ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    val wantsAutoBiometric =
        lockMethod != AppLockMethod.PIN_ONLY && canBio &&
            (lockMethod != AppLockMethod.BOTH || !showPinField)

    val showTapToRetryLayer =
        (lockMethod == AppLockMethod.BIOMETRIC_ONLY && canBio) ||
            (lockMethod == AppLockMethod.BOTH && canBio && !showPinField)

    DisposableEffect(Unit) {
        val window = (context as Activity).window
        val decor = window.decorView
        val controller = WindowCompat.getInsetsController(window, decor)
        val prevLightStatus = controller.isAppearanceLightStatusBars
        val prevLightNav = controller.isAppearanceLightNavigationBars
        // Conteúdo preto em ecrã completo (sem Window.statusBarColor/navigationBarColor — API deprecadas).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
        onDispose {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.isAppearanceLightStatusBars = prevLightStatus
            controller.isAppearanceLightNavigationBars = prevLightNav
        }
    }

    fun launchBiometric() {
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult,
                ) {
                    manager.unlockFromBiometric()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                }
            },
        )
        val b = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.app_lock_biometric_title))
            .setSubtitle(context.getString(R.string.app_lock_biometric_subtitle))
            .setNegativeButtonText(context.getString(R.string.common_cancel))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            b.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        }
        prompt.authenticate(b.build())
    }

    LaunchedEffect(lockMethod, showPinField, canBio) {
        pin = ""
        error = false
        if (wantsAutoBiometric) {
            delay(280)
            launchBiometric()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (showTapToRetryLayer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { launchBiometric() },
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_lock_brand_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = WellPaidGold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.app_lock_brand_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            when (lockMethod) {
                AppLockMethod.PIN_ONLY -> {
                    PinFieldsDark(
                        pin = pin,
                        onPinChange = { raw ->
                            pin = raw.filter { it.isDigit() }.take(8)
                            error = false
                        },
                        error = error,
                        onUnlock = {
                            if (manager.tryUnlockWithPin(pin)) {
                                pin = ""
                            } else {
                                error = true
                            }
                        },
                    )
                }

                AppLockMethod.BIOMETRIC_ONLY -> {
                    if (!canBio) {
                        Text(
                            text = stringResource(R.string.security_biometric_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF6B6B),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                AppLockMethod.BOTH -> {
                    when {
                        showPinField -> {
                            PinFieldsDark(
                                pin = pin,
                                onPinChange = { raw ->
                                    pin = raw.filter { it.isDigit() }.take(8)
                                    error = false
                                },
                                error = error,
                                onUnlock = {
                                    if (manager.tryUnlockWithPin(pin)) {
                                        pin = ""
                                        showPinField = false
                                    } else {
                                        error = true
                                    }
                                },
                            )
                        }
                        !canBio -> {
                            Text(
                                text = stringResource(R.string.security_biometric_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF6B6B),
                                textAlign = TextAlign.Center,
                            )
                        }
                        else -> Box(Modifier)
                    }
                }
            }
        }

        if (lockMethod == AppLockMethod.BOTH && !showPinField) {
            TextButton(
                onClick = { showPinField = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp),
            ) {
                Text(
                    text = stringResource(R.string.app_lock_enter_with_pin),
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun PinFieldsDark(
    pin: String,
    onPinChange: (String) -> Unit,
    error: Boolean,
    onUnlock: () -> Unit,
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White.copy(alpha = 0.92f),
        focusedBorderColor = WellPaidGold,
        unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
        focusedLabelColor = WellPaidGold,
        unfocusedLabelColor = Color.White.copy(alpha = 0.65f),
        cursorColor = WellPaidGold,
        focusedContainerColor = Color.White.copy(alpha = 0.07f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
        errorBorderColor = Color(0xFFFF6B6B),
        errorLabelColor = Color(0xFFFF6B6B),
        errorCursorColor = Color(0xFFFF6B6B),
    )
    OutlinedTextField(
        value = pin,
        onValueChange = onPinChange,
        label = { Text(stringResource(R.string.app_lock_pin_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        isError = error,
        supportingText = {
            if (error) {
                Text(
                    stringResource(R.string.app_lock_pin_error),
                    color = Color(0xFFFF6B6B),
                )
            }
        },
        colors = fieldColors,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onUnlock,
        enabled = pin.length >= 4,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = WellPaidGold,
            contentColor = Color(0xFF0D1B2A),
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            stringResource(R.string.app_lock_unlock),
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
        )
    }
}
