package com.wellpaid.ui.incomes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
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
import com.wellpaid.ui.components.WellPaidDatePickerField
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeFormScreen(
    onNavigateBack: () -> Unit,
    onFinishedNeedRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IncomeFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val canEdit = viewModel.canEditFields()
    val canDelete = viewModel.canDelete()
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val incomeFieldShape = RoundedCornerShape(14.dp)
    val incomeFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = WellPaidCreamMuted,
        unfocusedContainerColor = WellPaidCreamMuted,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(
                            if (viewModel.isEditMode) R.string.income_edit_title
                            else R.string.income_new_title,
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
            state.loadedIncome?.let { row ->
                if (!row.isMine) {
                    Text(
                        text = stringResource(R.string.income_readonly_not_mine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }

            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = { if (canEdit) viewModel.setDescription(it) },
                label = { Text(stringResource(R.string.income_field_description)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canEdit,
                singleLine = false,
                minLines = 2,
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(12.dp))

            WellPaidMoneyDigitKeypadField(
                valueText = state.amountText,
                onValueTextChange = { if (canEdit) viewModel.setAmountText(it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canEdit,
                label = { Text(stringResource(R.string.income_field_amount)) },
                placeholder = stringResource(R.string.expense_field_amount_hint),
            )
            Spacer(Modifier.height(12.dp))

            WellPaidDatePickerField(
                label = { Text(stringResource(R.string.income_field_date)) },
                isoDate = state.incomeDate,
                onIsoDateChange = { if (canEdit) viewModel.setIncomeDate(it) },
                enabled = canEdit,
                modifier = Modifier.fillMaxWidth(),
                colors = incomeFieldColors,
                shape = incomeFieldShape,
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.notes,
                onValueChange = { if (canEdit) viewModel.setNotes(it) },
                label = { Text(stringResource(R.string.income_field_notes)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = canEdit,
                singleLine = false,
                minLines = 2,
                supportingText = { Text(stringResource(R.string.income_field_notes_optional)) },
                shape = RoundedCornerShape(14.dp),
            )
            Spacer(Modifier.height(12.dp))

            if (state.categories.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { if (canEdit) categoryMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = canEdit),
                        readOnly = true,
                        enabled = canEdit,
                        value = state.categories.find { it.id == state.categoryId }?.name.orEmpty(),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.income_field_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        shape = RoundedCornerShape(14.dp),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                    ) {
                        state.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    viewModel.setCategoryId(cat.id)
                                    categoryMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.income_no_categories),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(24.dp))

            if (canEdit) {
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
                            stringResource(R.string.income_saving)
                        } else {
                            stringResource(R.string.income_save)
                        },
                    )
                }
            }

            if (canDelete) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.requestDeleteConfirm() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.income_delete))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = { Text(stringResource(R.string.income_delete_title)) },
            text = { Text(stringResource(R.string.income_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.delete(onFinishedNeedRefresh) },
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.income_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) {
                    Text(stringResource(R.string.income_delete_cancel))
                }
            },
        )
    }
}
