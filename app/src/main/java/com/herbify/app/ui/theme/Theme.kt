package com.herbify.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeonGreen = Color(0xFF39FF14)
val NeonGreenDim = Color(0xFF1DB510)
val DarkBg = Color(0xFF0A0F0A)
val DarkSurface = Color(0xFF111811)
val DarkCard = Color(0xFF162016)
val DarkBorder = Color(0xFF1F2F1F)
val TextPrimary = Color(0xFFE8F5E8)
val TextSecondary = Color(0xFF7BA87B)
val EcoGold = Color(0xFFFFD700)
val DangerRed = Color(0xFFFF4444)

private val HerbifyColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = DarkBg,
    secondary = NeonGreenDim,
    onSecondary = DarkBg,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = DangerRed,
    tertiary = EcoGold
)

@Composable
fun HerbifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HerbifyColorScheme,
        content = content
    )
}
