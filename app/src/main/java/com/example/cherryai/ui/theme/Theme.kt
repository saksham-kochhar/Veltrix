package com.example.cherryai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val LightMaterialScheme = lightColorScheme(
    primary = CherryUiLight.accent,
    onPrimary = CherryUiLight.surface,
    secondary = CherryUiLight.accent,
    onSecondary = CherryUiLight.surface,
    background = CherryUiLight.pageBackground,
    onBackground = CherryUiLight.textPrimary,
    surface = CherryUiLight.surface,
    onSurface = CherryUiLight.textPrimary,
    surfaceVariant = CherryUiLight.surfaceMuted,
    onSurfaceVariant = CherryUiLight.textMuted,
    outline = CherryUiLight.divider,
    outlineVariant = CherryUiLight.composerBorder,
)

private val DarkMaterialScheme = darkColorScheme(
    primary = CherryUiDark.accent,
    onPrimary = CherryUiDark.textPrimary,
    secondary = CherryUiDark.link,
    onSecondary = CherryUiDark.pageBackground,
    background = CherryUiDark.pageBackground,
    onBackground = CherryUiDark.textPrimary,
    surface = CherryUiDark.surface,
    onSurface = CherryUiDark.textPrimary,
    surfaceVariant = CherryUiDark.surfaceMuted,
    onSurfaceVariant = CherryUiDark.textMuted,
    outline = CherryUiDark.divider,
    outlineVariant = CherryUiDark.composerBorder,
)

@Composable
fun CherryAITheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val cherryColors = if (darkTheme) CherryUiDark else CherryUiLight
    val colorScheme = if (darkTheme) DarkMaterialScheme else LightMaterialScheme

    CompositionLocalProvider(LocalCherryUiColors provides cherryColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
