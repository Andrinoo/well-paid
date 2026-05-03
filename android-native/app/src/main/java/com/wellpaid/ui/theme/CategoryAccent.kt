package com.wellpaid.ui.theme

import androidx.compose.ui.graphics.Color

private val CategoryAccentPalette = listOf(
    Color(0xFF3949AB),
    Color(0xFF00897B),
    Color(0xFF6D4C41),
    Color(0xFFC62828),
    Color(0xFF1565C0),
    Color(0xFF2E7D32),
    Color(0xFFF57C00),
    Color(0xFF6A1B9A),
)

/** Cor estável por id (categoria, etc.) para ponto de acento na UI. */
fun categoryAccentColor(seed: String): Color {
    val h = seed.hashCode()
    val positive = if (h == Int.MIN_VALUE) 0 else kotlin.math.abs(h)
    return CategoryAccentPalette[positive % CategoryAccentPalette.size]
}
