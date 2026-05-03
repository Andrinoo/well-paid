package com.wellpaid.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import com.wellpaid.ui.components.WellPaidBrandCircularProgress
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidPrimaryAddRow
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.categoryAccentColor
import com.wellpaid.core.model.expense.ExpenseDto
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.formatIsoDateToBr
import com.wellpaid.util.parseIsoDateLocal
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val monthTitleFormatter =
    DateTimeFormatter.ofPattern("LLLL yyyy", Locale("pt", "PT"))

/** Etiquetas PAR / REC / ANT / Pago: mesma caixa para alinhar na horizontal sem “saltos”. */
private val ExpenseTypeTagHeight = 13.dp
private val ExpenseTypeTagFontSize = 7.sp
private val ExpenseTypeTagCorner = 2.dp

/** Largura fixa para as etiquetas alinharem entre linhas; valor com mínimo para não empurrar as tags. */
private val ExpenseRowTagColumnWidth = 50.dp
private val ExpenseRowWarningSlotWidth = 18.dp
/** Largura fixa do valor + alinhamento à direita, para a coluna das tags não se mover entre linhas. */
private val ExpenseRowAmountColumnWidth = 100.dp

private sealed class ExpenseDeletePrompt {
    data class Simple(val expense: ExpenseDto) : ExpenseDeletePrompt()
    data class InstallmentPick(val expense: ExpenseDto) : ExpenseDeletePrompt()
    data class InstallmentFullSecond(val expense: ExpenseDto) : ExpenseDeletePrompt()
    data class Recurring(val expense: ExpenseDto) : ExpenseDeletePrompt()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ExpensesListContent(
    mainRouteEntry: NavBackStackEntry,
    onExpenseClick: (String) -> Unit,
    onOpenInstallmentPlan: (String) -> Unit,
    onNewExpense: () -> Unit,
    modifier: Modifier = Modifier,
    tabSwipe: Modifier = Modifier,
    viewModel: ExpensesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var deletePrompt by remember { mutableStateOf<ExpenseDeletePrompt?>(null) }
    val dirtyFlow = remember(mainRouteEntry) {
        mainRouteEntry.savedStateHandle.getStateFlow("expense_list_dirty", 0L)
    }
    val dirty by dirtyFlow.collectAsStateWithLifecycle()
    LaunchedEffect(dirty) {
        if (dirty != 0L) {
            viewModel.refresh()
        }
    }

    val pendingStatusFlow = remember(mainRouteEntry) {
        mainRouteEntry.savedStateHandle.getStateFlow<String?>("pending_expense_status", null)
    }
    val pendingStatus by pendingStatusFlow.collectAsStateWithLifecycle()
    LaunchedEffect(pendingStatus) {
        val s = pendingStatus ?: return@LaunchedEffect
        if (s == "pending") {
            viewModel.setStatusFilter(ExpenseStatusFilter.PENDING)
            mainRouteEntry.savedStateHandle.remove<String>("pending_expense_status")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .fillMaxWidth(),
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WellPaidCreamMuted),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }, enabled = !state.isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.home_prev_month))
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = WellPaidNavy.copy(alpha = 0.07f),
                    ) {
                        Text(
                            text = state.period.format(monthTitleFormatter).replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString()
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = WellPaidNavy,
                        )
                    }
                    IconButton(onClick = { viewModel.nextMonth() }, enabled = !state.isLoading) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.home_next_month))
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = WellPaidNavy.copy(alpha = 0.1f),
                    thickness = 1.dp,
                )

                WellPaidPrimaryAddRow(
                    label = stringResource(R.string.expense_fab_new),
                    leadingIcon = Icons.AutoMirrored.Filled.List,
                    onPrimaryClick = onNewExpense,
                    onRefresh = { viewModel.refresh() },
                    refreshEnabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = state.statusFilter == ExpenseStatusFilter.ALL,
                            onClick = { viewModel.setStatusFilter(ExpenseStatusFilter.ALL) },
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(8.dp),
                            label = { Text(stringResource(R.string.expenses_filter_all)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.statusFilter == ExpenseStatusFilter.PENDING,
                            onClick = { viewModel.setStatusFilter(ExpenseStatusFilter.PENDING) },
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(8.dp),
                            label = { Text(stringResource(R.string.expenses_filter_pending)) },
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.statusFilter == ExpenseStatusFilter.PAID,
                            onClick = { viewModel.setStatusFilter(ExpenseStatusFilter.PAID) },
                            enabled = !state.isLoading,
                            shape = RoundedCornerShape(8.dp),
                            label = { Text(stringResource(R.string.expenses_filter_paid)) },
                        )
                    }
                }
            }

        Spacer(Modifier.height(4.dp))

        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (state.isLoading && state.expenses.isEmpty()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp)
                    .then(tabSwipe),
                horizontalArrangement = Arrangement.Center,
            ) {
                WellPaidBrandCircularProgress()
            }
        } else {
            val pullRefreshing = state.isLoading && state.expenses.isNotEmpty()
            val pullState = rememberPullRefreshState(
                refreshing = pullRefreshing,
                onRefresh = { viewModel.refresh() },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pullRefresh(pullState),
            ) {
                when {
                    state.expenses.isEmpty() && !state.isLoading -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(tabSwipe),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp,
                            ),
                        ) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(44.dp),
                                        tint = WellPaidGold.copy(alpha = 0.85f),
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.expenses_empty),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(tabSwipe),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 16.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                        ) {
                            itemsIndexed(
                                state.expenses,
                                key = { _, expense -> expense.id },
                            ) { index, expense ->
                                val zebra = if (index % 2 == 1) {
                                    WellPaidNavy.copy(alpha = 0.036f)
                                } else {
                                    Color.Transparent
                                }
                                ExpenseListRow(
                                    expense = expense,
                                    deleteEnabled = !state.isLoading,
                                    modifier = Modifier.background(zebra),
                                    onRowClick = {
                                        val gid = expense.installmentGroupId
                                        if (expense.installmentTotal > 1 && !gid.isNullOrBlank()) {
                                            onOpenInstallmentPlan(gid)
                                        } else {
                                            onExpenseClick(expense.id)
                                        }
                                    },
                                    onPayClick = {
                                        if (expense.status != "paid" && (expense.isMine || expense.isShared)) {
                                            viewModel.payExpense(expense.id)
                                        }
                                    },
                                    onDeleteClick = if (expense.isMine) {
                                        {
                                            deletePrompt = when {
                                                !expense.installmentGroupId.isNullOrBlank() &&
                                                    expense.installmentTotal > 1 ->
                                                    ExpenseDeletePrompt.InstallmentPick(expense)
                                                !expense.recurringSeriesId.isNullOrBlank() ->
                                                    ExpenseDeletePrompt.Recurring(expense)
                                                else -> ExpenseDeletePrompt.Simple(expense)
                                            }
                                        }
                                    } else {
                                        null
                                    },
                                )
                                if (index < state.expenses.lastIndex) {
                                    HorizontalDivider(
                                        color = WellPaidNavy.copy(alpha = 0.1f),
                                    )
                                }
                            }
                        }
                    }
                }
                PullRefreshIndicator(
                    refreshing = pullRefreshing,
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    contentColor = WellPaidNavy,
                )
            }
        }
    }

    when (val p = deletePrompt) {
        null -> Unit
        is ExpenseDeletePrompt.Simple -> {
            AlertDialog(
                onDismissRequest = { deletePrompt = null },
                title = { Text(stringResource(R.string.expense_list_delete_simple_title)) },
                text = { Text(stringResource(R.string.expense_delete_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deletePrompt = null
                            viewModel.deleteExpenseQuick(p.expense, ExpenseQuickDeleteMode.SIMPLE)
                        },
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(R.string.expense_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletePrompt = null }) {
                        Text(stringResource(R.string.expense_delete_cancel))
                    }
                },
            )
        }
        is ExpenseDeletePrompt.Recurring -> {
            AlertDialog(
                onDismissRequest = { deletePrompt = null },
                title = { Text(stringResource(R.string.expense_list_delete_recurring_title)) },
                text = { Text(stringResource(R.string.expense_list_delete_recurring_msg)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deletePrompt = null
                            viewModel.deleteExpenseQuick(p.expense, ExpenseQuickDeleteMode.RECURRING_OCCURRENCE)
                        },
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(R.string.expense_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletePrompt = null }) {
                        Text(stringResource(R.string.expense_delete_cancel))
                    }
                },
            )
        }
        is ExpenseDeletePrompt.InstallmentPick -> {
            AlertDialog(
                onDismissRequest = { deletePrompt = null },
                title = { Text(stringResource(R.string.expense_delete_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(stringResource(R.string.expense_delete_installment_choice_lead))
                        TextButton(
                            onClick = {
                                deletePrompt = null
                                viewModel.deleteExpenseQuick(p.expense, ExpenseQuickDeleteMode.INSTALLMENT_FUTURE)
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.expense_delete_future_only))
                        }
                        TextButton(
                            onClick = {
                                deletePrompt = ExpenseDeletePrompt.InstallmentFullSecond(p.expense)
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.expense_delete_entire_plan))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { deletePrompt = null }, enabled = !state.isLoading) {
                        Text(stringResource(R.string.expense_delete_cancel))
                    }
                },
                dismissButton = {},
            )
        }
        is ExpenseDeletePrompt.InstallmentFullSecond -> {
            AlertDialog(
                onDismissRequest = { deletePrompt = null },
                title = { Text(stringResource(R.string.expense_delete_full_wipe_title)) },
                text = { Text(stringResource(R.string.expense_delete_full_wipe_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            deletePrompt = null
                            viewModel.deleteExpenseQuick(p.expense, ExpenseQuickDeleteMode.INSTALLMENT_FULL)
                        },
                        enabled = !state.isLoading,
                    ) {
                        Text(stringResource(R.string.expense_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletePrompt = null }) {
                        Text(stringResource(R.string.expense_delete_cancel))
                    }
                },
            )
        }
    }
    }
}

private fun showParTag(expense: ExpenseDto): Boolean = expense.installmentTotal > 1

private fun showRecTag(expense: ExpenseDto): Boolean =
    expense.installmentTotal <= 1 && !expense.recurringSeriesId.isNullOrBlank()

private fun showAntTag(expense: ExpenseDto): Boolean =
    expense.isAdvancedPayment

private fun showPaidTag(expense: ExpenseDto): Boolean =
    expense.status == "paid"

@Composable
private fun ExpenseTypeTagChip(
    text: String,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(ExpenseTypeTagHeight),
        shape = RoundedCornerShape(ExpenseTypeTagCorner),
        color = background,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontSize = ExpenseTypeTagFontSize,
                lineHeight = ExpenseTypeTagFontSize,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseTypeTagPaid(paidAtRaw: String?) {
    val tooltipLine = paidTooltipLine(paidAtRaw)
    val scope = rememberCoroutineScope()
    val tooltipState = rememberTooltipState()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            PlainTooltip {
                Text(tooltipLine)
            }
        },
        state = tooltipState,
    ) {
        ExpenseTypeTagChip(
            text = stringResource(R.string.expenses_tag_paid),
            background = Color(0xFFC8E6C9),
            contentColor = Color(0xFF1B5E20),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                scope.launch {
                    tooltipState.show()
                    delay(2400)
                    tooltipState.dismiss()
                }
            },
        )
    }
}

@Composable
private fun paidTooltipLine(paidAtRaw: String?): String {
    val raw = paidAtRaw.orEmpty()
    val datePart = raw.take(10)
    return if (datePart.length >= 10) {
        val paidDay = parseIsoDateLocal(datePart)
        val formatted = if (paidDay != null) formatIsoDateToBr(datePart) else datePart
        stringResource(R.string.expenses_paid_tooltip, formatted)
    } else {
        stringResource(R.string.expenses_paid_tooltip_unknown)
    }
}

@Composable
private fun ExpenseListRow(
    expense: ExpenseDto,
    deleteEnabled: Boolean,
    onRowClick: () -> Unit,
    onPayClick: () -> Unit,
    onDeleteClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isPaid = expense.status == "paid"
    val statusWordPending = stringResource(R.string.expenses_status_pending)
    val expenseDateBr = formatIsoDateToBr(expense.expenseDate)
    val anchorDate = parseIsoDateLocal(expense.dueDate ?: expense.expenseDate)
    val pendingUrgency = if (!isPaid && anchorDate != null) {
        dueUrgencyForDays(daysUntilDue(anchorDate))
    } else {
        null
    }
    val dateStatusColor = if (isPaid) {
        WellPaidNavy.copy(alpha = 0.55f)
    } else if (pendingUrgency != null) {
        dueUrgencyColorOnLight(pendingUrgency)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dateStatusWeight = if (isPaid) {
        FontWeight.Medium
    } else {
        pendingUrgency?.let { dueUrgencyFontWeight(it) } ?: FontWeight.Medium
    }

    val nextDueDate =
        if (expense.installmentTotal > 1 && expense.installmentNumber < expense.installmentTotal) {
            parseIsoDateLocal(expense.dueDate ?: expense.expenseDate)?.plusMonths(1)
        } else {
            null
        }
    val nextInstallmentBr = nextDueDate?.let { d ->
        String.format(Locale("pt", "PT"), "%02d/%02d/%04d", d.dayOfMonth, d.monthValue, d.year)
    }
    val nextUrgency = nextDueDate?.let { dueUrgencyForDays(daysUntilDue(it)) }

    val accent = categoryAccentColor(expense.categoryId)
    val rowCd =
        stringResource(R.string.expenses_row_open_cd) + ". ${expense.description}, " +
            formatBrlFromCents(expense.amountCents)

    val installmentShort =
        if (expense.installmentTotal > 1) {
            stringResource(
                R.string.expenses_installment_short,
                expense.installmentNumber,
                expense.installmentTotal,
            )
        } else {
            null
        }

    /** Data, estado e parcela: em despesas pendentes com parcelas, “2/2” vai logo após “Pendente”. */
    val detailMetaLine = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = dateStatusColor,
                fontWeight = dateStatusWeight,
            ),
        ) {
            append(expenseDateBr)
            if (!isPaid) {
                append(" · ")
                append(statusWordPending)
            }
        }
        if (!isPaid && installmentShort != null) {
            append(" ")
            withStyle(SpanStyle(color = WellPaidNavy.copy(alpha = 0.58f))) {
                append(installmentShort)
            }
        }
        if (isPaid && installmentShort != null) {
            append(" · ")
            withStyle(SpanStyle(color = WellPaidNavy.copy(alpha = 0.58f))) {
                append(installmentShort)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (expense.isFamily) Color(0xFFFFF8E1) else Color.Transparent,
            )
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = rowCd }
                .clickable { onRowClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                    Text(
                        text = expense.description,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                    )
                }
                Text(
                    text = expense.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = WellPaidNavy.copy(alpha = 0.58f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 14.dp, top = 2.dp),
                )
                Text(
                    text = detailMetaLine,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 14.dp, top = 2.dp),
                )
                if (nextInstallmentBr != null && nextUrgency != null) {
                    Text(
                        text = stringResource(R.string.expenses_next_due, nextInstallmentBr),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        color = dueUrgencyColorOnLight(nextUrgency),
                        fontWeight = dueUrgencyFontWeight(nextUrgency),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 14.dp, top = 2.dp),
                    )
                }
                expense.sharedWithLabel?.let { label ->
                    Text(
                        text = stringResource(R.string.expenses_shared_with, label),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = WellPaidNavy.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 14.dp, top = 2.dp),
                    )
                }
                if (expense.isFamily) {
                    Text(
                        text = stringResource(R.string.expense_family_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = Color(0xFF8A6D00),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 14.dp, top = 2.dp),
                    )
                }
                if (expense.isProjected) {
                    Text(
                        text = stringResource(R.string.expenses_projected_suffix),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(start = 14.dp, top = 2.dp),
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier.width(ExpenseRowWarningSlotWidth),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (expense.sharedExpensePaymentAlert) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = stringResource(R.string.expense_share_alert_short),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier.width(ExpenseRowTagColumnWidth),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (showParTag(expense)) {
                                ExpenseTypeTagChip(
                                    text = "PAR",
                                    background = Color(0xFFE3F2FD),
                                    contentColor = Color(0xFF1565C0),
                                )
                            }
                            if (showRecTag(expense)) {
                                ExpenseTypeTagChip(
                                    text = "REC",
                                    background = WellPaidGold.copy(alpha = 0.38f),
                                    contentColor = WellPaidNavy,
                                )
                            }
                            if (showAntTag(expense)) {
                                ExpenseTypeTagChip(
                                    text = "ANT",
                                    background = Color(0xFFE8F5E9),
                                    contentColor = Color(0xFF2E7D32),
                                )
                            }
                            if (showPaidTag(expense)) {
                                ExpenseTypeTagPaid(paidAtRaw = expense.paidAt)
                            }
                        }
                    }
                    Text(
                        text = formatBrlFromCents(expense.amountCents),
                        modifier = Modifier.width(ExpenseRowAmountColumnWidth),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = WellPaidNavy,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = WellPaidNavy.copy(alpha = 0.34f),
                    )
                }
                if (!isPaid && (expense.isMine || expense.isShared)) {
                    val payLabel = stringResource(R.string.expenses_pay)
                    Surface(
                        modifier = Modifier
                            .semantics {
                                role = Role.Button
                                contentDescription = payLabel
                            }
                            .clip(RoundedCornerShape(4.dp))
                            .border(
                                BorderStroke(1.dp, WellPaidGold.copy(alpha = 0.7f)),
                                RoundedCornerShape(4.dp),
                            )
                            .clickable(
                                enabled = deleteEnabled,
                                onClick = onPayClick,
                            ),
                        color = WellPaidGold.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp),
                        shadowElevation = 0.dp,
                    ) {
                        Text(
                            text = payLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            lineHeight = 11.sp,
                            color = WellPaidNavy,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        if (onDeleteClick != null) {
            IconButton(
                onClick = onDeleteClick,
                enabled = deleteEnabled,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .align(Alignment.Top),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.expenses_row_delete_cd),
                    tint = WellPaidNavy.copy(alpha = 0.42f),
                )
            }
        }
    }
}
