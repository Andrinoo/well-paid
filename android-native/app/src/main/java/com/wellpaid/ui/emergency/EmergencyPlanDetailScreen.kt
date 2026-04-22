package com.wellpaid.ui.emergency

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.emergency.EmergencyReservePlanDto
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyPlanDetailScreen(
    planId: String,
    onNavigateBack: () -> Unit,
    onPlanDeletedNavigateBack: () -> Unit,
    onOpenPlanStatus: () -> Unit,
    onOpenMonthlyProgress: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmergencyReserveViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hideTargetEnd by viewModel.hideEmergencyPlanTargetEnd.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelEditingPlan()
        }
    }

    LaunchedEffect(planId) {
        viewModel.prepareEmergencyPlanDetail(planId)
    }

    val plan: EmergencyReservePlanDto? = state.plans.firstOrNull { it.id == planId }
    val title = plan?.title?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.emergency_plan_untitled)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.cancelEditingPlan()
                            onNavigateBack()
                        },
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    if (plan != null && plan.status == "active") {
                        IconButton(
                            onClick = { viewModel.requestDeletePlan(planId) },
                            enabled = !state.isUpdatingPlan,
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.emergency_plan_delete),
                                tint = Color.White,
                            )
                        }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            state.errorMessage?.let { msg ->
                if (state.editingPlanId == planId || plan == null) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }

            plan?.let {
                OutlinedButton(
                    onClick = onOpenPlanStatus,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WellPaidGold),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WellPaidNavy),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.emergency_plan_open_status),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Text(
                text = stringResource(R.string.emergency_plan_detail_contribution_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
            )
            Spacer(Modifier.height(6.dp))
            WellPaidMoneyDigitKeypadField(
                valueText = state.selectedPlanContributionText,
                onValueTextChange = { viewModel.setSelectedPlanContributionText(it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.selectedPlanId == planId,
                label = { Text(stringResource(R.string.emergency_selected_plan_contribution_label)) },
                placeholder = stringResource(R.string.emergency_monthly_placeholder),
                dense = true,
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { viewModel.saveSelectedPlanContribution() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving && state.selectedPlanId == planId,
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
                        stringResource(R.string.emergency_register_selected_contribution)
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.emergency_plan_detail_edit_section),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
            )
            Spacer(Modifier.height(6.dp))
            EmergencyPlanEditFields(
                state = state,
                hideEmergencyTargetEnd = hideTargetEnd,
                enabled = !state.isUpdatingPlan,
                viewModel = viewModel,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.saveEditingPlan() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isUpdatingPlan && state.editingPlanId == planId,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WellPaidGold,
                    contentColor = WellPaidNavy,
                ),
            ) {
                Text(
                    text = if (state.isUpdatingPlan) {
                        stringResource(R.string.emergency_saving)
                    } else {
                        stringResource(R.string.emergency_plan_save)
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenMonthlyProgress,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, WellPaidGold),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WellPaidNavy),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.emergency_plan_detail_breakdown_section),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (state.showDeletePlanConfirm && state.deletingPlanId == planId) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeletePlan() },
            title = { Text(stringResource(R.string.emergency_plan_delete_title)) },
            text = { Text(stringResource(R.string.emergency_plan_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlanConfirmed {
                            onPlanDeletedNavigateBack()
                        }
                    },
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
