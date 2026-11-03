package io.github.yudelieyang.kline.state

import io.github.yudelieyang.kline.data.KlineVersion
import io.github.yudelieyang.kline.data.MergeResult
import io.github.yudelieyang.kline.data.mergeKline
import io.github.yudelieyang.kline.data.reconcileSnapshotDetails
import io.github.yudelieyang.kline.domain.Candle
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class LiveMarketTest {
    @Test fun acceptedMessagesAdvanceLastUpdateWithoutMovingBackward() {
        val old = KlineVersion(Candle(180_000L, 10.0, 10.0, 10.0, 10.0, 0.0, false), 1)
        val newer = old.copy(trades = 2)
        val state = LiveState(candles = listOf(old.candle), lastUpdate = 200L)

        val accepted = mergeKline(listOf(old), newer)
        assertIs<MergeResult.Merged>(accepted)
        assertEquals(250L, liveStateAfterMerge(state, accepted, 250L).lastUpdate)
        assertEquals(200L, liveStateAfterMerge(state, accepted, 150L).lastUpdate)
        assertEquals(150L, liveStateAfterMerge(LiveState(), accepted, 150L).lastUpdate)

        val ignored = mergeKline(listOf(newer), old)
        assertIs<MergeResult.Ignored>(ignored)
        val unchanged = liveStateAfterMerge(state, ignored, 300L)
        assertEquals(200L, unchanged.lastUpdate)
        assertEquals(state.candles, unchanged.candles)
        assertEquals(1, unchanged.ignored)
    }

    @Test fun resetRequiresSixtySecondsAfterHandshakeAndAMessage() {
        val clock = TestTimeSource()
        clock += 9.seconds
        val connectedAt = clock.markNow()
        clock += 51.seconds
        assertFalse(shouldResetFailures(connectedAt, true))
        assertFalse(shouldResetFailures(null, true))
        clock += 9.seconds
        assertFalse(shouldResetFailures(connectedAt, false))
        assertTrue(shouldResetFailures(connectedAt, true))
    }

    @Test fun snapshotOnlyCountsBufferedMessagesAcceptedDuringReconciliation() {
        val old = KlineVersion(Candle(180_000L, 10.0, 10.0, 10.0, 10.0, 0.0, false), 1)
        val snapshot = old.copy(trades = 2)
        val next = old.copy(candle = old.candle.copy(openTime = 240_000L))
        val result = reconcileSnapshotDetails(emptyList(), listOf(snapshot), listOf(old, next))
        val times = mapOf(180_000L to 900L, 240_000L to 300L)
        assertEquals(300L, lastAcceptedBufferedTime(200L, result.acceptedBuffered, times))
        assertEquals(400L, lastAcceptedBufferedTime(400L, result.acceptedBuffered, times))
        assertNull(lastAcceptedBufferedTime(null, emptyList(), times))
    }

    @Test fun disconnectedStatusDoesNotDisplayNullOrBlankMessages() {
        assertEquals("Disconnected: Connection error", disconnectedStatus(Exception()))
        assertEquals("Disconnected: Connection error", disconnectedStatus(Exception("  ")))
        assertEquals("Disconnected: Network unavailable", disconnectedStatus(Exception("Network unavailable")))
    }

    @Test fun rejectsEventsFromEarlierSessions() {
        assertFalse(isCurrentSession(1, 2))
        assertTrue(isCurrentSession(2, 2))
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun cancellationStopsTheOwner() = runTest {
        val holder = LiveMarketHolder(wsUrl = "invalid://example")
        val job = launch { holder.run() }
        runCurrent()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
    }
}
