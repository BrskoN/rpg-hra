package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TarotWoodcutColorScheme = darkColorScheme(
    primary = AntiqueGold,
    onPrimary = DeepCharcoal,
    primaryContainer = Color(0xFF2C2410),
    onPrimaryContainer = AgedGold,
    secondary = MedievalCrimson,
    onSecondary = Color.White,
    tertiary = MedievalCrimsonDark,
    onTertiary = Color.White,
    background = DeepCharcoal,
    onBackground = LightInk,
    surface = DarkCanvasSurface,
    onSurface = LightInk,
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = AgedBone,
    outline = AgedGold
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TarotWoodcutColorScheme,
        typography = Typography,
        content = content
    )
}
