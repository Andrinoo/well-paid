package com.wellpaid.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = WellPaidNavy,
    onPrimary = Color.White,
    primaryContainer = WellPaidNavyMid,
    onPrimaryContainer = Color.White,
    secondary = WellPaidGold,
    onSecondary = WellPaidNavy,
    secondaryContainer = WellPaidGold.copy(alpha = 0.35f),
    onSecondaryContainer = WellPaidNavy,
    tertiary = WellPaidNavyMid,
    onTertiary = Color.White,
    background = WellPaidCream,
    onBackground = WellPaidNavy,
    surface = WellPaidCream,
    onSurface = WellPaidNavy,
    surfaceVariant = WellPaidCreamMuted,
    onSurfaceVariant = WellPaidNavy.copy(alpha = 0.62f),
    outline = WellPaidNavy.copy(alpha = 0.14f),
    outlineVariant = WellPaidNavy.copy(alpha = 0.08f),
    error = Color(0xFFB00020),
    onError = Color.White,
)

@Composable
fun WellPaidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        shapes = WellPaidShapes,
        content = content,
    )
}
