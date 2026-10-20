package com.nyx.chess.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode(val label: String) { SYSTEM("Sistema"), LIGHT("Claro"), DARK("Escuro") }

private val Light = lightColorScheme(primary = Color(0xFF4A5BC4), secondary = Color(0xFF6B5E9B), tertiary = Color(0xFF00796B))
private val Dark = darkColorScheme(primary = Color(0xFFB7C4FF), secondary = Color(0xFFCDBDFF), tertiary = Color(0xFF7CDCCB))

@Composable
fun ChessTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val ctx = LocalContext.current
    val scheme = if (Build.VERSION.SDK_INT >= 31) { if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx) }
    else if (dark) Dark else Light
    MaterialTheme(colorScheme = scheme, content = content)
}
