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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MaRow(values: List<Double?>, unclosed: Boolean) {
    val c = LocalKlineColors.current
    FlowRow(Modifier.fillMaxWidth().heightIn(min = 24.dp).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(5, 10, 20).forEachIndexed { i, period ->
            Text("MA$period ${values[i]?.let(::priceLabel) ?: "--"}", style = KlineTypography.maValue, color = c.chart.averages[i])
        }
        if (unclosed) Text(stringResource(Res.string.ma_unclosed), style = KlineTypography.caption, color = c.textTertiary)
    }
}
