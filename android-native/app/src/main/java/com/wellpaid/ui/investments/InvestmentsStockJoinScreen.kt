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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidNavy

@Composable
fun InvestmentsStockJoinScreen(
    state: InvestmentsUiState,
    onDescriptionChange: (String) -> Unit,
    onModeByValueChange: (Boolean) -> Unit,
    onQuantityChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onAveragePriceChange: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WellPaidCream)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.investments_stock_join_title),
            style = MaterialTheme.typography.titleMedium,
            color = WellPaidNavy,
            fontWeight = FontWeight.SemiBold,
        )
        Text(text = state.newPositionName, style = MaterialTheme.typography.titleSmall, color = WellPaidNavy)
        OutlinedTextField(
            value = state.stockJoinDescription,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.investments_field_description)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        WellPaidMoneyDigitKeypadField(
            valueText = state.averagePriceText,
            onValueTextChange = onAveragePriceChange,
            enabled = !state.isSavingPosition,
            label = { Text(stringResource(R.string.investments_field_average_price)) },
            modifier = Modifier.fillMaxWidth(),
        )
        state.quoteInfoMessage?.let { quote ->
            Text(
                text = quote,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1B5E20),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.investments_mode_by_value))
            Switch(checked = state.stockJoinModeByValue, onCheckedChange = onModeByValueChange)
        }
        if (state.stockJoinModeByValue) {
            WellPaidMoneyDigitKeypadField(
                valueText = state.newPositionPrincipalText,
                onValueTextChange = onValueChange,
                enabled = !state.isSavingPosition,
                label = { Text(stringResource(R.string.investments_field_principal)) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            OutlinedTextField(
                value = state.quantityText,
                onValueChange = onQuantityChange,
                label = { Text(stringResource(R.string.investments_field_quantity)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        state.stockJoinAdjustedAlert?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        }
        state.selectedFundamentals?.let { f ->
            Text(
                text = "DY ${f.dy ?: "—"} · P/L ${f.pl ?: "—"} · P/VP ${f.pvp ?: "—"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
            Text(stringResource(R.string.investments_save_position))
        }
        TextButton(onClick = onBack) { Text(stringResource(R.string.common_close)) }
    }
}

