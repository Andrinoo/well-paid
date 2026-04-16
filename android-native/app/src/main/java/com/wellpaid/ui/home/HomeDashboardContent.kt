package com.wellpaid.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.wellpaid.R
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidNavyDeep
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidMotion
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.DiscreetBalanceValue
import androidx.compose.animation.core.tween
import com.wellpaid.util.formatBrlFromCents
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeDashboardContent(
    mainRouteEntry: NavBackStackEntry,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0] ?: Locale.getDefault()
    val monthTitleFormatter = remember(locale) {
        DateTimeFormatter.ofPattern(
            if (locale.language.equals("en", ignoreCase = true)) "MMMM yyyy" else "LLLL yyyy",
            locale,
        )
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(WellPaidNavyDeep, WellPaidNavy),
                    ),
                ),
        ) {
            val density = LocalDensity.current
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) {
                val h = maxHeight.toPx()
                if (h > 0f) h else wPx * 0.42f
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.065f), Color.Transparent),
                            center = Offset(x = wPx * 0.28f, y = hPx * 0.14f),
                            radius = wPx * 1.02f,
                        ),
                    ),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.7f to Color.Transparent,
                                1f to Color(0xFF020617).copy(alpha = 0.12f),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 10.dp),
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val greetingText = state.userFirstName?.let { n ->
                    stringResource(R.string.home_greeting_named, n)
                } ?: stringResource(R.string.home_greeting_fallback)
                Text(
                    text = greetingText,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.1.sp,
                    ),
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.98f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            AnimatedVisibility(
                visible = state.overview != null,
                modifier = Modifier.fillMaxWidth(),
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = WellPaidMotion.FadeMediumMs,
                        easing = WellPaidMotion.StandardEasing,
                    ),
                ) + slideInVertically(
                    initialOffsetY = { it / 12 },
                    animationSpec = tween(
                        durationMillis = WellPaidMotion.FadeMediumMs,
                        easing = WellPaidMotion.StandardEasing,
                    ),
                ),
                exit = fadeOut(tween(WellPaidMotion.FadeShortMs)) +
                    slideOutVertically(
                        animationSpec = tween(WellPaidMotion.FadeShortMs),
                        targetOffsetY = { it / 20 },
                    ),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    state.overview?.let { o ->
                        HeaderMetricBalance(
                            label = stringResource(R.string.home_metric_balance),
                            balanceCents = o.monthBalanceCents,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(top = 6.dp, bottom = 2.dp),
                            align = Alignment.Start,
                            compact = true,
                        )
                    }
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
                            .padding(horizontal = 12.dp)
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
                        if (state.announcements.isNotEmpty()) {
                            val topAnnouncement = state.announcements.first()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = topAnnouncement.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WellPaidNavyDeep,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = topAnnouncement.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WellPaidNavy,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        } else if (state.announcementsError != null) {
                            Text(
                                text = state.announcementsError,
                                modifier = Modifier.padding(bottom = 4.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }

                        val overview = state.overview
                        val cashflow = state.cashflow
                        val monthTitle = state.period.format(monthTitleFormatter).replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                        }

                        if (overview != null && (cashflow != null || state.cashflowError != null)) {
                            val shortcutSegmentRadius = 5.dp
                            val shortcutSegmentShapeStart = RoundedCornerShape(
                                topStart = shortcutSegmentRadius,
                                bottomStart = shortcutSegmentRadius,
                            )
                            val shortcutSegmentShapeEnd = RoundedCornerShape(
                                topEnd = shortcutSegmentRadius,
                                bottomEnd = shortcutSegmentRadius,
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(shortcutSegmentRadius),
                                color = WellPaidCream,
                                shadowElevation = 1.dp,
                                tonalElevation = 0.dp,
                            ) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 32.dp),
                            ) {
                                SegmentedButton(
                                    shape = shortcutSegmentShapeStart,
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(0) }
                                    },
                                    selected = pagerState.currentPage == 0,
                                    icon = {
                                        SegmentedButtonDefaults.Icon(active = pagerState.currentPage == 0) {
                                            Icon(
                                                HomeChartTabIcons.Categories,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    },
                                ) {
                                    Text(
                                        stringResource(R.string.home_tab_categories),
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                SegmentedButton(
                                    shape = shortcutSegmentShapeEnd,
                                    onClick = {
                                        scope.launch { pagerState.animateScrollToPage(1) }
                                    },
                                    selected = pagerState.currentPage == 1,
                                    icon = {
                                        SegmentedButtonDefaults.Icon(active = pagerState.currentPage == 1) {
                                            Icon(
                                                HomeChartTabIcons.Flow,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    },
                                ) {
                                    Text(
                                        stringResource(R.string.home_tab_flow),
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            }

                            Spacer(Modifier.height(4.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                colors = CardDefaults.cardColors(containerColor = WellPaidCream),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 6.dp,
                                    hoveredElevation = 5.dp,
                                ),
                                shape = RoundedCornerShape(shortcutSegmentRadius),
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    HomeMonthNavigationCardBar(
                                        monthLabel = monthTitle,
                                        onPrev = { viewModel.previousMonth() },
                                        onNext = { viewModel.nextMonth() },
                                        enabled = !state.isLoading,
                                    )
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        beyondViewportPageCount = 1,
                                    ) { page ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(start = 12.dp, top = 0.dp, end = 12.dp, bottom = 8.dp),
                                    ) {
                                        when (page) {
                                            0 -> {
                                                CategoryDonutChartPage(
                                                    monthTitle = monthTitle,
                                                    totalExpenseCents = overview.monthExpenseTotalCents,
                                                    spending = overview.spendingByCategory,
                                                    showMonthInDonutCenter = false,
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
private fun HomeMonthNavigationCardBar(
    monthLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp, max = 44.dp)
            .padding(horizontal = 2.dp, vertical = 0.dp),
    ) {
        IconButton(
            onClick = onPrev,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.home_prev_month),
                tint = WellPaidGold,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = monthLabel,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp),
            style = MaterialTheme.typography.titleSmall.copy(lineHeight = 18.sp),
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        IconButton(
            onClick = onNext,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.home_next_month),
                tint = WellPaidGold,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun HeaderMetricBalance(
    label: String,
    balanceCents: Int,
    modifier: Modifier = Modifier,
    valueColor: Color = WellPaidGold,
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
                MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp)
            } else {
                MaterialTheme.typography.labelMedium
            },
            color = Color.White.copy(alpha = 0.92f),
            textAlign = when (align) {
                Alignment.CenterHorizontally -> TextAlign.Center
                Alignment.End -> TextAlign.End
                else -> TextAlign.Start
            },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val valueStyle = MaterialTheme.typography.titleMedium.copy(
            fontSize = if (compact) 16.sp else 22.sp,
            lineHeight = if (compact) 19.sp else 26.sp,
            fontWeight = FontWeight.Bold,
        )
        DiscreetBalanceValue(
            cents = balanceCents,
            style = valueStyle,
            color = valueColor,
            textAlign = when (align) {
                Alignment.CenterHorizontally -> TextAlign.Center
                Alignment.End -> TextAlign.End
                else -> TextAlign.Start
            },
        )
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
                MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp)
            } else {
                MaterialTheme.typography.labelMedium
            },
            color = Color.White.copy(alpha = 0.92f),
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
                    fontSize = if (compact) 16.sp else 22.sp,
                    lineHeight = if (compact) 19.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                if (compact) {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Bold,
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
