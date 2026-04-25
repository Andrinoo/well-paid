package com.wellpaid.ui.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidDatePickerField
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatBrlFromCents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    onNavigateBack: () -> Unit,
    onFinishedNeedRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val canShareExpense by viewModel.canShareExpenseState.collectAsStateWithLifecycle()
    val canEdit = viewModel.canEditFields()
    val canPay = viewModel.canPay()
    val canDelete = viewModel.canDelete()
    var showInstallmentFullWipeSecond by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    /** Uma linha por defeito; ícone para várias linhas quando precisar. */
    var descriptionExpanded by remember { mutableStateOf(false) }
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = WellPaidCreamMuted,
        unfocusedContainerColor = WellPaidCreamMuted,
        disabledContainerColor = WellPaidCreamMuted.copy(alpha = 0.6f),
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(
                            if (viewModel.isEditMode) R.string.expense_edit_title
                            else R.string.expense_new_title,
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
                .background(WellPaidCream)
                .padding(inner)
                .imePadding()
                .wellPaidScreenHorizontalPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            state.loadedExpense?.let { e ->
                when {
                    !e.isMine -> {
                        Text(
                            text = stringResource(R.string.expense_readonly_not_mine),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    e.isProjected -> {
                        Text(
                            text = stringResource(R.string.expense_readonly_projected),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                if (e.sharedExpensePaymentAlert) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = stringResource(
                                R.string.expense_share_alert_detail,
                                e.counterpartyLabel.orEmpty(),
                                formatBrlFromCents(e.myShareCents ?: 0),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (e.sharedExpensePeerDeclinedAlert && e.isMine) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = stringResource(R.string.expense_share_peer_declined_owner),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (e.myShareDeclined && !e.isMine) {
                    Text(
                        text = stringResource(R.string.expense_share_you_declined_peer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            if (!viewModel.isEditMode) {
                val isSingle = state.expenseKind == NewExpenseKind.SINGLE
                val isInst = state.expenseKind == NewExpenseKind.INSTALLMENTS
                val isRec = state.expenseKind == NewExpenseKind.RECURRING

                OutlinedTextField(
                    value = state.description,
                    onValueChange = { if (canEdit) viewModel.setDescription(it) },
                    label = { Text(stringResource(R.string.expense_field_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canEdit,
                    singleLine = !descriptionExpanded,
                    minLines = if (descriptionExpanded) 2 else 1,
                    maxLines = if (descriptionExpanded) 4 else 1,
                    shape = fieldShape,
                    colors = fieldColors,
                    trailingIcon = {
                        IconButton(
                            onClick = { descriptionExpanded = !descriptionExpanded },
                            enabled = canEdit,
                        ) {
                            Icon(
                                imageVector = if (descriptionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = stringResource(
                                    if (descriptionExpanded) {
                                        R.string.expense_description_collapse_cd
                                    } else {
                                        R.string.expense_description_expand_cd
                                    },
                                ),
                                tint = WellPaidNavy,
                            )
                        }
                    },
                    supportingText = {
                        Text(
                            stringResource(R.string.expense_desc_counter, state.description.length),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val kindLabelStyle = MaterialTheme.typography.labelMedium
                    val expenseKindChipColors = FilterChipDefaults.filterChipColors(
                        containerColor = WellPaidCreamMuted,
                        labelColor = WellPaidNavy,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                    )
                    FilterChip(
                        selected = isSingle,
                        onClick = { if (canEdit) viewModel.setExpenseKind(NewExpenseKind.SINGLE) },
                        label = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    stringResource(R.string.expense_type_single),
                                    style = kindLabelStyle,
                                    maxLines = 1,
                                )
                            }
                        },
                        enabled = canEdit,
                        colors = expenseKindChipColors,
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = isInst,
                        onClick = { if (canEdit) viewModel.setExpenseKind(NewExpenseKind.INSTALLMENTS) },
                        label = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    stringResource(R.string.expense_type_installments),
                                    style = kindLabelStyle,
                                    maxLines = 1,
                                )
                            }
                        },
                        enabled = canEdit,
                        colors = expenseKindChipColors,
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = isRec,
                        onClick = { if (canEdit) viewModel.setExpenseKind(NewExpenseKind.RECURRING) },
                        label = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    stringResource(R.string.expense_type_recurring),
                                    style = kindLabelStyle,
                                    maxLines = 1,
                                )
                            }
                        },
                        enabled = canEdit,
                        colors = expenseKindChipColors,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))

                WellPaidMoneyDigitKeypadField(
                    valueText = state.amountText,
                    onValueTextChange = { if (canEdit) viewModel.setAmountText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canEdit,
                    label = { Text(stringResource(R.string.expense_field_amount)) },
                    placeholder = stringResource(R.string.expense_field_amount_hint),
                    shape = fieldShape,
                    colors = fieldColors,
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.expense_toggle_paid),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WellPaidNavy,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = state.alreadyPaid,
                        onCheckedChange = { if (canEdit) viewModel.setAlreadyPaid(it) },
                        enabled = canEdit,
                    )
                }

                when {
                    isSingle -> {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.expense_toggle_due),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WellPaidNavy,
                                )
                                Text(
                                    text = stringResource(R.string.expense_toggle_due_sub),
                                    style = MaterialTheme.typography.labelSmall,
                                    lineHeight = 14.sp,
                                    color = WellPaidNavy.copy(alpha = 0.58f),
                                )
                            }
                            Switch(
                                checked = state.hasDueDate,
                                onCheckedChange = { if (canEdit) viewModel.setHasDueDate(it) },
                                enabled = canEdit,
                            )
                        }
                        if (state.hasDueDate) {
                            Spacer(Modifier.height(6.dp))
                            WellPaidDatePickerField(
                                label = { Text(stringResource(R.string.expense_field_due_date)) },
                                isoDate = state.dueDate,
                                onIsoDateChange = { if (canEdit) viewModel.setDueDate(it) },
                                enabled = canEdit,
                                modifier = Modifier.fillMaxWidth(),
                                colors = fieldColors,
                                shape = fieldShape,
                            )
                            Text(
                                text = stringResource(R.string.expense_field_due_optional),
                                style = MaterialTheme.typography.labelSmall,
                                color = WellPaidNavy.copy(alpha = 0.58f),
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        WellPaidDatePickerField(
                            label = { Text(stringResource(R.string.expense_field_expense_date)) },
                            isoDate = state.expenseDate,
                            onIsoDateChange = { if (canEdit) viewModel.setExpenseDate(it) },
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                            shape = fieldShape,
                        )
                    }
                    isInst -> {
                        Spacer(Modifier.height(8.dp))
                        WellPaidDatePickerField(
                            label = { Text(stringResource(R.string.expense_field_due_first)) },
                            isoDate = state.dueDate,
                            onIsoDateChange = { if (canEdit) viewModel.setDueDate(it) },
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                            shape = fieldShape,
                        )
                        Text(
                            text = stringResource(R.string.expense_installments_anchor_footer),
                            style = MaterialTheme.typography.labelSmall,
                            lineHeight = 14.sp,
                            color = WellPaidNavy.copy(alpha = 0.58f),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = state.installmentTotal.toString(),
                            onValueChange = { txt ->
                                if (!canEdit) return@OutlinedTextField
                                if (txt.isBlank()) {
                                    viewModel.setInstallmentTotal(2)
                                    return@OutlinedTextField
                                }
                                val n = txt.filter { it.isDigit() }.toIntOrNull() ?: return@OutlinedTextField
                                viewModel.setInstallmentTotal(n.coerceIn(2, 999))
                            },
                            label = { Text(stringResource(R.string.expense_installment_count)) },
                            placeholder = {
                                Text(stringResource(R.string.expense_installment_hint))
                            },
                            supportingText = {
                                Text(
                                    stringResource(R.string.expense_installment_helper),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = canEdit,
                            singleLine = true,
                            shape = fieldShape,
                            colors = fieldColors,
                        )
                    }
                    isRec -> {
                        Spacer(Modifier.height(8.dp))
                        WellPaidDatePickerField(
                            label = { Text(stringResource(R.string.expense_field_due_first)) },
                            isoDate = state.dueDate,
                            onIsoDateChange = { if (canEdit) viewModel.setDueDate(it) },
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                            shape = fieldShape,
                        )
                        Text(
                            text = stringResource(R.string.expense_due_required_par_rec),
                            style = MaterialTheme.typography.labelSmall,
                            color = WellPaidNavy.copy(alpha = 0.58f),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.expense_recurring_frequency),
                            style = MaterialTheme.typography.labelMedium,
                            color = WellPaidNavy,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(
                                "monthly" to R.string.expense_freq_monthly,
                                "weekly" to R.string.expense_freq_weekly,
                                "yearly" to R.string.expense_freq_yearly,
                            ).forEach { (key, strId) ->
                                FilterChip(
                                    selected = state.recurringFrequency.equals(key, ignoreCase = true),
                                    onClick = { if (canEdit) viewModel.setRecurringFrequency(key) },
                                    label = {
                                        Text(
                                            stringResource(strId),
                                            style = MaterialTheme.typography.labelLarge,
                                        )
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        WellPaidDatePickerField(
                            label = { Text(stringResource(R.string.expense_field_expense_date)) },
                            isoDate = state.expenseDate,
                            onIsoDateChange = { if (canEdit) viewModel.setExpenseDate(it) },
                            enabled = canEdit,
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                            shape = fieldShape,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
            } else {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { if (canEdit) viewModel.setDescription(it) },
                    label = { Text(stringResource(R.string.expense_field_description)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canEdit,
                    singleLine = !descriptionExpanded,
                    minLines = if (descriptionExpanded) 2 else 1,
                    maxLines = if (descriptionExpanded) 4 else 1,
                    shape = fieldShape,
                    colors = fieldColors,
                    trailingIcon = {
                        IconButton(
                            onClick = { descriptionExpanded = !descriptionExpanded },
                            enabled = canEdit,
                        ) {
                            Icon(
                                imageVector = if (descriptionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = stringResource(
                                    if (descriptionExpanded) {
                                        R.string.expense_description_collapse_cd
                                    } else {
                                        R.string.expense_description_expand_cd
                                    },
                                ),
                                tint = WellPaidNavy,
                            )
                        }
                    },
                    supportingText = {
                        Text(
                            stringResource(R.string.expense_desc_counter, state.description.length),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))

                WellPaidMoneyDigitKeypadField(
                    valueText = state.amountText,
                    onValueTextChange = { if (canEdit) viewModel.setAmountText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canEdit,
                    label = { Text(stringResource(R.string.expense_field_amount)) },
                    placeholder = stringResource(R.string.expense_field_amount_hint),
                    shape = fieldShape,
                    colors = fieldColors,
                )
                Spacer(Modifier.height(8.dp))

                WellPaidDatePickerField(
                    label = { Text(stringResource(R.string.expense_field_expense_date)) },
                    isoDate = state.expenseDate,
                    onIsoDateChange = { if (canEdit) viewModel.setExpenseDate(it) },
                    enabled = canEdit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    shape = fieldShape,
                )
                Spacer(Modifier.height(8.dp))

                Column(Modifier.fillMaxWidth()) {
                    WellPaidDatePickerField(
                        label = { Text(stringResource(R.string.expense_field_due_date)) },
                        isoDate = state.dueDate,
                        onIsoDateChange = { if (canEdit) viewModel.setDueDate(it) },
                        enabled = canEdit,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                        shape = fieldShape,
                    )
                    Text(
                        text = stringResource(R.string.expense_field_due_optional),
                        style = MaterialTheme.typography.labelSmall,
                        color = WellPaidNavy.copy(alpha = 0.58f),
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            if (state.categories.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { if (canEdit) categoryMenuExpanded = it },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = canEdit),
                        readOnly = true,
                        enabled = canEdit,
                        value = state.categories.find { it.id == state.categoryId }?.name.orEmpty(),
                        onValueChange = {},
                        label = { Text(stringResource(R.string.expense_field_category)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        shape = fieldShape,
                        colors = fieldColors,
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
                    text = stringResource(R.string.expense_no_categories),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            val showShareControls = !viewModel.isEditMode || canEdit
            if (showShareControls) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.expense_toggle_family),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WellPaidNavy,
                        )
                        Text(
                            text = stringResource(R.string.expense_toggle_family_sub),
                            style = MaterialTheme.typography.labelSmall,
                            lineHeight = 14.sp,
                            color = WellPaidNavy.copy(alpha = 0.58f),
                        )
                    }
                    Switch(
                        checked = state.isFamily,
                        onCheckedChange = { if (canEdit) viewModel.setFamily(it) },
                        enabled = canEdit,
                    )
                }
                if (canShareExpense) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.expense_toggle_share),
                                style = MaterialTheme.typography.bodyMedium,
                                color = WellPaidNavy,
                            )
                            Text(
                                text = stringResource(R.string.expense_toggle_share_sub),
                                style = MaterialTheme.typography.labelSmall,
                                lineHeight = 14.sp,
                                color = WellPaidNavy.copy(alpha = 0.58f),
                            )
                        }
                        Switch(
                            checked = state.isShared,
                            onCheckedChange = { if (canEdit) viewModel.setShared(it) },
                            enabled = canEdit,
                        )
                    }
                    if (state.isShared) {
                        Spacer(Modifier.height(6.dp))
                        var shareMenuExpanded by remember { mutableStateOf(false) }
                        val peers = viewModel.peerMembersForShare()
                        val selectedLabel = peers.find { it.userId == state.sharedWithUserId }
                            ?.let { m -> m.fullName?.takeIf { fn -> fn.isNotBlank() } ?: m.email }
                            ?: stringResource(R.string.expense_share_pick_member)
                        ExposedDropdownMenuBox(
                            expanded = shareMenuExpanded,
                            onExpandedChange = { if (canEdit) shareMenuExpanded = it },
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = canEdit),
                                readOnly = true,
                                enabled = canEdit,
                                value = selectedLabel,
                                onValueChange = {},
                                label = { Text(stringResource(R.string.expense_share_with_label)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = shareMenuExpanded)
                                },
                                shape = fieldShape,
                                colors = fieldColors,
                            )
                            ExposedDropdownMenu(
                                expanded = shareMenuExpanded,
                                onDismissRequest = { shareMenuExpanded = false },
                            ) {
                                peers.forEach { m ->
                                    val label = m.fullName?.takeIf { it.isNotBlank() } ?: m.email
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setSharedWithUserId(m.userId)
                                            shareMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                        if (canEdit) {
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.expense_split_use_percent),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = WellPaidNavy,
                                    )
                                    Text(
                                        text = stringResource(R.string.expense_split_use_percent_sub),
                                        style = MaterialTheme.typography.labelSmall,
                                        lineHeight = 14.sp,
                                        color = WellPaidNavy.copy(alpha = 0.58f),
                                    )
                                }
                                Switch(
                                    checked = state.usePercentSplit,
                                    onCheckedChange = { viewModel.setUsePercentSplit(it) },
                                    enabled = canEdit,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            if (!state.usePercentSplit) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    WellPaidMoneyDigitKeypadField(
                                        valueText = state.ownerShareText,
                                        onValueTextChange = { viewModel.setOwnerShareText(it) },
                                        modifier = Modifier.weight(1f),
                                        enabled = canEdit,
                                        label = {
                                            ExpenseSplitFieldLabel(
                                                stringResource(R.string.expense_split_owner_part),
                                            )
                                        },
                                        placeholder = stringResource(R.string.expense_field_amount_hint),
                                        shape = fieldShape,
                                        colors = fieldColors,
                                    )
                                    WellPaidMoneyDigitKeypadField(
                                        valueText = state.peerShareText,
                                        onValueTextChange = {},
                                        modifier = Modifier.weight(1f),
                                        enabled = false,
                                        label = {
                                            ExpenseSplitFieldLabel(
                                                stringResource(R.string.expense_split_peer_part_calculated),
                                            )
                                        },
                                        placeholder = stringResource(R.string.expense_field_amount_hint),
                                        shape = fieldShape,
                                        colors = fieldColors,
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    OutlinedTextField(
                                        value = state.ownerPercentText,
                                        onValueChange = { viewModel.setOwnerPercentText(it) },
                                        label = {
                                            ExpenseSplitFieldLabel(
                                                stringResource(R.string.expense_split_your_percent),
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = fieldShape,
                                        colors = fieldColors,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    )
                                    OutlinedTextField(
                                        value = state.peerPercentDisplayText,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = {
                                            ExpenseSplitFieldLabel(
                                                stringResource(R.string.expense_split_peer_percent_calculated),
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = fieldShape,
                                        colors = fieldColors,
                                    )
                                }
                                val (brlEstOwner, brlEstPeer) = viewModel.percentSplitDerivedBrlPreview()
                                if (brlEstOwner.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.expense_split_brl_estimated_section),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = WellPaidNavy,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        WellPaidMoneyDigitKeypadField(
                                            valueText = brlEstOwner,
                                            onValueTextChange = {},
                                            modifier = Modifier.weight(1f),
                                            enabled = false,
                                            label = {
                                                ExpenseSplitFieldLabel(
                                                    stringResource(R.string.expense_split_owner_part),
                                                )
                                            },
                                            placeholder = stringResource(R.string.expense_field_amount_hint),
                                            shape = fieldShape,
                                            colors = fieldColors,
                                        )
                                        WellPaidMoneyDigitKeypadField(
                                            valueText = brlEstPeer,
                                            onValueTextChange = {},
                                            modifier = Modifier.weight(1f),
                                            enabled = false,
                                            label = {
                                                ExpenseSplitFieldLabel(
                                                    stringResource(R.string.expense_split_peer_part_calculated),
                                                )
                                            },
                                            placeholder = stringResource(R.string.expense_field_amount_hint),
                                            shape = fieldShape,
                                            colors = fieldColors,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.expense_share_need_family),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            if (viewModel.isEditMode && canEdit) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.expense_field_status),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.status == "pending",
                        onClick = { viewModel.setStatus("pending") },
                        label = { Text(stringResource(R.string.expenses_status_pending)) },
                    )
                    FilterChip(
                        selected = state.status == "paid",
                        onClick = { viewModel.setStatus("paid") },
                        label = { Text(stringResource(R.string.expenses_status_paid)) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (canEdit) {
                Button(
                    onClick = { viewModel.save(onFinishedNeedRefresh) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidGold,
                        contentColor = WellPaidNavy,
                    ),
                ) {
                    Text(
                        text = if (state.isSaving) {
                            stringResource(R.string.expense_saving)
                        } else {
                            stringResource(R.string.expense_save)
                        },
                    )
                }
            }

            if (canPay) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.requestPayConfirm() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.expense_mark_paid))
                }
            }

            if (viewModel.canRequestCover()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.openCoverDialog() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.expense_cover_request))
                }
            }

            if (viewModel.canDeclineShare()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.declineShare(onFinishedNeedRefresh) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.expense_share_decline))
                }
            }

            if (viewModel.canAssumeFull()) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.assumeFullShare(onFinishedNeedRefresh) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidGold,
                        contentColor = WellPaidNavy,
                    ),
                ) {
                    Text(stringResource(R.string.expense_share_assume_full))
                }
            }

            if (canDelete) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.requestDeleteConfirm() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.expense_delete))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (state.showDeleteConfirm) {
        val le = state.loadedExpense
        val isInst = le?.installmentGroupId != null
        val hasPaid = le?.installmentPlanHasPaid == true
        if (isInst && hasPaid) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteConfirm() },
                title = { Text(stringResource(R.string.expense_delete_title)) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(stringResource(R.string.expense_delete_installment_choice_lead))
                        TextButton(
                            onClick = { viewModel.delete(onFinishedNeedRefresh, false) },
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.expense_delete_future_only))
                        }
                        TextButton(
                            onClick = {
                                viewModel.dismissDeleteConfirm()
                                showInstallmentFullWipeSecond = true
                            },
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.expense_delete_entire_plan))
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.dismissDeleteConfirm() },
                        enabled = !state.isSaving,
                    ) {
                        Text(stringResource(R.string.expense_delete_cancel))
                    }
                },
                dismissButton = {},
            )
        } else {
            val deleteBody = stringResource(R.string.expense_delete_message)
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteConfirm() },
                title = { Text(stringResource(R.string.expense_delete_title)) },
                text = { Text(deleteBody) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.delete(onFinishedNeedRefresh, false) },
                        enabled = !state.isSaving,
                    ) {
                        Text(stringResource(R.string.expense_delete_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteConfirm() }) {
                        Text(stringResource(R.string.expense_delete_cancel))
                    }
                },
            )
        }
    }

    if (showInstallmentFullWipeSecond) {
        AlertDialog(
            onDismissRequest = { showInstallmentFullWipeSecond = false },
            title = { Text(stringResource(R.string.expense_delete_full_wipe_title)) },
            text = { Text(stringResource(R.string.expense_delete_full_wipe_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInstallmentFullWipeSecond = false
                        viewModel.delete(onFinishedNeedRefresh, true)
                    },
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.expense_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallmentFullWipeSecond = false }) {
                    Text(stringResource(R.string.expense_delete_cancel))
                }
            },
        )
    }

    if (state.showPayConfirm) {
        val isRecurring = state.loadedExpense?.recurringSeriesId?.isNotBlank() == true
        AlertDialog(
            onDismissRequest = { viewModel.dismissPayConfirm() },
            title = { Text(stringResource(R.string.expense_pay_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.expense_pay_message))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.expense_pay_allow_advance),
                            style = MaterialTheme.typography.bodyMedium,
                            color = WellPaidNavy,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = state.payAllowAdvance,
                            onCheckedChange = { viewModel.setPayAllowAdvance(it) },
                            enabled = !state.isSaving,
                        )
                    }
                    if (isRecurring) {
                        WellPaidMoneyDigitKeypadField(
                            valueText = state.payAmountText,
                            onValueTextChange = { viewModel.setPayAmountText(it) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isSaving,
                            label = { Text(stringResource(R.string.expense_pay_amount_optional)) },
                            placeholder = stringResource(R.string.expense_field_amount_hint),
                            shape = fieldShape,
                            colors = fieldColors,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.pay(onFinishedNeedRefresh) },
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.expense_pay_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPayConfirm() }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (state.showCoverDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCoverDialog() },
            title = { Text(stringResource(R.string.expense_cover_request)) },
            text = {
                WellPaidDatePickerField(
                    label = { Text(stringResource(R.string.expense_cover_settle_by)) },
                    isoDate = state.coverSettleByIso,
                    onIsoDateChange = { viewModel.setCoverSettleByIso(it) },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                    shape = fieldShape,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.submitCover(onFinishedNeedRefresh) },
                    enabled = !state.isSaving,
                ) {
                    Text(stringResource(R.string.expense_cover_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCoverDialog() }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun ExpenseSplitFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 14.sp,
    )
}
