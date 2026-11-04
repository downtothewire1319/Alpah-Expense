package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = DeepNavy,
    primaryContainer = LightNavy,
    onPrimaryContainer = SoftWhite,
    secondary = AccentGold,
    onSecondary = DeepNavy,
    secondaryContainer = NavySurface,
    onSecondaryContainer = SoftWhite,
    background = DeepNavy,
    onBackground = SoftWhite,
    surface = NavyCard,
    onSurface = SoftWhite,
    surfaceVariant = LightNavy,
    onSurfaceVariant = MutedSlate,
    outline = LightNavy,
    error = AccentRed
)

private val LightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = PureWhite,
    primaryContainer = SoftWhite,
    onPrimaryContainer = DeepNavy,
    secondary = AccentBlue,
    onSecondary = PureWhite,
    secondaryContainer = SoftWhite,
    onSecondaryContainer = DeepNavy,
    background = SoftWhite,
    onBackground = DarkText,
    surface = PureWhite,
    onSurface = DarkText,
    surfaceVariant = SoftWhite,
    onSurfaceVariant = MutedSlate,
    outline = LightBorder,
    error = AccentRed
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
