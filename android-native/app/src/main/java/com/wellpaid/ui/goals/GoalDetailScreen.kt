package com.wellpaid.ui.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.goal.GoalPriceAlternativeDto
import com.wellpaid.core.model.goal.GoalPriceHistoryItemDto
import com.wellpaid.ui.components.WellPaidPullToRefreshBox
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidPositive
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import coil.compose.AsyncImage
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.formatIsoDateToBr
import com.wellpaid.util.parseBrlToCents
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    onNavigateBack: () -> Unit,
    onEditGoal: (String) -> Unit,
    onGoalDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showContribute by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showProductDetails by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        stringResource(R.string.goal_detail_title),
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                actions = {
                    val g = state.goal
                    if (g != null && g.isMine) {
                        IconButton(onClick = { onEditGoal(g.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.goal_edit_action))
                        }
                    }
                },
            )
        },
    ) { inner ->
        if (state.isLoading && state.goal == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val goal = state.goal
        if (goal == null) {
            Column(
                Modifier
                    .padding(inner)
                    .wellPaidScreenHorizontalPadding(),
            ) {
                state.errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }
            return@Scaffold
        }

        val url = goal.targetUrl?.trim().orEmpty()
        val canRefreshPriceByTitle = goal.title.trim().length >= 2
        val canSwipeRefreshPrice = goal.isMine && (url.isNotEmpty() || canRefreshPriceByTitle)

        WellPaidPullToRefreshBox(
            refreshing = state.isRefreshingFromLink,
            onRefresh = {
                if (canSwipeRefreshPrice) {
                    viewModel.refreshTargetFromLink()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            val progress = if (goal.targetCents > 0) {
                (goal.currentCents.toFloat() / goal.targetCents.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .wellPaidScreenHorizontalPadding(),
            ) {
            GoalProgressStickyHeader(
                currentValue = formatBrlFromCents(goal.currentCents),
                targetValue = formatBrlFromCents(goal.targetCents),
                progress = progress,
                dueAtLabel = goal.dueAt?.takeIf { it.isNotBlank() }?.let { dueAt ->
                    stringResource(R.string.goal_field_due_date) + ": " + formatIsoDateToBr(dueAt)
                },
            )
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                goal.referenceThumbnailUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { thumb ->
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = MaterialTheme.typography.titleLarge.fontSize * 0.78f,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (goal.isActive) {
                            stringResource(R.string.goals_status_active)
                        } else {
                            stringResource(R.string.goals_status_archived)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (!goal.isMine) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.goal_readonly_family),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            Spacer(Modifier.height(20.dp))

            val remainingCents = (goal.targetCents - goal.currentCents).coerceAtLeast(0)
            GoalQuickStatsRow(
                savedValue = formatBrlFromCents(goal.currentCents),
                remainingValue = formatBrlFromCents(remainingCents),
                updatedValue = formatIsoDateToBr(goal.updatedAt),
            )
            Spacer(Modifier.height(14.dp))

            Spacer(Modifier.height(20.dp))
            if (goal.isMine && goal.isActive) {
                Button(
                    onClick = { showContribute = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !state.isSaving && !state.isDeleting,
                ) {
                    Text(
                        text = stringResource(R.string.goal_contribute_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            GoalPriceLineChart(
                history = state.priceHistory,
                targetCents = goal.targetCents,
                goalCreatedAt = goal.createdAt,
                fallbackReferencePriceCents = goal.referencePriceCents,
                fallbackAlternatives = goal.priceAlternatives,
                modifier = Modifier.fillMaxWidth(),
            )

            // Novos campos: link e preço de referência
            if (url.isNotEmpty() || goal.referencePriceCents != null || goal.referenceProductName != null ||
                goal.priceAlternatives.isNotEmpty() || (goal.isMine && canRefreshPriceByTitle)
            ) {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.goal_detail_section_link),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(
                        onClick = { showProductDetails = !showProductDetails },
                    ) {
                        Text(
                            text = if (showProductDetails) {
                                stringResource(R.string.goal_card_less)
                            } else {
                                stringResource(R.string.goal_card_more)
                            },
                        )
                    }
                }
                if (showProductDetails) {
                    Spacer(Modifier.height(6.dp))
                    if (url.isNotEmpty()) {
                        TextButton(onClick = { runCatching { uriHandler.openUri(url) } }) {
                            Text(stringResource(R.string.goal_detail_open_link))
                        }
                    }
                    goal.referenceProductName?.takeIf { it.isNotBlank() }?.let { name ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.goal_detail_product_name, name),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    goal.referencePriceCents?.let { cents ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.goal_detail_reference_price,
                                formatBrlFromCents(cents),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (goal.priceAlternatives.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.goal_detail_alternatives_count,
                                goal.priceAlternatives.size,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            if (goal.isMine) {
                Spacer(Modifier.height(24.dp))
                val actions = buildList {
                    add(
                        GoalDetailAction(
                            label = stringResource(R.string.goal_detail_edit_button),
                            enabled = !state.isSaving && !state.isDeleting,
                            onClick = { onEditGoal(goal.id) },
                            isDestructive = false,
                        ),
                    )
                    if (goal.currentCents == 0) {
                        add(
                            GoalDetailAction(
                                label = stringResource(R.string.goal_detail_delete_button),
                                enabled = !state.isSaving && !state.isDeleting,
                                onClick = { showDeleteConfirm = true },
                                isDestructive = true,
                            ),
                        )
                    }
                }
                actions.chunked(2).forEachIndexed { index, rowActions ->
                    if (index > 0) Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        rowActions.forEach { action ->
                            OutlinedButton(
                                onClick = action.onClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                enabled = action.enabled,
                            ) {
                                Text(
                                    text = action.label,
                                    color = if (action.isDestructive) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = MaterialTheme.typography.labelMedium.fontSize * 0.88f,
                                    ),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (rowActions.size == 1) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
        }
        }
    }

    if (showContribute) {
        ContributeDialog(
            isSaving = state.isSaving,
            onDismiss = { showContribute = false },
            onConfirm = { amountText, note ->
                val cents = parseBrlToCents(amountText)
                if (cents == null || cents <= 0) {
                    return@ContributeDialog
                }
                viewModel.contribute(cents, note) {
                    showContribute = false
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isDeleting) showDeleteConfirm = false
            },
            title = { Text(stringResource(R.string.goal_delete_title)) },
            text = { Text(stringResource(R.string.goal_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGoal {
                            showDeleteConfirm = false
                            onGoalDeleted()
                        }
                    },
                    enabled = !state.isDeleting,
                ) {
                    Text(
                        text = if (state.isDeleting) {
                            stringResource(R.string.goal_saving)
                        } else {
                            stringResource(R.string.goal_delete_confirm)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    enabled = !state.isDeleting,
                ) {
                    Text(stringResource(R.string.goal_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun GoalPriceLineChart(
    history: List<GoalPriceHistoryItemDto>,
    targetCents: Int?,
    goalCreatedAt: String?,
    fallbackReferencePriceCents: Int?,
    fallbackAlternatives: List<GoalPriceAlternativeDto>,
    modifier: Modifier = Modifier,
) {
    val chartEntries = remember(history, goalCreatedAt, fallbackReferencePriceCents, fallbackAlternatives) {
        val startDate = parseIsoLocalDate(goalCreatedAt)
        val endDate = startDate?.plusMonths(3)
        val historyEntries = history
            .asSequence()
            .filter { it.priceCents > 0 }
            .filter { item ->
                val recordedDate = parseIsoLocalDate(item.recordedAt) ?: return@filter false
                if (startDate == null || endDate == null) return@filter true
                !recordedDate.isBefore(startDate) && recordedDate.isBefore(endDate)
            }
            .sortedBy { it.recordedAt }
            .map { item ->
                GoalBarEntry(
                    priceCents = item.priceCents,
                    shortLabel = formatRelativePriceDate(item.recordedAt),
                    fullLabel = formatIsoDateToBr(item.recordedAt),
                )
            }
            .toList()
        if (historyEntries.isNotEmpty()) {
            historyEntries
        } else {
            buildList {
                fallbackReferencePriceCents?.takeIf { it > 0 }?.let {
                    add(
                        GoalBarEntry(
                            priceCents = it,
                            shortLabel = "Referência",
                            fullLabel = "Preço de referência",
                        ),
                    )
                }
                fallbackAlternatives
                    .filter { it.priceCents > 0 }
                    .forEach { alt ->
                        add(
                            GoalBarEntry(
                                priceCents = alt.priceCents,
                                shortLabel = alt.label.ifBlank { "Alternativa" },
                                fullLabel = alt.label.ifBlank { "Alternativa de preço" },
                            ),
                        )
                    }
            }
        }
    }
    if (chartEntries.isEmpty()) return
    val currentEntry = chartEntries.last()
    val maxQuoteValue = chartEntries.maxOfOrNull { it.priceCents } ?: return
    val axisBottomValue = 0
    val axisTopValue = ((maxQuoteValue * 1.5f).toInt()).coerceAtLeast(1)
    val axisMiddleValue = axisTopValue / 2
    val valueRange = (axisTopValue - axisBottomValue).toFloat().coerceAtLeast(1f)
    val goalValue = (targetCents ?: 0).coerceAtLeast(0)
    val hasGoalValue = goalValue > 0
    val firstPrice = chartEntries.first().priceCents
    val deltaPercent = if (firstPrice > 0) {
        ((currentEntry.priceCents - firstPrice).toFloat() / firstPrice.toFloat()) * 100f
    } else {
        0f
    }
    val trendColor = when {
        deltaPercent >= 0.5f -> MaterialTheme.colorScheme.error
        deltaPercent <= -0.5f -> WellPaidPositive
        else -> WellPaidNavy
    }
    val barPalette = remember {
        listOf(
            WellPaidGold,
            WellPaidPositive,
            WellPaidNavy,
            Color(0xFF6C63FF),
            Color(0xFF14B8A6),
            Color(0xFFB45309),
            Color(0xFF8B5CF6),
            Color(0xFFBE123C),
            Color(0xFF0EA5E9),
            Color(0xFF7C3AED),
        )
    }
    val priceToColor = remember(chartEntries) {
        val colors = linkedMapOf<Int, Color>()
        chartEntries.forEach { entry ->
            if (!colors.containsKey(entry.priceCents)) {
                colors[entry.priceCents] = barPalette[colors.size % barPalette.size]
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
        Text(
            text = stringResource(R.string.goal_price_history_title),
            style = MaterialTheme.typography.labelLarge,
            color = WellPaidNavy,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${currentEntry.fullLabel} · ${formatBrlFromCents(currentEntry.priceCents)}",
            style = MaterialTheme.typography.labelMedium,
            color = WellPaidNavy,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(156.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxSize()
                    .padding(top = 6.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                GoalAxisLabel(axisTopValue)
                GoalAxisLabel(axisMiddleValue)
                GoalAxisLabel(axisBottomValue)
            }
            Spacer(Modifier.width(8.dp))
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(156.dp),
            ) {
                val width = size.width
                val height = size.height
                val topPadding = 6.dp.toPx()
                val bottomPadding = 8.dp.toPx()
                val drawableHeight = (height - topPadding - bottomPadding).coerceAtLeast(1f)
                val barSlot = width / chartEntries.size.toFloat()
                val barWidth = (barSlot * 0.62f).coerceAtLeast(8f)
                val selectedIndex = chartEntries.lastIndex

                val guideYTop = topPadding
                val guideYMid = height - bottomPadding - (((axisMiddleValue - axisBottomValue).toFloat() / valueRange) * drawableHeight)
                val guideYBottom = height - bottomPadding
                listOf(guideYTop, guideYMid, guideYBottom).forEach { y ->
                    drawLine(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                if (hasGoalValue) {
                    val goalNormalized = ((goalValue - axisBottomValue).toFloat() / valueRange).coerceIn(0f, 1f)
                    val goalY = height - bottomPadding - (goalNormalized * drawableHeight)
                    drawLine(
                        color = trendColor.copy(alpha = 0.28f),
                        start = androidx.compose.ui.geometry.Offset(0f, goalY),
                        end = androidx.compose.ui.geometry.Offset(width, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                }

                val selectedX = (selectedIndex * barSlot) + (barSlot / 2f)
                drawLine(
                    color = WellPaidNavy.copy(alpha = 0.18f),
                    start = androidx.compose.ui.geometry.Offset(selectedX, topPadding),
                    end = androidx.compose.ui.geometry.Offset(selectedX, height - bottomPadding),
                    strokeWidth = 2f,
                )

                chartEntries.forEachIndexed { index, entry ->
                    val x = (index * barSlot) + (barSlot / 2f)
                    val normalized = ((entry.priceCents - axisBottomValue).toFloat() / valueRange).coerceIn(0f, 1f)
                    val barHeight = (normalized * drawableHeight).coerceAtLeast(8f)
                    val top = height - bottomPadding - barHeight
                    val baseColor = priceToColor[entry.priceCents] ?: WellPaidGold
                    val hasPriceChange = index > 0 && chartEntries[index - 1].priceCents != entry.priceCents
                    val color = if (index == selectedIndex) {
                        baseColor.copy(alpha = (baseColor.alpha * 0.6f + 0.4f).coerceIn(0.5f, 1f))
                    } else if (hasPriceChange) {
                        baseColor.copy(alpha = 0.95f)
                    } else {
                        baseColor.copy(alpha = 0.68f)
                    }
                    drawRoundRect(
                        color = color,
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
                    } else if (hasPriceChange) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.55f),
                            topLeft = androidx.compose.ui.geometry.Offset(x - (barWidth / 2f), top),
                            size = androidx.compose.ui.geometry.Size(barWidth, 2f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
                        )
                    }
                }
            }
        }
        if (chartEntries.size >= 2) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = chartEntries.first().shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = chartEntries[chartEntries.lastIndex / 2].shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = chartEntries.last().shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GoalQuickStatsRow(
    savedValue: String,
    remainingValue: String,
    updatedValue: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GoalQuickStatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.goal_list_label_saved),
            value = savedValue,
        )
        GoalQuickStatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.goal_detail_label_remaining),
            value = remainingValue,
        )
        GoalQuickStatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.goal_list_label_updated),
            value = updatedValue,
        )
    }
}

@Composable
private fun GoalProgressStickyHeader(
    currentValue: String,
    targetValue: String,
    progress: Float,
    dueAtLabel: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(WellPaidCreamMuted.copy(alpha = 0.78f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.goal_detail_progress_label, currentValue, targetValue),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
        dueAtLabel?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GoalQuickStatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(WellPaidCreamMuted.copy(alpha = 0.72f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = WellPaidNavy,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class GoalDetailAction(
    val label: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
    val isDestructive: Boolean,
)

private data class GoalBarEntry(
    val priceCents: Int,
    val shortLabel: String,
    val fullLabel: String,
)

private fun preferredChartWidthForEntries(entryCount: Int): Dp {
    val slot = 24.dp
    val sidePadding = 12.dp
    return (slot * entryCount) + sidePadding
}

@Composable
private fun GoalAxisLabel(valueCents: Int) {
    Text(
        text = formatGoalAxisValueFromCents(valueCents),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.84f,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatGoalAxisValueFromCents(cents: Int): String {
    val absCents = kotlin.math.abs(cents.toLong())
    if (absCents >= 100_000L) {
        val mil = absCents / 100_000.0
        val compact = "${formatCompactNumber(mil)} mil"
        return if (cents < 0) "-$compact" else compact
    }
    val absValue = absCents / 100.0
    val formatted = String.format(Locale("pt", "BR"), "%.2f", absValue)
    return if (cents < 0) "-$formatted" else formatted
}

private fun formatCompactNumber(value: Double): String {
    val rounded = if (value >= 10) String.format("%.0f", value) else String.format("%.1f", value)
    return rounded.replace('.', ',')
}

private fun formatRelativePriceDate(raw: String): String {
    val date = parseIsoLocalDate(raw) ?: return formatIsoDateToBr(raw)
    val today = LocalDate.now()
    val days = java.time.temporal.ChronoUnit.DAYS.between(date, today)
    return when {
        days <= 0L -> "Hoje"
        days == 1L -> "Ontem"
        days in 2L..6L -> "$days dias atrás"
        else -> formatIsoDateToBr(raw)
    }
}

private fun parseIsoLocalDate(raw: String?): LocalDate? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null
    return runCatching {
        LocalDate.parse(value.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
    }.getOrNull()
        ?: runCatching {
            java.time.OffsetDateTime.parse(value).atZoneSameInstant(ZoneOffset.UTC).toLocalDate()
        }.getOrNull()
}

@Composable
private fun ContributeDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(stringResource(R.string.goal_contribute_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.goal_contribute_amount)) },
                    enabled = !isSaving,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.goal_contribute_note)) },
                    enabled = !isSaving,
                    singleLine = false,
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amount, note) },
                enabled = !isSaving,
            ) {
                Text(
                    text = if (isSaving) {
                        stringResource(R.string.goal_contribute_saving)
                    } else {
                        stringResource(R.string.goal_contribute_confirm)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.goal_contribute_cancel))
            }
        },
    )
}
