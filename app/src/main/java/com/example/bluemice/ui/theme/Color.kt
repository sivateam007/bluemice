package com.example.bluemice.ui.theme

import androidx.compose.ui.graphics.Color

// CSS-inspired professional dark theme palette
val NavyDark = Color(0xFF0A1628)
val NavyPrimary = Color(0xFF0C1F33)
val BlueSecondary = Color(0xFF125683)
val Periwinkle = Color(0xFF7F89D4)
val GitRed = Color(0xFFE24124)
val SuccessGreen = Color(0xFF27AE60)
val SurfaceDark = Color(0xFF1A2A3A)
val CardSurface = Color(0xFF1E3040)
val TextPrimary = Color(0xFFECF0F1)
val TextSecondary = Color(0xFF95A5A6)
val Overlay = Color(0x880C1F33)
val AccentPurple = Color(0xFFBB86FC)
val DividerColor = Color(0xFF2C3E50)

val CardBorder = Color(0xFF3A5060)

// Theme accent palette (reference: theme_color_values)
val AccentRed = Color(0xFFEF5350)
val AccentIndigo = Color(0xFF5C6BC0)
val AccentBlue = Color(0xFF42A5F5)
val AccentGreen = Color(0xFF27AE60)
val AccentYellow = Color(0xFFFFD700)
val AccentOrange = Color(0xFFFF9800)
val AccentBrown = Color(0xFF8D6E63)
val AccentGrey = Color(0xFF9E9E9E)
val AccentBlueGrey = Color(0xFF78909C)

// Tron style
val TronCyan = Color(0xFF00E5FF)
val TronMagenta = Color(0xFFFF4081)

fun accentColorFor(name: String): Color = when (name) {
    "red" -> AccentRed
    "purple" -> AccentPurple
    "indigo" -> AccentIndigo
    "blue" -> AccentBlue
    "green" -> AccentGreen
    "yellow" -> AccentYellow
    "orange" -> AccentOrange
    "brown" -> AccentBrown
    "grey" -> AccentGrey
    "blue_grey" -> AccentBlueGrey
    else -> Periwinkle
}
