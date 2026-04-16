package com.wellpaid.ui.main

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DefaultMinSwipeDistance: Dp = 80.dp

/**
 * Swipe horizontal nas abas secundárias: em direção ao **início** do layout → Início;
 * em direção ao **fim** → próxima aba (na última, «próxima» = Início).
 */
@Composable
fun rememberMainShellTabSwipeModifier(
    enabled: Boolean,
    currentTabIndex: Int,
    onNavigateHome: () -> Unit,
    onNavigateNext: () -> Unit,
    minSwipeDistance: Dp = DefaultMinSwipeDistance,
    includeForwardSwipe: Boolean = true,
): Modifier {
    if (!enabled) return Modifier
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val minPx = with(density) { minSwipeDistance.toPx() }
    val onHome by rememberUpdatedState(onNavigateHome)
    val onNext by rememberUpdatedState(onNavigateNext)

    return Modifier.pointerInput(currentTabIndex, layoutDirection, minPx, includeForwardSwipe) {
        var accumulated = 0f
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount ->
                accumulated += dragAmount
            },
            onDragEnd = {
                val towardStart = when (layoutDirection) {
                    LayoutDirection.Ltr -> accumulated > minPx
                    LayoutDirection.Rtl -> accumulated < -minPx
                }
                val towardEnd = when (layoutDirection) {
                    LayoutDirection.Ltr -> accumulated < -minPx
                    LayoutDirection.Rtl -> accumulated > minPx
                }
                accumulated = 0f
                when {
                    towardStart -> {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onHome()
                    }
                    towardEnd && includeForwardSwipe -> {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNext()
                    }
                }
            },
            onDragCancel = { accumulated = 0f },
        )
    }
}
