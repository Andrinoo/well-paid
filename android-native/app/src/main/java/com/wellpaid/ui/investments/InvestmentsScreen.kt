package com.wellpaid.ui.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.wellpaid.core.model.investment.InvestmentBucketDto
import com.wellpaid.core.model.investment.StockHistoryPointDto
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.components.WellPaidPullToRefreshBox
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidNavyDeep
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatBrlFromCents
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvestmentsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overview = state.overview
    val pullRefreshing = state.isLoading && (overview != null || state.positions.isNotEmpty())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WellPaidCream,
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.investments_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = Color.White,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
    WellPaidPullToRefreshBox(
        refreshing = pullRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
            .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        state.errorMessage?.let { msg ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WellPaidNavyDeep, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Savings,
                    contentDescription = null,
                    tint = WellPaidGold,
                )
                Text(
                    text = stringResource(R.string.investments_summary_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = formatBrlFromCents(overview?.totalAllocatedCents ?: 0),
                style = MaterialTheme.typography.headlineSmall,
                color = WellPaidGold,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.investments_summary_caption,
                    formatBrlFromCents(overview?.totalYieldCents ?: 0),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
            state.macroSnapshot?.let { macro ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "CDI ${macro.cdi ?: "—"} · SELIC ${macro.selic ?: "—"} · IPCA ${macro.ipca ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
            if (overview?.ratesFallbackUsed == true) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.investments_rates_estimated_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = WellPaidGold,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        if (state.showCreatePositionForm) viewModel.closeCreatePositionForm()
                        else viewModel.openCreatePositionForm()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidGold,
                        contentColor = WellPaidNavy,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        text = if (state.showCreatePositionForm) {
                            stringResource(R.string.common_cancel)
                        } else {
                            stringResource(R.string.investments_add_position)
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.investments_positions_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
            )
            Text(
                text = "${state.positions.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WellPaidNavy,
            )
        }
        Spacer(Modifier.height(8.dp))

        if (state.showCreatePositionForm) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp),
            ) {
                OutlinedTextField(
                    value = state.newPositionName,
                    onValueChange = { viewModel.setNewPositionName(it) },
                    label = { Text(stringResource(R.string.investments_field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                val detectedTicker = detectTickerFromText(state.newPositionName)
                if (state.isSearchingTickers || state.tickerSuggestions.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                }
                if (state.isSearchingTickers) {
                    Text(
                        text = stringResource(R.string.investments_loading_button),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.tickerSuggestions.forEach { suggestion ->
                        TextButton(
                            onClick = { viewModel.selectTickerSuggestion(suggestion.symbol) },
                        ) {
                            Text("${suggestion.symbol} · ${suggestion.name}")
                        }
                    }
                }
                if (!detectedTicker.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TextButton(
                            onClick = { viewModel.fetchB3StockQuote() },
                            enabled = !state.isFetchingQuote && !state.isSavingPosition,
                        ) {
                            if (state.isFetchingQuote) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(stringResource(R.string.investments_fetch_b3_quote))
                            }
                        }
                    }
                }
                state.quoteInfoMessage?.let { q ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = q,
                        style = MaterialTheme.typography.bodySmall,
                        color = WellPaidNavy,
                    )
                    val src = state.quoteSourceLabel
                    if (!src.isNullOrBlank()) {
                        Text(
                            text = "Fonte: ${src.uppercase(Locale.ROOT)}" + (state.quoteConfidence?.let { c -> " · conf ${"%.2f".format(Locale.US, c)}" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                WellPaidMoneyDigitKeypadField(
                    valueText = state.newPositionPrincipalText,
                    onValueTextChange = { viewModel.setNewPositionPrincipalText(it) },
                    enabled = !state.isSavingPosition,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.investments_field_principal)) },
                    placeholder = stringResource(R.string.emergency_monthly_placeholder),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.newPositionAnnualRateText,
                        onValueChange = { viewModel.setNewPositionAnnualRateText(it) },
                        label = { Text(stringResource(R.string.investments_field_rate)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    if (detectedTicker.isNullOrBlank()) {
                        TextButton(
                            onClick = { viewModel.applyMarketRateToForm() },
                            enabled = !state.isLoadingSuggestedRates && !state.isSavingPosition,
                        ) {
                            if (state.isLoadingSuggestedRates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(
                                    stringResource(R.string.investments_apply_market_rate),
                                    maxLines = 2,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.createPosition() },
                    enabled = !state.isSavingPosition,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidNavy,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = if (state.isSavingPosition) {
                            stringResource(R.string.investments_saving_position)
                        } else {
                            stringResource(R.string.investments_save_position)
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        state.positions.forEach { position ->
            val line = stringResource(
                R.string.investments_position_line,
                instrumentLabelForKey(position.instrumentType),
                formatBrlFromCents(position.principalCents),
                position.annualRateBps / 100f,
            )
            InvestmentPositionCard(
                name = position.name,
                line = line,
                onDetails = { viewModel.openPositionDetails(position.id) },
                onTopUp = { viewModel.startTopUpFromPosition(position.id) },
                onDelete = { viewModel.deletePosition(position.id) },
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.isLoading && overview == null) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        Spacer(Modifier.height(12.dp))
    }
    }
    state.selectedPositionId?.let { selectedId ->
        val selected = state.positions.firstOrNull { it.id == selectedId }
        if (selected != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.closePositionDetails() },
            ) {
                InvestmentPositionDetailsSheet(
                    name = selected.name,
                    line = stringResource(
                        R.string.investments_position_line,
                        instrumentLabelForKey(selected.instrumentType),
                        formatBrlFromCents(selected.principalCents),
                        selected.annualRateBps / 100f,
                    ),
                    onTopUp = { viewModel.startTopUpFromPosition(selected.id) },
                    onDelete = {
                        viewModel.deletePosition(selected.id)
                        viewModel.closePositionDetails()
                    },
                    historyPoints = state.selectedPositionHistory,
                    historyRange = state.selectedHistoryRange,
                    historySymbol = state.selectedPositionHistorySymbol,
                    historySource = state.selectedPositionHistorySource,
                    historyConfidence = state.selectedPositionHistoryConfidence,
                    isLoadingHistory = state.isLoadingHistory,
                    historyErrorMessage = state.historyErrorMessage,
                    buckets = overview?.buckets.orEmpty(),
                    onSelectRange = { viewModel.setHistoryRange(it) },
                    onClose = { viewModel.closePositionDetails() },
                )
            }
        }
    }
    }
}

@Composable
private fun InvestmentEvolutionChart(
    points: List<StockHistoryPointDto>,
    symbol: String?,
    range: String,
    isLoading: Boolean,
    historyErrorMessage: String?,
    buckets: List<InvestmentBucketDto>,
    source: String?,
    confidence: Double?,
    modifier: Modifier = Modifier,
) {
    val maxClose = max(1, points.maxOfOrNull { it.close }?.toInt() ?: 1)
    val minClose = points.minOfOrNull { it.close } ?: 0.0
    val selectedPoint = points.lastOrNull()

    Column(
        modifier = modifier
            .background(
                color = WellPaidCreamMuted.copy(alpha = 0.54f),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = WellPaidGold.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = symbol?.ifBlank { "—" } ?: "—",
                style = MaterialTheme.typography.labelLarge,
                color = WellPaidNavy,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = range.uppercase(Locale.ROOT),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!source.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Fonte: ${source.uppercase(Locale.ROOT)}" + (confidence?.let { c -> " · conf ${"%.2f".format(Locale.US, c)}" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        if (isLoading) {
            Text(
                text = stringResource(R.string.investments_loading_button),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (points.isEmpty()) {
            Text(
                text = historyErrorMessage ?: stringResource(R.string.investments_quote_unavailable, "empty"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = selectedPoint?.asOf ?: "—",
                style = MaterialTheme.typography.labelMedium,
                color = WellPaidNavy,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatBrlFromCents(((selectedPoint?.close ?: 0.0) * 100).toInt()),
                style = MaterialTheme.typography.titleMedium,
                color = WellPaidNavy,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                val width = size.width
                val height = size.height
                val barSlot = width / points.size.toFloat()
                val barWidth = (barSlot * 0.62f).coerceAtLeast(8f)
                val valueRange = (maxClose - minClose.toInt()).coerceAtLeast(1).toFloat()
                val selectedIndex = points.lastIndex
                val selectedX = (selectedIndex * barSlot) + (barSlot / 2f)

                drawLine(
                    color = WellPaidNavy.copy(alpha = 0.18f),
                    start = androidx.compose.ui.geometry.Offset(selectedX, 0f),
                    end = androidx.compose.ui.geometry.Offset(selectedX, height),
                    strokeWidth = 2f,
                )
                points.forEachIndexed { index, point ->
                    val x = (index * barSlot) + (barSlot / 2f)
                    val normalized = (point.close - minClose).toFloat() / valueRange
                    val barHeight = (normalized * (height - 6f)).coerceAtLeast(8f)
                    val top = height - barHeight
                    drawRoundRect(
                        color = WellPaidGold.copy(alpha = if (index == selectedIndex) 0.96f else 0.72f),
                        topLeft = androidx.compose.ui.geometry.Offset(x - (barWidth / 2f), top),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                    )
                    if (index == selectedIndex) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.95f),
                            topLeft = androidx.compose.ui.geometry.Offset(x - (barWidth / 2f), top),
                            size = androidx.compose.ui.geometry.Size(barWidth, 3f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                        )
                    }
                }
            }
        }

        if (buckets.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            buckets.forEach { bucket ->
                Text(
                    text = stringResource(
                        R.string.investments_bucket_line_template,
                        formatBrlFromCents(bucket.allocatedCents),
                        formatBrlFromCents(bucket.yieldCents),
                        bucket.yieldPctMonth,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InvestmentPositionCompactRow(
    name: String,
    line: String,
    onDetails: () -> Unit,
    onTopUp: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = WellPaidCreamMuted.copy(alpha = 0.52f),
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = WellPaidGold.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
                maxLines = 1,
            )
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        TextButton(onClick = onDetails) { Text(stringResource(R.string.investments_view_details)) }
        TextButton(onClick = onTopUp) { Text(stringResource(R.string.investments_top_up)) }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.investments_delete_position),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun InvestmentPositionCard(
    name: String,
    line: String,
    onDetails: () -> Unit,
    onTopUp: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = WellPaidCreamMuted.copy(alpha = 0.56f),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = WellPaidGold.copy(alpha = 0.38f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
        Text(
            text = line,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TextButton(onClick = onDetails) { Text(stringResource(R.string.investments_view_details)) }
            TextButton(onClick = onTopUp) { Text(stringResource(R.string.investments_top_up)) }
            TextButton(onClick = onDelete) { Text(stringResource(R.string.investments_delete_position)) }
        }
    }
}

@Composable
private fun InvestmentPositionDetailsSheet(
    name: String,
    line: String,
    historyPoints: List<StockHistoryPointDto>,
    historyRange: String,
    historySymbol: String?,
    historySource: String?,
    historyConfidence: Double?,
    isLoadingHistory: Boolean,
    historyErrorMessage: String?,
    buckets: List<InvestmentBucketDto>,
    onSelectRange: (String) -> Unit,
    onTopUp: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.investments_evolution_title),
            style = MaterialTheme.typography.titleSmall,
            color = WellPaidNavy,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            InvestmentHistoryRanges.forEach { preset ->
                FilterChip(
                    selected = preset == historyRange,
                    onClick = { onSelectRange(preset) },
                    label = { Text(preset.uppercase(Locale.ROOT)) },
                )
            }
        }
        InvestmentEvolutionChart(
            points = historyPoints,
            symbol = historySymbol,
            range = historyRange,
            isLoading = isLoadingHistory,
            historyErrorMessage = historyErrorMessage,
            buckets = buckets,
            source = historySource,
            confidence = historyConfidence,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onTopUp,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WellPaidNavy,
                    contentColor = Color.White,
                ),
            ) { Text(stringResource(R.string.investments_top_up)) }
            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) { Text(stringResource(R.string.investments_delete_position)) }
        }
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.common_close))
        }
    }
}

@Composable
private fun instrumentLabelForKey(
    key: String,
    fallback: String? = null,
): String {
    return when (key.lowercase(Locale.ROOT)) {
        "cdi" -> stringResource(R.string.investments_bucket_cdi)
        "cdb" -> stringResource(R.string.investments_bucket_cdb)
        "fixed_income" -> stringResource(R.string.investments_bucket_fixed_income)
        "tesouro" -> stringResource(R.string.investments_bucket_tesouro)
        "stocks" -> stringResource(R.string.investments_bucket_stocks)
        else -> fallback ?: key.uppercase(Locale.ROOT)
    }
}

private fun detectTickerFromText(text: String): String? {
    val rx = Regex("([A-Za-z]{4}\\d{1,2})")
    return rx.find(text)?.groupValues?.getOrNull(1)?.uppercase(Locale.ROOT)
}

@Composable
private fun InvestmentEvolutionBarRow(
    monthLabel: String,
    ratio: Float,
    projectedLabel: String,
    cumulativeLabel: String,
    isEstimated: Boolean,
) {
    val barColor = if (isEstimated) MaterialTheme.colorScheme.tertiary else WellPaidGold
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = WellPaidCreamMuted.copy(alpha = 0.50f),
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = WellPaidGold.copy(alpha = 0.30f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
            )
            Text(
                text = projectedLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0.05f, 1f))
                    .height(8.dp)
                    .background(
                        color = barColor,
                        shape = RoundedCornerShape(8.dp),
                    ),
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.investments_evolution_gain_line, cumulativeLabel),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isEstimated) {
                Text(
                    text = stringResource(R.string.investments_evolution_estimated_tag),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 14.dp, height = 8.dp)
                .background(color = color, shape = RoundedCornerShape(8.dp)),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InvestmentBucketCard(
    title: String,
    hint: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = WellPaidCreamMuted.copy(alpha = 0.54f),
                shape = RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = WellPaidGold.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
