package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = HoneyGold,
    secondary = HoneyGoldLight,
    background = NavyBg,
    surface = SlateCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE2E8F0), // Platinum/light grey for contrast on black
    onSurface = Color(0xFFE2E8F0),
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme( // Enforce professional cyber-dark signature for both
    primary = HoneyGold,
    secondary = HoneyGoldLight,
    background = NavyBg,
    surface = SlateCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun ThriftHiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retain the legacy name if needed for compiling with minimal edits, 
// or let's create a wrapper/composing name. Let's make sure both MyApplicationTheme 
// and ThriftHiveTheme can be referenced to avoid any build failures.
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // enforce brand identity
    content: @Composable () -> Unit
) {
    ThriftHiveTheme(darkTheme = darkTheme, content = content)
}
