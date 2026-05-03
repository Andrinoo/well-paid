package com.wellpaid.ui.version

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.wellpaid.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Linha prioritária de versionamento:
 * `SIGLA:1.x(Alembic)(dd/MM/yyyy)(d.h.m.s.mmmm)` — último bloco = tempo desde a âncora, atualizado em tempo real.
 */
@Composable
fun WellPaidLoveVersionLine(
    modifier: Modifier = Modifier,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign = TextAlign.Center,
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(1L)
            nowMs = System.currentTimeMillis()
        }
    }
    val line =
        remember(nowMs) {
            val sigla = BuildConfig.VERSION_SIGLA
            val alembic = BuildConfig.REVISION_CODE
            val today = DaughterTogetherClock.todayBrazil(nowMs)
            val elapsed = DaughterTogetherClock.formatElapsedNumeric(nowMs)
            "$sigla:1.${BuildConfig.VERSION_CODE}($alembic)($today)($elapsed)"
        }
    Text(
        text = line,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
    )
}
