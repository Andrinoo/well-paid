package com.wellpaid.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.goal.GoalProductHitDto
import com.wellpaid.core.model.shopping.ShoppingListDetailDto
import com.wellpaid.core.model.shopping.ShoppingListItemDto
import com.wellpaid.ui.components.WellPaidDatePickerField
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.centsToBrlInput
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.formatIsoDateForList
import com.wellpaid.util.localDateToIso
import com.wellpaid.util.parseBrlToCents
import java.time.LocalDate
import java.util.Locale

private fun ShoppingListDetailDto.sumLineCents(): Int = items.sumOf { row ->
    val u = row.lineAmountCents ?: return@sumOf 0
    u * row.quantity
}

private fun ShoppingListDetailDto.totalUnits(): Int = items.sumOf { it.quantity }

@Composable
private fun DraftListFooter(
    itemCount: Int,
    sumLineCents: Int,
    isSaving: Boolean,
    canCompletePurchase: Boolean,
    onAddItem: () -> Unit,
    onCompletePurchase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(WellPaidCream)
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
    ) {
        HorizontalDivider(color = WellPaidNavy.copy(alpha = 0.12f))
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = pluralStringResource(R.plurals.shopping_items_total_footer, itemCount, itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = WellPaidNavy.copy(alpha = 0.58f),
            )
            Spacer(Modifier.padding(top = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.shopping_estimated_total),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WellPaidNavy,
                )
                Text(
                    text = formatBrlFromCents(sumLineCents),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = WellPaidNavy,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onAddItem,
                    enabled = !isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.shopping_add_item),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                Button(
                    onClick = onCompletePurchase,
                    enabled = !isSaving && canCompletePurchase,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidGold,
                        contentColor = Color.Black,
                        disabledContainerColor = WellPaidGold.copy(alpha = 0.38f),
                        disabledContentColor = Color.Black.copy(alpha = 0.45f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.shopping_close_purchase),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListDetailScreen(
    onNavigateBack: () -> Unit,
    onOpenExpense: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShoppingListDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = state.detail
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val snackHost = remember { SnackbarHostState() }

    LaunchedEffect(state.infoMessage) {
        val m = state.infoMessage ?: return@LaunchedEffect
        snackHost.showSnackbar(m)
        viewModel.consumeInfoMessage()
    }
    LaunchedEffect(state.errorMessage) {
        val m = state.errorMessage ?: return@LaunchedEffect
        snackHost.showSnackbar(m)
    }

    var showEditMeta by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showAddItem by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<ShoppingListItemDto?>(null) }
    var showRemoveItem by remember { mutableStateOf<ShoppingListItemDto?>(null) }
    var showCompleteSheet by remember { mutableStateOf(false) }

    val isDraft = detail?.status?.equals("draft", true) == true
    val isCompleted = detail?.status?.equals("completed", true) == true
    val isMine = detail?.isMine == true
    val canEditMeta = detail != null && isMine && (isDraft || isCompleted)
    val canEditItems = detail != null && isMine && (isDraft || isCompleted)
    val readOnlyOtherDraft = detail != null && !isMine && isDraft

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WellPaidCream,
        snackbarHost = { SnackbarHost(snackHost) },
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = detail?.title?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.shopping_detail_title),
                        color = Color.White,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    if (canEditMeta) {
                        IconButton(onClick = { showEditMeta = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.shopping_edit_list), tint = Color.White)
                        }
                    }
                    if (isMine && isDraft) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.shopping_delete_list), tint = Color.White)
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (detail != null && isMine && isDraft) {
                val sumLines = detail.sumLineCents()
                DraftListFooter(
                    itemCount = detail.items.size,
                    sumLineCents = sumLines,
                    isSaving = state.isSaving,
                    canCompletePurchase = detail.items.isNotEmpty(),
                    onAddItem = { showAddItem = true },
                    onCompletePurchase = { showCompleteSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wellPaidScreenHorizontalPadding()
                        .wellPaidMaxContentWidth(WellPaidMaxContentWidth),
                )
            }
            if (detail != null && isMine && isCompleted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wellPaidScreenHorizontalPadding()
                        .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                        .padding(12.dp),
                ) {
                    OutlinedButton(
                        onClick = { showAddItem = true },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.shopping_add_item),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WellPaidCream)
                .padding(inner),
        ) {
            when {
                state.isLoading && detail == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WellPaidNavy)
                    }
                }
                state.errorMessage != null && detail == null -> {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        modifier = Modifier
                            .wellPaidScreenHorizontalPadding()
                            .padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                detail == null -> { }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .wellPaidScreenHorizontalPadding()
                            .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        item {
                            MetaBlock(detail, locale)
                            if (readOnlyOtherDraft) {
                                Text(
                                    text = stringResource(R.string.shopping_read_only_draft),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WellPaidNavy.copy(alpha = 0.72f),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                            if (isMine && isCompleted) {
                                Text(
                                    text = stringResource(R.string.shopping_completed_owner_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = WellPaidNavy.copy(alpha = 0.65f),
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                            if (isCompleted) {
                                CompletedSummary(
                                    detail = detail,
                                    sumLines = detail.sumLineCents(),
                                    totalUnits = detail.totalUnits(),
                                    isOwner = isMine,
                                    onSyncTotal = {
                                        if (
                                            isMine &&
                                            detail.totalCents != null &&
                                            detail.sumLineCents() > 0 &&
                                            detail.totalCents != detail.sumLineCents()
                                        ) {
                                            viewModel.syncTotalFromLines()
                                        }
                                    },
                                    onOpenExpense = { detail.expenseId?.let { onOpenExpense(it) } },
                                )
                            }
                            Spacer(Modifier.padding(top = 8.dp))
                            Text(
                                text = stringResource(R.string.shopping_items_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = WellPaidNavy,
                            )
                            Spacer(Modifier.padding(top = 8.dp))
                        }
                        items(detail.items, key = { it.id }) { line ->
                            ItemLineCard(
                                line = line,
                                canEdit = canEditItems,
                                isSaving = state.isSaving,
                                onEdit = { editItem = line },
                                onRemove = { showRemoveItem = line },
                                onCommitCost = { cents, clear ->
                                    viewModel.patchItem(
                                        itemId = line.id,
                                        lineAmountCents = cents,
                                        clearLineAmount = clear,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditMeta && detail != null) {
        EditMetaDialog(
            initialTitle = detail.title.orEmpty(),
            initialStore = detail.storeName.orEmpty(),
            isSaving = state.isSaving,
            onDismiss = { showEditMeta = false },
            onSave = { t, s ->
                viewModel.patchListMetadata(t, s) { showEditMeta = false }
            },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { if (!state.isSaving) showDelete = false },
            title = { Text(stringResource(R.string.shopping_delete_confirm_title)) },
            text = { Text(stringResource(R.string.shopping_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteList { onNavigateBack() }
                    },
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.shopping_delete_list))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }, enabled = !state.isSaving) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showAddItem) {
        AddEditItemBottomSheet(
            title = stringResource(R.string.shopping_add_item),
            initialLabel = "",
            initialQty = "1",
            initialAmount = "",
            isSaving = state.isSaving,
            groceryHits = state.groceryPriceHits,
            groceryLoading = state.groceryPriceSearchLoading,
            onLabelChanged = { viewModel.onShoppingItemLabelForPriceHints(it) },
            onDismiss = {
                viewModel.clearGroceryPriceHints()
                showAddItem = false
            },
            onSave = { label, qty, amountCents ->
                viewModel.addItem(label, qty, amountCents) {
                    viewModel.clearGroceryPriceHints()
                    showAddItem = false
                }
            },
        )
    }

    editItem?.let { line ->
        AddEditItemBottomSheet(
            title = stringResource(R.string.shopping_edit_item),
            initialLabel = line.label,
            initialQty = line.quantity.toString(),
            initialAmount = line.lineAmountCents?.let { com.wellpaid.util.centsToBrlInput(it) }.orEmpty(),
            isSaving = state.isSaving,
            showClearPrice = line.lineAmountCents != null,
            groceryHits = state.groceryPriceHits,
            groceryLoading = state.groceryPriceSearchLoading,
            onLabelChanged = { viewModel.onShoppingItemLabelForPriceHints(it) },
            onDismiss = {
                viewModel.clearGroceryPriceHints()
                editItem = null
            },
            onSave = { label, qty, amountCents ->
                val clear = line.lineAmountCents != null && amountCents == null
                viewModel.patchItem(
                    itemId = line.id,
                    label = label,
                    quantity = qty,
                    lineAmountCents = amountCents,
                    clearLineAmount = clear,
                )
                viewModel.clearGroceryPriceHints()
                editItem = null
            },
            onClearPrice = {
                viewModel.patchItem(itemId = line.id, clearLineAmount = true)
                viewModel.clearGroceryPriceHints()
                editItem = null
            },
        )
    }

    showRemoveItem?.let { line ->
        AlertDialog(
            onDismissRequest = { showRemoveItem = null },
            title = { Text(stringResource(R.string.shopping_remove_item_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeItem(line.id)
                        showRemoveItem = null
                    },
                ) {
                    Text(stringResource(R.string.shopping_remove_item))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveItem = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showCompleteSheet && detail != null) {
        CompletePurchaseSheet(
            categories = state.categories,
            isSaving = state.isSaving,
            onDismiss = { showCompleteSheet = false },
            onSubmit = { catId, dateIso, total, discount ->
                viewModel.completePurchase(catId, dateIso, total, discount) {
                    showCompleteSheet = false
                    viewModel.refresh()
                }
            },
        )
    }
}

@Composable
private fun MetaBlock(detail: ShoppingListDetailDto, locale: Locale) {
    detail.storeName?.takeIf { it.isNotBlank() }?.let { store ->
        Text(
            text = store,
            style = MaterialTheme.typography.bodyMedium,
            color = WellPaidNavy.copy(alpha = 0.65f),
        )
        Spacer(Modifier.padding(top = 4.dp))
    }
    val statusLabel = if (detail.status.equals("completed", true)) {
        stringResource(R.string.shopping_status_completed)
    } else {
        stringResource(R.string.shopping_status_draft)
    }
    Text(
        text = statusLabel,
        style = MaterialTheme.typography.labelLarge,
        color = WellPaidNavy.copy(alpha = 0.55f),
    )
    detail.completedAt?.takeIf { it.isNotBlank() }?.let { iso ->
        Spacer(Modifier.padding(top = 4.dp))
        Text(
            text = stringResource(R.string.shopping_completed_on, formatIsoDateForList(iso, locale)),
            style = MaterialTheme.typography.bodySmall,
            color = WellPaidNavy.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun CompletedSummary(
    detail: ShoppingListDetailDto,
    sumLines: Int,
    totalUnits: Int,
    isOwner: Boolean,
    onSyncTotal: () -> Unit,
    onOpenExpense: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = stringResource(R.string.shopping_units_total) + ": $totalUnits",
            style = MaterialTheme.typography.bodyMedium,
            color = WellPaidNavy.copy(alpha = 0.7f),
        )
        if (sumLines > 0) {
            Text(
                text = stringResource(R.string.shopping_subtotal_lines) + ": ${formatBrlFromCents(sumLines)}",
                style = MaterialTheme.typography.bodyMedium,
                color = WellPaidNavy.copy(alpha = 0.7f),
            )
        }
        detail.totalCents?.let { t ->
            Spacer(Modifier.padding(top = 8.dp))
            Text(
                text = stringResource(R.string.shopping_total_label),
                style = MaterialTheme.typography.labelMedium,
                color = WellPaidNavy.copy(alpha = 0.6f),
            )
            Text(
                text = formatBrlFromCents(t),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = WellPaidNavy,
            )
        }
        val mismatch = isOwner && detail.totalCents != null && sumLines > 0 && detail.totalCents != sumLines
        if (mismatch) {
            Spacer(Modifier.padding(top = 8.dp))
            OutlinedButton(onClick = onSyncTotal) {
                Text(stringResource(R.string.shopping_sync_total))
            }
        }
        if (detail.expenseId != null) {
            Spacer(Modifier.padding(top = 8.dp))
            OutlinedButton(onClick = onOpenExpense) {
                Text(stringResource(R.string.shopping_view_expense))
            }
        }
    }
}

@Composable
private fun ItemLineCostField(
    line: ShoppingListItemDto,
    canEdit: Boolean,
    isSaving: Boolean,
    onCommitCost: (cents: Int?, clearLineAmount: Boolean) -> Unit,
) {
    var keypadOpen by remember { mutableStateOf(false) }
    var localAmountText by remember(line.id) {
        mutableStateOf(line.lineAmountCents?.takeIf { it > 0 }?.let { centsToBrlInput(it) }.orEmpty())
    }
    LaunchedEffect(line.id, line.lineAmountCents) {
        if (!keypadOpen) {
            localAmountText =
                line.lineAmountCents?.takeIf { it > 0 }?.let { centsToBrlInput(it) }.orEmpty()
        }
    }

    val currencyPrefix = stringResource(R.string.shopping_currency_prefix)

    fun computeNewCents(): Int? = parseBrlToCents(localAmountText)?.takeIf { it > 0 }

    fun commitIfChanged() {
        val newC = computeNewCents()
        val had = line.lineAmountCents
        when {
            newC == null && had == null -> return
            newC != null && newC == had -> return
            newC == null && had != null -> onCommitCost(null, true)
            newC != null && newC != had -> onCommitCost(newC, false)
        }
    }

    if (!canEdit) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = WellPaidNavy.copy(alpha = 0.08f),
        ) {
            Text(
                text = line.lineAmountCents?.let { formatBrlFromCents(it) } ?: "—",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = WellPaidNavy.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .widthIn(max = 86.dp),
            )
        }
        return
    }

    WellPaidMoneyDigitKeypadField(
        valueText = localAmountText,
        onValueTextChange = { txt ->
            if (isSaving) return@WellPaidMoneyDigitKeypadField
            localAmountText = txt
        },
        enabled = !isSaving,
        prefix = {
            Text(
                text = currencyPrefix,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = WellPaidNavy.copy(alpha = 0.62f),
            )
        },
        placeholder = "0,00",
        modifier = Modifier
            // Largura para valores típicos de mercado (ex.: 125,50); prefixo R$/$
            .widthIn(min = 76.dp, max = 88.dp)
            .defaultMinSize(minHeight = 40.dp),
        shape = RoundedCornerShape(6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = WellPaidNavy,
            unfocusedTextColor = WellPaidNavy,
            focusedContainerColor = WellPaidCreamMuted,
            unfocusedContainerColor = WellPaidCreamMuted,
            disabledTextColor = WellPaidNavy.copy(alpha = 0.55f),
            disabledContainerColor = WellPaidCreamMuted,
            focusedBorderColor = WellPaidNavy.copy(alpha = 0.45f),
            unfocusedBorderColor = WellPaidNavy.copy(alpha = 0.22f),
            disabledBorderColor = WellPaidNavy.copy(alpha = 0.15f),
        ),
        onKeypadOpenChange = { open -> keypadOpen = open },
        onDone = {
            commitIfChanged()
        },
    )
}

@Composable
private fun ItemLineCard(
    line: ShoppingListItemDto,
    canEdit: Boolean,
    isSaving: Boolean,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onCommitCost: (cents: Int?, clearLineAmount: Boolean) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = WellPaidCreamMuted),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = line.label,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = WellPaidNavy.copy(alpha = 0.08f),
            ) {
                Text(
                    text = line.quantity.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    color = WellPaidNavy,
                    modifier = Modifier
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                        .widthIn(min = 22.dp),
                )
            }
            ItemLineCostField(
                line = line,
                canEdit = canEdit,
                isSaving = isSaving,
                onCommitCost = onCommitCost,
            )
            if (canEdit) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.shopping_edit_item),
                            tint = WellPaidNavy.copy(alpha = 0.72f),
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.shopping_edit_item)) },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.shopping_remove_item)) },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditMetaDialog(
    initialTitle: String,
    initialStore: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?, String?) -> Unit,
) {
    var title by remember { mutableStateOf(initialTitle) }
    var store by remember { mutableStateOf(initialStore) }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.shopping_edit_list)) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.shopping_field_title)) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.padding(top = 8.dp))
                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it },
                    label = { Text(stringResource(R.string.shopping_field_store)) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, store) },
                enabled = !isSaving,
            ) {
                Text(stringResource(R.string.shopping_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditItemBottomSheet(
    title: String,
    initialLabel: String,
    initialQty: String,
    initialAmount: String,
    isSaving: Boolean,
    showClearPrice: Boolean = false,
    groceryHits: List<GoalProductHitDto> = emptyList(),
    groceryLoading: Boolean = false,
    onLabelChanged: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (String, Int, Int?) -> Unit,
    onClearPrice: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var label by remember(initialLabel) { mutableStateOf(initialLabel) }
    var qty by remember(initialQty) { mutableStateOf(initialQty) }
    var amount by remember(initialAmount) { mutableStateOf(initialAmount) }

    LaunchedEffect(initialLabel) {
        val t = initialLabel.trim()
        if (t.length >= 2) {
            onLabelChanged(t)
        }
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = WellPaidCream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = WellPaidNavy,
            )
            Spacer(Modifier.padding(top = 16.dp))
            OutlinedTextField(
                value = label,
                onValueChange = {
                    label = it
                    onLabelChanged(it)
                },
                label = { Text(stringResource(R.string.shopping_field_label_item)) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.shopping_grocery_search_auto_hint),
                style = MaterialTheme.typography.bodySmall,
                color = WellPaidNavy.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.padding(top = 8.dp))
            OutlinedTextField(
                value = qty,
                onValueChange = { qty = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text(stringResource(R.string.shopping_field_qty)) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.padding(top = 8.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text(stringResource(R.string.shopping_field_unit_price)) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            if (showClearPrice && onClearPrice != null) {
                TextButton(onClick = onClearPrice, enabled = !isSaving) {
                    Text(stringResource(R.string.shopping_clear_unit_price))
                }
            }
            if (groceryLoading) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = WellPaidNavy,
                        strokeWidth = 2.dp,
                    )
                }
            }
            if (groceryHits.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.shopping_grocery_price_hints_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = WellPaidNavy.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = groceryHits,
                        key = { h -> h.url + h.priceCents + h.title },
                    ) { hit ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isSaving) {
                                    amount = centsToBrlInput(hit.priceCents)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = WellPaidCreamMuted,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = hit.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = WellPaidNavy,
                                    )
                                    Text(
                                        text = formatBrlFromCents(hit.priceCents),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WellPaidNavy,
                                    )
                                }
                                Text(
                                    text = hit.source,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WellPaidNavy.copy(alpha = 0.55f),
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
                TextButton(onClick = onDismiss, enabled = !isSaving) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = {
                        val q = qty.toIntOrNull()?.coerceIn(1, 9999) ?: 1
                        val cents = amount.trim().ifEmpty { null }?.let { parseBrlToCents(it) }
                        onSave(label.trim(), q, cents)
                    },
                    enabled = !isSaving && label.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidGold,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text(stringResource(R.string.shopping_save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompletePurchaseSheet(
    categories: List<com.wellpaid.core.model.expense.CategoryDto>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Int?, Int?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var categoryId by remember {
        mutableStateOf(categories.firstOrNull()?.id.orEmpty())
    }
    var expenseDateIso by remember {
        mutableStateOf(localDateToIso(LocalDate.now()))
    }
    var totalText by remember { mutableStateOf("") }
    var discountText by remember { mutableStateOf("") }
    var categoryMenu by remember { mutableStateOf(false) }

    LaunchedEffect(categories) {
        if (categoryId.isEmpty() && categories.isNotEmpty()) {
            categoryId = categories.first().id
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.shopping_complete_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = WellPaidNavy,
            )
            Text(
                text = stringResource(R.string.shopping_complete_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = WellPaidNavy.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            if (categories.isEmpty()) {
                Text(
                    text = stringResource(R.string.expense_no_categories),
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSaving) { categoryMenu = true },
                        readOnly = true,
                        value = categories.find { it.id == categoryId }?.name.orEmpty(),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.shopping_category_required)) },
                        enabled = !isSaving,
                    )
                    DropdownMenu(
                        expanded = categoryMenu,
                        onDismissRequest = { categoryMenu = false },
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    categoryId = cat.id
                                    categoryMenu = false
                                },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.padding(top = 12.dp))
            WellPaidDatePickerField(
                label = { Text(stringResource(R.string.expense_field_expense_date)) },
                isoDate = expenseDateIso,
                onIsoDateChange = { expenseDateIso = it },
                enabled = !isSaving,
            )
            Spacer(Modifier.padding(top = 12.dp))
            OutlinedTextField(
                value = totalText,
                onValueChange = {
                    totalText = it
                    if (it.isNotBlank()) discountText = ""
                },
                label = { Text(stringResource(R.string.shopping_total_override)) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = discountText,
                onValueChange = {
                    discountText = it
                    if (it.isNotBlank()) totalText = ""
                },
                label = { Text(stringResource(R.string.shopping_discount)) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (totalText.isNotBlank() && discountText.isNotBlank()) {
                        return@Button
                    }
                    val totalCents = totalText.trim().ifEmpty { null }?.let { parseBrlToCents(it) }
                    val discCents = discountText.trim().ifEmpty { null }?.let { parseBrlToCents(it) }
                    if (categoryId.isBlank()) return@Button
                    onSubmit(categoryId, expenseDateIso, totalCents, discCents)
                },
                enabled = !isSaving && categoryId.isNotBlank() && categories.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
            ) {
                Text(stringResource(R.string.shopping_close_purchase))
            }
        }
    }
}
