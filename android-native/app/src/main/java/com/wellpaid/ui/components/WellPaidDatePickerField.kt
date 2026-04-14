package com.wellpaid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wellpaid.R
import com.wellpaid.util.formatIsoToUiDate
import com.wellpaid.util.localDateToIso
import com.wellpaid.util.localDateToUtcStartMillis
import com.wellpaid.util.millisToLocalDate
import com.wellpaid.util.parseIsoDateLocal
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellPaidDatePickerField(
    label: @Composable () -> Unit,
    isoDate: String,
    onIsoDateChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
) {
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    var open by remember { mutableStateOf(false) }
    val display = if (isoDate.isNotBlank()) formatIsoToUiDate(isoDate, locale) else ""
    val initialMillis = remember(isoDate) {
        parseIsoDateLocal(isoDate)?.let { localDateToUtcStartMillis(it) }
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    OutlinedTextField(
        value = display,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = label,
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { open = true },
        trailingIcon = {
            IconButton(
                onClick = { open = true },
                enabled = enabled,
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
            }
        },
        shape = shape,
        colors = colors,
    )

    if (open) {
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { ms ->
                            val d = millisToLocalDate(ms)
                            onIsoDateChange(localDateToIso(d))
                        }
                        open = false
                    },
                ) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
