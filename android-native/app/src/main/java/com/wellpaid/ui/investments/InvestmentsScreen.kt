package com.wellpaid.ui.investments

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidNavyDeep
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatBrlFromCents
import java.time.Month
import java.time.format.TextStyle
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
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
            .padding(innerPadding)
            .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
            .padding(horizontal = 12.dp, vertical = 10.dp),
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
            if (overview?.ratesFallbackUsed == true) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.investments_rates_estimated_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = WellPaidGold,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
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
            TextButton(onClick = {
                if (state.showCreatePositionForm) viewModel.closeCreatePositionForm()
                else viewModel.openCreatePositionForm()
            }) {
                Text(
                    text = if (state.showCreatePositionForm) {
                        stringResource(R.string.common_cancel)
                    } else {
                        stringResource(R.string.investments_add_position)
                    }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.compactListMode,
                onClick = { viewModel.setCompactListMode(true) },
                label = { Text(stringResource(R.string.investments_view_compact)) },
            )
            FilterChip(
                selected = !state.compactListMode,
                onClick = { viewModel.setCompactListMode(false) },
                label = { Text(stringResource(R.string.investments_view_cards)) },
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("cdi", "cdb", "fixed_income", "tesouro", "stocks").forEach { type ->
                        FilterChip(
                            selected = state.newPositionType == type,
                            onClick = { viewModel.setNewPositionType(type) },
                            label = {
                                Text(
                                    text = instrumentLabelForKey(type),
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.newPositionName,
                    onValueChange = { viewModel.setNewPositionName(it) },
                    label = { Text(stringResource(R.string.investments_field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.newPositionType == "stocks") {
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
                    if (state.newPositionType != "stocks") {
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
            if (state.compactListMode) {
                InvestmentPositionCompactRow(
                    name = position.name,
                    line = line,
                    onDetails = { viewModel.openPositionDetails(position.id) },
                    onTopUp = { viewModel.startTopUpFromPosition(position.id) },
                    onDelete = { viewModel.deletePosition(position.id) },
                )
            } else {
                InvestmentPositionCard(
                    name = position.name,
                    line = line,
                    onDetails = { viewModel.openPositionDetails(position.id) },
                    onTopUp = { viewModel.startTopUpFromPosition(position.id) },
                    onDelete = { viewModel.deletePosition(position.id) },
                )
            }
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
        } else {
            Spacer(Modifier.height(12.dp))
            overview?.buckets.orEmpty().forEach { bucket ->
                InvestmentBucketCard(
                    title = instrumentLabelForKey(bucket.key, fallback = bucket.label),
                    hint = stringResource(
                        R.string.investments_bucket_line_template,
                        formatBrlFromCents(bucket.allocatedCents),
                        formatBrlFromCents(bucket.yieldCents),
                        bucket.yieldPctMonth,
                    ),
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.evolution.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.investments_evolution_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = WellPaidNavy,
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendDot(color = WellPaidGold, label = stringResource(R.string.investments_evolution_legend_normal))
                    LegendDot(
                        color = MaterialTheme.colorScheme.tertiary,
                        label = stringResource(R.string.investments_evolution_legend_estimated),
                    )
                }
                Spacer(Modifier.height(6.dp))
                val maxProjected = max(
                    1,
                    state.evolution.maxOf { it.projectedTotalCents },
                )
                state.evolution.forEach { point ->
                    val monthLabel = Month.of(point.month)
                        .getDisplayName(TextStyle.SHORT, locale)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                    val ratio = point.projectedTotalCents.toFloat() / maxProjected.toFloat()
                    InvestmentEvolutionBarRow(
                        monthLabel = "$monthLabel/${point.year}",
                        ratio = ratio,
                        projectedLabel = formatBrlFromCents(point.projectedTotalCents),
                        cumulativeLabel = formatBrlFromCents(point.cumulativeYieldCents),
                        isEstimated = point.isEstimated,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = { viewModel.refresh() },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WellPaidGold,
                contentColor = WellPaidNavy,
                disabledContainerColor = WellPaidGold.copy(alpha = 0.45f),
                disabledContentColor = WellPaidNavy.copy(alpha = 0.75f),
            ),
        ) {
            Text(
                text = if (state.isLoading) {
                    stringResource(R.string.investments_loading_button)
                } else {
                    stringResource(R.string.investments_refresh_button)
                },
                fontWeight = FontWeight.SemiBold,
            )
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
                    onClose = { viewModel.closePositionDetails() },
                )
            }
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
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
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
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
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
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
