package com.example.ecoscanner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EcoDarkColorScheme = darkColorScheme(
    primary           = EcoGreen,
    onPrimary         = EcoBackground,
    primaryContainer  = EcoGreenDim,
    onPrimaryContainer= EcoGreen,
    secondary         = EcoBlue,
    onSecondary       = EcoBackground,
    tertiary          = EcoGold,
    background        = EcoBackground,
    onBackground      = EcoTextPrimary,
    surface           = EcoSurface,
    onSurface         = EcoTextPrimary,
    surfaceVariant    = EcoSurface2,
    onSurfaceVariant  = EcoTextMuted,
    outline           = EcoBorder,
    error             = EcoRed,
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EcoDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}