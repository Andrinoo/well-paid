package com.wellpaid.ui.emergency

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidDatePickerField
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatBrlFromCents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyReservePlanFormScreen(
    onNavigateBack: () -> Unit,
    onCreatedNeedRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmergencyReserveViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hideTargetEnd by viewModel.hideEmergencyPlanTargetEnd.collectAsStateWithLifecycle()
    var moreOptionsExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.resetNewPlanFormFields()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WellPaidCream,
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.emergency_reserve_form_title),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

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
            OutlinedButton(
                onClick = { moreOptionsExpanded = !moreOptionsExpanded },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.emergency_new_plan_more_options),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                    )
                    Icon(
                        imageVector = if (moreOptionsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = WellPaidNavy,
                    )
                }
            }
            AnimatedVisibility(visible = moreOptionsExpanded) {
                Column(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.newPlanDetailsText,
                        onValueChange = { viewModel.setNewPlanDetailsText(it) },
                        label = { Text(stringResource(R.string.emergency_new_plan_details_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        enabled = !state.isCreatingPlan && !state.isSaving,
                        shape = RoundedCornerShape(16.dp),
                    )
                    if (!hideTargetEnd) {
                        Spacer(Modifier.height(8.dp))
                        WellPaidDatePickerField(
                            label = { Text(stringResource(R.string.emergency_target_end_date_label)) },
                            isoDate = state.newPlanTargetEndText,
                            onIsoDateChange = { viewModel.setNewPlanTargetEndText(it) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isCreatingPlan && !state.isSaving,
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            WellPaidDatePickerField(
                label = { Text(stringResource(R.string.emergency_tracking_start_date_label)) },
                isoDate = state.newPlanTrackingStartText,
                onIsoDateChange = { viewModel.setNewPlanTrackingStartText(it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isCreatingPlan && !state.isSaving,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.newPlanDurationMonthsText,
                onValueChange = { viewModel.setNewPlanDurationMonthsText(it) },
                label = { Text(stringResource(R.string.emergency_new_plan_duration_label)) },
                supportingText = { Text(stringResource(R.string.emergency_new_plan_duration_footnote)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !state.isCreatingPlan && !state.isSaving,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(8.dp))
            WellPaidMoneyDigitKeypadField(
                valueText = state.newPlanOpeningBalanceText,
                onValueTextChange = { viewModel.setNewPlanOpeningBalanceText(it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isCreatingPlan && !state.isSaving,
                label = { Text(stringResource(R.string.emergency_plan_opening_label)) },
                placeholder = stringResource(R.string.emergency_monthly_placeholder),
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
            Spacer(Modifier.height(8.dp))
            WellPaidMoneyDigitKeypadField(
                valueText = state.newPlanTargetText,
                onValueTextChange = { viewModel.setNewPlanTargetText(it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isCreatingPlan && !state.isSaving,
                label = { Text(stringResource(R.string.emergency_new_plan_target_label)) },
                placeholder = stringResource(R.string.emergency_monthly_placeholder),
            )
            state.newPlanRecommendedMonthlyCents?.let { rec ->
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.emergency_monthly_suggestion,
                        formatBrlFromCents(rec),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.newPlanRetroOffer?.let { offer ->
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
                            TextButton(onClick = { viewModel.dismissNewPlanRetroOffer() }) {
                                Text(stringResource(R.string.emergency_retroactive_dismiss))
                            }
                            TextButton(onClick = { viewModel.applyNewPlanRetroCorrection() }) {
                                Text(stringResource(R.string.emergency_retroactive_apply))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.createNamedPlan(onSuccess = onCreatedNeedRefresh) },
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
    }
}
