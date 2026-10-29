package com.engbase.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun EngBaseTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(
            primary = Color(0xFFF2B705),
            onPrimary = Color(0xFF1A1A1A),
            secondary = Color(0xFF7FB2E5),
            background = Color(0xFF0F1B27),
            onBackground = Color(0xFFEAF0F6),
            surface = Color(0xFF16273A),
            onSurface = Color(0xFFEAF0F6),
            surfaceVariant = Color(0xFF1F3550),
            onSurfaceVariant = Color(0xFFC9D6E3)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF12304D),
            onPrimary = Color.White,
            secondary = Color(0xFFB07F00),
            background = Color(0xFFF6F8FB),
            onBackground = Color(0xFF14202C),
            surface = Color.White,
            onSurface = Color(0xFF14202C),
            surfaceVariant = Color(0xFFE3EAF2),
            onSurfaceVariant = Color(0xFF3A4856)
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
