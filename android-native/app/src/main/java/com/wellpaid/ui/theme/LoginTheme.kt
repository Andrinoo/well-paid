package com.wellpaid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// VISUAL_DESIGN_WELL_PAID.md §1.2 — autenticação
val LoginBg = Color(0xFF000000)
val LoginCard = Color(0xFF141C2A)
val LoginCardBorder = Color(0xFFC9A94E)
val LoginFieldFill = Color(0xFF1B2C41)
val LoginOnCard = Color(0xFFF5F1E8)
val LoginOnCardMuted = Color(0xFFADA59A)
val LoginHint = Color(0xFF7A756D)

val LoginGold = Color(0xFFC9A94E)
val LoginGoldDeep = Color(0xFFB8943D)
val LoginGoldMuted = LoginCardBorder.copy(alpha = 0.75f)
val LoginOnGold = WellPaidNavy
val LoginFooter = LoginOnCardMuted
val LoginError = Color(0xFFFF6B6B)

/** Botão principal: predominância dourada (pouco ou nenhum navy no gradiente). */
val LoginButtonGradient: Brush
    get() = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF6B5420),
            LoginGoldDeep,
            LoginGold,
            Color(0xFFFFE9A8),
        ),
    )

private val LoginDarkScheme = darkColorScheme(
    primary = LoginGold,
    onPrimary = LoginOnGold,
    primaryContainer = LoginGoldDeep,
    onPrimaryContainer = Color.White,
    secondary = LoginGoldMuted,
    onSecondary = Color.White,
    tertiary = LoginGoldDeep,
    background = LoginBg,
    onBackground = LoginOnCard,
    surface = LoginCard,
    onSurface = LoginOnCard,
    surfaceVariant = LoginFieldFill,
    onSurfaceVariant = LoginOnCardMuted,
    outline = LoginCardBorder,
    outlineVariant = LoginCardBorder.copy(alpha = 0.35f),
    error = LoginError,
    onError = Color.White,
)

@Composable
fun WellPaidLoginTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LoginDarkScheme,
        typography = Typography,
        shapes = WellPaidShapes,
        content = content,
    )
}
