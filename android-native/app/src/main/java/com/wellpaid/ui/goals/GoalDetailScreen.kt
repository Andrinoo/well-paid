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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.goal.GoalPriceAlternativeDto
import com.wellpaid.core.model.goal.GoalPriceHistoryItemDto
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidExpenseLine
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding()
                .wellPaidScreenHorizontalPadding()
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize * 0.8f,
                        ),
                        fontWeight = FontWeight.SemiBold,
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

            val progress = if (goal.targetCents > 0) {
                (goal.currentCents.toFloat() / goal.targetCents.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            Text(
                text = stringResource(
                    R.string.goal_detail_progress_label,
                    formatBrlFromCents(goal.currentCents),
                    formatBrlFromCents(goal.targetCents),
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            goal.dueAt?.takeIf { it.isNotBlank() }?.let { dueAt ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.goal_field_due_date) + ": " + formatIsoDateToBr(dueAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))
            GoalPriceBarsChart(
                history = state.priceHistory,
                goalCreatedAt = goal.createdAt,
                fallbackReferencePriceCents = goal.referencePriceCents,
                fallbackAlternatives = goal.priceAlternatives,
                modifier = Modifier.fillMaxWidth(),
            )

            // Novos campos: link e preço de referência
            val url = goal.targetUrl?.trim().orEmpty()
            val canRefreshPriceByTitle = goal.title.trim().length >= 2
            if (url.isNotEmpty() || goal.referencePriceCents != null || goal.referenceProductName != null ||
                goal.priceAlternatives.isNotEmpty() || (goal.isMine && canRefreshPriceByTitle)
            ) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.goal_detail_section_link),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
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
                if (goal.isMine && (url.isNotEmpty() || canRefreshPriceByTitle)) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.refreshTargetFromLink() },
                        enabled = !state.isSaving && !state.isDeleting && !state.isRefreshingFromLink,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (state.isRefreshingFromLink) {
                                    stringResource(R.string.goal_refreshing_price)
                                } else {
                                    stringResource(R.string.goal_detail_refresh_target_from_link)
                                },
                            )
                        }
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
                    if (goal.isActive) {
                        add(
                            GoalDetailAction(
                                label = stringResource(R.string.goal_contribute_button),
                                enabled = !state.isSaving && !state.isDeleting,
                                onClick = { showContribute = true },
                                isDestructive = false,
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
                                    .height(46.dp),
                                enabled = action.enabled,
                            ) {
                                Text(
                                    text = action.label,
                                    color = if (action.isDestructive) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                        if (rowActions.size == 1) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
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
private fun GoalPriceBarsChart(
    history: List<GoalPriceHistoryItemDto>,
    goalCreatedAt: String?,
    fallbackReferencePriceCents: Int?,
    fallbackAlternatives: List<GoalPriceAlternativeDto>,
    modifier: Modifier = Modifier,
) {
    val chartEntries = remember(history, goalCreatedAt, fallbackReferencePriceCents, fallbackAlternatives) {
        val startDate = parseIsoLocalDate(goalCreatedAt)
        val endDate = startDate?.plusDays(30)
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
                    label = formatIsoDateToBr(item.recordedAt),
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
                            label = "Referência",
                        ),
                    )
                }
                fallbackAlternatives
                    .filter { it.priceCents > 0 }
                    .forEach { alt ->
                        add(
                            GoalBarEntry(
                                priceCents = alt.priceCents,
                                label = alt.label.ifBlank { "Alternativa" },
                            ),
                        )
                    }
            }
        }
    }
    if (chartEntries.isEmpty()) return
    var selectedBarIndex by remember(chartEntries) { mutableIntStateOf(-1) }

    val maxValue = chartEntries.maxOfOrNull { it.priceCents } ?: return
    if (maxValue <= 0) return
    val valueRange = maxValue.toFloat()
    val midValue = (maxValue / 2f).toInt()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val horizontalScroll = rememberScrollState()
    val palette = remember {
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
    val valueToColor = remember(chartEntries) {
        val map = linkedMapOf<Long, Color>()
        chartEntries.forEach { entry ->
            val key = entry.priceCents.toLong()
            if (!map.containsKey(key)) {
                map[key] = palette[map.size % palette.size]
            }
        }
        map
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
            text = stringResource(R.string.investments_evolution_title),
            style = MaterialTheme.typography.labelLarge,
            color = WellPaidNavy,
            fontWeight = FontWeight.SemiBold,
        )
        if (selectedBarIndex in chartEntries.indices) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${chartEntries[selectedBarIndex].label} · ${formatBrlFromCents(chartEntries[selectedBarIndex].priceCents)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(122.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(56.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = formatBrlFromCents(maxValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatBrlFromCents(midValue),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatBrlFromCents(0),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            BoxWithConstraints(modifier = Modifier.weight(1f)) {
                val minChartWidth = maxWidth
                val preferredChartWidth = preferredChartWidthForEntries(chartEntries.size)
                val chartWidth = max(minChartWidth, preferredChartWidth)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(horizontalScroll),
                ) {
                    Canvas(
                        modifier = Modifier
                            .requiredWidth(chartWidth)
                            .height(122.dp)
                            .pointerInput(chartEntries) {
                                detectTapGestures { offset ->
                                    val barSlot = size.width / chartEntries.size.toFloat()
                                    val tapped = (offset.x / barSlot).toInt()
                                    selectedBarIndex = tapped.coerceIn(0, chartEntries.lastIndex)
                                }
                            },
                    ) {
                        val width = size.width
                        val height = size.height
                        val barSlot = width / chartEntries.size.toFloat()
                        val barWidth = 14.dp.toPx().coerceAtMost(barSlot * 0.8f).coerceAtLeast(6.dp.toPx())

                        val guideYTop = 2.dp.toPx()
                        val guideYMid = height / 2f
                        val guideYBottom = height - 2.dp.toPx()
                        listOf(guideYTop, guideYMid, guideYBottom).forEach { y ->
                            drawLine(
                                color = gridColor,
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(width, y),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }

                        chartEntries.forEachIndexed { index, entry ->
                            val x = (index * barSlot) + (barSlot / 2f)
                            val normalized = entry.priceCents.toFloat() / valueRange
                            val barHeight = (normalized * (height - 8.dp.toPx())).coerceAtLeast(8.dp.toPx())
                            val top = height - barHeight
                            val colorKey = entry.priceCents.toLong()
                            val baseColor = valueToColor[colorKey] ?: WellPaidGold
                            val isSelected = selectedBarIndex == index
                            drawRoundRect(
                                color = if (isSelected) {
                                    baseColor.copy(alpha = 1f)
                                } else if (index == 0) {
                                    baseColor.copy(alpha = 0.95f)
                                } else {
                                    baseColor.copy(alpha = 0.74f)
                                },
                                topLeft = androidx.compose.ui.geometry.Offset(x - (barWidth / 2f), top),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                            )
                        }
                    }
                }
            }
        }
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
    val label: String,
)

private fun preferredChartWidthForEntries(entryCount: Int): Dp {
    val slot = 24.dp
    val sidePadding = 12.dp
    return (slot * entryCount) + sidePadding
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
