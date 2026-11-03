package io.github.yudelieyang.kline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yudelieyang.kline.chart.priceLabel
import io.github.yudelieyang.kline.chart.volumeLabel
import io.github.yudelieyang.kline.ui.model.*
import io.github.yudelieyang.kline.ui.theme.*
import kline_kmp_app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun RecentCandleList(rows: List<RecentRow>, height: Dp, baseAsset: String) {
    val c = LocalKlineColors.current
    Column(Modifier.fillMaxWidth().height(height)) {
        Row(Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(Res.string.list_time, Res.string.list_close, Res.string.list_change, Res.string.list_volume).forEachIndexed { i, res ->
                Text(if (i == 3) stringResource(res, baseAsset) else stringResource(res), Modifier.weight(listOf(1.1f, 1.2f, 1f, .9f)[i]), style = KlineTypography.listHeader, color = c.textSecondary,
                    textAlign = if (i == 0) TextAlign.Start else TextAlign.End, maxLines = 1)
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = c.divider)
        if (rows.isEmpty()) Text(stringResource(Res.string.list_empty), Modifier.padding(start = 12.dp, top = 16.dp), style = KlineTypography.listRow, color = c.textTertiary)
        else LazyColumn {
            items(rows) { row ->
                Row(Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(utcMinute(row.openTime), Modifier.weight(1.1f), style = KlineTypography.listRow, color = c.textSecondary, maxLines = 1)
                    Text(priceLabel(row.close), Modifier.weight(1.2f), style = KlineTypography.listRow, color = c.textPrimary, textAlign = TextAlign.End, maxLines = 1)
                    val color = when { row.percent == null -> c.textTertiary; row.percent > 0 -> c.upText; row.percent < 0 -> c.downText; else -> c.textPrimary }
                    Text(row.percent?.let { signedNumber(it) + "%" } ?: "--", Modifier.weight(1f), style = KlineTypography.listRow, color = color, textAlign = TextAlign.End, maxLines = 1)
                    Text(volumeLabel(row.volume), Modifier.weight(.9f), style = KlineTypography.listRow, color = c.textSecondary, textAlign = TextAlign.End, maxLines = 1)
                }
                HorizontalDivider(thickness = .5.dp, color = c.divider)
            }
        }
    }
}
