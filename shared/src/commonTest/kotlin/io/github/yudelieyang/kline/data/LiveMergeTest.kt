package io.github.yudelieyang.kline.data

import io.github.yudelieyang.kline.domain.Candle
import kotlin.test.*

class LiveMergeTest {
    private fun version(time: Long, count: Long, closed: Boolean = false, price: Double = 10.0) =
        KlineVersion(Candle(time, price, price, price, price, 0.0, closed), count)

    @Test fun replacesOlderAndIgnoresDuplicates() {
        val old = version(180_000, 1)
        val newer = version(180_000, 2)
        assertEquals(listOf(newer), (mergeKline(listOf(old), newer) as MergeResult.Merged).candles)
        assertEquals(MergeResult.Ignored, mergeKline(listOf(newer), old))
        assertEquals(MergeResult.Ignored, mergeKline(listOf(newer), newer))
    }

    @Test fun closedWinsAtEqualCountAndContradictionsAreReportedBothWays() {
        val open = version(180_000, 5)
        val closed = version(180_000, 5, true)
        assertEquals(listOf(closed), (mergeKline(listOf(open), closed) as MergeResult.Merged).candles)
        assertEquals(MergeResult.Ignored, mergeKline(listOf(closed), open))
        val moreTrades = version(180_000, 6)
        assertIs<MergeResult.Conflict>(mergeKline(listOf(closed), moreTrades))
        assertIs<MergeResult.Conflict>(mergeKline(listOf(moreTrades), closed))
        assertIs<MergeResult.Conflict>(mergeKline(listOf(open), version(180_000, 5, price = 11.0)))
    }

    @Test fun insertsWithinWindowAndIgnoresEarlierUpdates() {
        val list = listOf(version(60_000, 1), version(180_000, 1))
        assertEquals(listOf(60_000L, 120_000L, 180_000L), (mergeKline(list, version(120_000, 1)) as MergeResult.Merged).candles.map { it.candle.openTime })
        assertEquals(MergeResult.Ignored, mergeKline(list, version(0, 1)))
    }

    @Test fun backoffIsCappedAndJittered() {
        assertEquals(500L, reconnectDelayMillis(0, 0.5))
        assertEquals(4_000L, reconnectDelayMillis(2, 1.0))
        assertEquals(15_000L, reconnectDelayMillis(10, 0.5))
        assertFailsWith<IllegalArgumentException> { reconnectDelayMillis(0, 0.1) }
    }

    @Test fun reconcilesBufferedUpdatesAndDetectsActualGaps() {
        val snapshot = listOf(version(60_000, 2, true), version(120_000, 5))
        val buffered = listOf(version(120_000, 4), version(180_000, 1))
        val result = reconcileSnapshot(emptyList(), snapshot, buffered)
        assertEquals(listOf(60_000L, 120_000L, 180_000L), result.map { it.candle.openTime })
        assertEquals(5L, result[1].trades)
        assertFailsWith<IllegalStateException> { reconcileSnapshot(emptyList(), snapshot, listOf(version(240_000, 1))) }
        assertFailsWith<IllegalStateException> { reconcileSnapshot(emptyList(), snapshot, listOf(version(120_000, 5, price = 11.0))) }
    }

    @Test fun reportsOnlyBufferedVersionsAcceptedAfterSnapshot() {
        val snapshot = listOf(version(60_000, 1, true), version(120_000, 5))
        val stale = version(120_000, 4)
        val next = version(180_000, 1)
        val result = reconcileSnapshotDetails(emptyList(), snapshot, listOf(stale, next))
        assertEquals(listOf(next), result.acceptedBuffered)
        assertEquals(listOf(60_000L, 120_000L, 180_000L), result.candles.map { it.candle.openTime })
    }
}
