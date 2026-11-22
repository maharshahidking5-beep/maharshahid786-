package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.RoundedCornerShape

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF041418),
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = NeonRose,
    onSecondary = Color.White,
    secondaryContainer = NeonRoseContainer,
    onSecondaryContainer = Color(0xFFFFD8E6),
    tertiary = VioletPulse,
    onTertiary = Color.White,
    tertiaryContainer = VioletContainer,
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = StudioBackground,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = StudioBackground,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighlight,
    outline = StudioBorder,
    outlineVariant = Color(0xFF1F232E),
    error = Color(0xFFFF5370),
    onError = Color.White
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SsStudioTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
