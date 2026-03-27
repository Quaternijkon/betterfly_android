package com.betterfly.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.betterfly.app.util.parseHexColor

val LocalThemeColor = compositionLocalOf { Color(0xFF4285F4) }

private fun buildColorScheme(seed: Color, dark: Boolean): ColorScheme =
    if (dark) darkColorScheme(
        primary = seed,
        onPrimary = Color.White,
        primaryContainer = seed.copy(alpha = 0.22f),
        secondary = seed.copy(alpha = 0.7f),
        background = Color(0xFF0F1117),
        surface = Color(0xFF1A1D24),
        surfaceVariant = Color(0xFF252830),
        onBackground = Color(0xFFE2E8F0),
        onSurface = Color(0xFFE2E8F0),
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0xFF374151),
        error = Color(0xFFEF4444)
    ) else lightColorScheme(
        primary = seed,
        onPrimary = Color.White,
        primaryContainer = seed.copy(alpha = 0.12f),
        secondary = seed.copy(alpha = 0.8f),
        background = Color(0xFFF8FAFC),
        surface = Color.White,
        surfaceVariant = Color(0xFFF1F5F9),
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF0F172A),
        onSurfaceVariant = Color(0xFF64748B),
        outline = Color(0xFFE2E8F0),
        error = Color(0xFFEF4444)
    )

private val AppTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

@Composable
fun BetterFlyTheme(
    themeColor: String = "#4285F4",
    darkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val seed = parseHexColor(themeColor)
    val colorScheme = buildColorScheme(seed, darkMode)
    CompositionLocalProvider(LocalThemeColor provides seed) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
