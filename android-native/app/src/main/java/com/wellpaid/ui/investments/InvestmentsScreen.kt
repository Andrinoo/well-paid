package com.wellpaid.ui.investments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.investment.InvestmentBucketDto
import com.wellpaid.core.model.investment.InvestmentAssetType
import com.wellpaid.core.model.investment.InvestmentPositionDto
import com.wellpaid.core.model.investment.StockHistoryPointDto
import com.wellpaid.core.model.investment.StockQuoteDto
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.components.WellPaidPullToRefreshBox
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidCardWhite
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidPositive
import com.wellpaid.ui.theme.WellPaidExpenseLine
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidNavyDeep
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.LocalPrivacyHideBalance
import com.wellpaid.ui.theme.formatBrlFromCentsRespectPrivacy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatDecimalPtBr
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsScreen(
    onNavigateBack: () -> Unit,
    onOpenAporte: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: InvestmentsViewModel = hiltViewModel(),
) {
    val hideBalance = LocalPrivacyHideBalance.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val overview = state.overview
    val pullRefreshing = state.isLoading && (overview != null || state.positions.isNotEmpty())
    val inInvestmentsSubFlow =
        state.showSearchResultsScreen || state.showStockJoinScreen || state.showFixedIncomeJoinScreen
    BackHandler(enabled = inInvestmentsSubFlow) {
        when {
            state.showStockJoinScreen -> viewModel.closeStockJoin()
            state.showFixedIncomeJoinScreen -> viewModel.closeFixedIncomeJoin()
            state.showSearchResultsScreen -> viewModel.closeSearchResults()
        }
    }

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
                    IconButton(
                        onClick = {
                            when {
                                state.showStockJoinScreen -> viewModel.closeStockJoin()
                                state.showFixedIncomeJoinScreen -> viewModel.closeFixedIncomeJoin()
                                state.showSearchResultsScreen -> viewModel.closeSearchResults()
                                else -> onNavigateBack()
                            }
                        },
                    ) {
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
            .imePadding()
            .padding(innerPadding),
    ) {
    if (state.showStockJoinScreen || state.showFixedIncomeJoinScreen) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                .padding(horizontal = 12.dp, vertical = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
            ) {
                state.errorMessage?.let { msg ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                state.infoMessage?.let { msg ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = WellPaidNavy,
                    )
                }
                if (state.showStockJoinScreen) {
                    InvestmentsStockJoinScreen(
                        state = state,
                        onDescriptionChange = { viewModel.setStockJoinDescription(it) },
                        onModeByValueChange = { viewModel.setStockJoinModeByValue(it) },
                        onQuantityChange = { viewModel.setQuantityText(it) },
                        onValueChange = { viewModel.setStockJoinValueText(it) },
                    )
                } else {
                    InvestmentsFixedIncomeJoinScreen(
                        state = state,
                        onDescriptionChange = { viewModel.setFixedIncomeDescription(it) },
                        onPrincipalChange = { viewModel.setNewPositionPrincipalText(it) },
                        onRateChange = { viewModel.setNewPositionAnnualRateText(it) },
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
            ) {
                Button(
                    onClick = { viewModel.createPosition() },
                    enabled = !state.isSavingPosition,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        text = if (state.isSavingPosition) {
                            stringResource(R.string.investments_saving_position)
                        } else {
                            stringResource(R.string.investments_save_position)
                        },
                    )
                }
                TextButton(
                    onClick = {
                        if (state.showStockJoinScreen) {
                            viewModel.closeStockJoin()
                        } else {
                            viewModel.closeFixedIncomeJoin()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.common_close)) }
            }
        }
    } else {
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
        state.infoMessage?.let { msg ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = WellPaidNavy,
            )
        }

        if (state.showSearchResultsScreen) {
            InvestmentsSearchScreen(
                query = state.globalSearchText,
                suggestions = state.globalTickerSuggestions,
                isSearching = state.isSearchingGlobalTickers,
                isLoadingTopMovers = state.isLoadingTopMovers,
                topHour = state.topMoversHour,
                topDay = state.topMoversDay,
                topWeek = state.topMoversWeek,
                onQueryChange = { viewModel.setGlobalSearchText(it) },
                onSelectTicker = { viewModel.selectTickerSuggestion(it, fromGlobalSearch = true) },
                onBack = { viewModel.closeSearchResults() },
            )
        } else {
        Spacer(Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(WellPaidNavyDeep, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Savings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = WellPaidGold,
                )
                Text(
                    text = stringResource(R.string.investments_summary_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatBrlFromCentsRespectPrivacy(overview?.totalAllocatedCents ?: 0, hideBalance),
                style = MaterialTheme.typography.titleLarge,
                color = WellPaidGold,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(
                    R.string.investments_summary_caption,
                    formatBrlFromCentsRespectPrivacy(overview?.totalYieldCents ?: 0, hideBalance),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
            )
            state.macroSnapshot?.let { macro ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "CDI ${macro.cdi ?: "—"} · SELIC ${macro.selic ?: "—"} · IPCA ${macro.ipca ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
            if (overview?.ratesFallbackUsed == true) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.investments_rates_estimated_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = WellPaidGold,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(5.dp))
            OutlinedTextField(
                value = state.globalSearchText,
                onValueChange = { viewModel.setGlobalSearchText(it) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.investments_global_search_label),
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.labelMedium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = WellPaidCreamMuted,
                    unfocusedContainerColor = WellPaidCreamMuted,
                    focusedTextColor = WellPaidNavy,
                    unfocusedTextColor = WellPaidNavy,
                    focusedPlaceholderColor = WellPaidNavy.copy(alpha = 0.65f),
                    unfocusedPlaceholderColor = WellPaidNavy.copy(alpha = 0.65f),
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.investments_family_search_toggle),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Switch(
                    checked = state.familySearchEnabled,
                    onCheckedChange = { viewModel.setFamilySearchEnabled(it) },
                )
            }
            Text(
                text = stringResource(R.string.investments_global_search_helper),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
            )
        }

        Spacer(Modifier.height(8.dp))
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

        if (
            state.showCreatePositionForm &&
            !state.showStockJoinScreen &&
            !state.showFixedIncomeJoinScreen &&
            !state.showSearchResultsScreen
        ) {
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
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("${suggestion.symbol} · ${suggestion.name}")
                                Text(
                                    text = "${investmentInstrumentLabel(suggestion.instrumentType)} · ${suggestion.source.uppercase(Locale.ROOT)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
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
                if (isTraditionalEquityTypeRule(state.newPositionType)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = state.quantityText,
                            onValueChange = { viewModel.setQuantityText(it) },
                            label = { Text(stringResource(R.string.investments_field_quantity)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = state.averagePriceText,
                            onValueChange = { viewModel.setAveragePriceText(it) },
                            label = { Text(stringResource(R.string.investments_field_average_price)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.targetPriceText,
                        onValueChange = { viewModel.setTargetPriceText(it) },
                        label = { Text(stringResource(R.string.investments_field_target_price)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.selectedFundamentals?.let { f ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = fundamentalsSummaryLine(f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (!isVariableIncomeTypeRule(state.newPositionType)) {
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

        InvestmentPositionsCarousel(
            positions = state.positions,
            fundamentalsByPositionId = state.positionCardFundamentals,
            quotesByPositionId = state.positionCardQuotes,
            onDetails = { id -> viewModel.openPositionDetails(id) },
        )
        if (state.positions.isNotEmpty()) {
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
    }
    }
    state.selectedPositionId?.let { selectedId ->
        val selected = state.positions.firstOrNull { it.id == selectedId }
        if (selected != null) {
            key(selectedId) {
                val positionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val positionSheetTopFlat = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp,
                )
                ModalBottomSheet(
                    onDismissRequest = { viewModel.closePositionDetails() },
                    sheetState = positionSheetState,
                    shape = positionSheetTopFlat,
                    containerColor = WellPaidCream,
                    dragHandle = null,
                ) {
                    InvestmentPositionDetailsSheet(
                        name = selected.name,
                        line = stringResource(
                            R.string.investments_position_line,
                            investmentInstrumentLabel(selected.instrumentType),
                            formatBrlFromCentsRespectPrivacy(selected.principalCents, hideBalance),
                            selected.annualRateBps / 100f,
                        ),
                        fundamentals = state.positionDetailsFundamentals,
                        isLoadingFundamentals = state.isLoadingPositionDetailsFundamentals,
                        onTopUp = {
                            val id = selected.id
                            viewModel.closePositionDetails()
                            onOpenAporte(id)
                        },
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
    val hideBalance = LocalPrivacyHideBalance.current
    val maxClose = max(1, points.maxOfOrNull { it.close }?.toInt() ?: 1)
    val minClose = points.minOfOrNull { it.close } ?: 0.0
    val selectedPoint = points.lastOrNull()
    val pricePalette = remember {
        listOf(
            WellPaidGold,
            WellPaidPositive,
            WellPaidExpenseLine,
            WellPaidNavy,
            Color(0xFF4F46E5),
            Color(0xFF0E7490),
            Color(0xFFB45309),
            Color(0xFF7C3AED),
            Color(0xFFBE123C),
            Color(0xFF15803D),
        )
    }
    val priceToColor = remember(points) {
        val colors = linkedMapOf<Long, Color>()
        points.forEach { point ->
            val priceKey = (point.close * 100.0).roundToLong()
            if (!colors.containsKey(priceKey)) {
                colors[priceKey] = pricePalette[colors.size % pricePalette.size]
            }
        }
        colors
    }

    Column(
        modifier = modifier
            .background(
                color = WellPaidCreamMuted.copy(alpha = 0.54f),
                shape = RoundedCornerShape(10.dp),
            )
            .border(
                width = 1.dp,
                color = WellPaidGold.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
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
        Spacer(Modifier.height(4.dp))
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
                text = formatAsOfForDisplay(selectedPoint?.asOf),
                style = MaterialTheme.typography.labelMedium,
                color = WellPaidNavy,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatBrlFromCentsRespectPrivacy(((selectedPoint?.close ?: 0.0) * 100).toInt(), hideBalance),
                style = MaterialTheme.typography.titleMedium,
                color = WellPaidNavy,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
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
                    val priceKey = (point.close * 100.0).roundToLong()
                    val baseColor = priceToColor[priceKey] ?: WellPaidGold
                    var bar = baseColor
                    if (index == selectedIndex) {
                        bar = bar.copy(alpha = (bar.alpha * 0.6f + 0.4f).coerceIn(0.5f, 1f))
                    } else {
                        bar = bar.copy(alpha = 0.72f)
                    }
                    drawRoundRect(
                        color = bar,
                        topLeft = androidx.compose.ui.geometry.Offset(x - (barWidth / 2f), top),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    )
                    if (index == selectedIndex) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.9f),
                            topLeft = androidx.compose.ui.geometry.Offset(x - (barWidth / 2f), top),
                            size = androidx.compose.ui.geometry.Size(barWidth, 2.5f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                        )
                    }
                }
            }
        }

        if (buckets.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            buckets.forEach { bucket ->
                Text(
                    text = stringResource(
                        R.string.investments_bucket_line_template,
                        formatBrlFromCentsRespectPrivacy(bucket.allocatedCents, hideBalance),
                        formatBrlFromCentsRespectPrivacy(bucket.yieldCents, hideBalance),
                        bucket.yieldPctMonth,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InvestmentPositionsCarousel(
    positions: List<InvestmentPositionDto>,
    fundamentalsByPositionId: Map<String, FundamentalPreviewUi>,
    quotesByPositionId: Map<String, StockQuoteDto>,
    onDetails: (String) -> Unit,
) {
    if (positions.isEmpty()) return
    val startPage = remember(positions.size) {
        if (positions.size <= 1) 0 else {
            val mid = Int.MAX_VALUE / 2
            mid - (mid % positions.size)
        }
    }
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { if (positions.size <= 1) 1 else Int.MAX_VALUE },
    )
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxWidth(),
        pageSpacing = 0.dp,
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) { page ->
        val index = if (positions.size <= 1) 0 else page % positions.size
        val position = positions[index]
        Box(modifier = Modifier.fillMaxWidth()) {
            InvestmentPositionCard(
                position = position,
                fundamentals = fundamentalsByPositionId[position.id],
                quote = quotesByPositionId[position.id],
                onDetails = { onDetails(position.id) },
            )
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onDetails),
        ) {
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
        FilledTonalButton(
            onClick = onTopUp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) { Text(stringResource(R.string.investments_top_up), maxLines = 1) }
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
    position: InvestmentPositionDto,
    fundamentals: FundamentalPreviewUi?,
    quote: StockQuoteDto?,
    onDetails: () -> Unit,
) {
    val hideBalance = LocalPrivacyHideBalance.current
    val normalizedType = position.instrumentType.trim().lowercase(Locale.ROOT)
    val isCrypto = normalizedType == "crypto" || normalizedType == "cripto"
    val instrumentKey = if (isCrypto) "crypto" else InvestmentAssetType.fromRaw(position.instrumentType).key
    val cardBackground = when (instrumentKey) {
        "stock" -> WellPaidGold
        "fii" -> WellPaidNavy
        "etf" -> Color(0xFF0F4C5C)
        "bdr" -> Color(0xFF5A3E2B)
        else -> Color(0xFF2F3E5C)
    }
    val onCardColor = if (instrumentKey == "stock") WellPaidNavy else Color.White
    val titleHighlightColor = when (instrumentKey) {
        "stock" -> WellPaidNavyDeep
        "fii" -> WellPaidGold
        "etf" -> Color(0xFF7AE0F8)
        "bdr" -> Color(0xFFFFD27A)
        else -> WellPaidGold
    }
    val iconContainerColor = if (instrumentKey == "stock") WellPaidNavy else Color.White.copy(alpha = 0.2f)
    val iconTint = if (instrumentKey == "stock") WellPaidGold else Color.White
    val borderColor = titleHighlightColor.copy(alpha = if (instrumentKey == "stock") 0.55f else 0.48f)
    val instrumentLabel = when (instrumentKey) {
        "crypto" -> stringResource(R.string.investments_bucket_crypto)
        "fii" -> stringResource(R.string.investments_instrument_fii)
        "etf" -> stringResource(R.string.investments_instrument_etf)
        "bdr" -> stringResource(R.string.investments_instrument_bdr)
        "stock" -> stringResource(R.string.investments_instrument_stock)
        else -> stringResource(R.string.investments_instrument_equity)
    }
    val leadingIcon = when (instrumentKey) {
        "crypto" -> Icons.Filled.Savings
        "fii" -> Icons.Filled.PieChart
        "etf" -> Icons.AutoMirrored.Filled.ShowChart
        "bdr" -> Icons.Filled.TrackChanges
        else -> Icons.Filled.BarChart
    }
    val principal = formatBrlFromCentsRespectPrivacy(position.principalCents, hideBalance)
    val metricOr = { s: String? -> if (s.isNullOrBlank()) "—" else s }
    val pvp = metricOr(fundamentals?.pvp)
    val dailyLiquidity = metricOr(fundamentals?.dailyLiquidity)
    val pl = metricOr(fundamentals?.pl)
    val roe = metricOr(fundamentals?.roe)
    val dy = metricOr(fundamentals?.dy)
    val evEbitda = metricOr(fundamentals?.evEbitda)
    val netMargin = metricOr(fundamentals?.netMargin)
    val netDebtEbitda = metricOr(fundamentals?.netDebtEbitda)
    val lpa = metricOr(fundamentals?.eps)
    val quotePrice = quotePriceLabelForCard(quote = quote, hideBalance = hideBalance)
    val quoteChange = quoteChangeLabelForCard(quote = quote)
    val quoteHigh = quoteExtremaLabelForCard(quote?.dayHigh, quote?.currency, hideBalance)
    val quoteLow = quoteExtremaLabelForCard(quote?.dayLow, quote?.currency, hideBalance)
    val quoteVolume = quote?.volume24h?.let { formatDecimalPtBr(it, minFractionDigits = 0, maxFractionDigits = 0) } ?: "—"
    val quoteSource = quote?.source?.uppercase(Locale.ROOT) ?: "—"
    val annualRateLabel = if (position.annualRateBps > 0) {
        formatDecimalPtBr(position.annualRateBps / 100.0) + "% a.a."
    } else {
        "—"
    }
    val maturityLabel = metricOr(formatMaturityForPositionCard(position.maturityDate))
    val liquidityLabel = if (position.isLiquid) {
        stringResource(R.string.investments_liquidity_high)
    } else {
        stringResource(R.string.investments_liquidity_low)
    }
    val fixedIncomeTypeLabel = when (instrumentKey) {
        "treasury" -> stringResource(R.string.investments_instrument_treasury)
        "cdb" -> stringResource(R.string.investments_instrument_cdb)
        "cdi" -> stringResource(R.string.investments_instrument_cdi)
        "fixed_income" -> stringResource(R.string.investments_instrument_fixed_income)
        else -> "—"
    }
    val cardShape = RoundedCornerShape(14.dp)
    val iconCellBg = Color.White.copy(alpha = 0.28f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = cardShape,
                ambientColor = WellPaidNavy.copy(alpha = 0.32f),
                spotColor = WellPaidNavy.copy(alpha = 0.32f),
            ),
        shape = cardShape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackground, cardShape)
                .clickable(onClick = onDetails)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconContainerColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = iconTint,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = position.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleHighlightColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = instrumentLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = titleHighlightColor.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.investments_position_card_value_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = onCardColor.copy(alpha = 0.8f),
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = principal,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onCardColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.investments_view_details),
                    tint = onCardColor.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = onCardColor.copy(alpha = 0.2f),
            )
            Spacer(Modifier.height(1.dp))
            val metrics = when (instrumentKey) {
                "crypto" -> listOf(
                    Triple(Icons.Filled.MonetizationOn, stringResource(R.string.investments_metric_price), quotePrice),
                    Triple(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.investments_quote_metric_change), quoteChange),
                    Triple(Icons.Filled.BarChart, stringResource(R.string.investments_quote_metric_high), quoteHigh),
                    Triple(Icons.Filled.TrackChanges, stringResource(R.string.investments_quote_metric_low), quoteLow),
                    Triple(Icons.Filled.Receipt, stringResource(R.string.investments_quote_metric_volume), quoteVolume),
                    Triple(Icons.Filled.Balance, stringResource(R.string.goal_list_label_source), quoteSource),
                )
                "fii" -> listOf(
                    Triple(Icons.Filled.MonetizationOn, stringResource(R.string.investments_metric_dy), dy),
                    Triple(Icons.Filled.BarChart, stringResource(R.string.investments_metric_pvp), pvp),
                    Triple(Icons.Filled.TrackChanges, stringResource(R.string.investments_metric_daily_liquidity), dailyLiquidity),
                    Triple(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.investments_metric_pl), pl),
                    Triple(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.investments_metric_ev_ebitda), evEbitda),
                    Triple(Icons.Filled.Balance, stringResource(R.string.investments_position_fundamental_net_debt), netDebtEbitda),
                )
                "etf" -> listOf(
                    Triple(Icons.Filled.BarChart, stringResource(R.string.investments_metric_pvp), pvp),
                    Triple(Icons.Filled.MonetizationOn, stringResource(R.string.investments_metric_dy), dy),
                    Triple(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.investments_metric_pl), pl),
                    Triple(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.investments_metric_ev_ebitda), evEbitda),
                )
                "treasury", "cdb", "cdi", "fixed_income" -> listOf(
                    Triple(Icons.Filled.TrackChanges, stringResource(R.string.investments_metric_annual_rate), annualRateLabel),
                    Triple(Icons.Filled.Receipt, stringResource(R.string.investments_metric_maturity), maturityLabel),
                    Triple(Icons.Filled.Balance, stringResource(R.string.investments_metric_liquidity), liquidityLabel),
                    Triple(Icons.Filled.Savings, stringResource(R.string.investments_metric_class), fixedIncomeTypeLabel),
                )
                else -> listOf(
                    Triple(Icons.Filled.BarChart, stringResource(R.string.investments_metric_pl), pl),
                    Triple(Icons.Filled.TrackChanges, stringResource(R.string.investments_metric_roe), roe),
                    Triple(Icons.Filled.MonetizationOn, stringResource(R.string.investments_metric_dy), dy),
                    Triple(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.investments_metric_ev_ebitda), evEbitda),
                    Triple(Icons.Filled.PieChart, stringResource(R.string.investments_position_fundamental_net_margin), netMargin),
                    Triple(Icons.Filled.Balance, stringResource(R.string.investments_position_fundamental_net_debt), netDebtEbitda),
                    Triple(Icons.Filled.Receipt, stringResource(R.string.investments_metric_eps), lpa),
                )
            }
            val targetMetricRows = 7 // Keep all cards same size as stock-ticker cards.
            for (index in 0 until targetMetricRows) {
                if (index < metrics.size) {
                    val metric = metrics[index]
                    PositionFundamentalRow(
                        icon = metric.first,
                        label = metric.second,
                        value = metric.third,
                        iconBoxColor = iconCellBg,
                        contentColor = onCardColor,
                        showDivider = index < targetMetricRows - 1,
                    )
                } else {
                    PositionFundamentalRowPlaceholder(
                        iconBoxColor = iconCellBg,
                        dividerColor = onCardColor.copy(alpha = 0.12f),
                        showDivider = index < targetMetricRows - 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PositionFundamentalRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconBoxColor: Color,
    contentColor: Color,
    showDivider: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(iconBoxColor, RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = contentColor,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
            )
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = contentColor.copy(alpha = 0.12f),
            )
        }
    }
}

@Composable
private fun PositionFundamentalRowPlaceholder(
    iconBoxColor: Color,
    dividerColor: Color,
    showDivider: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(iconBoxColor, RoundedCornerShape(7.dp)),
            )
            Spacer(Modifier.width(8.dp))
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(24.dp))
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = dividerColor,
            )
        }
    }
}

@Composable
private fun InvestmentPositionDetailsSheet(
    name: String,
    line: String,
    fundamentals: FundamentalPreviewUi?,
    isLoadingFundamentals: Boolean,
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
    val config = LocalConfiguration.current
    val maxBodyHeight = kotlin.math.min(
        (config.screenHeightDp * 0.92f).toInt(),
        700,
    ).dp
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxBodyHeight)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
        fundamentals?.companyName?.trim()?.takeIf { cn ->
            cn.isNotEmpty() && !cn.equals(name, ignoreCase = true)
        }?.let { company ->
            Text(
                text = company,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = line,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isLoadingFundamentals) {
            Text(
                text = stringResource(R.string.investments_loading_button),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            fundamentals?.let { f ->
                Text(
                    text = stringResource(R.string.investments_stock_join_section_market),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = WellPaidNavy.copy(alpha = 0.88f),
                )
                Text(
                    text = fundamentalsSummaryLine(f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Fonte: ${f.source.uppercase(Locale.ROOT)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = onTopUp,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.investments_top_up), maxLines = 1) }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.investments_delete_position),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        TextButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.common_close))
        }
    }
}

private fun formatMaturityForPositionCard(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        val s = raw.trim()
        if (s.length < 10) return null
        val date = java.time.LocalDate.parse(s.take(10))
        date.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault()),
        )
    }.getOrNull()
}

private fun quotePriceLabelForCard(quote: StockQuoteDto?, hideBalance: Boolean): String {
    val price = quote?.lastPrice ?: return "—"
    if (price <= 0.0) return "—"
    if (hideBalance) return "---"
    val prefix = when (quote.currency.trim().uppercase(Locale.ROOT)) {
        "BRL" -> "R$ "
        "USD" -> "US$ "
        else -> ""
    }
    return prefix + formatDecimalPtBr(price)
}

private fun quoteExtremaLabelForCard(value: Double?, currency: String?, hideBalance: Boolean): String {
    val v = value ?: return "—"
    if (v <= 0.0) return "—"
    if (hideBalance) return "---"
    val prefix = when (currency?.trim()?.uppercase(Locale.ROOT)) {
        "BRL" -> "R$ "
        "USD" -> "US$ "
        else -> ""
    }
    return prefix + formatDecimalPtBr(v)
}

private fun quoteChangeLabelForCard(quote: StockQuoteDto?): String {
    val pct = quote?.change24hPercent
    if (pct == null) return "—"
    val sign = if (pct > 0) "+" else ""
    return sign + formatDecimalPtBr(pct, 2, 2) + "%"
}

private fun fundamentalsSummaryLine(f: FundamentalPreviewUi): String {
    return "DY ${f.dy ?: "—"} · P/L ${f.pl ?: "—"} · ROE ${f.roe ?: "—"} · EV/EBITDA ${f.evEbitda ?: "—"} · Mrg. liq. ${f.netMargin ?: "—"} · Div. liq./EBITDA ${f.netDebtEbitda ?: "—"} · LPA ${f.eps ?: "—"}"
}

private fun formatAsOfForDisplay(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val parsed = java.time.OffsetDateTime.parse(
            iso,
            java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        )
        parsed.atZoneSameInstant(ZoneId.systemDefault()).format(
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT),
        )
    } catch (_: Exception) {
        try {
            val ins = java.time.Instant.parse(iso)
            ins.atZone(ZoneId.systemDefault()).format(
                DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT),
            )
        } catch (_: Exception) {
            iso
        }
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
