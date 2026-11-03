package io.github.yudelieyang.kline.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalKlineColors = staticCompositionLocalOf { LightKlineColors }
@Composable
fun KlineTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkKlineColors else LightKlineColors
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(LocalKlineColors provides c) {
        MaterialTheme(colorScheme = scheme.copy(background = c.background, surface = c.surface, onBackground = c.textPrimary,
            onSurface = c.textPrimary, primary = c.accent, outline = c.divider), content = content)
    }
}
