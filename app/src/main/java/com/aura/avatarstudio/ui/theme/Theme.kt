package com.aura.avatarstudio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF904EDD),
    secondary = Color(0xFFE62040),
    tertiary = Color(0xFF00F2FE),
    background = Color(0xFF07070D),
    surface = Color(0xFF11111D)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF904EDD),
    secondary = Color(0xFFE62040),
    tertiary = Color(0xFF00F2FE)
)

@Composable
fun AvatarStudioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep it false to use our cyberpunk theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Force dark theme for cyberpunk feel
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
