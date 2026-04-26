package com.wellpaid.ui.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.headlineSmall,
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
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
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
                OutlinedButton(
                    onClick = { onEditGoal(goal.id) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && !state.isDeleting,
                ) {
                    Text(stringResource(R.string.goal_detail_edit_button))
                }
                if (goal.currentCents == 0) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving && !state.isDeleting,
                    ) {
                        Text(
                            text = stringResource(R.string.goal_detail_delete_button),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (goal.isMine && goal.isActive) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showContribute = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving && !state.isDeleting,
                ) {
                    Text(stringResource(R.string.goal_contribute_button))
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
    val chartValues = remember(history, goalCreatedAt, fallbackReferencePriceCents, fallbackAlternatives) {
        val startDate = parseIsoLocalDate(goalCreatedAt)
        val endDate = startDate?.plusDays(30)
        val historyValues = history
            .asSequence()
            .filter { it.priceCents > 0 }
            .filter { item ->
                val recordedDate = parseIsoLocalDate(item.recordedAt) ?: return@filter false
                if (startDate == null || endDate == null) return@filter true
                !recordedDate.isBefore(startDate) && recordedDate.isBefore(endDate)
            }
            .sortedBy { it.recordedAt }
            .map { it.priceCents }
            .toList()
        if (historyValues.isNotEmpty()) {
            historyValues
        } else {
            buildList {
                fallbackReferencePriceCents?.takeIf { it > 0 }?.let { add(it) }
                fallbackAlternatives
                    .map { it.priceCents }
                    .filter { it > 0 }
                    .forEach { add(it) }
            }
        }
    }
    if (chartValues.isEmpty()) return

    val maxValue = chartValues.maxOrNull() ?: return
    if (maxValue <= 0) return
    val valueRange = maxValue.toFloat()
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
    val valueToColor = remember(chartValues) {
        val map = linkedMapOf<Long, Color>()
        chartValues.forEach { cents ->
            val key = cents.toLong()
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
        Spacer(Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp),
        ) {
            val width = size.width
            val height = size.height
            val barSlot = width / chartValues.size.toFloat()
            val barWidth = (barSlot * 0.62f).coerceAtLeast(8f)
            chartValues.forEachIndexed { index, value ->
                val x = (index * barSlot) + (barSlot / 2f)
                val normalized = value.toFloat() / valueRange
                val barHeight = (normalized * (height - 6f)).coerceAtLeast(10f)
                val top = height - barHeight
                val colorKey = value.toLong()
                val baseColor = valueToColor[colorKey] ?: WellPaidGold
                drawRoundRect(
                    color = if (index == 0) baseColor.copy(alpha = 0.95f) else baseColor.copy(alpha = 0.74f),
                    topLeft = androidx.compose.ui.geometry.Offset(x - (barWidth / 2f), top),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
            }
        }
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
