package com.wellpaid.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.expense.ExpenseDto
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.categoryAccentColor
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.formatIsoDateToBr
import com.wellpaid.util.parseIsoDateLocal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentPlanScreen(
    onNavigateBack: () -> Unit,
    onOpenExpenseDetail: (String) -> Unit,
    onPlanDeletedNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InstallmentPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFutureDialog by remember { mutableStateOf(false) }
    var showAllFirstDialog by remember { mutableStateOf(false) }
    var showAllSecondDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    val paid = state.installments.filter { it.status == "paid" }
    val pendingSorted = remember(state.installments) {
        state.installments
            .filter { it.status != "paid" }
            .sortedWith(
                compareBy<ExpenseDto> {
                    parseIsoDateLocal(it.dueDate ?: it.expenseDate) ?: LocalDate.MAX
                }.thenBy { it.installmentNumber },
            )
    }
    val total = state.installments.firstOrNull()?.installmentTotal ?: state.installments.size
    val paidFraction = if (total > 0) paid.size.toFloat() / total.toFloat() else 0f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.installment_plan_title),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.installments.isNotEmpty()) {
                            Text(
                                text = viewModel.descriptionFromFirst(),
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
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
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.installment_plan_help_cd),
                            tint = Color.White,
                        )
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WellPaidCream)
                .padding(inner)
                .wellPaidScreenHorizontalPadding(),
        ) {
            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = WellPaidNavy)
                    }
                }
                state.installments.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.installment_plan_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = WellPaidCreamMuted,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, WellPaidNavy.copy(alpha = 0.08f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.installment_plan_progress_label,
                                    paid.size,
                                    total,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = WellPaidNavy.copy(alpha = 0.85f),
                            )
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { paidFraction.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = WellPaidGold,
                                trackColor = WellPaidNavy.copy(alpha = 0.12f),
                                strokeCap = StrokeCap.Round,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showFutureDialog = true },
                            enabled = !state.isDeleting && pendingSorted.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.installment_plan_action_future),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        OutlinedButton(
                            onClick = { showAllFirstDialog = true },
                            enabled = !state.isDeleting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.installment_plan_action_delete_all),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        when {
                            pendingSorted.isNotEmpty() -> {
                                item {
                                    InstallmentSectionHeader(
                                        title = stringResource(R.string.installment_plan_section_unpaid),
                                        hint = stringResource(R.string.installment_plan_list_hint),
                                        modifier = Modifier.padding(bottom = 8.dp),
                                    )
                                }
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = WellPaidCreamMuted,
                                        shadowElevation = 2.dp,
                                        border = BorderStroke(1.dp, WellPaidNavy.copy(alpha = 0.09f)),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        ) {
                                            val last = pendingSorted.lastIndex
                                            pendingSorted.forEachIndexed { index, row ->
                                                val zebra = if (index % 2 == 1) {
                                                    WellPaidNavy.copy(alpha = 0.042f)
                                                } else {
                                                    Color.Transparent
                                                }
                                                InstallmentPlanRow(
                                                    expense = row,
                                                    compact = true,
                                                    today = today,
                                                    isNextDue = index == 0,
                                                    modifier = Modifier.background(zebra),
                                                    onClick = { onOpenExpenseDetail(row.id) },
                                                )
                                                if (index < last) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(start = 16.dp),
                                                        color = WellPaidNavy.copy(alpha = 0.1f),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            state.installments.isNotEmpty() -> {
                                item {
                                    Text(
                                        text = stringResource(R.string.installment_plan_all_paid),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text(stringResource(R.string.installment_plan_help_title)) },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = stringResource(R.string.installment_plan_help_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(stringResource(R.string.common_ok))
                }
            },
        )
    }

    if (showFutureDialog) {
        AlertDialog(
            onDismissRequest = { showFutureDialog = false },
            title = { Text(stringResource(R.string.installment_plan_confirm_future_title)) },
            text = { Text(stringResource(R.string.installment_plan_confirm_future_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFutureDialog = false
                        viewModel.removeFutureUnpaid(onPlanDeletedNavigateBack)
                    },
                    enabled = !state.isDeleting,
                ) {
                    Text(stringResource(R.string.expense_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFutureDialog = false }) {
                    Text(stringResource(R.string.expense_delete_cancel))
                }
            },
        )
    }

    if (showAllFirstDialog) {
        AlertDialog(
            onDismissRequest = { showAllFirstDialog = false },
            title = { Text(stringResource(R.string.installment_plan_confirm_all_1_title)) },
            text = { Text(stringResource(R.string.installment_plan_confirm_all_1_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAllFirstDialog = false
                        showAllSecondDialog = true
                    },
                    enabled = !state.isDeleting,
                ) {
                    Text(stringResource(R.string.common_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAllFirstDialog = false }) {
                    Text(stringResource(R.string.expense_delete_cancel))
                }
            },
        )
    }

    if (showAllSecondDialog) {
        AlertDialog(
            onDismissRequest = { showAllSecondDialog = false },
            title = { Text(stringResource(R.string.installment_plan_confirm_all_2_title)) },
            text = { Text(stringResource(R.string.installment_plan_confirm_all_2_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAllSecondDialog = false
                        viewModel.removeEntirePlanIncludingPaid(onPlanDeletedNavigateBack)
                    },
                    enabled = !state.isDeleting,
                ) {
                    Text(stringResource(R.string.expense_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAllSecondDialog = false }) {
                    Text(stringResource(R.string.expense_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun InstallmentSectionHeader(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarMonth,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp),
            tint = WellPaidGold.copy(alpha = 0.95f),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy.copy(alpha = 0.62f),
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = WellPaidNavy.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun InstallmentPlanRow(
    expense: ExpenseDto,
    onClick: () -> Unit,
    compact: Boolean,
    today: LocalDate,
    modifier: Modifier = Modifier,
    isNextDue: Boolean = false,
) {
    val isPaid = expense.status == "paid"
    val vPad = if (compact) 4.dp else 8.dp
    val stripeW = if (isNextDue) 4.dp else 3.dp
    val stripe = when {
        isNextDue -> WellPaidGold.copy(alpha = 0.92f)
        else -> rowStripeColor(expense, isPaid, today)
    }
    val interestLine = interestPercentLabel(expense.monthlyInterestBps)
    val categoryAccent = categoryAccentColor(expense.categoryId)
    val rowCd = stringResource(R.string.installment_plan_row_open_cd) + ". " +
        stringResource(
            R.string.expenses_line_installment,
            expense.installmentNumber,
            expense.installmentTotal,
        ) + ", " + formatBrlFromCents(expense.amountCents)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = rowCd }
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(stripeW)
                .fillMaxHeight()
                .background(stripe),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = stripeW)
                .padding(vertical = vPad, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .padding(end = 4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (isNextDue) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.installment_plan_next_cd),
                            modifier = Modifier.size(14.dp),
                            tint = WellPaidGold,
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.expenses_line_installment,
                            expense.installmentNumber,
                            expense.installmentTotal,
                        ),
                        style = if (compact) {
                            MaterialTheme.typography.bodyMedium
                        } else {
                            MaterialTheme.typography.titleSmall
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                DueDateAnnotatedLine(
                    expense = expense,
                    today = today,
                    isPaid = isPaid,
                    compact = compact,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1.15f)
                    .padding(horizontal = 2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(categoryAccent),
                    )
                    Text(
                        text = expense.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (compact) 10.sp else 11.sp,
                        lineHeight = 13.sp,
                        fontStyle = if (expense.isProjected) FontStyle.Italic else FontStyle.Normal,
                        color = if (expense.isProjected) {
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)
                        } else {
                            WellPaidNavy.copy(alpha = 0.78f)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (interestLine != null) {
                    Text(
                        text = stringResource(R.string.installment_plan_interest_am, interestLine),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = if (compact) 10.sp else 11.sp,
                        lineHeight = 13.sp,
                        color = WellPaidNavy.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                InstallmentStatusChip(isPaid, expense, compact, today)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatBrlFromCents(expense.amountCents),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = WellPaidNavy,
                    maxLines = 1,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(18.dp),
                tint = WellPaidNavy.copy(alpha = 0.36f),
            )
        }
    }
}

@Composable
private fun DueDateAnnotatedLine(
    expense: ExpenseDto,
    today: LocalDate,
    isPaid: Boolean,
    compact: Boolean,
) {
    val fs = if (compact) 10.sp else 11.sp
    val lh = 13.sp
    if (isPaid) {
        Text(
            text = paidSubtitle(expense),
            style = MaterialTheme.typography.labelSmall,
            fontSize = fs,
            lineHeight = lh,
            color = WellPaidNavy.copy(alpha = 0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    val due = parseIsoDateLocal(expense.dueDate ?: expense.expenseDate)
    val dateBr = formatIsoDateToBr(expense.dueDate ?: expense.expenseDate)
    val suffix = if (due != null) {
        val days = ChronoUnit.DAYS.between(today, due)
        when {
            days < 0 -> stringResource(R.string.installment_plan_relative_overdue, -days)
            days == 0L -> stringResource(R.string.installment_plan_relative_today)
            days == 1L -> stringResource(R.string.installment_plan_relative_tomorrow)
            days <= 7L -> stringResource(R.string.installment_plan_relative_in_days, days)
            else -> null
        }
    } else {
        null
    }
    val urgencyColor = when {
        due == null -> null
        due.isBefore(today) -> Color(0xFFB71C1C)
        ChronoUnit.DAYS.between(today, due) <= 7L -> Color(0xFFE65100)
        else -> null
    }
    val annotated = buildAnnotatedString {
        withStyle(SpanStyle(color = WellPaidNavy.copy(alpha = 0.52f))) {
            append(dateBr)
        }
        if (suffix != null) {
            append(" · ")
            withStyle(
                SpanStyle(
                    color = urgencyColor ?: WellPaidNavy.copy(alpha = 0.62f),
                    fontWeight = if (urgencyColor != null) FontWeight.SemiBold else FontWeight.Normal,
                ),
            ) {
                append(suffix)
            }
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.labelSmall,
        fontSize = fs,
        lineHeight = lh,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun InstallmentStatusChip(
    paid: Boolean,
    expense: ExpenseDto,
    compact: Boolean,
    today: LocalDate,
) {
    val label = if (paid) {
        stringResource(R.string.installment_plan_status_paid)
    } else {
        val due = parseIsoDateLocal(expense.dueDate ?: expense.expenseDate)
        when {
            due?.isBefore(today) == true -> stringResource(R.string.installment_plan_chip_late)
            due != null && ChronoUnit.DAYS.between(today, due) <= 7L ->
                stringResource(R.string.installment_plan_chip_soon)
            else -> stringResource(R.string.installment_plan_chip_scheduled)
        }
    }
    val bg = when {
        paid -> Color(0xFFC8E6C9)
        else -> {
            val due = parseIsoDateLocal(expense.dueDate ?: expense.expenseDate)
            when {
                due == null -> WellPaidGold.copy(alpha = 0.38f)
                due.isBefore(today) -> Color(0xFFFFCDD2)
                ChronoUnit.DAYS.between(today, due) <= 7L -> Color(0xFFFFE0B2)
                else -> WellPaidGold.copy(alpha = 0.38f)
            }
        }
    }
    val fg = when {
        paid -> Color(0xFF1B5E20)
        else -> {
            val due = parseIsoDateLocal(expense.dueDate ?: expense.expenseDate)
            when {
                due == null -> WellPaidNavy
                due.isBefore(today) -> Color(0xFFB71C1C)
                ChronoUnit.DAYS.between(today, due) <= 7L -> Color(0xFFE65100)
                else -> WellPaidNavy
            }
        }
    }
    val padH = if (compact) 6.dp else 8.dp
    val padV = if (compact) 2.dp else 4.dp
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bg,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = padH, vertical = padV),
            style = MaterialTheme.typography.labelSmall,
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg,
        )
    }
}

@Composable
private fun paidSubtitle(expense: ExpenseDto): String {
    val raw = expense.paidAt.orEmpty()
    val datePart = raw.take(10)
    val paidDay = parseIsoDateLocal(datePart)
    val formatted = if (paidDay != null) formatIsoDateToBr(datePart) else datePart
    return stringResource(R.string.installment_plan_paid_on_line, formatted)
}

private fun rowStripeColor(expense: ExpenseDto, isPaid: Boolean, today: LocalDate): Color {
    if (isPaid) return Color(0xFF66BB6A).copy(alpha = 0.65f)
    val due = parseIsoDateLocal(expense.dueDate ?: expense.expenseDate)
        ?: return WellPaidGold.copy(alpha = 0.45f)
    val days = ChronoUnit.DAYS.between(today, due)
    return when {
        days < 0 -> Color(0xFFE53935).copy(alpha = 0.75f)
        days <= 7L -> Color(0xFFFFB74D).copy(alpha = 0.85f)
        else -> WellPaidGold.copy(alpha = 0.45f)
    }
}

private fun interestPercentLabel(bps: Int?): String? {
    if (bps == null || bps <= 0) return null
    val pct = bps / 100.0
    val nf = NumberFormat.getNumberInstance(Locale("pt", "BR"))
    nf.minimumFractionDigits = 0
    nf.maximumFractionDigits = 2
    return nf.format(pct) + "%"
}
