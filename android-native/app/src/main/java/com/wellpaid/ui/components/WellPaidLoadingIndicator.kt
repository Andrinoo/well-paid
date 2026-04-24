package com.wellpaid.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wellpaid.ui.theme.WellPaidGold
import com.wellpaid.ui.theme.WellPaidNavy

/**
 * Indicador alinhado à marca (Navy trilha, Gold progresso) — preferir a spinners M3 padrão.
 *
 * @param forDarkHeader fundo do header navy (trilha clara). Caso contrário, trilha suave no cream.
 */
@Composable
fun WellPaidBrandCircularProgress(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    stroke: Dp = 3.dp,
    forDarkHeader: Boolean = false,
) {
    val track = if (forDarkHeader) {
        Color.White.copy(alpha = 0.22f)
    } else {
        WellPaidNavy.copy(alpha = 0.12f)
    }
    CircularProgressIndicator(
        modifier = modifier.size(size),
        color = WellPaidGold,
        trackColor = track,
        strokeWidth = stroke,
    )
}
