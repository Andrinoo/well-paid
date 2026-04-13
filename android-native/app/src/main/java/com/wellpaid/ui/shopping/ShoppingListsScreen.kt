package com.wellpaid.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.shopping.ShoppingListSummaryDto
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatBrlFromCents

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ShoppingListsScreen(
    onNavigateBack: () -> Unit,
    onOpenList: (String) -> Unit,
    onEmptyListCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShoppingListsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drafts = state.lists.filter { it.status.equals("draft", ignoreCase = true) }
    val completed = state.lists.filter { it.status.equals("completed", ignoreCase = true) }
    val pullRefreshing = state.isLoading && state.lists.isNotEmpty()
    val pullState = rememberPullRefreshState(
        refreshing = pullRefreshing,
        onRefresh = { viewModel.refresh() },
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WellPaidCream,
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.shopping_lists_title),
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = Color.White,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.shopping_fab_new)) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    if (!state.isLoading) {
                        viewModel.createEmptyList(onEmptyListCreated)
                    }
                },
                containerColor = WellPaidGold,
                contentColor = WellPaidNavy,
            )
        },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WellPaidCream)
                .padding(inner),
        ) {
            when {
                state.isLoading && state.lists.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WellPaidNavy)
                    }
                }
                state.errorMessage != null && state.lists.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .wellPaidScreenHorizontalPadding()
                            .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                            .padding(top = 24.dp),
                    ) {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                state.lists.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.shopping_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = WellPaidNavy.copy(alpha = 0.72f),
                            modifier = Modifier.wellPaidScreenHorizontalPadding(),
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pullRefresh(pullState),
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .wellPaidScreenHorizontalPadding()
                                .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                                .padding(bottom = 88.dp),
                        ) {
                            if (drafts.isNotEmpty()) {
                                item {
                                    SectionHeader(stringResource(R.string.shopping_section_active))
                                    Spacer(Modifier.height(8.dp))
                                }
                                items(drafts, key = { it.id }) { row ->
                                    SummaryRow(
                                        row = row,
                                        onClick = { onOpenList(row.id) },
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                            if (completed.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(8.dp))
                                    SectionHeader(stringResource(R.string.shopping_section_history))
                                    Spacer(Modifier.height(8.dp))
                                }
                                items(completed, key = { it.id }) { row ->
                                    SummaryRow(
                                        row = row,
                                        onClick = { onOpenList(row.id) },
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                        PullRefreshIndicator(
                            refreshing = pullRefreshing,
                            state = pullState,
                            modifier = Modifier.align(Alignment.TopCenter),
                            contentColor = WellPaidNavy,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = WellPaidNavy.copy(alpha = 0.55f),
        letterSpacing = 0.4.sp,
    )
}

@Composable
private fun SummaryRow(
    row: ShoppingListSummaryDto,
    onClick: () -> Unit,
) {
    val title = row.title?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.shopping_no_title)
    val subtitle = buildString {
        append(stringResource(R.string.shopping_items_count, row.itemsCount))
        val total = row.totalCents
        if (row.status.equals("completed", true) && total != null) {
            append(" · ")
            append(formatBrlFromCents(total))
        }
    }
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = WellPaidCreamMuted,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = WellPaidNavy,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = WellPaidNavy.copy(alpha = 0.58f),
                )
            }
            if (row.status.equals("draft", true)) {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = WellPaidNavy.copy(alpha = 0.45f),
                )
            } else {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = WellPaidGold,
                )
            }
        }
    }
}
