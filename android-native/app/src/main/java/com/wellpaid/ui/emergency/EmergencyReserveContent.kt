package com.wellpaid.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.wellpaid.core.model.emergency.EmergencyReserveDto
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyReserveContent(
    mainRouteEntry: NavBackStackEntry,
    onOpenReserveNew: () -> Unit,
    onOpenPlanDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    tabSwipe: Modifier = Modifier,
    viewModel: EmergencyReserveViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dirtyFlow = remember(mainRouteEntry) {
        mainRouteEntry.savedStateHandle.getStateFlow("emergency_reserve_dirty", 0L)
    }
    val emergencyDirty by dirtyFlow.collectAsStateWithLifecycle()
    var showPolicyTipsDialog by remember { mutableStateOf(false) }
    var globalMetaExpanded by remember { mutableStateOf(false) }
    var accrualsExpanded by remember { mutableStateOf(false) }
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(R.string.emergency_tips_policy_tooltip))
                        }
                    },
                    state = rememberTooltipState(),
                ) {
                    IconButton(onClick = { showPolicyTipsDialog = true }) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = stringResource(R.string.emergency_info_icon_a11y),
                        )
                    }
                }
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
                EmergencyReserveCompactHero(
                    r = r,
                    progress = progress,
                    pctAnnual = pctAnnual,
                    planCount = planCount,
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
                        EmergencyReservePlanCompactCard(
                            plan = plan,
                            enabled = !state.isUpdatingPlan && !state.isSaving,
                            onOpen = { onOpenPlanDetail(plan.id) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = { globalMetaExpanded = !globalMetaExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.emergency_hub_global_meta_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                    )
                }
                if (globalMetaExpanded) {
                    Text(
                        text = stringResource(R.string.emergency_hub_global_meta_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    WellPaidMoneyDigitKeypadField(
                        valueText = state.monthlyTargetText,
                        onValueTextChange = { viewModel.setMonthlyTargetText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                        label = { Text(stringResource(R.string.emergency_new_plan_monthly_label)) },
                        placeholder = stringResource(R.string.emergency_monthly_placeholder),
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
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.saveMonthlyTarget() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WellPaidNavy,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.emergency_hub_save_global_meta),
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

                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { accrualsExpanded = !accrualsExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.emergency_accruals_collapsed_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                    )
                }
                if (accrualsExpanded) {
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
            if (r == null && !state.isLoading) {
                Spacer(Modifier.height(8.dp))
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
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.emergency_not_configured_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showPolicyTipsDialog) {
        val res = state.reserve
        EmergencyTipsPolicyDialog(
            onDismiss = { showPolicyTipsDialog = false },
            balanceCents = res?.balanceCents ?: 0,
            monthlyTargetCents = (res?.monthlyTargetCents ?: 0).coerceAtLeast(1),
            showRecomposition = state.plans.size > 1,
        )
    }
}

@Composable
private fun EmergencyReserveCompactHero(
    r: EmergencyReserveDto,
    progress: Float,
    pctAnnual: Int,
    planCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WellPaidNavyDeep)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.NightsStay,
                contentDescription = null,
                tint = WellPaidGold,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.emergency_hero_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(4.dp))
        DiscreetBalanceValue(
            cents = r.balanceCents,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = WellPaidGold,
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.emergency_hero_subline,
                formatBrlFromCents(r.monthlyTargetCents),
                stringResource(R.string.emergency_hero_plans_count_short, planCount),
                pctAnnual,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 2,
        )
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = WellPaidGold,
            trackColor = Color.White.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun EmergencyReservePlanCompactCard(
    plan: EmergencyReservePlanDto,
    enabled: Boolean,
    onOpen: () -> Unit,
) {
    val statusLabel = when (plan.status) {
        "active" -> stringResource(R.string.emergency_plan_status_active)
        "completed" -> stringResource(R.string.emergency_plan_status_completed)
        else -> plan.status
    }
    val targetCents = plan.targetCents
    val estimateLine = if (targetCents != null && targetCents > 0) {
        estimateMonthsToReachTarget(
            currentCents = plan.balanceCents,
            monthlyCents = plan.monthlyTargetCents,
            targetCents = targetCents,
        )?.let { months ->
            stringResource(R.string.emergency_plan_estimate_line, months)
        }
    } else {
        null
    }
    val line4 = if (targetCents != null && targetCents > 0) {
        stringResource(R.string.emergency_plan_target_line, formatBrlFromCents(targetCents)) +
            (estimateLine?.let { " · $it" } ?: "")
    } else {
        stringResource(R.string.emergency_plan_no_ceiling)
    }
    val paceLabel = when (plan.paceStatus) {
        "below" -> stringResource(R.string.emergency_pace_below)
        "above" -> stringResource(R.string.emergency_pace_above)
        "on_track" -> stringResource(R.string.emergency_pace_on_track)
        else -> stringResource(R.string.emergency_pace_unknown)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable(
                enabled = enabled,
                onClick = onOpen,
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = plan.title.ifBlank { stringResource(R.string.emergency_plan_untitled) },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(
                R.string.emergency_plan_one_line,
                statusLabel,
                formatBrlFromCents(plan.balanceCents),
                formatBrlFromCents(plan.monthlyTargetCents),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.emergency_plan_opening_line, formatBrlFromCents(plan.openingBalanceCents)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = line4,
            style = MaterialTheme.typography.labelSmall,
            color = WellPaidNavy,
            maxLines = 2,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(
                R.string.emergency_pace_delta_line,
                paceLabel,
                formatBrlFromCents(plan.paceDeltaCents),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = if (plan.paceStatus == "below") MaterialTheme.colorScheme.error else WellPaidNavy,
            maxLines = 1,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.emergency_plan_open_detail),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = WellPaidNavy,
        )
    }
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
private fun EmergencyTipsPolicyDialog(
    onDismiss: () -> Unit,
    balanceCents: Int,
    monthlyTargetCents: Int,
    showRecomposition: Boolean,
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
    val scroll = rememberScrollState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.emergency_tips_policy_dialog_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(scroll),
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
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.emergency_guidance_tips_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = WellPaidNavy,
                )
                Spacer(Modifier.height(6.dp))
                EmergencyGuidanceTipBullet(
                    stringResource(R.string.emergency_tip_named_plans),
                )
                Spacer(Modifier.height(4.dp))
                EmergencyGuidanceTipBullet(
                    stringResource(R.string.emergency_tip_rebalance),
                )
                Spacer(Modifier.height(4.dp))
                EmergencyGuidanceTipBullet(
                    stringResource(R.string.emergency_tip_liquidity),
                )
                Spacer(Modifier.height(4.dp))
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_ok))
            }
        },
    )
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
