package com.wellpaid.ui.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidDatePickerField
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.LocalPrivacyHideBalance
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.formatBrlFromCentsRespectPrivacy

@Composable
private fun PlanFieldLabel(
    compact: Boolean,
    text: String,
) {
    Text(
        text = text,
        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        maxLines = if (compact) 1 else 2,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Campos de edição de um plano (partilhados pelo ecrã de detalhe). */
@Composable
fun EmergencyPlanEditFields(
    state: EmergencyReserveUiState,
    hideEmergencyTargetEnd: Boolean,
    enabled: Boolean,
    viewModel: EmergencyReserveViewModel,
    modifier: Modifier = Modifier,
    /** Rótulos mais curtos, campos mais baixos (ecrã de detalhe do plano). */
    compactLayout: Boolean = false,
) {
    val hideBalance = LocalPrivacyHideBalance.current
    val gap = if (compactLayout) 4.dp else 6.dp
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        OutlinedTextField(
            value = state.editingPlanTitleText,
            onValueChange = { viewModel.setEditingPlanTitleText(it) },
            label = {
                PlanFieldLabel(
                    compactLayout,
                    stringResource(R.string.emergency_new_plan_name_label),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            textStyle = if (compactLayout) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = state.editingPlanDetailsText,
            onValueChange = { viewModel.setEditingPlanDetailsText(it) },
            label = {
                PlanFieldLabel(
                    compactLayout,
                    stringResource(R.string.emergency_new_plan_details_label),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = if (compactLayout) 1 else 2,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
            textStyle = if (compactLayout) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WellPaidDatePickerField(
                label = {
                    PlanFieldLabel(
                        compactLayout,
                        stringResource(R.string.emergency_tracking_start_date_label),
                    )
                },
                isoDate = state.editingPlanTrackingStartText,
                onIsoDateChange = { viewModel.setEditingPlanTrackingStartText(it) },
                modifier = Modifier.weight(1.55f),
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                dense = true,
                extraCompact = compactLayout,
            )
            OutlinedTextField(
                value = state.editingPlanDurationMonthsText,
                onValueChange = { viewModel.setEditingPlanDurationMonthsText(it) },
                label = {
                    PlanFieldLabel(
                        compactLayout,
                        stringResource(R.string.emergency_duration_months_short),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(0.95f)
                    .then(if (compactLayout) Modifier.heightIn(max = 52.dp) else Modifier),
                singleLine = true,
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                textStyle = if (compactLayout) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            )
        }
        if (!hideEmergencyTargetEnd) {
            WellPaidDatePickerField(
                label = {
                    PlanFieldLabel(
                        compactLayout,
                        stringResource(R.string.emergency_target_end_date_label),
                    )
                },
                isoDate = state.editingPlanTargetEndText,
                onIsoDateChange = { viewModel.setEditingPlanTargetEndText(it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                dense = true,
                extraCompact = compactLayout,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WellPaidMoneyDigitKeypadField(
                valueText = state.editingPlanOpeningBalanceText,
                onValueTextChange = { viewModel.setEditingPlanOpeningBalanceText(it) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                label = {
                    PlanFieldLabel(
                        compactLayout,
                        stringResource(
                            if (compactLayout) {
                                R.string.emergency_plan_opening_label_compact
                            } else {
                                R.string.emergency_plan_opening_label
                            },
                        ),
                    )
                },
                placeholder = stringResource(R.string.emergency_monthly_placeholder),
                dense = true,
                extraCompact = compactLayout,
            )
            WellPaidMoneyDigitKeypadField(
                valueText = state.editingPlanMonthlyText,
                onValueTextChange = { viewModel.setEditingPlanMonthlyText(it) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                label = {
                    PlanFieldLabel(
                        compactLayout,
                        stringResource(
                            if (compactLayout) {
                                R.string.emergency_new_plan_monthly_label_compact
                            } else {
                                R.string.emergency_new_plan_monthly_label
                            },
                        ),
                    )
                },
                placeholder = stringResource(R.string.emergency_monthly_placeholder),
                dense = true,
                extraCompact = compactLayout,
            )
        }
        WellPaidMoneyDigitKeypadField(
            valueText = state.editingPlanTargetText,
            onValueTextChange = { viewModel.setEditingPlanTargetText(it) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = {
                PlanFieldLabel(
                    compactLayout,
                    stringResource(
                        if (compactLayout) {
                            R.string.emergency_new_plan_target_label_compact
                        } else {
                            R.string.emergency_new_plan_target_label
                        },
                    ),
                )
            },
            placeholder = stringResource(R.string.emergency_monthly_placeholder),
            dense = true,
            extraCompact = compactLayout,
        )
        state.editingPlanRecommendedMonthlyCents?.let { rec ->
            Text(
                text = stringResource(
                    R.string.emergency_monthly_suggestion,
                    formatBrlFromCentsRespectPrivacy(rec, hideBalance),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.editingPlanRetroOffer?.let { offer ->
            Spacer(Modifier.height(if (compactLayout) 2.dp else 4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(Modifier.padding(if (compactLayout) 8.dp else 12.dp)) {
                    Text(
                        text = stringResource(R.string.emergency_retroactive_plan_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            R.string.emergency_retroactive_plan_message,
                            offer.monthsPassed,
                            offer.monthsRemaining,
                            formatBrlFromCentsRespectPrivacy(offer.goalCentsForMessage, hideBalance),
                            formatBrlFromCentsRespectPrivacy(offer.adjustedMonthlyCents, hideBalance),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { viewModel.dismissEditingPlanRetroOffer() }) {
                            Text(stringResource(R.string.emergency_retroactive_dismiss))
                        }
                        TextButton(onClick = { viewModel.applyEditingPlanRetroCorrection() }) {
                            Text(stringResource(R.string.emergency_retroactive_apply))
                        }
                    }
                }
            }
        }
    }
}
