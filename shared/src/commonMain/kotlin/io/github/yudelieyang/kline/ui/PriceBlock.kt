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
fun PriceBlock(change: PriceChange?, stats: LoadedStats?, mode: DataSourceMode, phase: ConnectionPhase, lastUpdate: Long?, compact: Boolean, baseAsset: String) {
    val c = LocalKlineColors.current
    val stale = mode == DataSourceMode.Live && phase != ConnectionPhase.Live
    val color = when {
        change == null -> c.textPrimary
        stale -> c.textSecondary
        change.change == null || change.change == 0.0 -> c.textPrimary
        change.change > 0 -> c.upText
        else -> c.downText
    }
    val price: @Composable () -> Unit = {
        Column {
            Text(change?.let { priceLabel(it.price) } ?: "--", style = KlineTypography.priceLarge, color = color)
            Row {
                Text(change?.change?.let { signedNumber(it) } ?: "--", style = KlineTypography.priceChange, color = color)
                Spacer(Modifier.width(6.dp))
                Text(change?.percent?.let { signedNumber(it) + "%" } ?: "--", style = KlineTypography.priceChange, color = color)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.vs_previous), style = KlineTypography.caption, color = c.textSecondary)
            }
            val detail = when {
                mode == DataSourceMode.Offline -> stringResource(Res.string.offline_not_live)
                phase == ConnectionPhase.Live && lastUpdate != null -> stringResource(Res.string.updated_at, utcClock(lastUpdate))
                lastUpdate != null -> stringResource(Res.string.stopped_at, utcClock(lastUpdate))
                else -> stringResource(Res.string.waiting_data)
            }
            Text(detail, style = KlineTypography.caption, color = c.textSecondary)
        }
    }
    val grid: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat(stringResource(Res.string.stat_high), stats?.high?.let(::priceLabel) ?: "--", Modifier.weight(1f))
                Stat(stringResource(Res.string.stat_low), stats?.low?.let(::priceLabel) ?: "--", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat(stringResource(Res.string.stat_volume, baseAsset), stats?.volume?.let(::volumeLabel) ?: "--", Modifier.weight(1f))
                val span = stats?.let { formatSpan(it.endTime - it.startTime) }
                val spanText = if (span == null) "--" else when {
                    span.days > 0 -> stringResource(Res.string.span_dhm, span.days, span.hours, span.minutes)
                    span.hours > 0 -> stringResource(Res.string.span_hm, span.hours, span.minutes)
                    else -> stringResource(Res.string.span_m, span.minutes)
                }
                Stat(stringResource(Res.string.stat_span), spanText, Modifier.weight(1f))
            }
        }
    }
    if (compact) Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { price(); grid() }
    else Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f)) { price() }; Box(Modifier.weight(1f)) { grid() }
    }
}
@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    val c = LocalKlineColors.current
    Column(modifier) {
        Text(label, style = KlineTypography.caption, color = c.textSecondary, maxLines = 2)
        Text(value, style = KlineTypography.statValue, color = c.textPrimary, maxLines = 1)
    }
}
