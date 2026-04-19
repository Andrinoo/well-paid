package com.wellpaid.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.goal.GoalProductHitDto
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidCreamMuted
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
    var showProductPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.productSearchResults.isEmpty()) {
        if (state.productSearchResults.isEmpty()) showProductPicker = false
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
                    IconButton(onClick = onNavigateBack) {
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
                supportingText = { Text(stringResource(R.string.goal_field_title_search_hint)) },
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

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.targetUrl,
                onValueChange = { viewModel.onTargetUrlChange(it) },
                label = { Text(stringResource(R.string.goal_field_link)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                supportingText = {
                    Column {
                        Text(stringResource(R.string.goal_field_link_hint))
                        Text(
                            text = stringResource(R.string.goal_field_link_autocomplete_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.goal_search_auto_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.isSearchingProducts) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                }
                TextButton(
                    onClick = { viewModel.searchProductsByTitle() },
                    enabled = !state.isSaving && !state.isSearchingProducts,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.goal_search_refresh_suggestions))
                }
            }
            if (state.lastProductSearchHadNoResults && !state.isSearchingProducts) {
                Text(
                    text = stringResource(R.string.goal_search_no_results),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (state.productSearchResults.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showProductPicker = true },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidNavy,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(stringResource(R.string.goal_choose_listing_action))
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

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.loadPriceFromLink() },
                enabled = !state.isSaving && !state.isRefreshingPrice,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.isRefreshingPrice) {
                            stringResource(R.string.goal_refreshing_price)
                        } else {
                            stringResource(R.string.goal_fetch_target_from_link)
                        },
                    )
                }
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

    if (showProductPicker && state.productSearchResults.isNotEmpty()) {
        GoalProductPickerBottomSheet(
            hits = state.productSearchResults,
            onDismiss = { showProductPicker = false },
            onConfirm = { hit ->
                viewModel.applyProductListing(hit)
                showProductPicker = false
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalProductPickerBottomSheet(
    hits: List<GoalProductHitDto>,
    onDismiss: () -> Unit,
    onConfirm: (GoalProductHitDto) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIndex by remember(hits) { mutableIntStateOf(0) }

    LaunchedEffect(hits) {
        if (hits.isNotEmpty()) {
            selectedIndex = selectedIndex.coerceIn(0, hits.lastIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = WellPaidCream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.goal_product_picker_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = WellPaidNavy,
            )
            Text(
                text = stringResource(R.string.goal_product_picker_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = WellPaidNavy.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(
                    items = hits,
                    key = { _, h -> h.url },
                ) { index, hit ->
                    val selected = index == selectedIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIndex = index },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = WellPaidCreamMuted),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) WellPaidGold else WellPaidNavy.copy(alpha = 0.12f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { selectedIndex = index },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = hit.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    color = WellPaidNavy,
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatMinorCurrencyFromCents(hit.priceCents, hit.currencyId),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WellPaidNavy,
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = {
                        val hit = hits.getOrNull(selectedIndex) ?: return@Button
                        onConfirm(hit)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidGold,
                        contentColor = WellPaidNavy,
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(stringResource(R.string.goal_product_picker_confirm))
                }
            }
        }
    }
}
