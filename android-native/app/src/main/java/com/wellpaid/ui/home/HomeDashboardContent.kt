package com.wellpaid.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.wellpaid.R
import com.wellpaid.ui.theme.WellPaidCardWhite
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidNavyDeep
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.util.formatBrlFromCents
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeDashboardContent(
    mainRouteEntry: NavBackStackEntry,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
    onOpenDisplayName: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var menuOpen by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0] ?: Locale.getDefault()
    val monthTitleFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(
            if (locale.language.equals("en", ignoreCase = true)) "MMMM yyyy" else "LLLL yyyy",
            locale,
        )
    }
    val monthShortNavFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("MM/yyyy", locale)
    }
    val pullRefreshing = state.isLoading && state.overview != null
    val pullRefreshState = rememberPullRefreshState(
        refreshing = pullRefreshing,
        onRefresh = { viewModel.refresh() },
    )

    LaunchedEffect(mainRouteEntry) {
        snapshotFlow {
            mainRouteEntry.savedStateHandle.get<Long>("user_profile_dirty") ?: 0L
        }.distinctUntilChanged().collect { t ->
            if (t > 0L) viewModel.refresh()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WellPaidCream),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(WellPaidNavyDeep, WellPaidNavy),
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 2.dp),
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val greeting = state.userFirstName?.let { n ->
                    stringResource(R.string.home_greeting_named, n)
                }
                if (greeting != null) {
                    Text(
                        text = greeting,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                            tint = Color.White,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_title)) },
                            onClick = {
                                menuOpen = false
                                onOpenSettings()
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Settings, contentDescription = null)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.display_name_menu_item)) },
                            onClick = {
                                menuOpen = false
                                onOpenDisplayName()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.logout)) },
                            onClick = {
                                menuOpen = false
                                onLogout()
                            },
                        )
                    }
                }
            }

            state.overview?.let { o ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                        .padding(horizontal = 8.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    HeaderMetric(
                        label = stringResource(R.string.home_metric_income),
                        value = formatBrlFromCents(o.monthIncomeCents),
                        valueColor = Color.White,
                        modifier = Modifier.weight(1f),
                        align = Alignment.Start,
                        compact = true,
                    )
                    HeaderMetric(
                        label = stringResource(R.string.home_metric_balance),
                        value = formatBrlFromCents(o.monthBalanceCents),
                        valueColor = WellPaidGold,
                        valueLarge = true,
                        modifier = Modifier.weight(1f),
                        align = Alignment.CenterHorizontally,
                        compact = true,
                    )
                    HeaderMetric(
                        label = stringResource(R.string.home_metric_expenses),
                        value = formatBrlFromCents(o.monthExpenseTotalCents),
                        valueColor = WellPaidGold,
                        modifier = Modifier.weight(1f),
                        align = Alignment.End,
                        compact = true,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                    .padding(horizontal = 8.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = { viewModel.previousMonth() },
                    enabled = !state.isLoading,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.home_prev_month),
                        tint = WellPaidGold,
                    )
                }
                Text(
                    text = state.period.format(monthShortNavFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                IconButton(
                    onClick = { viewModel.nextMonth() },
                    enabled = !state.isLoading,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.home_next_month),
                        tint = WellPaidGold,
                    )
                }
            }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.isLoading && state.overview == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wellPaidMaxContentWidth(WellPaidMaxContentWidth),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                        .pullRefresh(pullRefreshState),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp)
                            .padding(top = 6.dp, bottom = 4.dp),
                    ) {
                        state.errorMessage?.let { msg ->
                            Text(
                                text = msg,
                                modifier = Modifier.padding(vertical = 4.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        val overview = state.overview
                        val cashflow = state.cashflow
                        val monthTitle = state.period.format(monthTitleFormatter).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                        }

                        if (overview != null && (cashflow != null || state.cashflowError != null)) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 40.dp),
                            ) {
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(0) }
                                    },
                                    selected = pagerState.currentPage == 0,
                                    icon = {
                                        SegmentedButtonDefaults.Icon(active = pagerState.currentPage == 0) {
                                            Icon(HomeChartTabIcons.Categories, contentDescription = null)
                                        }
                                    },
                                ) {
                                    Text(
                                        stringResource(R.string.home_tab_categories),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    selected = pagerState.currentPage == 1,
                                    icon = {
                                        SegmentedButtonDefaults.Icon(active = pagerState.currentPage == 1) {
                                            Icon(HomeChartTabIcons.Flow, contentDescription = null)
                                        }
                                    },
                                ) {
                                    Text(
                                        stringResource(R.string.home_tab_flow),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                colors = CardDefaults.cardColors(containerColor = WellPaidCardWhite),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(20.dp),
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    beyondViewportPageCount = 1,
                                ) { page ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 10.dp, top = 6.dp, end = 10.dp, bottom = 8.dp),
                                    ) {
                                        when (page) {
                                            0 -> {
                                                CategoryDonutChartPage(
                                                    monthTitle = monthTitle,
                                                    totalExpenseCents = overview.monthExpenseTotalCents,
                                                    spending = overview.spendingByCategory,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                            }
                                            else -> {
                                                Column(Modifier.fillMaxSize()) {
                                                    state.cashflowError?.let { msg ->
                                                        Text(
                                                            text = msg,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.padding(bottom = 6.dp),
                                                        )
                                                    }
                                                    cashflow?.let { cf ->
                                                        CashflowChartCard(
                                                            cashflow = cf,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxWidth(),
                                                            cashflowDynamic = state.cashflowDynamic,
                                                            forecastMonths = state.cashflowForecastMonths,
                                                            onCashflowDynamicChange = { viewModel.setCashflowDynamic(it) },
                                                            onForecastMonthsDelta = { viewModel.shiftForecastMonths(it) },
                                                        )
                                                    } ?: run {
                                                        if (state.cashflowError == null) {
                                                            Text(
                                                                text = stringResource(R.string.home_cashflow_empty),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (overview != null && cashflow == null && state.cashflowError == null && !state.isLoading) {
                            Text(
                                text = stringResource(R.string.home_cashflow_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    PullRefreshIndicator(
                        refreshing = pullRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Color.White,
    valueLarge: Boolean = false,
    align: Alignment.Horizontal = Alignment.Start,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier.widthIn(max = if (compact) 120.dp else 140.dp),
        horizontalAlignment = when (align) {
            Alignment.CenterHorizontally -> Alignment.CenterHorizontally
            Alignment.End -> Alignment.End
            else -> Alignment.Start
        },
    ) {
        Text(
            text = label,
            style = if (compact) {
                MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 11.sp)
            } else {
                MaterialTheme.typography.labelMedium
            },
            color = Color.White.copy(alpha = 0.88f),
            textAlign = when (align) {
                Alignment.CenterHorizontally -> TextAlign.Center
                Alignment.End -> TextAlign.End
                else -> TextAlign.Start
            },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = if (valueLarge) {
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = if (compact) 14.sp else 22.sp,
                    lineHeight = if (compact) 16.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                if (compact) {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                }
            },
            color = valueColor,
            textAlign = when (align) {
                Alignment.CenterHorizontally -> TextAlign.Center
                Alignment.End -> TextAlign.End
                else -> TextAlign.Start
            },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
