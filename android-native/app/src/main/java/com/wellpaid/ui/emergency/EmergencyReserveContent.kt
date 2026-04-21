package com.wellpaid.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import com.wellpaid.R
import com.wellpaid.core.model.emergency.EmergencyReservePlanDto
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidNavyDeep
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.DiscreetBalanceValue
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.centsToBrlInput
import com.wellpaid.util.formatIsoDateToBr
import kotlin.math.roundToInt
import kotlin.math.ceil

@Composable
fun EmergencyReserveContent(
    mainRouteEntry: NavBackStackEntry,
    onOpenReserveNew: () -> Unit,
    modifier: Modifier = Modifier,
    tabSwipe: Modifier = Modifier,
    viewModel: EmergencyReserveViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dirtyFlow = remember(mainRouteEntry) {
        mainRouteEntry.savedStateHandle.getStateFlow("emergency_reserve_dirty", 0L)
    }
    val emergencyDirty by dirtyFlow.collectAsStateWithLifecycle()
    LaunchedEffect(emergencyDirty) {
        if (emergencyDirty != 0L) {
            viewModel.refresh()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .fillMaxWidth()
            .wellPaidMaxContentWidth(WellPaidMaxContentWidth),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = { viewModel.refresh() }, enabled = !state.isLoading) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.home_refresh))
            }
        }

        if (state.isLoading && state.reserve == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(tabSwipe)
                .verticalScroll(rememberScrollState()),
        ) {
            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            val r = state.reserve
            if (r != null) {
                val annualTarget = (r.monthlyTargetCents.toLong() * 12L).coerceAtLeast(1L)
                val progress = (r.balanceCents.toFloat() / annualTarget.toFloat()).coerceIn(0f, 1f)
                val pctAnnual = (progress * 100f).roundToInt().coerceIn(0, 100)
                val planCount = state.plans.size

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(WellPaidNavyDeep)
                        .padding(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.NightsStay,
                            contentDescription = null,
                            tint = WellPaidGold,
                        )
                        Text(
                            text = stringResource(R.string.emergency_hero_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.emergency_balance_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                    Spacer(Modifier.height(4.dp))
                    DiscreetBalanceValue(
                        cents = r.balanceCents,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = WellPaidGold,
                        textAlign = TextAlign.Start,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(
                            R.string.emergency_meta_monthly_line,
                            formatBrlFromCents(r.monthlyTargetCents),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.95f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.emergency_hero_annual_line, pctAnnual),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.95f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.emergency_hero_plans_count_line, planCount),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.95f),
                    )
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = WellPaidGold,
                        trackColor = Color.White.copy(alpha = 0.15f),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.emergency_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onOpenReserveNew,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isCreatingPlan && !state.isSaving && !state.isUpdatingPlan,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidNavy,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = stringResource(R.string.emergency_add_reserve),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (state.plans.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.emergency_plans_section_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                    )
                    Spacer(Modifier.height(8.dp))
                    state.plans.forEach { plan ->
                        EmergencyReservePlanFullCard(
                            plan = plan,
                            enabled = !state.isUpdatingPlan && !state.isSaving,
                            onEdit = { viewModel.startEditingPlan(plan) },
                            onDelete = { viewModel.requestDeletePlan(plan.id) },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                WellPaidMoneyDigitKeypadField(
                    valueText = state.monthlyTargetText,
                    onValueTextChange = { viewModel.setMonthlyTargetText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    label = { Text(stringResource(R.string.emergency_monthly_target_field)) },
                    placeholder = stringResource(R.string.emergency_monthly_placeholder),
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.emergency_shortcuts_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = WellPaidNavy,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(20_000, 50_000, 100_000).forEach { cents ->
                        val label = formatBrlFromCents(cents)
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.setMonthlyTargetText(centsToBrlInput(cents)) },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isSaving,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.saveMonthlyTarget() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidGold,
                        contentColor = WellPaidNavy,
                    ),
                ) {
                    Text(
                        text = if (state.isSaving) {
                            stringResource(R.string.emergency_saving)
                        } else {
                            stringResource(R.string.emergency_save_meta)
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (!r.configured) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.emergency_not_configured_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                Spacer(Modifier.height(16.dp))
                EmergencyReservePolicyGuidance(
                    balanceCents = r.balanceCents,
                    monthlyTargetCents = r.monthlyTargetCents,
                )

                Spacer(Modifier.height(12.dp))
                EmergencyReservePracticalGuidance(
                    showRecomposition = state.plans.size > 1,
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        R.string.emergency_tracking_start,
                        formatIsoDateToBr(r.trackingStart),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(28.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.emergency_accruals_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                if (state.accruals.isEmpty()) {
                    Text(
                        text = stringResource(R.string.emergency_accruals_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.accruals.forEach { a ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = viewModel.formatAccrualMonth(a.year, a.month),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = formatBrlFromCents(a.amountCents),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
            if (r == null && !state.isLoading) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.emergency_not_configured_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onOpenReserveNew,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isCreatingPlan && !state.isSaving && !state.isUpdatingPlan,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidNavy,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = stringResource(R.string.emergency_add_reserve),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                WellPaidMoneyDigitKeypadField(
                    valueText = state.monthlyTargetText,
                    onValueTextChange = { viewModel.setMonthlyTargetText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    label = { Text(stringResource(R.string.emergency_monthly_target_field)) },
                    placeholder = stringResource(R.string.emergency_monthly_placeholder),
                )
                    Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.saveMonthlyTarget() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidGold,
                        contentColor = WellPaidNavy,
                    ),
                ) {
                    Text(
                        text = if (state.isSaving) stringResource(R.string.emergency_saving)
                        else stringResource(R.string.emergency_save_meta),
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.emergency_shortcuts_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(20_000, 50_000, 100_000).forEach { cents ->
                            val label = formatBrlFromCents(cents)
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.setMonthlyTargetText(centsToBrlInput(cents)) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.weight(1f),
                                enabled = !state.isSaving,
                            )
                        }
                    }
            }
        }
    }

    if (state.editingPlanId != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelEditingPlan() },
            title = { Text(stringResource(R.string.emergency_plan_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.editingPlanTitleText,
                        onValueChange = { viewModel.setEditingPlanTitleText(it) },
                        label = { Text(stringResource(R.string.emergency_new_plan_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isUpdatingPlan,
                        shape = RoundedCornerShape(14.dp),
                    )
                    OutlinedTextField(
                        value = state.editingPlanDetailsText,
                        onValueChange = { viewModel.setEditingPlanDetailsText(it) },
                        label = { Text(stringResource(R.string.emergency_new_plan_details_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        enabled = !state.isUpdatingPlan,
                        shape = RoundedCornerShape(14.dp),
                    )
                    WellPaidMoneyDigitKeypadField(
                        valueText = state.editingPlanMonthlyText,
                        onValueTextChange = { viewModel.setEditingPlanMonthlyText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isUpdatingPlan,
                        label = { Text(stringResource(R.string.emergency_new_plan_monthly_label)) },
                        placeholder = stringResource(R.string.emergency_monthly_placeholder),
                    )
                    WellPaidMoneyDigitKeypadField(
                        valueText = state.editingPlanTargetText,
                        onValueTextChange = { viewModel.setEditingPlanTargetText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isUpdatingPlan,
                        label = { Text(stringResource(R.string.emergency_new_plan_target_label)) },
                        placeholder = stringResource(R.string.emergency_monthly_placeholder),
                    )
                    OutlinedTextField(
                        value = state.editingPlanDurationMonthsText,
                        onValueChange = { viewModel.setEditingPlanDurationMonthsText(it) },
                        label = { Text(stringResource(R.string.emergency_new_plan_duration_label)) },
                        supportingText = { Text(stringResource(R.string.emergency_new_plan_duration_footnote)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isUpdatingPlan,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.saveEditingPlan() },
                    enabled = !state.isUpdatingPlan,
                ) {
                    Text(stringResource(R.string.emergency_plan_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelEditingPlan() },
                    enabled = !state.isUpdatingPlan,
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (state.showDeletePlanConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeletePlan() },
            title = { Text(stringResource(R.string.emergency_plan_delete_title)) },
            text = { Text(stringResource(R.string.emergency_plan_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deletePlanConfirmed() },
                    enabled = !state.isUpdatingPlan,
                ) {
                    Text(stringResource(R.string.emergency_plan_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDeletePlan() },
                    enabled = !state.isUpdatingPlan,
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
private fun EmergencyReservePlanFullCard(
    plan: EmergencyReservePlanDto,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val statusLabel = when (plan.status) {
        "active" -> stringResource(R.string.emergency_plan_status_active)
        "completed" -> stringResource(R.string.emergency_plan_status_completed)
        else -> plan.status
    }
    val detailsText = plan.details?.trim().orEmpty()
    val targetCents = plan.targetCents
    val estimateMonths = if (targetCents != null && targetCents > 0) {
        estimateMonthsToReachTarget(
            currentCents = plan.balanceCents,
            monthlyCents = plan.monthlyTargetCents,
            targetCents = targetCents,
        )
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(14.dp),
    ) {
        Text(
            text = plan.title.ifBlank { stringResource(R.string.emergency_plan_untitled) },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
        Spacer(Modifier.height(10.dp))
        ReservePlanLabeledField(
            label = stringResource(R.string.emergency_plan_status_label),
            value = statusLabel,
        )
        Spacer(Modifier.height(6.dp))
        ReservePlanLabeledField(
            label = stringResource(R.string.emergency_plan_label_balance),
            value = formatBrlFromCents(plan.balanceCents),
        )
        Spacer(Modifier.height(6.dp))
        ReservePlanLabeledField(
            label = stringResource(R.string.emergency_plan_label_monthly),
            value = formatBrlFromCents(plan.monthlyTargetCents),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.emergency_new_plan_details_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = if (detailsText.isNotEmpty()) {
                detailsText
            } else {
                stringResource(R.string.emergency_plan_no_details)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (detailsText.isNotEmpty()) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.emergency_plan_label_validity),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = plan.planDurationMonths?.let { months ->
                stringResource(R.string.emergency_plan_duration_months, months)
            } ?: stringResource(R.string.emergency_plan_validity_open_ended),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(6.dp))
        if (targetCents != null && targetCents > 0) {
            ReservePlanLabeledField(
                label = stringResource(R.string.emergency_new_plan_target_label),
                value = formatBrlFromCents(targetCents),
            )
            estimateMonths?.let { months ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.emergency_plan_estimate_line, months),
                    style = MaterialTheme.typography.bodySmall,
                    color = WellPaidNavy,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            ReservePlanLabeledField(
                label = stringResource(R.string.emergency_new_plan_target_label),
                value = stringResource(R.string.emergency_plan_no_ceiling),
            )
        }
        Spacer(Modifier.height(6.dp))
        ReservePlanLabeledField(
            label = stringResource(R.string.emergency_plan_label_counting),
            value = formatIsoDateToBr(plan.trackingStart),
        )
        plan.completedAt?.takeIf { it.isNotBlank() }?.let { completedAt ->
            Spacer(Modifier.height(6.dp))
            ReservePlanLabeledField(
                label = stringResource(R.string.emergency_plan_label_completed),
                value = formatIsoDateToBr(completedAt),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WellPaidNavy,
                    contentColor = Color.White,
                ),
            ) {
                Text(stringResource(R.string.emergency_plan_edit))
            }
            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                Text(stringResource(R.string.emergency_plan_delete))
            }
        }
    }
}

@Composable
private fun ReservePlanLabeledField(
    label: String,
    value: String,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

private fun estimateMonthsToReachTarget(
    currentCents: Int,
    monthlyCents: Int,
    targetCents: Int,
): Int? {
    if (monthlyCents <= 0) return null
    val remaining = (targetCents - currentCents).coerceAtLeast(0)
    if (remaining == 0) return 0
    return ceil(remaining / monthlyCents.toDouble()).toInt()
}

@Composable
private fun EmergencyReservePolicyGuidance(
    balanceCents: Int,
    monthlyTargetCents: Int,
) {
    val safeMonthly = monthlyTargetCents.coerceAtLeast(1)
    val monthsCovered = balanceCents.toFloat() / safeMonthly.toFloat()
    val maturityLabel = when {
        monthsCovered < 3f -> stringResource(R.string.emergency_policy_stage_build)
        monthsCovered < 6f -> stringResource(R.string.emergency_policy_stage_stabilize)
        monthsCovered < 12f -> stringResource(R.string.emergency_policy_stage_protect)
        else -> stringResource(R.string.emergency_policy_stage_invest)
    }
    val actionLabel = when {
        monthsCovered < 3f -> stringResource(R.string.emergency_policy_action_build)
        monthsCovered < 6f -> stringResource(R.string.emergency_policy_action_stabilize)
        monthsCovered < 12f -> stringResource(R.string.emergency_policy_action_protect)
        else -> stringResource(R.string.emergency_policy_action_invest)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.emergency_policy_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.emergency_policy_coverage, monthsCovered),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.emergency_policy_stage_label, maturityLabel),
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmergencyReservePracticalGuidance(
    showRecomposition: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WellPaidNavy.copy(alpha = 0.05f))
            .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.emergency_guidance_tips_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
        Spacer(Modifier.height(8.dp))
        EmergencyGuidanceTipBullet(
            stringResource(R.string.emergency_tip_named_plans),
        )
        Spacer(Modifier.height(6.dp))
        EmergencyGuidanceTipBullet(
            stringResource(R.string.emergency_tip_rebalance),
        )
        Spacer(Modifier.height(6.dp))
        EmergencyGuidanceTipBullet(
            stringResource(R.string.emergency_tip_liquidity),
        )
        Spacer(Modifier.height(6.dp))
        EmergencyGuidanceTipBullet(
            stringResource(R.string.emergency_tip_separate_purpose),
        )
        if (showRecomposition) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.emergency_recomposition_title),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.emergency_recomposition_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmergencyGuidanceTipBullet(
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "•\u00A0",
            style = MaterialTheme.typography.bodySmall,
            color = WellPaidNavy,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}
