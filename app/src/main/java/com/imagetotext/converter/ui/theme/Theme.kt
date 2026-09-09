package com.imagetotext.converter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = LightSurface,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealPrimary,
    secondary = AmberSecondary,
    secondaryContainer = AmberSecondaryContainer,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealPrimaryContainerDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealPrimaryDark,
    secondary = AmberSecondaryDark,
    secondaryContainer = AmberSecondaryContainerDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = ErrorRedDark
)

/**
 * [darkModeOverride] is the user's saved preference (from DataStore).
 * When it's null (no explicit choice made yet), we fall back to the
 * device's system theme.
 */
@Composable
fun ImageToTextTheme(
    darkModeOverride: Boolean?,
    content: @Composable () -> Unit
) {
    val useDarkTheme = darkModeOverride ?: isSystemInDarkTheme()
    val colorScheme = if (useDarkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
