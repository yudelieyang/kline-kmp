package io.github.yudelieyang.kline.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yudelieyang.kline.state.LiveState
import io.github.yudelieyang.kline.ui.model.*
import io.github.yudelieyang.kline.ui.theme.*
import kline_kmp_app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDetailsSheet(mode: DataSourceMode, connection: ConnectionUi, state: LiveState, stats: LoadedStats?, detail: String,
                           wsUrl: String, restUrl: String, onDismiss: () -> Unit) {
    val c = LocalKlineColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surface, scrimColor = c.scrim) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 28.dp)) {
            Text(stringResource(Res.string.sheet_title), style = KlineTypography.sheetTitle, color = c.textPrimary)
            SheetRow(stringResource(Res.string.sheet_status)) { StatusChip(mode, connection.phase, state.malformed + state.invalid + state.unknown > 0, null) }
            SheetRow(stringResource(Res.string.sheet_detail)) { Text(detail, style = KlineTypography.sheetBody, color = c.textPrimary) }
            SheetRow(stringResource(Res.string.sheet_raw)) { SelectionContainer { Text(state.status, style = KlineTypography.mono, color = c.textPrimary) } }
            SheetRow(stringResource(Res.string.sheet_last_update)) { Text(state.lastUpdate?.let { utcFullDateTime(it) + " UTC" } ?: "--", style = KlineTypography.sheetBody, color = c.textPrimary) }
            SheetRow("WebSocket") { SelectionContainer { Text(wsUrl, style = KlineTypography.mono, color = c.textPrimary) } }
            SheetRow("REST") { SelectionContainer { Text(restUrl, style = KlineTypography.mono, color = c.textPrimary) } }
            if (mode == DataSourceMode.Offline) Text(stringResource(Res.string.sheet_source_offline), style = KlineTypography.caption, color = c.textSecondary)
            SheetRow(stringResource(Res.string.sheet_loaded)) {
                Text(stats?.let { stringResource(Res.string.sheet_loaded_value, it.count, utcShortDateTime(it.startTime), utcShortDateTime(it.endTime)) } ?: "--", style = KlineTypography.sheetBody, color = c.textPrimary)
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(Res.string.sheet_counts) + if (mode == DataSourceMode.Offline) " " + stringResource(Res.string.sheet_counts_live_suffix) else "", style = KlineTypography.caption, color = c.textSecondary)
            Row(Modifier.fillMaxWidth()) {
                val labels = listOf(Res.string.count_malformed, Res.string.count_invalid, Res.string.count_unknown, Res.string.count_ignored)
                val values = listOf(state.malformed, state.invalid, state.unknown, state.ignored)
                labels.forEachIndexed { i, label -> Column(Modifier.weight(1f)) {
                    Text(values[i].toString(), style = KlineTypography.statValue, color = c.textPrimary)
                    Text(stringResource(label), style = KlineTypography.caption, color = c.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                } }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(Res.string.sheet_footer), style = KlineTypography.caption, color = c.textTertiary)
        }
    }
}
@Composable
private fun SheetRow(label: String, content: @Composable RowScope.() -> Unit) {
    val c = LocalKlineColors.current
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, Modifier.width(72.dp), style = KlineTypography.sheetBody, color = c.textSecondary)
        content()
    }
}
