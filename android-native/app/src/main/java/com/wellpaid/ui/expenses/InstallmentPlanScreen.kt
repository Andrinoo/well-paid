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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier,
    viewModel: InstallmentPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
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
                    val total = state.installments.first().installmentTotal
                    Text(
                        text = stringResource(R.string.installment_plan_summary, state.installments.size, total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WellPaidNavy.copy(alpha = 0.75f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.installments, key = { it.id }) { row ->
                            InstallmentPlanRow(
                                expense = row,
                                onClick = { onOpenExpenseDetail(row.id) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstallmentPlanRow(
    expense: ExpenseDto,
    onClick: () -> Unit,
) {
    val isPaid = expense.status == "paid"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
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
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
            )
            Text(
                text = stringResource(
                    R.string.installment_plan_due_line,
                    formatIsoDateToBr(expense.dueDate ?: expense.expenseDate),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = WellPaidNavy.copy(alpha = 0.65f),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            StatusChip(isPaid)
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatBrlFromCents(expense.amountCents),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = WellPaidNavy,
            )
        }
    }
}

@Composable
private fun StatusChip(paid: Boolean) {
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
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
    }
}
