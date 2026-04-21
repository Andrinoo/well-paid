package com.wellpaid.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.wellpaid.core.model.goal.GoalProductHitDto
import com.wellpaid.ui.components.ProductPriceHitCard
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatMinorCurrencyFromCents
import kotlinx.coroutines.delay

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
    var showSearchResultsSheet by remember { mutableStateOf(false) }
    var wasSearchingProducts by remember { mutableStateOf(false) }
    val formScrollState = rememberScrollState()
    var pendingScrollFormToSave by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSearchingProducts, state.productSearchResults) {
        if (wasSearchingProducts && !state.isSearchingProducts && state.productSearchResults.isNotEmpty()) {
            showSearchResultsSheet = true
        }
        wasSearchingProducts = state.isSearchingProducts
    }

    LaunchedEffect(state.productSearchResults) {
        if (state.productSearchResults.isEmpty()) {
            showSearchResultsSheet = false
        }
    }

    LaunchedEffect(pendingScrollFormToSave) {
        if (!pendingScrollFormToSave) return@LaunchedEffect
        delay(48)
        formScrollState.animateScrollTo(formScrollState.maxValue)
        pendingScrollFormToSave = false
    }

    fun tryLeave() {
        if (viewModel.hasUnsavedChanges()) {
            showDiscardExitDialog = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(enabled = showSearchResultsSheet) {
        showSearchResultsSheet = false
    }
    BackHandler(enabled = !showSearchResultsSheet && viewModel.hasUnsavedChanges()) {
        showDiscardExitDialog = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WellPaidCream,
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
                CircularProgressIndicator(color = WellPaidNavy)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.goal_form_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WellPaidNavy.copy(alpha = 0.78f),
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .wellPaidScreenHorizontalPadding()
                .verticalScroll(formScrollState),
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
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = stringResource(R.string.goal_auto_product_search),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                    )
                    Text(
                        text = stringResource(R.string.goal_auto_product_search_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = WellPaidNavy.copy(alpha = 0.72f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Switch(
                    checked = state.autoProductPriceSearchEnabled,
                    onCheckedChange = { viewModel.setAutoProductPriceSearchEnabled(it) },
                    enabled = !state.isSaving,
                )
            }
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

            if (state.productSearchResults.isNotEmpty() && !state.isSearchingProducts) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isSaving) { showSearchResultsSheet = true },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, WellPaidNavy.copy(alpha = 0.14f)),
                    color = WellPaidCreamMuted,
                    tonalElevation = 1.dp,
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    R.string.goal_search_results_banner_title,
                                    state.productSearchResults.size,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WellPaidNavy,
                            )
                            Text(
                                text = stringResource(R.string.goal_search_results_banner_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = WellPaidNavy.copy(alpha = 0.72f),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.goal_search_results_title),
                            tint = WellPaidNavy.copy(alpha = 0.55f),
                        )
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

    if (showSearchResultsSheet && state.productSearchResults.isNotEmpty()) {
        GoalProductSearchResultsSheet(
            hits = state.productSearchResults,
            isSaving = state.isSaving,
            onDismiss = { showSearchResultsSheet = false },
            onSelect = { hit ->
                viewModel.applyProductListing(hit)
                showSearchResultsSheet = false
                pendingScrollFormToSave = true
            },
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalProductSearchResultsSheet(
    hits: List<GoalProductHitDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSelect: (GoalProductHitDto) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { new -> !(isSaving && new == SheetValue.Hidden) },
    )
    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        dragHandle = null,
        containerColor = WellPaidCream,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 20.dp),
        ) {
            item {
                Surface(
                    color = WellPaidNavy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.goal_search_results_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Text(
                                text = stringResource(R.string.goal_search_results_sheet_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.88f),
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            enabled = !isSaving,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.common_close),
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            items(
                items = hits,
                key = { h -> h.url + h.priceCents + h.title },
            ) { hit ->
                ProductPriceHitCard(
                    title = hit.title,
                    priceLabel = formatMinorCurrencyFromCents(hit.priceCents, hit.currencyId),
                    source = hit.source,
                    enabled = !isSaving,
                    onClick = { onSelect(hit) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    thumbnailUrl = hit.thumbnail,
                    showLeadingThumbnailSlot = true,
                )
            }
        }
    }
}
