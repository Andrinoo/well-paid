package com.wellpaid.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.expense.ExpenseDto
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.formatIsoDateToBr

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

    val pending = state.installments.filter { it.status != "paid" }
    val paid = state.installments.filter { it.status == "paid" }
    val total = state.installments.firstOrNull()?.installmentTotal ?: state.installments.size

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
                    Text(
                        text = stringResource(
                            R.string.installment_plan_counts_line,
                            pending.size,
                            paid.size,
                            total,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = WellPaidNavy.copy(alpha = 0.72f),
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { showFutureDialog = true },
                            enabled = !state.isDeleting && pending.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
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
                            shape = RoundedCornerShape(10.dp),
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
                    ) {
                        if (pending.isNotEmpty()) {
                            item {
                                SectionLabel(stringResource(R.string.installment_plan_section_due))
                            }
                            items(pending, key = { it.id }) { row ->
                                InstallmentPlanRow(
                                    expense = row,
                                    compact = true,
                                    onClick = { onOpenExpenseDetail(row.id) },
                                )
                                HorizontalDivider(color = WellPaidNavy.copy(alpha = 0.08f))
                            }
                        }
                        if (paid.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(4.dp))
                                SectionLabel(stringResource(R.string.installment_plan_section_paid))
                            }
                            items(paid, key = { it.id }) { row ->
                                InstallmentPlanRow(
                                    expense = row,
                                    compact = true,
                                    onClick = { onOpenExpenseDetail(row.id) },
                                )
                                HorizontalDivider(color = WellPaidNavy.copy(alpha = 0.08f))
                            }
                        }
                    }
                }
            }
        }
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
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = WellPaidNavy.copy(alpha = 0.55f),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun InstallmentPlanRow(
    expense: ExpenseDto,
    onClick: () -> Unit,
    compact: Boolean,
) {
    val isPaid = expense.status == "paid"
    val vPad = if (compact) 5.dp else 10.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = vPad),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.expenses_line_installment,
                    expense.installmentNumber,
                    expense.installmentTotal,
                ),
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.installment_plan_due_line,
                    formatIsoDateToBr(expense.dueDate ?: expense.expenseDate),
                ),
                style = MaterialTheme.typography.labelSmall,
                fontSize = if (compact) 10.sp else 12.sp,
                lineHeight = 13.sp,
                color = WellPaidNavy.copy(alpha = 0.62f),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            StatusChip(isPaid, compact)
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatBrlFromCents(expense.amountCents),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = WellPaidNavy,
            )
        }
    }
}

@Composable
private fun StatusChip(paid: Boolean, compact: Boolean) {
    val label = if (paid) {
        stringResource(R.string.installment_plan_status_paid)
    } else {
        stringResource(R.string.installment_plan_status_due)
    }
    val bg = if (paid) {
        Color(0xFFE8F5E9)
    } else {
        WellPaidGold.copy(alpha = 0.35f)
    }
    val padH = if (compact) 6.dp else 8.dp
    val padV = if (compact) 2.dp else 4.dp
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = padH, vertical = padV),
            style = MaterialTheme.typography.labelSmall,
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
    }
}
