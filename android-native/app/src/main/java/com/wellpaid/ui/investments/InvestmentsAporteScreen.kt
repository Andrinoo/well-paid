package com.wellpaid.ui.investments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.LocalPrivacyHideBalance
import com.wellpaid.ui.theme.WellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.formatBrlFromCentsRespectPrivacy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.wellPaidScreenHorizontalPadding
import com.wellpaid.ui.theme.wellPaidTopAppBarColors
import androidx.compose.material3.CenterAlignedTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentsAporteScreen(
    positionId: String,
    onNavigateBack: () -> Unit,
    onAporteSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvestmentsViewModel = hiltViewModel(),
) {
    val hideBalance = LocalPrivacyHideBalance.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val position = state.positions.firstOrNull { it.id == positionId }
    val line = if (position != null) {
        val label = investmentInstrumentLabel(position.instrumentType)
        stringResource(
            R.string.investments_position_line,
            label,
            formatBrlFromCentsRespectPrivacy(position.principalCents, hideBalance),
            position.annualRateBps / 100f,
        )
    } else {
        "—"
    }

    LaunchedEffect(positionId) {
        viewModel.initAporteForPosition(positionId)
    }
    DisposableEffect(positionId) {
        onDispose { viewModel.clearAporteState() }
    }

    LaunchedEffect(state.isLoading, state.positions, position) {
        if (state.isLoading) return@LaunchedEffect
        if (state.positions.isNotEmpty() && position == null) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WellPaidCream,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.investments_aporte_title),
                        color = Color.White,
                    )
                },
                colors = wellPaidTopAppBarColors(),
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
    ) { inner ->
        if (position == null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(inner),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.isLoading) {
                    Spacer(Modifier.height(32.dp))
                    CircularProgressIndicator(color = WellPaidNavy)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .padding(inner)
                    .wellPaidScreenHorizontalPadding()
                    .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = position.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = WellPaidNavy,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.isLoadingAporteFundamentals) {
                    CircularProgressIndicator(
                        color = WellPaidNavy,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(8.dp)
                            .size(28.dp),
                    )
                } else {
                    state.aporteFundamentals?.let { f ->
                        AporteFundamentalsSummary(fundamentals = f)
                    }
                }
                state.aporteErrorMessage?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                WellPaidMoneyDigitKeypadField(
                    valueText = state.aporteAmountText,
                    onValueTextChange = viewModel::setAporteAmountText,
                    enabled = !state.isSubmittingAporte,
                    label = { Text(stringResource(R.string.investments_aporte_field_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { viewModel.submitAporte(positionId) { onAporteSuccess() } },
                    enabled = !state.isSubmittingAporte,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    if (state.isSubmittingAporte) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Text(stringResource(R.string.investments_aporte_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun AporteFundamentalsSummary(fundamentals: FundamentalPreviewUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AporteMinStat(
                stringResource(R.string.investments_metric_dy),
                fundamentals.dy,
                Modifier.weight(1f),
            )
            AporteMinStat(
                stringResource(R.string.investments_metric_pl),
                fundamentals.pl,
                Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AporteMinStat(
                stringResource(R.string.investments_metric_roe),
                fundamentals.roe,
                Modifier.weight(1f),
            )
            AporteMinStat(
                stringResource(R.string.investments_metric_ev_ebitda),
                fundamentals.evEbitda,
                Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AporteMinStat(
                stringResource(R.string.investments_metric_net_margin),
                fundamentals.netMargin,
                Modifier.weight(1f),
            )
            AporteMinStat(
                stringResource(R.string.investments_metric_net_debt_ebitda),
                fundamentals.netDebtEbitda,
                Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AporteMinStat(
                stringResource(R.string.investments_metric_eps),
                fundamentals.eps,
                Modifier.weight(0.6f),
            )
        }
    }
}

@Composable
private fun AporteMinStat(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = WellPaidNavy,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
