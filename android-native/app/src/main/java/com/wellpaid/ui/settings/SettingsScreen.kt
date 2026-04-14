package com.wellpaid.ui.settings

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
    onOpenDisplayName: () -> Unit = {},
    onOpenFamily: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    onOpenManageCategories: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val profile by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.loggedOutEvents.collect {
            onLoggedOut()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val snackHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(68.dp),
                colors = wellPaidCenterTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(top = 6.dp),
                    ) {
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
                .verticalScroll(rememberScrollState())
                .wellPaidScreenHorizontalPadding()
                .padding(vertical = 12.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_profile_section))
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 0.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val initial = profile.userFirstName?.firstOrNull()?.uppercaseChar()
                            ?: profile.userEmail?.firstOrNull()?.uppercaseChar()
                            ?: '?'
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = initial.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .padding(start = 14.dp)
                                .weight(1f),
                        ) {
                            Text(
                                text = profile.userFirstName?.let { n ->
                                    stringResource(R.string.home_greeting_named, n)
                                } ?: stringResource(R.string.home_greeting_fallback),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            profile.userEmail?.let { em ->
                                Text(
                                    text = em,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    SettingsNavRow(
                        icon = Icons.Outlined.Person,
                        title = stringResource(R.string.settings_tile_display_name),
                        onClick = onOpenDisplayName,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel(stringResource(R.string.settings_section_prefs))
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 0.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Language,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.settings_tile_language),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
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
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel(stringResource(R.string.settings_section_categories))
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 0.dp,
            ) {
                SettingsNavRow(
                    icon = Icons.Outlined.Category,
                    title = stringResource(R.string.settings_tile_manage_categories),
                    onClick = onOpenManageCategories,
                )
            }

            Spacer(Modifier.height(18.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 0.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    SettingsNavRow(
                        icon = Icons.Outlined.Shield,
                        title = stringResource(R.string.settings_tile_security),
                        onClick = onOpenSecurity,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    SettingsNavRow(
                        icon = Icons.Outlined.Groups,
                        title = stringResource(R.string.settings_tile_family),
                        onClick = onOpenFamily,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel(stringResource(R.string.settings_section_about))
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 0.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(R.string.settings_tile_version),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        supportingContent = {
                            Text(
                                BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    Text(
                        text = "${BuildConfig.REVISION_CODE}:WP_VER: ${BuildConfig.VERSION_NAME} \"${BuildConfig.BUILD_TIMESTAMP}\"",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    )
                    if (BuildConfig.DEBUG) {
                        Text(
                            text = stringResource(R.string.settings_api_debug, BuildConfig.API_BASE_URL),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel(stringResource(R.string.settings_section_session))
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 0.dp,
            ) {
                SettingsLogoutRow(onClick = { viewModel.logout() })
            }
        }
    }
}

@Composable
private fun SettingsLogoutRow(onClick: () -> Unit) {
    val err = MaterialTheme.colorScheme.error
    ListItem(
        headlineContent = {
            Text(
                stringResource(R.string.logout),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = err,
            )
        },
        leadingContent = {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = stringResource(R.string.home_logout_cd),
                modifier = Modifier.size(22.dp),
                tint = err,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
    )
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
