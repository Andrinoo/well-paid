package com.wellpaid.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.goal.GoalProductHitDto
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.GoalProductSearchExternalUrls
import com.wellpaid.util.formatMinorCurrencyFromCents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormScreen(
    onNavigateBack: () -> Unit,
    onFinishedNeedRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleted: (() -> Unit)? = null,
    viewModel: GoalFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(
                            if (viewModel.isEditMode) R.string.goal_form_edit_title
                            else R.string.goal_form_new_title,
                        ),
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.common_close),
                            tint = Color.White,
                        )
                    }
                },
            )
        },
    ) { inner ->
        if (state.isLoading) {
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

            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.setTitle(it) },
                label = { Text(stringResource(R.string.goal_field_title)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.targetText,
                onValueChange = { viewModel.setTargetText(it) },
                label = { Text(stringResource(R.string.goal_field_target)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text(stringResource(R.string.expense_field_amount_hint)) },
                shape = RoundedCornerShape(14.dp),
            )

            if (!viewModel.isEditMode) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.initialText,
                    onValueChange = { viewModel.setInitialText(it) },
                    label = { Text(stringResource(R.string.goal_field_initial)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text(stringResource(R.string.goal_field_initial_hint)) },
                    shape = RoundedCornerShape(14.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.targetUrl,
                onValueChange = { viewModel.onTargetUrlChange(it) },
                label = { Text(stringResource(R.string.goal_field_link)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                supportingText = {
                    Column {
                        Text(stringResource(R.string.goal_field_link_hint))
                        Text(
                            text = stringResource(R.string.goal_field_link_autocomplete_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.goal_search_auto_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.isSearchingProducts) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                }
                TextButton(
                    onClick = { viewModel.searchProductsByTitle() },
                    enabled = !state.isSaving && !state.isSearchingProducts,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.goal_search_refresh_suggestions))
                }
            }
            if (state.lastProductSearchHadNoResults && !state.isSearchingProducts) {
                Text(
                    text = stringResource(R.string.goal_search_no_results),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.goal_search_other_stores_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    TextButton(
                        onClick = {
                            val q = viewModel.resolveSearchQueryOrShowError() ?: return@TextButton
                            runCatching { uriHandler.openUri(GoalProductSearchExternalUrls.googleShopping(q)) }
                        },
                        enabled = !state.isSaving,
                    ) {
                        Text(stringResource(R.string.goal_search_open_google))
                    }
                }
                item {
                    TextButton(
                        onClick = {
                            val q = viewModel.resolveSearchQueryOrShowError() ?: return@TextButton
                            runCatching { uriHandler.openUri(GoalProductSearchExternalUrls.amazonBr(q)) }
                        },
                        enabled = !state.isSaving,
                    ) {
                        Text(stringResource(R.string.goal_search_open_amazon_br))
                    }
                }
                item {
                    TextButton(
                        onClick = {
                            val q = viewModel.resolveSearchQueryOrShowError() ?: return@TextButton
                            runCatching { uriHandler.openUri(GoalProductSearchExternalUrls.buscape(q)) }
                        },
                        enabled = !state.isSaving,
                    ) {
                        Text(stringResource(R.string.goal_search_open_buscape))
                    }
                }
                item {
                    TextButton(
                        onClick = {
                            val q = viewModel.resolveSearchQueryOrShowError() ?: return@TextButton
                            runCatching { uriHandler.openUri(GoalProductSearchExternalUrls.magazineLuiza(q)) }
                        },
                        enabled = !state.isSaving,
                    ) {
                        Text(stringResource(R.string.goal_search_open_magalu))
                    }
                }
            }

            if (state.productSearchResults.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.goal_search_top_suggestions),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.goal_product_tap_to_apply),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                val topHits = state.productSearchResults.take(3)
                val moreHits = state.productSearchResults.drop(3).take(12)
                topHits.forEach { hit ->
                    GoalProductHitCard(
                        hit = hit,
                        enabled = !state.isSaving,
                        onClick = { viewModel.applyProductHit(hit) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (moreHits.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.goal_search_more_results),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                    moreHits.forEach { hit ->
                        GoalProductHitCard(
                            hit = hit,
                            enabled = !state.isSaving,
                            onClick = { viewModel.applyProductHit(hit) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            state.referencePriceLabel?.let { ref ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.goal_reference_price_label) + ": " + ref,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.loadPriceFromLink() },
                enabled = !state.isSaving && !state.isRefreshingPrice,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.isRefreshingPrice) {
                            stringResource(R.string.goal_refreshing_price)
                        } else {
                            stringResource(R.string.goal_fetch_target_from_link)
                        },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.goal_field_active),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Switch(
                    checked = state.isActive,
                    onCheckedChange = { viewModel.setActive(it) },
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.save(onFinishedNeedRefresh) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isSaving,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WellPaidGold,
                    contentColor = WellPaidNavy,
                ),
            ) {
                Text(
                    text = if (state.isSaving) {
                        stringResource(R.string.goal_saving)
                    } else {
                        stringResource(R.string.goal_save)
                    },
                )
            }

            if (viewModel.isEditMode && viewModel.canDelete()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.requestDeleteConfirm() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.goal_delete))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text(stringResource(R.string.goal_delete_title)) },
            text = { Text(stringResource(R.string.goal_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(
                            onSuccess = {
                                if (onDeleted != null) {
                                    onDeleted()
                                } else {
                                    onFinishedNeedRefresh()
                                }
                            },
                        )
                    },
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.goal_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) {
                    Text(stringResource(R.string.goal_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun GoalProductHitCard(
    hit: GoalProductHitDto,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = hit.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatMinorCurrencyFromCents(hit.priceCents, hit.currencyId),
                style = MaterialTheme.typography.titleMedium,
                color = WellPaidNavy,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.goal_product_link_label) + ": " + hit.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
