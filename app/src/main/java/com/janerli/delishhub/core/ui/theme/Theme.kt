package com.janerli.delishhub.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()

@Composable
fun DelishHubTheme(
    darkTheme: Boolean = true, // временно включим тёмную тему по умолчанию (по требованиям)
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
