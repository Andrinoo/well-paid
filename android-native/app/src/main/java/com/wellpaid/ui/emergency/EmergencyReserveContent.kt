package com.wellpaid.ui.emergency

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.wellpaid.ui.components.WellPaidPullToRefreshBox
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.WellPaidNavyDeep
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.DiscreetBalanceValue
import com.wellpaid.ui.theme.LocalPrivacyHideBalance
import com.wellpaid.ui.theme.formatBrlFromCentsRespectPrivacy
import com.wellpaid.util.formatBrlFromCents
import com.wellpaid.util.centsToBrlInput
import com.wellpaid.util.formatIsoDateToBr
import kotlin.math.roundToInt

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
    val tipsOpenFlow = remember(mainRouteEntry) {
        mainRouteEntry.savedStateHandle.getStateFlow("emergency_open_policy_tips", 0L)
    }
    val tipsOpenSignal by tipsOpenFlow.collectAsStateWithLifecycle()
    var showPolicyTipsDialog by remember { mutableStateOf(false) }
    var globalMetaExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(emergencyDirty) {
        if (emergencyDirty != 0L) {
            viewModel.refresh()
        }
    }
    LaunchedEffect(tipsOpenSignal) {
        if (tipsOpenSignal != 0L) {
            showPolicyTipsDialog = true
        }
    }
    val pullRefreshing = state.isLoading && state.reserve != null

    WellPaidPullToRefreshBox(
        refreshing = pullRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = modifier
            .fillMaxSize()
            .fillMaxWidth()
            .wellPaidMaxContentWidth(WellPaidMaxContentWidth),
    ) {
        if (state.isLoading && state.reserve == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@WellPaidPullToRefreshBox
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
                val activePlans = state.plans.count { it.status.equals("active", ignoreCase = true) }
                val belowPaceCount = state.plans.count { it.paceStatus.equals("below", ignoreCase = true) }
                val monthlyCommitmentCents = state.plans.sumOf { it.monthlyTargetCents }
                val nearestEndDate = state.plans
                    .mapNotNull { it.targetEndDate }
                    .minOrNull()

                Button(
                    onClick = onOpenReserveNew,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isCreatingPlan && !state.isSaving && !state.isUpdatingPlan,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidGold,
                        contentColor = WellPaidNavy,
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
                Spacer(Modifier.height(10.dp))
                EmergencyReserveInsightsGrid(
                    activePlans = activePlans,
                    belowPaceCount = belowPaceCount,
                    monthlyCommitmentCents = monthlyCommitmentCents,
                    nearestEndDate = nearestEndDate,
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
                OutlinedButton(
                    onClick = { globalMetaExpanded = !globalMetaExpanded },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WellPaidGold),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WellPaidNavy,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.emergency_hub_global_meta_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Icon(
                            imageVector = if (globalMetaExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                        )
                    }
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
                            containerColor = WellPaidGold,
                            contentColor = WellPaidNavy,
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.emergency_hub_save_global_meta),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.emergency_tracking_start,
                            formatIsoDateToBr(r.trackingStart),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            }
            if (r == null && !state.isLoading) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onOpenReserveNew,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isCreatingPlan && !state.isSaving && !state.isUpdatingPlan,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WellPaidGold,
                        contentColor = WellPaidNavy,
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
    val hideBalance = LocalPrivacyHideBalance.current
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
                formatBrlFromCentsRespectPrivacy(r.monthlyTargetCents, hideBalance),
                stringResource(R.string.emergency_hero_plans_count_short, planCount),
                pctAnnual,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 1,
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
private fun EmergencyReserveInsightsGrid(
    activePlans: Int,
    belowPaceCount: Int,
    monthlyCommitmentCents: Int,
    nearestEndDate: String?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EmergencyInsightCard(
                title = stringResource(R.string.emergency_insight_active_plans),
                value = activePlans.toString(),
                modifier = Modifier.weight(1f),
            )
            EmergencyInsightCard(
                title = stringResource(R.string.emergency_insight_monthly_commitment),
                value = formatBrlFromCents(monthlyCommitmentCents),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EmergencyInsightCard(
                title = stringResource(R.string.emergency_insight_pace_attention),
                value = belowPaceCount.toString(),
                modifier = Modifier.weight(1f),
            )
            EmergencyInsightCard(
                title = stringResource(R.string.emergency_insight_nearest_target),
                value = nearestEndDate?.let { formatIsoDateToBr(it) }
                    ?: stringResource(R.string.emergency_insight_not_defined),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EmergencyInsightCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmergencyReservePlanCompactCard(
    plan: EmergencyReservePlanDto,
    enabled: Boolean,
    onOpen: () -> Unit,
) {
    val hideBalance = LocalPrivacyHideBalance.current
    val targetCents = plan.targetCents
    val compactLine = if (targetCents != null && targetCents > 0) {
        val pct = ((plan.balanceCents * 100L) / targetCents).toInt().coerceIn(0, 999)
        stringResource(
            R.string.emergency_plan_card_compact_vs_target,
            formatBrlFromCentsRespectPrivacy(plan.balanceCents, hideBalance),
            pct,
        )
    } else {
        formatBrlFromCentsRespectPrivacy(plan.balanceCents, hideBalance)
    }
    val showBelowPace = plan.paceStatus == "below"
    val timelineLine = buildString {
        plan.targetEndDate?.takeIf { it.isNotBlank() }?.let {
            append("→ ")
            append(formatIsoDateToBr(it))
        }
        plan.monthsRemaining?.takeIf { it >= 0 }?.let { m ->
            if (isNotEmpty()) append(" · ")
            append(stringResource(R.string.emergency_plan_summary_months_remaining, m))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onOpen,
            ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(WellPaidGold.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = null,
                    tint = WellPaidNavy,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = plan.title.ifBlank { stringResource(R.string.emergency_plan_untitled) },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = WellPaidNavy,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = compactLine,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WellPaidNavy,
                    maxLines = 1,
                )
                if (timelineLine.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = timelineLine.replace("→ ", ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                if (showBelowPace) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = stringResource(R.string.emergency_pace_below),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
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
