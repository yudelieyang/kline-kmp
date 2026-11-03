package io.github.yudelieyang.kline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yudelieyang.kline.chart.priceLabel
import io.github.yudelieyang.kline.chart.volumeLabel
import io.github.yudelieyang.kline.ui.model.*
import io.github.yudelieyang.kline.ui.theme.*
import kline_kmp_app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadedRangeNote(stats: LoadedStats?, mode: DataSourceMode) {
    if (stats == null) return
    val c = LocalKlineColors.current
    val text = stringResource(if (mode == DataSourceMode.Live) Res.string.range_note_live else Res.string.range_note_offline,
        stats.count, utcShortDateTime(stats.startTime), utcShortDateTime(stats.endTime))
    Text(text, Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = KlineTypography.caption, color = c.textTertiary)
}
