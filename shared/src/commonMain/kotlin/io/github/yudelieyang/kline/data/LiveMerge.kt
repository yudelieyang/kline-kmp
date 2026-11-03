package io.github.yudelieyang.kline.data

import io.github.yudelieyang.kline.domain.Candle

data class KlineVersion(val candle: Candle, val trades: Long)

sealed interface MergeResult {
    data class Merged(val candles: List<KlineVersion>) : MergeResult
    data object Ignored : MergeResult
    data class Conflict(val reason: String) : MergeResult
}

fun mergeKline(candles: List<KlineVersion>, update: KlineVersion, windowed: Boolean = true): MergeResult {
    val time = update.candle.openTime
    if (windowed && candles.isNotEmpty() && time < candles.last().candle.openTime - 120_000L) return MergeResult.Ignored
    val index = candles.indexOfFirst { it.candle.openTime >= time }
    if (index < 0) return MergeResult.Merged(candles + update)
    val previous = candles[index]
    if (previous.candle.openTime != time) {
        return MergeResult.Merged(candles.toMutableList().apply { add(index, update) })
    }
    if (previous.candle.isClosed != update.candle.isClosed) {
        val closed = if (previous.candle.isClosed) previous else update
        val open = if (previous.candle.isClosed) update else previous
        if (closed.trades < open.trades) return MergeResult.Conflict("Closed candle has fewer trades than an open version")
    }
    if (previous.candle.isClosed == update.candle.isClosed && previous.trades == update.trades) {
        return if (previous.candle == update.candle) MergeResult.Ignored
        else MergeResult.Conflict("Equal versions have different candle values")
    }
    val replace = if (previous.candle.isClosed != update.candle.isClosed) update.candle.isClosed
    else update.trades > previous.trades
    return if (replace) MergeResult.Merged(candles.toMutableList().apply { set(index, update) }) else MergeResult.Ignored
}

fun reconnectDelayMillis(failures: Int, jitter: Double): Long {
    require(jitter in 0.5..1.0)
    val base = minOf(30_000L, 1_000L * (1L shl failures.coerceIn(0, 5)))
    return (base * jitter).toLong()
}

data class ReconciledSnapshot(val candles: List<KlineVersion>, val acceptedBuffered: List<KlineVersion>)

fun reconcileSnapshot(previous: List<KlineVersion>, snapshot: List<KlineVersion>, buffered: List<KlineVersion>): List<KlineVersion> =
    reconcileSnapshotDetails(previous, snapshot, buffered).candles

fun reconcileSnapshotDetails(previous: List<KlineVersion>, snapshot: List<KlineVersion>, buffered: List<KlineVersion>): ReconciledSnapshot {
    require(snapshot.isNotEmpty()) { "Empty snapshot" }
    if (buffered.isNotEmpty() && buffered.minOf { it.candle.openTime } > snapshot.last().candle.openTime + 60_000L) {
        error("Snapshot and buffered updates have a time gap")
    }
    var result = previous
    val accepted = mutableListOf<KlineVersion>()
    for ((index, item) in (snapshot + buffered).withIndex()) when (val merged = mergeKline(result, item, false)) {
        is MergeResult.Merged -> {
            result = merged.candles
            if (index >= snapshot.size) accepted.add(item)
        }
        is MergeResult.Conflict -> error(merged.reason)
        MergeResult.Ignored -> Unit
    }
    if (result.zipWithNext().any { (a, b) -> b.candle.openTime - a.candle.openTime > 60_000L }) error("History contains a time gap")
    return ReconciledSnapshot(result, accepted)
}
