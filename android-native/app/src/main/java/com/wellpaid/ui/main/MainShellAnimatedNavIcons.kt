package com.wellpaid.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ícone da barra principal (5 tabs): tab seleccionada com pop de mola + loop distinto por tab.
 * Translações usam dp→px; ícone num [Box] maior para o movimento não ser cortado.
 */
@Composable
fun MainBottomNavIcon(
    tabIndex: Int,
    selected: Boolean,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val tint = LocalContentColor.current
    if (!selected) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier,
        )
    } else {
        MainBottomNavIconActive(
            tabIndex = tabIndex,
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier,
        )
    }
}

@Composable
private fun MainBottomNavIconActive(
    tabIndex: Int,
    imageVector: ImageVector,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val slidePx = with(density) { 5.dp.toPx() }
    val slideSmallPx = with(density) { 2.5.dp.toPx() }
    val bobPx = with(density) { 4.dp.toPx() }

    val pop = remember { Animatable(1f) }
    LaunchedEffect(tabIndex) {
        pop.snapTo(1.22f)
        pop.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.58f, stiffness = 440f),
        )
    }

    val infinite = rememberInfiniteTransition(label = "main_tab_phase_$tabIndex")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (tabIndex) {
                    0 -> 2200
                    1 -> 1600
                    2 -> 1400
                    3 -> 2000
                    else -> 2600
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    val wobble = sin((phase * 2 * PI).toFloat())
    val wobbleSlow = sin((phase * 2 * PI * 0.62f).toFloat())

    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin.Center
                    scaleX = pop.value
                    scaleY = pop.value
                    when (tabIndex) {
                        0 -> {
                            translationY = wobble * bobPx
                            val m = 1f + wobble * 0.12f
                            scaleX *= m
                            scaleY *= m
                        }
                        1 -> {
                            translationX = wobble * slidePx
                            translationY = wobbleSlow * slideSmallPx
                        }
                        2 -> {
                            val pulse = 1f + wobble * 0.14f
                            scaleX *= pulse
                            scaleY *= pulse
                        }
                        3 -> {
                            rotationZ = wobble * 14f
                            translationY = wobbleSlow * (bobPx * 0.45f)
                        }
                        else -> {
                            val breath = (wobble + 1f) * 0.5f
                            val s = 1f + breath * 0.12f
                            scaleX *= s
                            scaleY *= s
                        }
                    }
                },
        )
    }
}

/**
 * Atalhos expandidos: esta composição só existe com a faixa aberta — anima sempre, com motion por índice.
 * [selected] só reforça um pop ao toque.
 */
@Composable
fun ShortcutBottomNavIcon(
    shortcutIndex: Int,
    selected: Boolean,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val tint = LocalContentColor.current
    val density = LocalDensity.current
    val slidePx = with(density) { 5.dp.toPx() }
    val hopPx = with(density) { 4.dp.toPx() }

    val pop = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            pop.snapTo(1.18f)
            pop.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 500f))
        } else {
            pop.snapTo(1f)
        }
    }

    val infinite = rememberInfiniteTransition(label = "shortcut_phase_$shortcutIndex")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (shortcutIndex) {
                    0 -> 1900
                    1 -> 1500
                    else -> 2100
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val t = sin((phase * 2 * PI).toFloat())

    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .size(26.dp)
                .graphicsLayer {
                    transformOrigin = TransformOrigin.Center
                    scaleX = pop.value
                    scaleY = pop.value
                    when (shortcutIndex) {
                        0 -> {
                            translationX = t * slidePx
                            rotationZ = t * 8f
                        }
                        1 -> {
                            translationY = -kotlin.math.abs(t) * hopPx
                            rotationZ = t * 12f
                        }
                        else -> {
                            val c = cos((phase * 2 * PI).toFloat())
                            scaleX *= 1f + c * 0.12f
                            scaleY *= 1f + c * 0.12f
                            translationY = t * slidePx * 0.35f
                        }
                    }
                },
        )
    }
}
