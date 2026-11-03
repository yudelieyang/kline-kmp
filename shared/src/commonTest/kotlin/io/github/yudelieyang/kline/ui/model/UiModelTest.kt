package io.github.yudelieyang.kline.ui.model

import kotlin.test.*
import io.github.yudelieyang.kline.domain.Candle

class ConnectionStatusTest {
    @Test fun baseStatusesAndRetry() {
        val bases = mapOf("Connecting" to ConnectionPhase.Connecting, "Synchronizing" to ConnectionPhase.Synchronizing,
            "Live" to ConnectionPhase.Live, "Reconnecting (4)" to ConnectionPhase.Reconnecting,
            "Server restarting; reconnecting" to ConnectionPhase.Reconnecting, "Connection timed out" to ConnectionPhase.Disconnected,
            "Disconnected: unknown" to ConnectionPhase.Disconnected, "unknown" to ConnectionPhase.Disconnected)
        for ((raw, phase) in bases) { assertEquals(phase, parseConnectionStatus(raw).phase); assertEquals(raw, parseConnectionStatus(raw).raw) }
        assertEquals(4, parseConnectionStatus("Reconnecting (4)").attempt)
        assertEquals(ConnectionPhase.Reconnecting, parseConnectionStatus("Connection timed out; retry 2").phase)
        assertEquals(DisconnectReason.ConnectTimeout, parseConnectionStatus("Connection timed out; retry 2").reason)
        assertEquals(DisconnectReason.Other, parseConnectionStatus("Disconnected: Equal versions have different candle values extra").reason)
        val retry = parseConnectionStatus("Disconnected: Older candle remains unclosed; resynchronization required; retry 3")
        assertEquals(ConnectionPhase.Reconnecting, retry.phase); assertEquals(3, retry.attempt); assertEquals(DisconnectReason.Resync, retry.reason)
        assertEquals(DisconnectReason.ServerRestart, parseConnectionStatus("Server restarting; reconnecting").reason)
        assertEquals(DisconnectReason.ConnectTimeout, parseConnectionStatus("Connection timed out").reason)
    }
    @Test fun reasons() {
        val reasons = mapOf("Live data has a time gap" to DisconnectReason.TimeGap, "MESSAGE OVERFLOW" to DisconnectReason.Overflow,
            "resynchronization required" to DisconnectReason.Resync, "REST request timed out" to DisconnectReason.RestTimeout,
            "REST: failure" to DisconnectReason.RestError, "WebSocket closed" to DisconnectReason.SocketClosed,
            "WebSocket error" to DisconnectReason.SocketClosed, "Unexpected symbol or interval" to DisconnectReason.WrongStream,
            "Closed candle has fewer trades than an open version" to DisconnectReason.DataConflict,
            "Equal versions have different candle values" to DisconnectReason.DataConflict,
            "Equal versions have different" to DisconnectReason.Other, "unknown" to DisconnectReason.Other)
        reasons.forEach { (message, reason) -> assertEquals(reason, parseConnectionStatus("Disconnected: $message").reason) }
        assertEquals(DisconnectReason.TimeGap, parseConnectionStatus("Disconnected: REST: time gap").reason)
    }
}
class MarketSummaryTest {
    private fun candle(i: Int, close: Double = i.toDouble() + 1, closed: Boolean = true) = Candle(i * 60_000L, close, close + 1, close - 1, close, 2.0, closed)
    @Test fun changeAndStats() {
        assertNull(lastPriceChange(emptyList())); assertNull(loadedStats(emptyList()))
        assertNull(lastPriceChange(listOf(candle(0)))?.change)
        assertEquals(0.0, lastPriceChange(listOf(candle(0, 2.0), candle(1, 2.0)))?.change)
        assertEquals(-1.0, lastPriceChange(listOf(candle(0, 2.0), candle(1, 1.0)))?.change)
        val candles = listOf(candle(0), candle(1, 3.0))
        assertEquals(2.0, lastPriceChange(candles)?.change); assertEquals(200.0, lastPriceChange(candles)?.percent)
        assertEquals(LoadedStats(2, 0, 120_000, 4.0, 0.0, 4.0), loadedStats(candles))
        assertNull(lastPriceChange(listOf(candle(0), candle(2)))?.change)
        assertNull(lastPriceChange(listOf(candle(0, 0.0), candle(1)))?.percent)
    }
    @Test fun rowsAndAverages() {
        val candles = (0..21).map { candle(it, closed = it != 21) }
        val rows = recentClosedRows(candles)
        assertEquals(20, rows.size); assertEquals(20 * 60_000L, rows.first().openTime)
        assertEquals(60_000L, rows.last().openTime)
        assertEquals(100.0, rows.last().percent)
        assertEquals(3, recentClosedRows(candles.take(3)).size)
        assertNull(recentClosedRows(listOf(candle(0), candle(2))).first().percent)
        assertNull(recentClosedRows(listOf(candle(0, 0.0), candle(1))).first().percent)
        assertEquals(listOf(20.0, 17.5, 12.5), movingAverageSnapshot(candles))
        assertEquals(listOf(null, null, null), movingAverageSnapshot(candles.take(4)))
        assertNull(movingAverageSnapshot((0..4).map { candle(it * 2) }).first())
    }
}
class BannerTest {
    @Test fun liveAndOffline() {
        val live = parseConnectionStatus("Live")
        assertNull(bannerFor(DataSourceMode.Live, live, null, false, null))
        for (status in listOf("Connecting", "Synchronizing", "Reconnecting (2)", "Disconnected: unknown")) {
            assertTrue(bannerFor(DataSourceMode.Live, parseConnectionStatus(status), null, false, null) is BannerUi.Warning)
        }
        val warning = bannerFor(DataSourceMode.Live, parseConnectionStatus("Connecting"), 42, true, null) as BannerUi.Warning
        assertEquals(42, warning.stoppedAt); assertTrue(warning.hasData)
        val stats = LoadedStats(1, 0, 60_000, 1.0, 1.0, 1.0)
        assertEquals(BannerUi.Offline(0, 60_000), bannerFor(DataSourceMode.Offline, live, null, true, stats))
        assertNull(bannerFor(DataSourceMode.Offline, live, null, false, null))
        assertEquals(OfflineLoadState.Loading, offlineLoadState(loaded = false, failed = false))
        assertEquals(OfflineLoadState.Failed, offlineLoadState(loaded = false, failed = true))
        assertEquals(OfflineLoadState.Ready, offlineLoadState(loaded = true, failed = false))
    }
}
class DisplayFormatTest {
    @Test fun timeAndSpan() {
        assertEquals("2024-02-29 23:59:59", utcFullDateTime(1_709_251_199_000))
        assertEquals("03-01 00:00", utcShortDateTime(1_709_251_200_000))
        assertEquals("2025-01-01 00:00:00", utcFullDateTime(1_735_689_600_000))
        assertEquals(Span(0, 0, 0), formatSpan(0)); assertEquals(Span(0, 0, 59), formatSpan(59 * 60_000L))
        assertEquals(Span(0, 8, 22), formatSpan(502 * 60_000L)); assertEquals(Span(1, 2, 5), formatSpan(1565 * 60_000L))
        assertEquals("+12.35", signedNumber(12.35)); assertEquals("-19.26", signedNumber(-19.26)); assertEquals("0.00", signedNumber(0.0)); assertEquals("0.00", signedNumber(-0.001))
    }
}
