package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OavDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = SiteBgDarker,
    primaryContainer = CyanDark,
    onPrimaryContainer = TextWhite,
    secondary = EmeraldSuccess,
    onSecondary = SiteBgDarker,
    secondaryContainer = EmeraldSoftBg,
    onSecondaryContainer = EmeraldSuccess,
    tertiary = AmberAccent,
    onTertiary = SiteBgDarker,
    background = SiteBg,
    onBackground = TextWhite,
    surface = CardBg,
    onSurface = TextWhite,
    surfaceVariant = CardBgElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = CardBorderActive
)

private val OavLightColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = SiteBgDarker,
    primaryContainer = CyanDark,
    onPrimaryContainer = TextWhite,
    secondary = EmeraldSuccess,
    onSecondary = SiteBgDarker,
    background = SiteBg,
    onBackground = TextWhite,
    surface = CardBg,
    onSurface = TextWhite,
    outline = CardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) OavDarkColorScheme else OavLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

