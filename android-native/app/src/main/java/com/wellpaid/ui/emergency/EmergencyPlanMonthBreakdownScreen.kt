package com.wellpaid.ui.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatBrlFromCents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyPlanMonthBreakdownScreen(
    planId: String,
    onNavigateBack: () -> Unit,
    viewModel: EmergencyReserveViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(planId) {
        viewModel.loadPlanMonthBreakdown(planId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.emergency_plan_detail_breakdown_section),
                        style = MaterialTheme.typography.titleLarge,
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
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            when {
                state.planDetailMonthsLoading -> {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                state.planDetailMonthsError != null -> {
                    Text(
                        text = state.planDetailMonthsError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                state.planDetailMonthRows.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.emergency_plan_detail_months_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    state.planDetailMonthRows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = viewModel.formatAccrualMonth(row.year, row.month),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(
                                        R.string.emergency_plan_month_row_expected_deposited,
                                        formatBrlFromCents(row.expectedCents),
                                        formatBrlFromCents(row.depositedCents),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.emergency_plan_month_row_cumulative_delta,
                                        formatBrlFromCents(row.cumulativeDeltaCents),
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = WellPaidNavy,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
