package io.github.yudelieyang.kline.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.yudelieyang.kline.chart.ChartColors

val LightChartColors = ChartColors(
    Color(0xFFE64B4B), Color(0xFF1F9D6C), Color(0xFFE2E7EE), Color(0xFF5F6B7A),
    Color(0xFF64748B), Color(0xFF8A94A3), listOf(Color(0xFFB45309), Color(0xFF2474C7), Color(0xFF8E44AD)),
    Color(0xFFFFFFFF).copy(alpha = 0.92f), Color(0xFFE2E7EE), Color(0xFF374151),
)
val DarkChartColors = ChartColors(
    Color(0xFFF0605D), Color(0xFF2EBD85), Color(0xFF26303B), Color(0xFF98A3B3),
    Color(0xFF8A96A6), Color(0xFF6E7A8A), listOf(Color(0xFFF39C4A), Color(0xFF4C9BE8), Color(0xFFB07CD8)),
    Color(0xFF171D24).copy(alpha = 0.92f), Color(0xFF2F3A47), Color(0xFFC9D1DB),
)

 data class KlineColors(
    val background: Color, val surface: Color, val surfaceVariant: Color, val divider: Color,
    val textPrimary: Color, val textSecondary: Color, val textTertiary: Color, val accent: Color,
    val upText: Color, val downText: Color, val statusConnecting: Color, val statusWarning: Color, val statusError: Color,
    val bannerWarningBg: Color, val bannerWarningText: Color, val bannerWarningBorder: Color,
    val bannerInfoBg: Color, val bannerInfoText: Color, val bannerInfoBorder: Color,
    val scrim: Color, val chart: ChartColors,
)
val LightKlineColors = KlineColors(
    Color(0xFFFFFFFF), Color(0xFFF4F6F9), Color(0xFFEEF1F5), Color(0xFFE2E7EE),
    Color(0xFF111827), Color(0xFF5F6B7A), Color(0xFF8A94A3), Color(0xFF4F46E5),
    Color(0xFFD23535), Color(0xFF0F7F55), Color(0xFF8A94A3), Color(0xFFD97706), Color(0xFF4B5563),
    Color(0xFFFFF4E0), Color(0xFF7A4100), Color(0xFFF3D19C),
    Color(0xFFEEF1F5), Color(0xFF374151), Color(0xFFD9DFE7), Color(0xFF111827).copy(alpha = 0.4f), LightChartColors,
)
val DarkKlineColors = KlineColors(
    Color(0xFF0F141A), Color(0xFF171D24), Color(0xFF1F2731), Color(0xFF26303B),
    Color(0xFFE6EAF0), Color(0xFF98A3B3), Color(0xFF6E7A8A), Color(0xFF8B8BF5),
    Color(0xFFF0605D), Color(0xFF2EBD85), Color(0xFF6E7A8A), Color(0xFFF5A524), Color(0xFF98A3B3),
    Color(0xFF33270F), Color(0xFFF7C67A), Color(0xFF5C4316),
    Color(0xFF1F2731), Color(0xFFC9D1DB), Color(0xFF2F3A47), Color(0xFF000000).copy(alpha = 0.55f), DarkChartColors,
)
