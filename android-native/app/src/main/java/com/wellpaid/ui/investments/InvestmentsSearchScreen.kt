package com.wellpaid.ui.investments

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wellpaid.R
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidNavy
import java.util.Locale

@Composable
fun InvestmentsSearchScreen(
    query: String,
    suggestions: List<TickerSuggestionUi>,
    isSearching: Boolean,
    isLoadingTopMovers: Boolean,
    topHour: List<TopMoverUi>,
    topDay: List<TopMoverUi>,
    topWeek: List<TopMoverUi>,
    onQueryChange: (String) -> Unit,
    onSelectTicker: (String) -> Unit,
    onBack: () -> Unit,
) {
    val q = query.trim().lowercase(Locale.ROOT)
    val showHour = q.contains("hora")
    val showDay = q.contains("dia")
    val showWeek = q.contains("semana")
    val explicitWindow = showHour || showDay || showWeek

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WellPaidCream)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.investments_search_results_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = WellPaidNavy,
            )
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.common_close))
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp)),
            singleLine = true,
            label = { Text(stringResource(R.string.investments_global_search_label)) },
        )
        Spacer(Modifier.height(8.dp))

        if (isSearching) {
            CircularProgressIndicator()
        } else {
            suggestions.forEach { item ->
                Button(
                    onClick = { onSelectTicker(item.symbol) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("${item.symbol} · ${item.name}")
                        Text(
                            text = "${item.source.uppercase(Locale.ROOT)} · ${instrumentLabelForKeyLocal(item.instrumentType)}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        if (isLoadingTopMovers) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.investments_loading_button),
                style = MaterialTheme.typography.bodySmall,
            )
            return
        }

        if (!explicitWindow || showHour) {
            TopMoversSection(
                title = stringResource(R.string.investments_top_movers_hour),
                items = topHour,
                onSelectTicker = onSelectTicker,
            )
        }
        if (!explicitWindow || showDay) {
            TopMoversSection(
                title = stringResource(R.string.investments_top_movers_day),
                items = topDay,
                onSelectTicker = onSelectTicker,
            )
        }
        if (!explicitWindow || showWeek) {
            TopMoversSection(
                title = stringResource(R.string.investments_top_movers_week),
                items = topWeek,
                onSelectTicker = onSelectTicker,
            )
        }
    }
}

@Composable
private fun instrumentLabelForKeyLocal(key: String): String {
    return when (key.lowercase(Locale.ROOT)) {
        "cdi" -> stringResource(R.string.investments_bucket_cdi)
        "cdb" -> stringResource(R.string.investments_bucket_cdb)
        "fixed_income" -> stringResource(R.string.investments_bucket_fixed_income)
        "tesouro" -> stringResource(R.string.investments_bucket_tesouro)
        "stocks" -> stringResource(R.string.investments_bucket_stocks)
        else -> key.uppercase(Locale.ROOT)
    }
}

@Composable
private fun TopMoversSection(
    title: String,
    items: List<TopMoverUi>,
    onSelectTicker: (String) -> Unit,
) {
    Spacer(Modifier.height(10.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = WellPaidNavy,
    )
    Spacer(Modifier.height(6.dp))
    items.forEach { item ->
        TextButton(
            onClick = { onSelectTicker(item.symbol) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${item.symbol} · ${item.name}", color = WellPaidNavy)
                Text(
                    text = String.format(Locale.US, "%.2f%%", item.changePercent),
                    color = if (item.changePercent >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

