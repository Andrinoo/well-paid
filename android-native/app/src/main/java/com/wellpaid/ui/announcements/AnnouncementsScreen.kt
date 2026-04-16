package com.wellpaid.ui.announcements

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.announcement.AnnouncementDto
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AnnouncementsScreen(
    onNavigateBack: () -> Unit,
    onEngagementChanged: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: AnnouncementsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pullRefreshing = state.isLoading && state.items.isNotEmpty()
    val pullState = rememberPullRefreshState(
        refreshing = pullRefreshing,
        onRefresh = { viewModel.refresh() },
    )
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WellPaidCream,
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.announcements_screen_title),
                        style = MaterialTheme.typography.titleLarge,
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullState)
                .wellPaidMaxContentWidth(WellPaidMaxContentWidth),
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WellPaidGold)
                    }
                }
                else -> {
                    Column(Modifier.fillMaxSize()) {
                        state.errorMessage?.let { msg ->
                            Text(
                                text = msg,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (state.items.isEmpty() && !state.isLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Campaign,
                                    contentDescription = null,
                                    tint = WellPaidNavy.copy(alpha = 0.35f),
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.announcements_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(16.dp),
                            ) {
                                items(state.items, key = { it.id }) { row ->
                                    AnnouncementCard(
                                        row = row,
                                        onOpenLink = { url -> runCatching { uriHandler.openUri(url) } },
                                        onMarkRead = {
                                            viewModel.markAsRead(row.id, onEngagementChanged)
                                        },
                                        onRemove = {
                                            viewModel.hideFromList(row.id, onEngagementChanged)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    PullRefreshIndicator(
                        refreshing = pullRefreshing,
                        state = pullState,
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
}

private const val ANNOUNCEMENT_BODY_EXPAND_THRESHOLD = 100

@Composable
private fun AnnouncementCard(
    row: AnnouncementDto,
    onOpenLink: (String) -> Unit,
    onMarkRead: () -> Unit,
    onRemove: () -> Unit,
) {
    val accent = kindAccent(row.kind)
    val needsExpansion = row.body.length > ANNOUNCEMENT_BODY_EXPAND_THRESHOLD
    var expanded by remember(row.id) { mutableStateOf(false) }
    LaunchedEffect(row.id) {
        expanded = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (needsExpansion) {
                            Modifier.clickable { expanded = !expanded }
                        } else {
                            Modifier
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = WellPaidNavy,
                    modifier = Modifier.weight(1f),
                )
                if (needsExpansion) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.announcements_toggle_expand),
                        tint = WellPaidNavy.copy(alpha = 0.75f),
                    )
                }
            }
            when {
                !needsExpansion -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = row.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val ctaShort = row.ctaUrl?.trim().orEmpty()
                    val labelShort = row.ctaLabel?.trim().orEmpty()
                    if (ctaShort.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { onOpenLink(ctaShort) }) {
                            Text(
                                text = labelShort.ifEmpty { stringResource(R.string.announcements_open_link) },
                                color = accent,
                            )
                        }
                    }
                }
                expanded -> {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = row.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val cta = row.ctaUrl?.trim().orEmpty()
                    val label = row.ctaLabel?.trim().orEmpty()
                    if (cta.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { onOpenLink(cta) }) {
                            Text(
                                text = label.ifEmpty { stringResource(R.string.announcements_open_link) },
                                color = accent,
                            )
                        }
                    }
                }
                else -> {
                    /* longo e recolhido: só o título até expandir */
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (row.userReadAt.isNullOrBlank()) {
                    TextButton(onClick = onMarkRead) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = accent,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.announcements_mark_read),
                                color = accent,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.announcements_read_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onRemove) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.announcements_remove),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

private fun kindAccent(kind: String): Color = when (kind.lowercase()) {
    "warning" -> Color(0xFFB45309)
    "tip" -> Color(0xFF047857)
    "material" -> Color(0xFFEC4899)
    else -> WellPaidGold
}
