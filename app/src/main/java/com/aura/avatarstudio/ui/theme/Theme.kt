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

private val CyberpunkDarkColorScheme = darkColorScheme(
    // The requested purple neon accent
    primary = Color(0xFF904EDD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFF00F2FE),
    // Deep space black
    background = Color(0xFF0A0B10),
    // Dark panels
    surface = Color(0xFF13141C),
    // Lighter panels
    surfaceVariant = Color(0xFF1C1D29),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFA0A0B0),
    outline = Color(0xFF2D2E3A)
)

@Composable
fun AvatarStudioTheme(
    // Force dark theme for this UI
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberpunkDarkColorScheme,
        content = content
    )
}
