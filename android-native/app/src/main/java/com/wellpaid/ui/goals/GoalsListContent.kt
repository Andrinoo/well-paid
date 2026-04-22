package com.wellpaid.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidPrimaryAddRow
import com.wellpaid.ui.components.WellPaidPullToRefreshBox
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.core.model.goal.GoalDto
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.formatIsoDateToBr

@Composable
fun GoalsListContent(
    mainRouteEntry: NavBackStackEntry,
    onGoalClick: (String) -> Unit,
    onEditGoal: (String) -> Unit,
    onNewGoal: () -> Unit,
    modifier: Modifier = Modifier,
    tabSwipe: Modifier = Modifier,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dirtyFlow = remember(mainRouteEntry) {
        mainRouteEntry.savedStateHandle.getStateFlow("goal_list_dirty", 0L)
    }
    val dirty by dirtyFlow.collectAsStateWithLifecycle()
    LaunchedEffect(dirty) {
        if (dirty != 0L) {
            viewModel.refresh()
        }
    }

    val pullRefreshing = state.isLoading && state.goals.isNotEmpty()
    WellPaidPullToRefreshBox(
        refreshing = pullRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier
            .fillMaxSize()
            .fillMaxWidth()
            .wellPaidMaxContentWidth(WellPaidMaxContentWidth),
    ) {
        Column(Modifier.fillMaxSize()) {
        WellPaidPrimaryAddRow(
            label = stringResource(R.string.goals_primary_add),
            leadingIcon = Icons.Filled.Flag,
            onPrimaryClick = onNewGoal,
            onRefresh = { viewModel.refresh() },
            refreshEnabled = !state.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 10.dp),
        )

        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                modifier = Modifier.padding(bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        if (state.isLoading && state.goals.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .then(tabSwipe),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .then(tabSwipe),
                contentPadding = PaddingValues(bottom = 16.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.goals.isEmpty() && !state.isLoading) {
                    item {
                        Text(
                            text = stringResource(R.string.goals_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
                items(state.goals, key = { it.id }) { goal ->
                    GoalCompactCard(
                        goal = goal,
                        expanded = state.expandedGoalId == goal.id,
                        onToggleExpand = { viewModel.toggleGoalExpanded(goal.id) },
                        onClick = { onGoalClick(goal.id) },
                        onEditClick = if (goal.isMine) {
                            { onEditGoal(goal.id) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun GoalCompactCard(
    goal: GoalDto,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val progress = if (goal.targetCents > 0) {
        (goal.currentCents.toFloat() / goal.targetCents.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val context = LocalContext.current
    val thumbUrl = goal.referenceThumbnailUrl?.trim()?.takeIf { it.isNotEmpty() }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
            Box(
                modifier = Modifier
                        .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(WellPaidNavy.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbUrl)
                            .size(Size(96, 96))
                            .crossfade(140)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                                .size(42.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        tint = WellPaidNavy.copy(alpha = 0.38f),
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = formatBrlFromCents(goal.currentCents),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(modifier = Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onToggleExpand) {
                    Text(if (expanded) stringResource(R.string.goal_card_less) else stringResource(R.string.goal_card_more))
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                    )
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.size(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.size(8.dp))
                GoalCardExpandedPanel(goal = goal)
                Spacer(modifier = Modifier.size(6.dp))
                if (onEditClick != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.goal_edit_action),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalListLabeledField(
    label: String,
    value: String,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(2.dp))
    Text(
        text = value,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun GoalCardExpandedPanel(
    goal: GoalDto,
) {
    val uriHandler = LocalUriHandler.current
    val status = if (goal.isActive) {
        stringResource(R.string.goals_status_active)
    } else {
        stringResource(R.string.goals_status_archived)
    }
    val remainingCents = (goal.targetCents - goal.currentCents).coerceAtLeast(0)
    val url = goal.targetUrl?.trim().orEmpty()

    GoalListLabeledField(
        label = stringResource(R.string.goal_list_label_status),
        value = status,
    )
    if (!goal.isMine) {
        Spacer(Modifier.size(4.dp))
        Text(
            text = stringResource(R.string.goal_readonly_family),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
    Spacer(Modifier.size(6.dp))
    GoalListLabeledField(
        label = stringResource(R.string.goal_list_label_saved),
        value = formatBrlFromCents(goal.currentCents),
    )
    Spacer(Modifier.size(4.dp))
    GoalListLabeledField(
        label = stringResource(R.string.goal_list_label_target),
        value = formatBrlFromCents(goal.targetCents),
    )
    Spacer(Modifier.size(4.dp))
    Text(
        text = stringResource(R.string.goal_list_remaining, formatBrlFromCents(remainingCents)),
        style = MaterialTheme.typography.bodySmall,
        color = WellPaidNavy,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.size(6.dp))
    goal.referenceProductName?.takeIf { it.isNotBlank() }?.let { name ->
        Text(
            text = stringResource(R.string.goal_detail_product_name, name),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(4.dp))
    }
    goal.referencePriceCents?.let { cents ->
        Text(
            text = stringResource(R.string.goal_detail_reference_price, formatBrlFromCents(cents)),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.size(4.dp))
    }
    goal.priceCheckedAt?.takeIf { it.isNotBlank() }?.let { checkedAt ->
        Text(
            text = stringResource(R.string.goal_list_price_checked, formatIsoDateToBr(checkedAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(4.dp))
    }
    goal.priceSource?.takeIf { it.isNotBlank() }?.let { source ->
        GoalListLabeledField(
            label = stringResource(R.string.goal_list_label_source),
            value = source,
        )
        Spacer(Modifier.size(4.dp))
    }
    if (url.isNotEmpty()) {
        Text(
            text = stringResource(R.string.goal_list_url_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(2.dp))
        TextButton(
            onClick = { runCatching { uriHandler.openUri(url) } },
        ) {
            Text(stringResource(R.string.goal_detail_open_link))
        }
    }
    if (goal.priceAlternatives.isNotEmpty()) {
        Spacer(Modifier.size(4.dp))
        Text(
            text = stringResource(R.string.goal_detail_alternatives_count, goal.priceAlternatives.size),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary,
        )
        goal.priceAlternatives.forEach { alt ->
            Spacer(Modifier.size(2.dp))
            val line = if (alt.label.isNotBlank()) {
                stringResource(
                    R.string.goal_list_alternative_item,
                    alt.label,
                    formatBrlFromCents(alt.priceCents),
                )
            } else {
                formatBrlFromCents(alt.priceCents)
            }
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(Modifier.size(6.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    Spacer(Modifier.size(4.dp))
    GoalListLabeledField(
        label = stringResource(R.string.goal_list_label_created),
        value = formatIsoDateToBr(goal.createdAt),
    )
    Spacer(Modifier.size(4.dp))
    GoalListLabeledField(
        label = stringResource(R.string.goal_list_label_updated),
        value = formatIsoDateToBr(goal.updatedAt),
    )
    Spacer(Modifier.size(4.dp))
    Text(
        text = stringResource(R.string.goal_list_tap_for_full),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
