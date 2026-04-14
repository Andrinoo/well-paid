package com.wellpaid.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/** Durações e curvas partilhadas para micro‑movimento consistente (efeito “app premium”). */
object WellPaidMotion {
    const val FadeShortMs = 260
    const val FadeMediumMs = 340
    const val FadeLongMs = 420
    val StandardEasing = FastOutSlowInEasing

    fun <T> fadeTween(durationMillis: Int = FadeMediumMs) =
        tween<T>(durationMillis = durationMillis, easing = StandardEasing)
}
