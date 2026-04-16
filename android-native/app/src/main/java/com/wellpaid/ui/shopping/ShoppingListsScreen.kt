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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.wellpaid.ui.components.WellPaidPrimaryAddRow
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.ui.main.rememberMainShellTabSwipeModifier
import com.wellpaid.util.formatBrlFromCents

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun ShoppingListsScreen(
    onNavigateBack: () -> Unit,
    onSwipeNavigateToMainHome: () -> Unit,
    onOpenList: (String) -> Unit,
    onListCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShoppingListsViewModel = hiltViewModel(),
) {
    val swipeToMainHome = rememberMainShellTabSwipeModifier(
        enabled = true,
        currentTabIndex = 0,
        onNavigateHome = onSwipeNavigateToMainHome,
        onNavigateNext = {},
        includeForwardSwipe = false,
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drafts = state.lists.filter { it.status.equals("draft", ignoreCase = true) }
    val completed = state.lists.filter { it.status.equals("completed", ignoreCase = true) }
    val pullRefreshing = state.isRefreshing
    val pullState = rememberPullRefreshState(
        refreshing = pullRefreshing,
        onRefresh = { viewModel.refresh() },
    )

    var showCreateDialog by remember { mutableStateOf(false) }
    var newListTitle by remember { mutableStateOf("") }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isCreatingList) {
                    showCreateDialog = false
                    newListTitle = ""
                }
            },
            title = { Text(stringResource(R.string.shopping_new_title)) },
            text = {
                OutlinedTextField(
                    value = newListTitle,
                    onValueChange = { newListTitle = it },
                    label = { Text(stringResource(R.string.shopping_list_name_required_label)) },
                    singleLine = true,
                    enabled = !state.isCreatingList,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val t = newListTitle.trim()
                        if (t.isNotEmpty()) {
                            viewModel.createListWithTitle(t) { id ->
                                showCreateDialog = false
                                newListTitle = ""
                                onListCreated(id)
                            }
                        }
                    },
                    enabled = newListTitle.trim().isNotEmpty() && !state.isCreatingList,
                ) {
                    if (state.isCreatingList) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(stringResource(R.string.shopping_save))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateDialog = false
                        newListTitle = ""
                    },
                    enabled = !state.isCreatingList,
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text(stringResource(R.string.shopping_delete_confirm_title)) },
            text = { Text(stringResource(R.string.shopping_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteId?.let(viewModel::deleteList)
                        pendingDeleteId = null
                    },
                ) {
                    Text(stringResource(R.string.shopping_delete_list))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

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
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(WellPaidCream)
                .padding(inner)
                .wellPaidScreenHorizontalPadding()
                .wellPaidMaxContentWidth(WellPaidMaxContentWidth),
        ) {
            WellPaidPrimaryAddRow(
                label = stringResource(R.string.shopping_fab_new),
                leadingIcon = Icons.Filled.Add,
                onPrimaryClick = { showCreateDialog = true },
                onRefresh = { viewModel.refresh() },
                refreshEnabled = !state.isLoading && !state.isRefreshing,
                primaryEnabled = !state.isCreatingList && !(state.isLoading && state.lists.isEmpty()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 10.dp),
            )

            when {
                state.isLoading && state.lists.isEmpty() -> {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(swipeToMainHome),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = WellPaidNavy)
                    }
                }
                state.errorMessage != null && state.lists.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                            .then(swipeToMainHome),
                    ) {
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                state.lists.isEmpty() -> {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(swipeToMainHome),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.shopping_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = WellPaidNavy.copy(alpha = 0.72f),
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pullRefresh(pullState),
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(swipeToMainHome),
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
                                        onRequestDelete = {
                                            pendingDeleteId = row.id
                                        },
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
                                        onRequestDelete = null,
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
    onRequestDelete: (() -> Unit)?,
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
    val isDraft = row.status.equals("draft", true)
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
                .padding(horizontal = 10.dp, vertical = 12.dp),
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
            when {
                isDraft && onRequestDelete != null -> {
                    IconButton(onClick = onRequestDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.shopping_delete_row_cd),
                            tint = WellPaidNavy.copy(alpha = 0.55f),
                        )
                    }
                }
                isDraft -> {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = null,
                        tint = WellPaidNavy.copy(alpha = 0.45f),
                    )
                }
                else -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = WellPaidGold,
                    )
                }
            }
        }
    }
}
