package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CyberColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = CyberBlack,
    secondary = CyberPink,
    onSecondary = CyberBlack,
    tertiary = CyberGreen,
    onTertiary = CyberBlack,
    background = CyberBlack,
    onBackground = TermWhite,
    surface = CyberDark,
    onSurface = TermWhite,
    surfaceVariant = CyberGray,
    onSurfaceVariant = TermMuted,
    outline = CyberGrayLight,
    error = CyberPink,
    onError = CyberBlack
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
