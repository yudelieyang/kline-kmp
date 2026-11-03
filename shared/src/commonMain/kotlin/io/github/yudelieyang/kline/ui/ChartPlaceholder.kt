package io.github.yudelieyang.kline.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import io.github.yudelieyang.kline.ui.theme.*
import kline_kmp_app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChartPlaceholder(modifier: Modifier = Modifier) {
    val c = LocalKlineColors.current
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0..4) drawLine(c.divider, Offset(0f, size.height * i / 4), Offset(size.width, size.height * i / 4), 0.5.dp.toPx())
            for (i in 0..13) {
                val h = size.height * (0.08f + (i % 4) * 0.04f)
                drawRoundRect(c.surfaceVariant, Offset(size.width * (i + 0.5f) / 15, size.height * 0.48f - h / 2), Size(size.width / 32, h))
            }
        }
        Text(stringResource(Res.string.chart_waiting), Modifier.align(Alignment.BottomCenter).padding(8.dp), style = KlineTypography.caption, color = c.textSecondary)
    }
}
