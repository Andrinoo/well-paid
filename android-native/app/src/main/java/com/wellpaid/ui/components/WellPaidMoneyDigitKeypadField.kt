package com.wellpaid.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.focus.onFocusChanged
import com.wellpaid.R
import com.wellpaid.util.BRL_CENT_DIGIT_CHAIN_MAX
import com.wellpaid.util.brlDigitChainToCents
import com.wellpaid.util.centsToBrlInput
import com.wellpaid.util.parseBrlToCents
import com.wellpaid.ui.theme.WellPaidCreamMuted
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy

private fun digitsFromValueText(valueText: String): String {
    val cents = parseBrlToCents(valueText) ?: return ""
    if (cents <= 0) return ""
    return cents.toString()
}

private fun formattedValueFromDigits(digits: String): String {
    if (digits.isEmpty()) return ""
    val centsLong = brlDigitChainToCents(digits)
    if (centsLong <= 0L) return ""
    val centsInt =
        centsLong.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    return centsToBrlInput(centsInt.toLong())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellPaidMoneyDigitKeypadField(
    valueText: String,
    onValueTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    label: @Composable (() -> Unit)? = null,
    placeholder: String = "0,00",
    prefix: @Composable (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    maxDigits: Int = BRL_CENT_DIGIT_CHAIN_MAX,
    onDone: () -> Unit = {},
    onKeypadOpenChange: (Boolean) -> Unit = {},
) {
    var keypadOpen by remember { mutableStateOf(false) }
    var digits by remember(valueText) { mutableStateOf(digitsFromValueText(valueText)) }
    val haptic = LocalHapticFeedback.current
    val keyShape = RoundedCornerShape(12.dp)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun setDigits(newDigits: String) {
        digits = newDigits
        val newFormatted = formattedValueFromDigits(newDigits)
        onValueTextChange(newFormatted)
    }

    LaunchedEffect(valueText) {
        // Se o valor externo mudou (ex.: carregou do backend), sincroniza quando o keypad estiver fechado.
        if (!keypadOpen) {
            setDigits(digitsFromValueText(valueText))
        }
    }

    LaunchedEffect(keypadOpen) {
        onKeypadOpenChange(keypadOpen)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(keypadOpen) {
        // Garante que a sheet realmente aparece (não só renderiza) ao abrir o keypad.
        if (keypadOpen) {
            sheetState.show()
            keyboardController?.hide()
        } else {
            sheetState.hide()
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = formattedValueFromDigits(digits),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = label,
            placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            prefix = prefix,
            shape = shape,
            colors = colors,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { st ->
                    if (st.isFocused && enabled && !keypadOpen) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        keypadOpen = true
                    }
                },
        )

        if (keypadOpen) {
            ModalBottomSheet(
                onDismissRequest = {
                    keypadOpen = false
                    focusManager.clearFocus(force = true)
                    onDone()
                },
                sheetState = sheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.money_keypad_sheet_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        color = WellPaidNavy,
                    )
                    Spacer(Modifier.size(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        @Composable
                        fun KeyButton(
                            text: String,
                            background: Color = WellPaidCreamMuted,
                            modifier: Modifier,
                            onClick: () -> Unit,
                        ) {
                            Button(
                                onClick = onClick,
                                modifier = modifier,
                                shape = keyShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = background,
                                    contentColor = WellPaidNavy,
                                ),
                                enabled = enabled,
                            ) {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = WellPaidNavy,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            KeyButton(
                                text = "1",
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { setDigits(digits + "1") }
                            KeyButton(
                                text = "2",
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { setDigits(digits + "2") }
                            KeyButton(
                                text = "3",
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { setDigits(digits + "3") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            KeyButton(
                                text = "4",
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { setDigits(digits + "4") }
                            KeyButton(
                                text = "5",
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { setDigits(digits + "5") }
                            KeyButton(
                                text = "6",
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { setDigits(digits + "6") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            KeyButton(
                                text = "7",
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { setDigits(digits + "7") }
                            KeyButton(
                                text = "8",
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { setDigits(digits + "8") }
                            KeyButton(
                                text = "9",
                                modifier = Modifier.weight(1f).height(52.dp),
                            ) { setDigits(digits + "9") }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (!enabled) return@Button
                                    if (digits.isNotEmpty()) {
                                        setDigits(digits.dropLast(1))
                                    }
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = keyShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WellPaidCreamMuted,
                                    contentColor = WellPaidNavy,
                                ),
                                enabled = enabled,
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Filled.Backspace,
                                    contentDescription = "Apagar",
                                    tint = WellPaidNavy,
                                )
                            }

                            Button(
                                onClick = {
                                    if (!enabled) return@Button
                                    if (digits.length >= maxDigits) return@Button
                                    setDigits(digits + "0")
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = keyShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WellPaidGold,
                                    contentColor = WellPaidNavy,
                                ),
                                enabled = enabled,
                            ) {
                                Text(
                                    text = "0",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = WellPaidNavy,
                                )
                            }

                            Button(
                                onClick = {
                                    keypadOpen = false
                                    focusManager.clearFocus(force = true)
                                    onDone()
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = keyShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WellPaidGold,
                                    contentColor = WellPaidNavy,
                                ),
                                enabled = enabled,
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "OK",
                                        tint = WellPaidNavy,
                                    )
                                    Spacer(Modifier.padding(start = 6.dp))
                                    Text(
                                        text = "OK",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = WellPaidNavy,
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                setDigits("")
                            },
                            enabled = enabled,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = "Limpar", color = WellPaidNavy)
                        }
                    }
                }
            }
        }
    }
}

