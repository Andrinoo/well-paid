package com.wellpaid.ui.goals

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidPrimaryAddRow
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.core.model.goal.GoalDto
import com.wellpaid.util.formatBrlFromCents

@Composable
fun GoalsListContent(
    mainRouteEntry: NavBackStackEntry,
    onGoalClick: (String) -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .fillMaxWidth()
            .wellPaidMaxContentWidth(WellPaidMaxContentWidth),
    ) {
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
                contentPadding = PaddingValues(bottom = 16.dp),
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
                    GoalListRow(
                        goal = goal,
                        modifier = Modifier.clickable { onGoalClick(goal.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun GoalListRow(
    goal: GoalDto,
    modifier: Modifier = Modifier,
) {
    val progress = if (goal.targetCents > 0) {
        (goal.currentCents.toFloat() / goal.targetCents.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val status = if (goal.isActive) {
        stringResource(R.string.goals_status_active)
    } else {
        stringResource(R.string.goals_status_archived)
    }
    val sub = buildString {
        append(formatBrlFromCents(goal.currentCents))
        append(" / ")
        append(formatBrlFromCents(goal.targetCents))
        append(" · ")
        append(status)
        if (!goal.isMine) {
            append(" · ")
            append(stringResource(R.string.goals_family_member))
        }
    }

    ListItem(
        modifier = modifier,
        headlineContent = {
            Text(
                text = goal.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        },
        supportingContent = {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
    )
}
