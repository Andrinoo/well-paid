package com.wellpaid.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.wellpaid.R
import com.wellpaid.util.formatBrlFromCents

/** When `true`, only **balance** amounts use [DiscreetBalanceValue] (tap to reveal). */
val LocalPrivacyHideBalance = staticCompositionLocalOf { false }

@Composable
fun DiscreetBalanceValue(
    cents: Int,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
) {
    val hideBalance = LocalPrivacyHideBalance.current
    var revealed by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { revealed = false }
    }
    val display = when {
        !hideBalance -> formatBrlFromCents(cents)
        revealed -> formatBrlFromCents(cents)
        else -> stringResource(R.string.privacy_amount_masked)
    }
    Text(
        text = display,
        style = style,
        color = color,
        textAlign = textAlign,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (hideBalance) {
                    Modifier.clickable { revealed = !revealed }
                } else {
                    Modifier
                },
            ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
