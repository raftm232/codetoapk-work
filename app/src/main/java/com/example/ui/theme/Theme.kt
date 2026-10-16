package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LuxuryDarkColorScheme = darkColorScheme(
    primary = CyberBlueLight,
    onPrimary = Color.White,
    primaryContainer = CyberBlue,
    onPrimaryContainer = Color.White,
    secondary = EmeraldGreenLight,
    onSecondary = Color.Black,
    secondaryContainer = EmeraldGreen,
    onSecondaryContainer = Color.White,
    tertiary = LuxuryGoldLight,
    onTertiary = Color.Black,
    tertiaryContainer = LuxuryGold,
    onTertiaryContainer = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = DarkCardBorder,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LuxuryDarkColorScheme,
        typography = Typography,
        content = content
    )
}
