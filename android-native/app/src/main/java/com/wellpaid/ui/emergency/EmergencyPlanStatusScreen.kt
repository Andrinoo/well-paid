package com.wellpaid.ui.emergency

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.core.model.emergency.EmergencyReservePlanDto
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.formatIsoDateToBr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyPlanStatusScreen(
    planId: String,
    onNavigateBack: () -> Unit,
    viewModel: EmergencyReserveViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val plan: EmergencyReservePlanDto? = state.plans.firstOrNull { it.id == planId }
    var fetchTried by remember(planId) { mutableStateOf(false) }

    LaunchedEffect(planId, state.isLoading, state.plans) {
        if (state.isLoading) return@LaunchedEffect
        if (state.plans.any { it.id == planId }) return@LaunchedEffect
        if (fetchTried) return@LaunchedEffect
        fetchTried = true
        viewModel.refresh(rebindEditingPlanId = planId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                colors = wellPaidTopAppBarColors(),
                title = {
                    Text(
                        text = stringResource(R.string.emergency_plan_status_title),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (plan == null) {
                Text(
                    text = stringResource(R.string.emergency_plan_detail_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                return@Column
            }
            EmergencyPlanStatusSummaryBody(plan = plan)
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.emergency_plan_status_tracking_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatIsoDateToBr(plan.trackingStart),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = WellPaidNavy,
            )
            plan.targetCents?.takeIf { it > 0 }?.let { t ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.emergency_plan_status_ceiling_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatBrlFromCents(t),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = WellPaidNavy,
                )
            }
        }
    }
}

@Composable
private fun EmergencyPlanStatusSummaryBody(plan: EmergencyReservePlanDto) {
    val paceLabel = when (plan.paceStatus) {
        "below" -> stringResource(R.string.emergency_pace_below)
        "above" -> stringResource(R.string.emergency_pace_above)
        "on_track" -> stringResource(R.string.emergency_pace_on_track)
        else -> stringResource(R.string.emergency_pace_unknown)
    }
    val statusStr = when (plan.status) {
        "active" -> stringResource(R.string.emergency_plan_status_active)
        "completed" -> stringResource(R.string.emergency_plan_status_completed)
        else -> plan.status
    }
    val line1 = buildString {
        append(statusStr)
        append(" · ")
        append(formatBrlFromCents(plan.balanceCents))
        append(" · ")
        append(formatBrlFromCents(plan.monthlyTargetCents))
        append("/mo")
        plan.targetEndDate?.takeIf { it.isNotBlank() }?.let {
            append(" · → ")
            append(formatIsoDateToBr(it))
        }
        plan.monthsRemaining?.takeIf { it >= 0 }?.let { m ->
            append(" · ")
            append(stringResource(R.string.emergency_plan_summary_months_remaining, m))
        }
    }
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = line1,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.emergency_pace_delta_line,
                paceLabel,
                formatBrlFromCents(plan.paceDeltaCents),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = if (plan.paceStatus == "below") {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
