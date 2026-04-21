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
import androidx.compose.runtime.getValue
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
import com.wellpaid.R
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

@Composable
fun EmergencyReserveContent(
    modifier: Modifier = Modifier,
    tabSwipe: Modifier = Modifier,
    viewModel: EmergencyReserveViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                    Spacer(Modifier.height(12.dp))
                    DiscreetBalanceValue(
                        cents = r.balanceCents,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = WellPaidGold,
                        textAlign = TextAlign.Start,
                    )
                    Text(
                        text = stringResource(R.string.emergency_balance_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(
                            R.string.emergency_meta_monthly_line,
                            formatBrlFromCents(r.monthlyTargetCents),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = WellPaidGold,
                        trackColor = Color.White.copy(alpha = 0.15f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.emergency_percent_annual, pctAnnual),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.emergency_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(12.dp),
                        ) {
                            Text(
                                text = plan.title.ifBlank {
                                    stringResource(R.string.emergency_plan_untitled)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "${formatBrlFromCents(plan.balanceCents)} · ${
                                    formatBrlFromCents(plan.monthlyTargetCents)
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            plan.planDurationMonths?.let { months ->
                                Text(
                                    text = stringResource(R.string.emergency_plan_duration_months, months),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                            val statusLabel = when (plan.status) {
                                "active" -> stringResource(R.string.emergency_plan_status_active)
                                "completed" -> stringResource(R.string.emergency_plan_status_completed)
                                else -> plan.status
                            }
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (state.canEditReserve && plan.status == "active") {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = { viewModel.startEditingPlan(plan) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !state.isUpdatingPlan && !state.isSaving,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = WellPaidNavy,
                                            contentColor = Color.White,
                                        ),
                                    ) {
                                        Text(stringResource(R.string.emergency_plan_edit))
                                    }
                                    Button(
                                        onClick = { viewModel.requestDeletePlan(plan.id) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !state.isUpdatingPlan && !state.isSaving,
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
                        Spacer(Modifier.height(8.dp))
                    }
                }

                if (!state.canEditReserve) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.emergency_readonly_not_owner),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

                Spacer(Modifier.height(16.dp))
                WellPaidMoneyDigitKeypadField(
                    valueText = state.monthlyTargetText,
                    onValueTextChange = { viewModel.setMonthlyTargetText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canEditReserve && !state.isSaving,
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
                            enabled = state.canEditReserve && !state.isSaving,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.saveMonthlyTarget() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.canEditReserve && !state.isSaving,
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

                if (state.canEditReserve) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.emergency_new_plan_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.newPlanTitleText,
                        onValueChange = { viewModel.setNewPlanTitleText(it) },
                        label = { Text(stringResource(R.string.emergency_new_plan_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isCreatingPlan && !state.isSaving,
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    WellPaidMoneyDigitKeypadField(
                        valueText = state.newPlanMonthlyText,
                        onValueTextChange = { viewModel.setNewPlanMonthlyText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isCreatingPlan && !state.isSaving,
                        label = { Text(stringResource(R.string.emergency_new_plan_monthly_label)) },
                        placeholder = stringResource(R.string.emergency_monthly_placeholder),
                    )
                    Text(
                        text = stringResource(R.string.emergency_new_plan_monthly_footnote),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.newPlanDurationMonthsText,
                        onValueChange = { viewModel.setNewPlanDurationMonthsText(it) },
                        label = { Text(stringResource(R.string.emergency_new_plan_duration_label)) },
                        supportingText = { Text(stringResource(R.string.emergency_new_plan_duration_footnote)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isCreatingPlan && !state.isSaving,
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.createNamedPlan() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isCreatingPlan && !state.isSaving,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WellPaidNavy,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = if (state.isCreatingPlan) {
                                stringResource(R.string.emergency_plan_creating)
                            } else {
                                stringResource(R.string.emergency_create_plan)
                            },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                if (!r.configured) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.emergency_not_configured_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }

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
                    WellPaidMoneyDigitKeypadField(
                        valueText = state.editingPlanMonthlyText,
                        onValueTextChange = { viewModel.setEditingPlanMonthlyText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isUpdatingPlan,
                        label = { Text(stringResource(R.string.emergency_new_plan_monthly_label)) },
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
