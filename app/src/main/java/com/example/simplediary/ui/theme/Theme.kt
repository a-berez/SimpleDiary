package com.example.simplediary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MutedBluePrimaryDark,
    secondary = MutedBlueSecondaryDark,
    background = MutedBlueBackgroundDark,
    surface = MutedBlueSurfaceDark,
    onPrimary = MutedBlueOnPrimaryDark,
    onBackground = MutedBlueOnBackgroundDark,
    onSurface = MutedBlueOnBackgroundDark,
)

private val LightColorScheme = lightColorScheme(
    primary = MutedBluePrimaryLight,
    secondary = MutedBlueSecondaryLight,
    background = MutedBlueBackgroundLight,
    surface = MutedBlueSurfaceLight,
    onPrimary = MutedBlueOnPrimaryLight,
    onBackground = MutedBlueOnBackgroundLight,
    onSurface = MutedBlueOnBackgroundLight,
)

@Composable
fun SimpleDiaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}