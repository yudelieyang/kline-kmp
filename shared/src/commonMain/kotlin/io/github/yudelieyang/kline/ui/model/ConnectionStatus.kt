package io.github.yudelieyang.kline.ui.model

enum class ConnectionPhase { Connecting, Synchronizing, Live, Reconnecting, Disconnected }
enum class DisconnectReason { TimeGap, Overflow, Resync, RestTimeout, RestError, SocketClosed, ConnectTimeout, ServerRestart, WrongStream, DataConflict, Other }
data class ConnectionUi(val phase: ConnectionPhase, val attempt: Int?, val reason: DisconnectReason?, val raw: String)

private fun classifyReason(message: String): DisconnectReason = when {
    "time gap" in message -> DisconnectReason.TimeGap
    message.contains("overflow", ignoreCase = true) -> DisconnectReason.Overflow
    "resynchronization" in message -> DisconnectReason.Resync
    message == "REST request timed out" -> DisconnectReason.RestTimeout
    message.startsWith("REST:") -> DisconnectReason.RestError
    message == "WebSocket closed" || message == "WebSocket error" -> DisconnectReason.SocketClosed
    message == "Unexpected symbol or interval" -> DisconnectReason.WrongStream
    message == "Closed candle has fewer trades than an open version" || message == "Equal versions have different candle values" -> DisconnectReason.DataConflict
    else -> DisconnectReason.Other
}

fun parseConnectionStatus(raw: String): ConnectionUi {
    val suffix = Regex("; retry (\\d+)$").find(raw)
    val retry = suffix?.groupValues?.get(1)?.toIntOrNull()
    val base = if (suffix != null) raw.substring(0, suffix.range.first) else raw
    val reconnect = Regex("Reconnecting \\((\\d+)\\)").matchEntire(base)
    val result = when {
        base == "Connecting" -> ConnectionUi(ConnectionPhase.Connecting, null, null, raw)
        base == "Synchronizing" -> ConnectionUi(ConnectionPhase.Synchronizing, null, null, raw)
        base == "Live" -> ConnectionUi(ConnectionPhase.Live, null, null, raw)
        reconnect != null -> ConnectionUi(ConnectionPhase.Reconnecting, reconnect.groupValues[1].toIntOrNull(), null, raw)
        base == "Server restarting; reconnecting" -> ConnectionUi(ConnectionPhase.Reconnecting, null, DisconnectReason.ServerRestart, raw)
        base == "Connection timed out" -> ConnectionUi(ConnectionPhase.Disconnected, null, DisconnectReason.ConnectTimeout, raw)
        base.startsWith("Disconnected: ") -> ConnectionUi(ConnectionPhase.Disconnected, null, classifyReason(base.removePrefix("Disconnected: ")), raw)
        else -> ConnectionUi(ConnectionPhase.Disconnected, null, DisconnectReason.Other, raw)
    }
    return if (retry != null) result.copy(phase = ConnectionPhase.Reconnecting, attempt = retry) else result
}
