package io.github.yudelieyang.kline.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.yudelieyang.kline.ui.model.*
import io.github.yudelieyang.kline.ui.theme.*
import kline_kmp_app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun reasonText(reason: DisconnectReason?): String = stringResource(when (reason) {
    null -> Res.string.reason_none
    DisconnectReason.TimeGap -> Res.string.reason_time_gap
    DisconnectReason.Overflow -> Res.string.reason_overflow
    DisconnectReason.Resync -> Res.string.reason_resync
    DisconnectReason.RestTimeout -> Res.string.reason_rest_timeout
    DisconnectReason.RestError -> Res.string.reason_rest_error
    DisconnectReason.SocketClosed -> Res.string.reason_socket_closed
    DisconnectReason.ConnectTimeout -> Res.string.reason_connect_timeout
    DisconnectReason.ServerRestart -> Res.string.reason_server_restart
    DisconnectReason.WrongStream -> Res.string.reason_wrong_stream
    DisconnectReason.DataConflict -> Res.string.reason_data_conflict
    DisconnectReason.Other -> Res.string.reason_other
})
@Composable
fun warningText(warning: BannerUi.Warning): String {
    val connection = warning.connection
    val reason = reasonText(connection.reason)
    val main = when (connection.phase) {
        ConnectionPhase.Connecting -> stringResource(if (warning.hasData) Res.string.banner_reconnecting else Res.string.banner_connecting)
        ConnectionPhase.Synchronizing -> stringResource(if (warning.hasData) Res.string.banner_resyncing else Res.string.banner_syncing)
        ConnectionPhase.Reconnecting -> when {
            connection.attempt != null -> stringResource(Res.string.banner_retry, reason, connection.attempt)
            connection.reason == DisconnectReason.ServerRestart -> stringResource(Res.string.banner_server_restart)
            else -> stringResource(Res.string.banner_reconnecting)
        }
        ConnectionPhase.Disconnected -> stringResource(Res.string.banner_disconnected, reason)
        ConnectionPhase.Live -> stringResource(Res.string.sheet_live_ok)
    }
    return main + (warning.stoppedAt?.let { " " + stringResource(Res.string.stopped_at, utcClock(it)) } ?: "")
}
@Composable
fun StatusBanner(text: String?, offline: Boolean, onAction: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalKlineColors.current
    AnimatedVisibility(text != null, modifier, enter = expandVertically(tween(200)) + fadeIn(tween(200)), exit = shrinkVertically(tween(200)) + fadeOut(tween(200))) {
        val bg = if (offline) c.bannerInfoBg else c.bannerWarningBg
        val ink = if (offline) c.bannerInfoText else c.bannerWarningText
        val border = if (offline) c.bannerInfoBorder else c.bannerWarningBorder
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).border(1.dp, border, RoundedCornerShape(8.dp))
            .background(bg, RoundedCornerShape(8.dp)).heightIn(min = 40.dp).padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(if (offline) c.statusConnecting else c.statusWarning, RoundedCornerShape(50)))
            Spacer(Modifier.width(8.dp))
            Text(text ?: "", Modifier.weight(1f), style = KlineTypography.banner, color = ink, maxLines = 3)
            Box(Modifier.heightIn(min = 48.dp).clickable(onClick = onAction).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(if (offline) Res.string.action_back_live else Res.string.action_view_offline), style = KlineTypography.banner.copy(fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline), color = ink)
            }
        }
    }
}
