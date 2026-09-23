package com.pkfuturegkgs.hardsecurityguard.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = HsgPrimary,
    background = HsgBackgroundDark,
    surface = HsgSurfaceDark,
    surfaceVariant = HsgSurfaceVariantDark,
    onBackground = HsgOnDark,
    onSurface = HsgOnDark,
    error = HsgAccentRed
)

private val LightColors = lightColorScheme(
    primary = HsgPrimary,
    background = HsgBackgroundLight,
    surface = HsgSurfaceLight,
    onBackground = HsgOnLight,
    onSurface = HsgOnLight,
    error = HsgAccentRed
)

@Composable
fun HardSecurityGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HsgTypography,
        content = content
    )
}
