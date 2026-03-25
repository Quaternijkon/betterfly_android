package com.betterfly.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.betterfly.app.util.parseHexColor

val LocalThemeColor = compositionLocalOf { Color(0xFF4285F4) }

private fun buildColorScheme(seed: Color, dark: Boolean): ColorScheme {
    return if (dark) {
        darkColorScheme(
            primary = seed,
            onPrimary = Color.White,
            primaryContainer = seed.copy(alpha = 0.2f),
            secondary = seed.copy(alpha = 0.7f),
            background = Color(0xFF111318),
            surface = Color(0xFF1C1F26),
            surfaceVariant = Color(0xFF252830),
            onBackground = Color(0xFFE2E8F0),
            onSurface = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF374151)
        )
    } else {
        lightColorScheme(
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
            outline = Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun BetterFlyTheme(
    themeColor: String = "#4285F4",
    darkMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val seed = parseHexColor(themeColor)
    val colorScheme = buildColorScheme(seed, darkMode)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
