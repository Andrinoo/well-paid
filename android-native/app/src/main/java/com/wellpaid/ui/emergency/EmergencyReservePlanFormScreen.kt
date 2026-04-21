package com.wellpaid.ui.emergency

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyReservePlanFormScreen(
    onNavigateBack: () -> Unit,
    onCreatedNeedRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmergencyReserveViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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

            Text(
                text = stringResource(R.string.emergency_new_plan_section),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

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
            WellPaidMoneyDigitKeypadField(
                valueText = state.newPlanTargetText,
                onValueTextChange = { viewModel.setNewPlanTargetText(it) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isCreatingPlan && !state.isSaving,
                label = { Text(stringResource(R.string.emergency_new_plan_target_label)) },
                placeholder = stringResource(R.string.emergency_monthly_placeholder),
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
