package com.tamaade.ecommerce.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = White,
    primaryContainer = BrandGreenDark,
    onPrimaryContainer = White,
    secondary = BrandBlue,
    onSecondary = White,
    tertiary = BrandYellow,
    onTertiary = Foreground,
    background = PageBackground,
    onBackground = Foreground,
    surface = White,
    onSurface = Foreground,
    surfaceVariant = CardImageBg,
    onSurfaceVariant = Muted,
    outline = Border,
    error = Color(0xFFF01F0E)
)

@Composable
fun TamaadeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = TamaadeTypography,
        content = content
    )
}
