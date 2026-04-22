package com.wellpaid.ui.investments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wellpaid.R
import com.wellpaid.ui.components.WellPaidMoneyDigitKeypadField
import com.wellpaid.ui.theme.WellPaidCream
import com.wellpaid.ui.theme.WellPaidNavy

@Composable
fun InvestmentsFixedIncomeJoinScreen(
    state: InvestmentsUiState,
    onDescriptionChange: (String) -> Unit,
    onPrincipalChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
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
            text = stringResource(R.string.investments_fixed_income_join_title),
            style = MaterialTheme.typography.titleMedium,
            color = WellPaidNavy,
            fontWeight = FontWeight.SemiBold,
        )
        Text("${state.fixedIncomeType.uppercase()} · ${state.newPositionName}", color = WellPaidNavy)
        OutlinedTextField(
            value = state.fixedIncomeDescription,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.investments_field_description)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        WellPaidMoneyDigitKeypadField(
            valueText = state.newPositionPrincipalText,
            onValueTextChange = onPrincipalChange,
            enabled = !state.isSavingPosition,
            label = { Text(stringResource(R.string.investments_field_principal)) },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.newPositionAnnualRateText,
            onValueChange = onRateChange,
            label = { Text(stringResource(R.string.investments_field_rate)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.investments_save_position))
        }
        TextButton(onClick = onBack) { Text(stringResource(R.string.common_close)) }
    }
}

