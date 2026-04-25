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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.wellpaid.ui.components.WellPaidBrandCircularProgress
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
                WellPaidBrandCircularProgress()
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
                        text = "${formatBrlFromCents(goal.currentCents)} / ${formatBrlFromCents(goal.targetCents)}",
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
                if (onEditClick != null) {
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
