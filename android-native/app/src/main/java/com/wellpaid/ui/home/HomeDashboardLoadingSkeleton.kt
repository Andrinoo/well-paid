package com.wellpaid.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wellpaid.ui.components.WellPaidBrandCircularProgress
import com.wellpaid.ui.theme.WellPaidNavy
import com.wellpaid.ui.theme.wellPaidMaxContentWidth
import com.wellpaid.ui.theme.WellPaidMaxContentWidth

/**
 * Estrutura aproximada da home (cartões) enquanto a API não responde — evita tela vazia só com spinner.
 */
@Composable
fun HomeDashboardLoadingSkeleton(
    modifier: Modifier = Modifier,
) {
    val t = rememberInfiniteTransition(label = "sk")
    val pulse by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "p",
    )
    val barBase = 0.06f + pulse * 0.1f
    val bar = WellPaidNavy.copy(alpha = barBase)

    Column(
        modifier = modifier
            .fillMaxSize()
            .wellPaidMaxContentWidth(WellPaidMaxContentWidth)
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(12.dp),
            color = bar,
            content = {},
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp),
            color = bar,
            content = {},
        )
        RowThreeSkeleton(loadingAlpha = barBase)
        Spacer(Modifier.height(4.dp))
        WellPaidBrandCircularProgress(size = 32.dp, stroke = 2.5.dp, forDarkHeader = false)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RowThreeSkeleton(loadingAlpha: Float) {
    val bar = WellPaidNavy.copy(alpha = loadingAlpha)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            shape = RoundedCornerShape(6.dp),
            color = bar,
            content = {},
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1.2f, 1f, 0.9f).forEach { w ->
                Surface(
                    modifier = Modifier
                        .weight(w)
                        .height(64.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = WellPaidNavy.copy(alpha = loadingAlpha * 0.95f),
                    content = {},
                )
            }
        }
    }
}
