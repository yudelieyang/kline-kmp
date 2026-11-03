package io.github.yudelieyang.kline.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import io.github.yudelieyang.kline.ui.theme.*
import kline_kmp_app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun statusLabel(mode: DataSourceMode, phase: ConnectionPhase): String = stringResource(
    if (mode == DataSourceMode.Offline) Res.string.chip_offline else when (phase) {
        ConnectionPhase.Connecting -> Res.string.chip_connecting
        ConnectionPhase.Synchronizing -> Res.string.chip_syncing
        ConnectionPhase.Live -> Res.string.chip_live
        ConnectionPhase.Reconnecting -> Res.string.chip_reconnecting
        ConnectionPhase.Disconnected -> Res.string.chip_disconnected
    }
)
@Composable
fun StatusChip(mode: DataSourceMode, phase: ConnectionPhase, warning: Boolean, onClick: (() -> Unit)?) {
    val c = LocalKlineColors.current
    val label = statusLabel(mode, phase)
    val description = stringResource(Res.string.cd_status_chip, label)
    val color = if (mode == DataSourceMode.Offline) c.statusConnecting else when (phase) {
        ConnectionPhase.Connecting -> c.statusConnecting
        ConnectionPhase.Synchronizing, ConnectionPhase.Live -> c.accent
        ConnectionPhase.Reconnecting -> c.statusWarning
        ConnectionPhase.Disconnected -> c.statusError
    }
    val animation = rememberInfiniteTransition()
    val alpha by animation.animateFloat(1f, 0.35f, infiniteRepeatable(tween(600), RepeatMode.Reverse))
    val modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick).semantics { contentDescription = description }
    Box(modifier.heightIn(min = 48.dp).widthIn(min = 48.dp), contentAlignment = Alignment.Center) {
        Row(Modifier.background(c.surfaceVariant, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(if (mode == DataSourceMode.Live && phase == ConnectionPhase.Disconnected) 12.dp else 8.dp)) {
                if (mode == DataSourceMode.Live && phase == ConnectionPhase.Disconnected) {
                    drawCircle(color, style = Stroke(1.5.dp.toPx()))
                    drawLine(color, Offset(center.x, size.height * .24f), Offset(center.x, size.height * .58f), 1.5.dp.toPx())
                    drawCircle(color, 0.8.dp.toPx(), Offset(center.x, size.height * .76f))
                } else drawCircle(color.copy(alpha = if (mode == DataSourceMode.Live && phase == ConnectionPhase.Live) alpha else 1f))
            }
            Spacer(Modifier.width(6.dp)); Text(label, style = KlineTypography.chip, color = c.textPrimary)
        }
        if (warning) Box(Modifier.align(Alignment.TopEnd).padding(top = 7.dp, end = 2.dp).size(6.dp).background(c.statusWarning, RoundedCornerShape(50)))
    }
}
