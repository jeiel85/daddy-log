package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = CharcoalBackground,
    primaryContainer = DeepGreenDark,
    onPrimaryContainer = TextLightGrey,
    secondary = SageGreenSecondary,
    onSecondary = TextLightGrey,
    background = CharcoalBackground,
    onBackground = TextLightGrey,
    surface = DarkSurfaceCard,
    onSurface = TextLightGrey,
    surfaceVariant = DeepGreenDark,
    onSurfaceVariant = TextLightGrey
)

private val LightColorScheme = lightColorScheme(
    primary = DeepGreenPrimary,
    onPrimary = CardSurfaceWhite,
    primaryContainer = LightMintAccent,
    onPrimaryContainer = DeepGreenDark,
    secondary = SageGreenSecondary,
    onSecondary = CardSurfaceWhite,
    background = WarmGreyBackground,
    onBackground = TextDarkGrey,
    surface = CardSurfaceWhite,
    onSurface = TextDarkGrey,
    surfaceVariant = WarmGreyBackground,
    onSurfaceVariant = TextDarkGrey,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
