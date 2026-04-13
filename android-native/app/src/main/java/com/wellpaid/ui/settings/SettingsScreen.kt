package com.wellpaid.ui.settings

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.BuildConfig
import com.wellpaid.R
import com.wellpaid.locale.AppLocalePreferences
import com.wellpaid.ui.theme.wellPaidCenterTopAppBarColors
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun stringInLocale(context: Context, resId: Int, locale: Locale): String {
    val cfg = Configuration(context.resources.configuration)
    cfg.setLocales(LocaleList(locale))
    return context.createConfigurationContext(cfg).getString(resId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit,
    onOpenFamily: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.loggedOutEvents.collect {
            onLoggedOut()
        }
    }
    val snackHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val setUi by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(setUi.snackbarMessage) {
        val msg = setUi.snackbarMessage ?: return@LaunchedEffect
        snackHostState.showSnackbar(msg)
        viewModel.consumeSnackbarMessage()
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidCenterTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
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
        val context = LocalContext.current
        var isEnglish by remember {
            mutableStateOf(AppLocalePreferences.isEnglishInterface(context.applicationContext))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .wellPaidScreenHorizontalPadding()
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_display_name_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            if (setUi.isLoadingProfile) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .size(28.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                OutlinedTextField(
                    value = setUi.displayNameDraft,
                    onValueChange = viewModel::onDisplayNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_display_name_label)) },
                    singleLine = true,
                    enabled = !setUi.isSavingDisplayName,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                )
                Text(
                    text = stringResource(R.string.settings_display_name_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                TextButton(
                    onClick = { viewModel.saveDisplayName() },
                    enabled = !setUi.isSavingDisplayName,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (setUi.isSavingDisplayName) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(stringResource(R.string.settings_display_name_save))
                    }
                }
            }
            setUi.profileError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            setUi.displayNameSaveError?.let { err ->
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.settings_language_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            LanguageRow(
                label = stringResource(R.string.settings_language_pt),
                selected = !isEnglish,
                onSelect = {
                    if (isEnglish) {
                        scope.launch {
                            AppLocalePreferences.setAndApply(context, false)
                            val msg = stringInLocale(
                                context,
                                R.string.settings_language_applied,
                                Locale.forLanguageTag("pt-BR"),
                            )
                            snackHostState.showSnackbar(msg)
                            delay(450)
                            (context as? Activity)?.recreate()
                        }
                        isEnglish = false
                    }
                },
            )
            LanguageRow(
                label = stringResource(R.string.settings_language_en),
                selected = isEnglish,
                onSelect = {
                    if (!isEnglish) {
                        scope.launch {
                            AppLocalePreferences.setAndApply(context, true)
                            val msg = stringInLocale(
                                context,
                                R.string.settings_language_applied,
                                Locale.US,
                            )
                            snackHostState.showSnackbar(msg)
                            delay(450)
                            (context as? Activity)?.recreate()
                        }
                        isEnglish = true
                    }
                },
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.settings_family_section),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenFamily) {
                Text(stringResource(R.string.settings_open_family))
            }
            TextButton(
                onClick = { viewModel.logout() },
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(
                    text = stringResource(R.string.logout),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = stringResource(R.string.settings_version_label, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (BuildConfig.DEBUG) {
                Text(
                    text = stringResource(R.string.settings_api_debug, BuildConfig.API_BASE_URL),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Text(
                text = stringResource(R.string.settings_more_coming),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
    }
}

@Composable
private fun LanguageRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
