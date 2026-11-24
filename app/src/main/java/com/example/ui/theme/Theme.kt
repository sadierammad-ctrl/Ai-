package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalThemePreset = staticCompositionLocalOf { ThemePreset.NEON_CYBER }

@Composable
fun BabyAiTheme(
    themePreset: ThemePreset = ThemePreset.NEON_CYBER,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = themePreset.primaryColor,
        onPrimary = CosmicBlack,
        primaryContainer = themePreset.primaryColor.copy(alpha = 0.22f),
        onPrimaryContainer = TextPrimary,
        secondary = themePreset.secondaryColor,
        onSecondary = Color.White,
        secondaryContainer = themePreset.secondaryColor.copy(alpha = 0.24f),
        onSecondaryContainer = TextPrimary,
        tertiary = themePreset.tertiaryColor,
        onTertiary = CosmicBlack,
        background = CosmicBlack,
        onBackground = TextPrimary,
        surface = DeepNavy,
        onSurface = TextPrimary,
        surfaceVariant = GlassCardDark,
        onSurfaceVariant = TextSecondary,
        outline = themePreset.primaryColor.copy(alpha = 0.35f),
        error = CoralAlert,
        onError = Color.White
    )

    CompositionLocalProvider(LocalThemePreset provides themePreset) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BabyAiTypography,
            content = content
        )
    }
}
