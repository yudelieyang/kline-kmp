package io.github.yudelieyang.kline.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yudelieyang.kline.ui.model.*
import io.github.yudelieyang.kline.data.MarketSymbol
import io.github.yudelieyang.kline.ui.theme.*
import kline_kmp_app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun TopBar(mode: DataSourceMode, phase: ConnectionPhase, warning: Boolean, dark: Boolean, symbol: MarketSymbol,
           onSymbol: (MarketSymbol) -> Unit, onDetails: () -> Unit, onTheme: () -> Unit) {
    val c = LocalKlineColors.current
    var expanded by remember { mutableStateOf(false) }
    val description = stringResource(if (mode == DataSourceMode.Offline) Res.string.symbol_offline else Res.string.symbol_select)
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            Column(Modifier.heightIn(min = 48.dp).clickable(enabled = mode == DataSourceMode.Live) { expanded = true }
                .semantics { contentDescription = description }, verticalArrangement = Arrangement.Center) {
                Text(symbol.name + if (mode == DataSourceMode.Live) " ▾" else "", style = KlineTypography.symbolTitle, color = c.textPrimary, maxLines = 1)
                Text(stringResource(Res.string.subtitle), style = KlineTypography.symbolSubtitle, color = c.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = expanded && mode == DataSourceMode.Live, onDismissRequest = { expanded = false }) {
                MarketSymbol.entries.forEach { item ->
                    DropdownMenuItem(text = { Text(item.name) }, onClick = { expanded = false; onSymbol(item) })
                }
            }
        }
        StatusChip(mode, phase, warning, onDetails)
        ThemeToggleButton(dark, onTheme)
    }
}

