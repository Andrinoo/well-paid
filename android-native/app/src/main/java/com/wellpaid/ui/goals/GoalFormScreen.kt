package com.wellpaid.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.components.ProductPriceHitCard
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatMinorCurrencyFromCents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormScreen(
    onNavigateBack: () -> Unit,
    onFinishedNeedRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleted: (() -> Unit)? = null,
    viewModel: GoalFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDiscardExitDialog by remember { mutableStateOf(false) }

    fun tryLeave() {
        if (viewModel.hasUnsavedChanges()) {
            showDiscardExitDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = viewModel.hasUnsavedChanges()) {
        showDiscardExitDialog = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(
                            if (viewModel.isEditMode) R.string.goal_form_edit_title
                            else R.string.goal_form_new_title,
                        ),
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { tryLeave() }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = Color.White,
                        )
                    }
                },
            )
        },
    ) { inner ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .wellPaidScreenHorizontalPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.setTitle(it) },
                label = { Text(stringResource(R.string.goal_field_title)) },
                supportingText = { Text(stringResource(R.string.goal_field_title_hint_short)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.targetText,
                onValueChange = { viewModel.setTargetText(it) },
                label = { Text(stringResource(R.string.goal_field_target)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text(stringResource(R.string.expense_field_amount_hint)) },
                shape = RoundedCornerShape(14.dp),
            )

            if (!viewModel.isEditMode) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.initialText,
                    onValueChange = { viewModel.setInitialText(it) },
                    label = { Text(stringResource(R.string.goal_field_initial)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.goal_field_initial_hint)) },
                    shape = RoundedCornerShape(14.dp),
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.goal_price_search_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
            )
            OutlinedTextField(
                value = state.targetUrl,
                onValueChange = { viewModel.onTargetUrlChange(it) },
                label = { Text(stringResource(R.string.goal_optional_search_or_link_label)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = false,
                minLines = 2,
                supportingText = { Text(stringResource(R.string.goal_optional_search_or_link_hint)) },
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.isSearchingProducts || state.isRefreshingPrice) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Button(
                    onClick = { viewModel.unifiedPriceSearch() },
                    enabled = !state.isSaving && !state.isSearchingProducts && !state.isRefreshingPrice,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidNavy,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.goal_unified_search_prices))
                }
            }
            if (state.lastProductSearchHadNoResults && !state.isSearchingProducts && !state.isRefreshingPrice) {
                Text(
                    text = stringResource(R.string.goal_search_no_results),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (state.productSearchResults.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, WellPaidNavy.copy(alpha = 0.12f)),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.goal_search_results_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = WellPaidNavy,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.productSearchResults.take(12).forEach { hit ->
                                ProductPriceHitCard(
                                    title = hit.title,
                                    priceLabel = formatMinorCurrencyFromCents(hit.priceCents, hit.currencyId),
                                    source = hit.source,
                                    enabled = !state.isSaving,
                                    onClick = { viewModel.applyProductListing(hit) },
                                )
                            }
                        }
                    }
                }
            }

            state.referencePriceLabel?.let { ref ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.goal_reference_price_label) + ": " + ref,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.goal_field_active),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = state.isActive,
                    onCheckedChange = { viewModel.setActive(it) },
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.save(onFinishedNeedRefresh) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WellPaidGold,
                    contentColor = WellPaidNavy,
                ),
            ) {
                Text(
                    text = if (state.isSaving) {
                        stringResource(R.string.goal_saving)
                    } else {
                        stringResource(R.string.goal_save)
                    },
                )
            }

            if (viewModel.isEditMode && viewModel.canDelete()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.requestDeleteConfirm() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.goal_delete))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text(stringResource(R.string.goal_delete_title)) },
            text = { Text(stringResource(R.string.goal_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(
                            onSuccess = {
                                if (onDeleted != null) {
                                    onDeleted()
                                } else {
                                    onFinishedNeedRefresh()
                                }
                            },
                        )
                    },
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.goal_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) {
                    Text(stringResource(R.string.goal_delete_cancel))
                }
            },
        )
    }

    if (showDiscardExitDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardExitDialog = false },
            title = { Text(stringResource(R.string.goal_exit_discard_title)) },
            text = { Text(stringResource(R.string.goal_exit_discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardExitDialog = false
                        onNavigateBack()
                    },
                ) {
                    Text(stringResource(R.string.goal_exit_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardExitDialog = false }) {
                    Text(stringResource(R.string.goal_exit_discard_keep))
                }
            },
        )
    }
}
