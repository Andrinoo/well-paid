package com.wellpaid.ui.investments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidCardWhite
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidPositive

private val JoinCardCorner = RoundedCornerShape(14.dp)
private val JoinHeroCorner = RoundedCornerShape(16.dp)
private val JoinFieldCorner = RoundedCornerShape(12.dp)
private data class JoinMetric(val label: String, val value: String)

@Composable
fun InvestmentsStockJoinScreen(
    state: InvestmentsUiState,
    onDescriptionChange: (String) -> Unit,
    onModeByValueChange: (Boolean) -> Unit,
    onQuantityChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WellPaidCream)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.investments_stock_join_title),
            style = MaterialTheme.typography.titleSmall,
            color = WellPaidNavy,
            fontWeight = FontWeight.SemiBold,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = JoinHeroCorner,
            color = WellPaidNavy,
            shadowElevation = 1.dp,
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = investmentInstrumentLabel(
                        key = state.newPositionType,
                        fallback = stringResource(R.string.investments_bucket_stocks),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.newPositionName,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        val hasMarketBlock = state.quoteInfoMessage != null || state.selectedFundamentals != null
        if (hasMarketBlock) {
            val quoteMeta = state.quoteInfoMessage
                ?.substringAfter("·", missingDelimiterValue = "")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WellPaidCreamMuted.copy(alpha = 0.72f), JoinCardCorner)
                    .border(1.dp, WellPaidGold.copy(alpha = 0.38f), JoinCardCorner)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.investments_stock_join_section_market),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = WellPaidNavy.copy(alpha = 0.88f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(0.9f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = "Cotação",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WellPaidNavy.copy(alpha = 0.88f),
                            fontWeight = FontWeight.Medium,
                        )
                        state.quoteLastPrice?.takeIf { it > 0.0 }?.let { price ->
                            val priceLabel = String.format(java.util.Locale.US, "%.2f", price).replace('.', ',')
                            Text(
                                text = "R$ $priceLabel",
                                style = MaterialTheme.typography.headlineMedium,
                                color = WellPaidPositive,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        quoteMeta?.let { meta ->
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.labelSmall,
                                color = WellPaidNavy.copy(alpha = 0.72f),
                            )
                        }
                    }
                    state.selectedFundamentals?.let { f ->
                        Column(modifier = Modifier.weight(1.8f)) {
                            StockJoinFundamentalsRow(
                                assetType = state.newPositionType,
                                fundamentals = f,
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WellPaidCardWhite, JoinCardCorner)
                .border(1.dp, WellPaidGold.copy(alpha = 0.38f), JoinCardCorner)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.investments_stock_join_section_form),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy.copy(alpha = 0.88f),
            )
            OutlinedTextField(
                value = state.stockJoinDescription,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.investments_field_description)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = JoinFieldCorner,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = JoinFieldCorner,
                color = WellPaidCreamMuted.copy(alpha = 0.55f),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.investments_mode_by_value),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WellPaidNavy,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.stockJoinModeByValue,
                        onCheckedChange = onModeByValueChange,
                    )
                }
            }
            if (state.stockJoinModeByValue) {
                WellPaidMoneyDigitKeypadField(
                    valueText = state.newPositionPrincipalText,
                    onValueTextChange = onValueChange,
                    enabled = !state.isSavingPosition,
                    label = { Text(stringResource(R.string.investments_field_principal)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OutlinedTextField(
                    value = state.quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text(stringResource(R.string.investments_field_quantity)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = JoinFieldCorner,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }

        state.stockJoinAdjustedAlert?.let { alert ->
            Text(
                text = alert,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
        }
    }
}

@Composable
private fun StockJoinFundamentalsRow(
    assetType: String,
    fundamentals: FundamentalPreviewUi,
) {
    val metrics = metricsForAssetType(assetType, fundamentals)
    val firstRow = metrics.take(4)
    val secondRow = metrics.drop(4).take(4)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            firstRow.forEach { metric ->
                StockJoinMetricCell(
                    modifier = Modifier.weight(1f),
                    label = metric.label,
                    value = metric.value,
                )
            }
        }
        if (secondRow.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                secondRow.forEach { metric ->
                    StockJoinMetricCell(
                        modifier = Modifier.weight(1f),
                        label = metric.label,
                        value = metric.value,
                    )
                }
            }
        }
    }
}

@Composable
private fun metricsForAssetType(
    assetType: String,
    fundamentals: FundamentalPreviewUi,
): List<JoinMetric> {
    val dy = JoinMetric(stringResource(R.string.investments_metric_dy), fundamentals.dy ?: "—")
    val dy12m = JoinMetric(stringResource(R.string.investments_metric_dy_12m), fundamentals.dy12m ?: fundamentals.dy ?: "—")
    val pvp = JoinMetric(stringResource(R.string.investments_metric_pvp), fundamentals.pvp ?: "—")
    val pl = JoinMetric(stringResource(R.string.investments_metric_pl), fundamentals.pl ?: "—")
    val dailyLiquidity = JoinMetric(
        stringResource(R.string.investments_metric_daily_liquidity),
        fundamentals.dailyLiquidity ?: "—",
    )
    val vacancyFinancial = JoinMetric(
        stringResource(R.string.investments_metric_vacancy_financial),
        fundamentals.vacancyFinancial ?: "—",
    )
    val contractTermWault = JoinMetric(
        stringResource(R.string.investments_metric_contract_term_wault),
        fundamentals.contractTermWault ?: "—",
    )
    val atypicalContractsRatio = JoinMetric(
        stringResource(R.string.investments_metric_atypical_contracts),
        fundamentals.atypicalContractsRatio ?: "—",
    )
    val top5TenantsConcentration = JoinMetric(
        stringResource(R.string.investments_metric_top5_tenants),
        fundamentals.top5TenantsConcentration ?: "—",
    )
    val roe = JoinMetric(stringResource(R.string.investments_metric_roe), fundamentals.roe ?: "—")
    val ev = JoinMetric(stringResource(R.string.investments_metric_ev_ebitda), fundamentals.evEbitda ?: "—")
    val netMargin = JoinMetric(stringResource(R.string.investments_metric_net_margin), fundamentals.netMargin ?: "—")
    val netDebt = JoinMetric(stringResource(R.string.investments_metric_net_debt_ebitda), fundamentals.netDebtEbitda ?: "—")
    val eps = JoinMetric(stringResource(R.string.investments_metric_eps), fundamentals.eps ?: "—")

    return when (assetType.lowercase()) {
        // FIIs: show income/asset-value indicators first.
        "fii" -> listOf(
            dy12m,
            pvp,
            dailyLiquidity,
            vacancyFinancial,
            contractTermWault,
            atypicalContractsRatio,
            top5TenantsConcentration,
        )
        // ETFs usually have limited fundamentals from free providers.
        "etf" -> listOf(pvp, dy, pl, ev)
        // BDRs behave similar to equities, keep equity set with P/VP visible.
        "bdr", "stock", "stocks" -> listOf(dy, pl, pvp, roe, ev, netMargin, netDebt, eps)
        else -> listOf(dy, pl, pvp, roe, ev, netMargin, netDebt, eps)
    }
}

@Composable
private fun StockJoinMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = WellPaidCardWhite,
        border = BorderStroke(1.dp, WellPaidGold.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
