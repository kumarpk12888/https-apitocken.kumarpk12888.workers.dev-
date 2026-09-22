package com.kumarpk12888.hardsecurityguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF66B2FF),
    secondary = androidx.compose.ui.graphics.Color(0xFF89E0A5),
    tertiary = androidx.compose.ui.graphics.Color(0xFFB8C7FF),
    background = androidx.compose.ui.graphics.Color(0xFF0E1726),
    surface = androidx.compose.ui.graphics.Color(0xFF162233),
    onBackground = androidx.compose.ui.graphics.Color(0xFFEAF3FF),
    onSurface = androidx.compose.ui.graphics.Color(0xFFEAF3FF),
    error = androidx.compose.ui.graphics.Color(0xFFFF6B6B),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF061826),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF061826)
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF1565C0),
    secondary = androidx.compose.ui.graphics.Color(0xFF2E7D32),
    tertiary = androidx.compose.ui.graphics.Color(0xFF7E57C2),
    background = androidx.compose.ui.graphics.Color(0xFFF3F7FF),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onBackground = androidx.compose.ui.graphics.Color(0xFF101827),
    onSurface = androidx.compose.ui.graphics.Color(0xFF101827),
    error = androidx.compose.ui.graphics.Color(0xFFD32F2F),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSecondary = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
)

@Composable
fun HardSecurityGuardTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
