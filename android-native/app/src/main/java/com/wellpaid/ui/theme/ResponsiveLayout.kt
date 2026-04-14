package com.wellpaid.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Largura máxima do conteúdo em telemóveis largos / tablets (legível e centrado). */
val WellPaidMaxContentWidth: Dp = 560.dp

/** Formulários de auth um pouco mais estreitos. */
val WellPaidMaxAuthWidth: Dp = 440.dp

/**
 * Padding horizontal conforme a largura do ecrã — evita texto colado às bordas em ecrãs estreitos
 * e mantém respiro em tablets.
 */
@Composable
fun Modifier.wellPaidScreenHorizontalPadding(): Modifier {
    val wDp = LocalConfiguration.current.screenWidthDp.dp
    val pad = when {
        wDp < 340.dp -> 12.dp
        wDp < 400.dp -> 16.dp
        wDp < 600.dp -> 20.dp
        else -> 24.dp
    }
    return padding(horizontal = pad)
}

/**
 * Limita largura e centra (útil dentro de um [androidx.compose.foundation.layout.Box] ou Column full width).
 */
fun Modifier.wellPaidMaxContentWidth(maxWidth: Dp = WellPaidMaxContentWidth): Modifier =
    fillMaxWidth()
        .widthIn(max = maxWidth)
        .wrapContentWidth(Alignment.CenterHorizontally)
