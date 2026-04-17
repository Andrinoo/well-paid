package com.wellpaid.ui.expenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidPrimaryAddRow
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.core.model.expense.ExpenseDto
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.formatIsoDateToBr
import com.wellpaid.util.parseIsoDateLocal
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthTitleFormatter =
    DateTimeFormatter.ofPattern("LLLL yyyy", Locale("pt", "PT"))

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
            .fillMaxWidth()
            .wellPaidMaxContentWidth(WellPaidMaxContentWidth),
    ) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { viewModel.previousMonth() }, enabled = !state.isLoading) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.home_prev_month))
            }
            Text(
                text = state.period.format(monthTitleFormatter).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale("pt", "PT")) else it.toString()
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { viewModel.nextMonth() }, enabled = !state.isLoading) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.home_next_month))
            }
        }

        WellPaidPrimaryAddRow(
            label = stringResource(R.string.expense_fab_new),
            leadingIcon = Icons.AutoMirrored.Filled.List,
            onPrimaryClick = onNewExpense,
            onRefresh = { viewModel.refresh() },
            refreshEnabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 10.dp),
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = state.statusFilter == ExpenseStatusFilter.ALL,
                    onClick = { viewModel.setStatusFilter(ExpenseStatusFilter.ALL) },
                    enabled = !state.isLoading,
                    label = { Text(stringResource(R.string.expenses_filter_all)) },
                )
            }
            item {
                FilterChip(
                    selected = state.statusFilter == ExpenseStatusFilter.PENDING,
                    onClick = { viewModel.setStatusFilter(ExpenseStatusFilter.PENDING) },
                    enabled = !state.isLoading,
                    label = { Text(stringResource(R.string.expenses_filter_pending)) },
                )
            }
            item {
                FilterChip(
                    selected = state.statusFilter == ExpenseStatusFilter.PAID,
                    onClick = { viewModel.setStatusFilter(ExpenseStatusFilter.PAID) },
                    enabled = !state.isLoading,
                    label = { Text(stringResource(R.string.expenses_filter_paid)) },
                )
            }
        }

        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                modifier = Modifier.padding(bottom = 8.dp),
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
                CircularProgressIndicator(color = WellPaidNavy)
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(tabSwipe),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    if (state.expenses.isEmpty() && !state.isLoading) {
                        item {
                            Text(
                                text = stringResource(R.string.expenses_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    }
                    items(state.expenses, key = { it.id }) { expense ->
                        ExpenseListRow(
                            expense = expense,
                            deleteEnabled = !state.isLoading,
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
                        HorizontalDivider()
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

@Composable
private fun ExpenseTypeTagPar() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFE3F2FD),
    ) {
        Text(
            text = "PAR",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1565C0),
        )
    }
}

@Composable
private fun ExpenseTypeTagRec() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = WellPaidGold.copy(alpha = 0.38f),
    ) {
        Text(
            text = "REC",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
    }
}

@Composable
private fun ExpenseTypeTagAnt() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFE8F5E9),
    ) {
        Text(
            text = "ANT",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2E7D32),
        )
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
    val statusWord = when {
        isPaid -> stringResource(R.string.expenses_status_paid)
        else -> stringResource(R.string.expenses_status_pending)
    }
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

    val metaLine = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = WellPaidNavy.copy(alpha = 0.58f),
                fontWeight = FontWeight.Normal,
            ),
        ) {
            append(expense.categoryName)
        }
        append(" · ")
        withStyle(
            SpanStyle(
                color = dateStatusColor,
                fontWeight = dateStatusWeight,
            ),
        ) {
            append(expenseDateBr)
            append(" · ")
            append(statusWord)
        }
        if (expense.installmentTotal > 1) {
            append(" · ")
            withStyle(SpanStyle(color = WellPaidNavy.copy(alpha = 0.58f))) {
                append(
                    stringResource(
                        R.string.expenses_installment_short,
                        expense.installmentNumber,
                        expense.installmentTotal,
                    ),
                )
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onRowClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp),
            ) {
                Text(
                    text = expense.description,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = WellPaidNavy,
                )
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
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
                        modifier = Modifier.padding(top = 2.dp),
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
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (expense.isProjected) {
                    Text(
                        text = stringResource(R.string.expenses_projected_suffix),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    if (expense.sharedExpensePaymentAlert) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = stringResource(R.string.expense_share_alert_short),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    if (showParTag(expense)) {
                        ExpenseTypeTagPar()
                    }
                    if (showRecTag(expense)) {
                        ExpenseTypeTagRec()
                    }
                    if (showAntTag(expense)) {
                        ExpenseTypeTagAnt()
                    }
                    Text(
                        text = formatBrlFromCents(expense.amountCents),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = WellPaidNavy,
                    )
                }
                if (!isPaid && (expense.isMine || expense.isShared)) {
                    Text(
                        text = stringResource(R.string.expenses_pay),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { onPayClick() },
                    )
                }
            }
        }
        if (onDeleteClick != null) {
            IconButton(
                onClick = onDeleteClick,
                enabled = deleteEnabled,
                modifier = Modifier.heightIn(min = 40.dp),
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
