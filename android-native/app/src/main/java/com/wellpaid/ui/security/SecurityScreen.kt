package com.wellpaid.ui.security

import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.security.AppLockMethod
import com.wellpaid.security.AppSecurityEntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.wellpaid.ui.theme.wellPaidCenterTopAppBarColors
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    biometricQuickLoginViewModel: BiometricQuickLoginViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val manager = remember(context.applicationContext) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppSecurityEntryPoint::class.java,
        ).appSecurityManager()
    }
    val privacyHide by manager.privacyHideAmounts.collectAsStateWithLifecycle()
    val lockMethod by manager.lockMethod.collectAsStateWithLifecycle()
    var appLock by remember { mutableStateOf(manager.appLockEnabled()) }
    var biometric by remember { mutableStateOf(manager.biometricUnlockEnabled()) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showDisableLockDialog by remember { mutableStateOf(false) }
    var pinDialogTargetMode by remember { mutableStateOf(AppLockMethod.BOTH) }
    var pinFirst by remember { mutableStateOf("") }
    var pinSecond by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var modeError by remember { mutableStateOf<String?>(null) }
    val quickLoginEnabled by biometricQuickLoginViewModel.quickLoginEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        biometricQuickLoginViewModel.refreshEnabled()
    }

    fun syncFromManager() {
        appLock = manager.appLockEnabled()
        biometric = manager.biometricUnlockEnabled()
    }

    fun biometricStatus(): Int =
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG,
        )

    fun applyModeSelection(target: AppLockMethod) {
        modeError = null
        if (!manager.appLockEnabled()) {
            manager.setLockMethod(target)
            syncFromManager()
            return
        }
        when (target) {
            AppLockMethod.BIOMETRIC_ONLY -> {
                if (biometricStatus() != BiometricManager.BIOMETRIC_SUCCESS) {
                    modeError = context.getString(R.string.security_biometric_unavailable)
                    return
                }
                manager.enableBiometricOnlyLock()
                syncFromManager()
            }
            AppLockMethod.PIN_ONLY, AppLockMethod.BOTH -> {
                if (manager.hasPin()) {
                    manager.setLockMethod(target)
                    syncFromManager()
                } else {
                    pinDialogTargetMode = target
                    pinFirst = ""
                    pinSecond = ""
                    pinError = null
                    showPinDialog = true
                }
            }
        }
    }

    fun enableAppLockSwitch() {
        modeError = null
        when (lockMethod) {
            AppLockMethod.BIOMETRIC_ONLY -> {
                if (biometricStatus() != BiometricManager.BIOMETRIC_SUCCESS) {
                    modeError = context.getString(R.string.security_biometric_unavailable)
                    return
                }
                manager.enableBiometricOnlyLock()
                syncFromManager()
            }
            AppLockMethod.PIN_ONLY, AppLockMethod.BOTH -> {
                if (manager.hasPin()) {
                    manager.setLockMethod(lockMethod)
                    manager.setAppLockEnabled(true)
                    syncFromManager()
                } else {
                    pinDialogTargetMode = lockMethod
                    pinFirst = ""
                    pinSecond = ""
                    pinError = null
                    showPinDialog = true
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidCenterTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.security_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .wellPaidScreenHorizontalPadding()
                .padding(vertical = 16.dp),
        ) {
            SecuritySwitchRow(
                title = stringResource(R.string.security_privacy_hide_title),
                subtitle = stringResource(R.string.security_privacy_hide_subtitle),
                checked = privacyHide,
                onCheckedChange = { v ->
                    manager.setPrivacyHideAmounts(v)
                },
            )
            Spacer(Modifier.height(20.dp))
            SecuritySwitchRow(
                title = stringResource(R.string.security_quick_login_title),
                subtitle = stringResource(R.string.security_quick_login_subtitle),
                checked = quickLoginEnabled,
                onCheckedChange = { want ->
                    modeError = null
                    if (want) {
                        if (biometricStatus() != BiometricManager.BIOMETRIC_SUCCESS) {
                            modeError = context.getString(R.string.security_biometric_unavailable)
                        } else {
                            biometricQuickLoginViewModel.enrollWithCurrentSession(activity) { res ->
                                when (res) {
                                    BiometricQuickLoginViewModel.EnrollResult.Success -> Unit
                                    BiometricQuickLoginViewModel.EnrollResult.NetworkError -> {
                                        modeError =
                                            context.getString(R.string.security_quick_login_error_network)
                                    }
                                    BiometricQuickLoginViewModel.EnrollResult.NeedRememberedCredentials -> {
                                        modeError =
                                            context.getString(R.string.security_quick_login_need_remember)
                                    }
                                    BiometricQuickLoginViewModel.EnrollResult.BiometricCancelled -> Unit
                                }
                            }
                        }
                    } else {
                        biometricQuickLoginViewModel.disableQuickLogin()
                    }
                },
            )
            Spacer(Modifier.height(20.dp))
            SecuritySwitchRow(
                title = stringResource(R.string.security_app_lock_title),
                subtitle = stringResource(R.string.security_app_lock_subtitle),
                checked = appLock,
                onCheckedChange = { v ->
                    if (v) {
                        enableAppLockSwitch()
                    } else {
                        showDisableLockDialog = true
                    }
                },
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.security_lock_method_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            LockMethodRow(
                selected = lockMethod == AppLockMethod.PIN_ONLY,
                title = stringResource(R.string.security_lock_method_pin),
                subtitle = stringResource(R.string.security_lock_method_pin_sub),
                onClick = { applyModeSelection(AppLockMethod.PIN_ONLY) },
            )
            LockMethodRow(
                selected = lockMethod == AppLockMethod.BIOMETRIC_ONLY,
                title = stringResource(R.string.security_lock_method_biometric),
                subtitle = stringResource(R.string.security_lock_method_biometric_sub),
                onClick = { applyModeSelection(AppLockMethod.BIOMETRIC_ONLY) },
            )
            LockMethodRow(
                selected = lockMethod == AppLockMethod.BOTH,
                title = stringResource(R.string.security_lock_method_both),
                subtitle = stringResource(R.string.security_lock_method_both_sub),
                onClick = { applyModeSelection(AppLockMethod.BOTH) },
            )
            modeError?.let { err ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (appLock && lockMethod == AppLockMethod.BOTH) {
                Spacer(Modifier.height(12.dp))
                SecuritySwitchRow(
                    title = stringResource(R.string.security_biometric_title),
                    subtitle = stringResource(R.string.security_biometric_subtitle),
                    checked = biometric,
                    onCheckedChange = { v ->
                        biometric = v
                        manager.setBiometricUnlockEnabled(v)
                    },
                )
            }
            if (appLock && lockMethod == AppLockMethod.BIOMETRIC_ONLY) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.security_lock_biometric_only_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(stringResource(R.string.security_pin_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.security_pin_dialog_body))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinFirst,
                        onValueChange = {
                            pinFirst = it.filter { ch -> ch.isDigit() }.take(8)
                            pinError = null
                        },
                        label = { Text(stringResource(R.string.security_pin_field)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinSecond,
                        onValueChange = {
                            pinSecond = it.filter { ch -> ch.isDigit() }.take(8)
                            pinError = null
                        },
                        label = { Text(stringResource(R.string.security_pin_confirm)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                    )
                    pinError?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinFirst.length < 4) {
                            pinError = context.getString(R.string.security_pin_too_short)
                            return@TextButton
                        }
                        if (pinFirst != pinSecond) {
                            pinError = context.getString(R.string.security_pin_mismatch)
                            return@TextButton
                        }
                        manager.saveNewPin(pinFirst)
                        manager.setLockMethod(pinDialogTargetMode)
                        showPinDialog = false
                        syncFromManager()
                    },
                ) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showDisableLockDialog) {
        AlertDialog(
            onDismissRequest = { showDisableLockDialog = false },
            title = { Text(stringResource(R.string.security_disable_lock_title)) },
            text = { Text(stringResource(R.string.security_disable_lock_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        manager.setAppLockEnabled(false)
                        syncFromManager()
                        showDisableLockDialog = false
                    },
                ) {
                    Text(stringResource(R.string.security_disable_lock_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableLockDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

}

@Composable
private fun LockMethodRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SecuritySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
