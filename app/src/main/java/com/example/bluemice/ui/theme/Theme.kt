package com.example.bluemice.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

/**
 * Picks black or white text for [onColor] by relative luminance.
 *
 * A flat `Color.White` is unreadable on the yellow and gold accents (roughly
 * 1.2:1 against #FFD700), which is why those theme options looked broken.
 */
private fun onColorFor(onColor: Color): Color {
    // sRGB -> linear, per WCAG 2.1 relative luminance.
    fun lin(c: Float) = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    val l = 0.2126f * lin(onColor.red) + 0.7152f * lin(onColor.green) + 0.0722f * lin(onColor.blue)
    return if (l > 0.179f) Color.Black else Color.White
}

private fun proDarkScheme(accent: Color, tron: Boolean = false): ColorScheme {
    val primary = if (tron) TronCyan else accent
    val secondary = if (tron) TronMagenta else BlueSecondary
    return darkColorScheme(
        primary = primary,
        onPrimary = onColorFor(primary),
        primaryContainer = NavyPrimary,
        onPrimaryContainer = TextPrimary,
        secondary = secondary,
        onSecondary = TextPrimary,
        secondaryContainer = CardSurface,
        tertiary = SuccessGreen,
        background = NavyDark,
        onBackground = TextPrimary,
        surface = SurfaceDark,
        onSurface = TextPrimary,
        surfaceVariant = CardSurface,
        onSurfaceVariant = TextSecondary,
        error = GitRed,
        onError = Color.White,
        outline = if (tron) TronCyan else CardBorder,
        inversePrimary = AccentPurple,
    )
}

private fun proLightScheme(accent: Color, tron: Boolean = false): ColorScheme {
    val primary = if (tron) TronCyan else accent
    val secondary = if (tron) TronMagenta else Color(0xFF125683)
    return lightColorScheme(
        primary = primary,
        onPrimary = onColorFor(primary),
        primaryContainer = Color(0xFFE4E6F5),
        onPrimaryContainer = Color(0xFF1A2233),
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD7E5F2),
        tertiary = Color(0xFF1E8449),
        background = Color(0xFFF5F6FA),
        onBackground = Color(0xFF1A2233),
        surface = Color.White,
        onSurface = Color(0xFF1A2233),
        surfaceVariant = Color(0xFFE8EAF0),
        onSurfaceVariant = Color(0xFF5A6472),
        error = GitRed,
        onError = Color.White,
        outline = if (tron) TronCyan else Color(0xFFB8BEC9),
        inversePrimary = AccentPurple,
    )
}

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.5.sp),
)

@Composable
fun BluemiceTheme(
    darkTheme: Boolean = true,
    themeColor: String = "default",
    outlineTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accent = if (themeColor == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val scheme = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        scheme.primary
    } else {
        accentColorFor(themeColor)
    }
    val colorScheme = when {
        themeColor == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> proDarkScheme(accent, outlineTheme)
        else -> proLightScheme(accent, outlineTheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
