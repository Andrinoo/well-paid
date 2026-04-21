package com.wellpaid.ui.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidDatePickerField
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.util.formatBrlFromCents

/** Campos de edição de um plano (partilhados pelo ecrã de detalhe). */
@Composable
fun EmergencyPlanEditFields(
    state: EmergencyReserveUiState,
    hideEmergencyTargetEnd: Boolean,
    enabled: Boolean,
    viewModel: EmergencyReserveViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = state.editingPlanTitleText,
            onValueChange = { viewModel.setEditingPlanTitleText(it) },
            label = { Text(stringResource(R.string.emergency_new_plan_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
            value = state.editingPlanDetailsText,
            onValueChange = { viewModel.setEditingPlanDetailsText(it) },
            label = { Text(stringResource(R.string.emergency_new_plan_details_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
        )
        WellPaidDatePickerField(
            label = { Text(stringResource(R.string.emergency_tracking_start_date_label)) },
            isoDate = state.editingPlanTrackingStartText,
            onIsoDateChange = { viewModel.setEditingPlanTrackingStartText(it) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
        )
        OutlinedTextField(
            value = state.editingPlanDurationMonthsText,
            onValueChange = { viewModel.setEditingPlanDurationMonthsText(it) },
            label = { Text(stringResource(R.string.emergency_new_plan_duration_label)) },
            supportingText = { Text(stringResource(R.string.emergency_new_plan_duration_footnote)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = enabled,
            shape = RoundedCornerShape(14.dp),
        )
        Text(
            text = stringResource(R.string.emergency_plan_duration_target_sync_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!hideEmergencyTargetEnd) {
            WellPaidDatePickerField(
                label = { Text(stringResource(R.string.emergency_target_end_date_label)) },
                isoDate = state.editingPlanTargetEndText,
                onIsoDateChange = { viewModel.setEditingPlanTargetEndText(it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
            )
        }
        WellPaidMoneyDigitKeypadField(
            valueText = state.editingPlanOpeningBalanceText,
            onValueTextChange = { viewModel.setEditingPlanOpeningBalanceText(it) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(stringResource(R.string.emergency_plan_opening_label)) },
            placeholder = stringResource(R.string.emergency_monthly_placeholder),
        )
        Text(
            text = stringResource(R.string.emergency_plan_opening_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WellPaidMoneyDigitKeypadField(
            valueText = state.editingPlanMonthlyText,
            onValueTextChange = { viewModel.setEditingPlanMonthlyText(it) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(stringResource(R.string.emergency_new_plan_monthly_label)) },
            placeholder = stringResource(R.string.emergency_monthly_placeholder),
        )
        WellPaidMoneyDigitKeypadField(
            valueText = state.editingPlanTargetText,
            onValueTextChange = { viewModel.setEditingPlanTargetText(it) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            label = { Text(stringResource(R.string.emergency_new_plan_target_label)) },
            placeholder = stringResource(R.string.emergency_monthly_placeholder),
        )
        state.editingPlanRecommendedMonthlyCents?.let { rec ->
            Text(
                text = stringResource(
                    R.string.emergency_monthly_suggestion,
                    formatBrlFromCents(rec),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.editingPlanRetroOffer?.let { offer ->
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ) {
                Column(Modifier.padding(12.dp)) {
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
                            formatBrlFromCents(offer.goalCentsForMessage),
                            formatBrlFromCents(offer.adjustedMonthlyCents),
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
