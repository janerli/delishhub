package com.janerli.delishhub.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun DelishHubTheme(
    darkTheme: Boolean = true, // оставил для совместимости, но ниже приоритет у ThemeStore
    content: @Composable () -> Unit
) {
    // ✅ ВСЕГДА подписываемся — тогда будут рекомпозиции при init() и при смене режима
    val mode by ThemeStore.mode.collectAsState()

    val isDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    MaterialTheme(
        colorScheme = if (isDark) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
