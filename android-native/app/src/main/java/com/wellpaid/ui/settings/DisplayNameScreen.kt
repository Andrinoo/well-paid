package com.wellpaid.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidNavyDeep
import com.wellpaid.ui.theme.wellPaidCenterTopAppBarColors
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayNameScreen(
    onNavigateBack: () -> Unit,
    onSavedNotifyMain: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DisplayNameViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val previewName = ui.draft.trim().ifEmpty {
        stringResource(R.string.display_name_preview_placeholder)
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidCenterTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.display_name_screen_title),
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
                .padding(vertical = 12.dp),
        ) {
            Text(
                text = stringResource(R.string.display_name_preview_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(WellPaidNavyDeep, WellPaidNavy),
                        ),
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_greeting_named, previewName),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.display_name_preview_subline),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.62f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            if (ui.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
            } else {
                OutlinedTextField(
                    value = ui.draft,
                    onValueChange = viewModel::onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_display_name_label)) },
                    singleLine = true,
                    enabled = !ui.isSaving,
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
                ui.loadError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                ui.saveError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.save {
                            onSavedNotifyMain()
                            onNavigateBack()
                        }
                    },
                    enabled = !ui.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (ui.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .height(20.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(stringResource(R.string.settings_display_name_save))
                    }
                }
                TextButton(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        }
    }
}
