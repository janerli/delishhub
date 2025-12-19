package com.janerli.delishhub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.janerli.delishhub.core.ui.theme.ThemeMode
import com.janerli.delishhub.core.ui.theme.ThemeStore

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun DelishHubTheme(
    darkTheme: Boolean = true, // оставлено для совместимости (не используем напрямую)
    dynamicColor: Boolean = false, // ✅ выключаем, чтобы тема была стабильной и одинаковой на всех девайсах
    content: @Composable () -> Unit
) {
    // ✅ твой реальный источник режима темы
    val mode by ThemeStore.mode.collectAsState()

    val isDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
