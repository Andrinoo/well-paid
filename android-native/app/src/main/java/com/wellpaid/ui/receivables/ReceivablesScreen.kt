package com.wellpaid.ui.receivables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.receivable.ReceivableDto
import com.wellpaid.ui.components.WellPaidPullToRefreshBox
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.formatIsoDateToBr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivablesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReceivablesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var settleTarget by remember { mutableStateOf<ReceivableDto?>(null) }
    var createIncome by remember { mutableStateOf(false) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    val pullRefreshing = state.isLoading && (state.asCreditor.isNotEmpty() || state.asDebtor.isNotEmpty())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        stringResource(R.string.receivables_title),
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.main_back_to_home),
                            tint = Color.White,
                        )
                    }
                },
            )
        },
    ) { inner ->
        if (state.isLoading && state.asCreditor.isEmpty() && state.asDebtor.isEmpty()) {
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

        WellPaidPullToRefreshBox(
            refreshing = pullRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            state.errorMessage?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (state.asCreditor.isEmpty() && state.asDebtor.isEmpty() && !state.isLoading) {
                Text(
                    text = stringResource(R.string.receivables_empty_both),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        Text(
                            stringResource(R.string.receivables_section_owed_to_you),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(state.asCreditor, key = { it.id }) { row ->
                        ReceivableRow(
                            row = row,
                            subtitle = row.debtorDisplayName.orEmpty(),
                            onSettle = {
                                settleTarget = row
                                createIncome = false
                                categoryId = state.incomeCategories.firstOrNull()?.id
                            },
                            working = state.isWorking,
                        )
                        HorizontalDivider()
                    }
                    item {
                        Spacer(Modifier.padding(8.dp))
                        Text(
                            stringResource(R.string.receivables_section_you_owe),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    items(state.asDebtor, key = { it.id }) { row ->
                        ReceivableRow(
                            row = row,
                            subtitle = row.creditorDisplayName.orEmpty(),
                            onSettle = null,
                            working = state.isWorking,
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        }
    }

    settleTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { settleTarget = null },
            title = { Text(stringResource(R.string.receivables_settle)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(formatBrlFromCents(target.amountCents))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = createIncome,
                            onCheckedChange = { createIncome = it },
                        )
                        Text(stringResource(R.string.receivables_settle_create_income))
                    }
                    if (createIncome && state.incomeCategories.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        val label = state.incomeCategories.find { it.id == categoryId }?.name.orEmpty()
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(
                                        MenuAnchorType.PrimaryNotEditable,
                                        enabled = true,
                                    ),
                                readOnly = true,
                                value = label,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.income_field_category)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                state.incomeCategories.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name) },
                                        onClick = {
                                            categoryId = c.id
                                            expanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.settle(
                            target.id,
                            createIncome,
                            categoryId,
                        ) { settleTarget = null }
                    },
                    enabled = !state.isWorking,
                ) {
                    Text(stringResource(R.string.receivables_settle))
                }
            },
            dismissButton = {
                TextButton(onClick = { settleTarget = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ReceivableRow(
    row: ReceivableDto,
    subtitle: String,
    onSettle: (() -> Unit)?,
    working: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = formatBrlFromCents(row.amountCents),
                style = MaterialTheme.typography.titleMedium,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.receivables_due_by, formatIsoDateToBr(row.settleBy)),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (onSettle != null) {
            OutlinedButton(
                onClick = onSettle,
                enabled = !working,
            ) {
                Text(stringResource(R.string.receivables_settle))
            }
        }
    }
}
