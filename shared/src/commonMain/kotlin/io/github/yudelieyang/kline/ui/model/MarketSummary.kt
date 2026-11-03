package io.github.yudelieyang.kline.ui.model

import io.github.yudelieyang.kline.domain.Candle
import io.github.yudelieyang.kline.indicators.movingAverage

data class PriceChange(val price: Double, val change: Double?, val percent: Double?)
data class LoadedStats(val count: Int, val startTime: Long, val endTime: Long, val high: Double, val low: Double, val volume: Double)
data class RecentRow(val openTime: Long, val close: Double, val percent: Double?, val volume: Double)

private fun previous(candles: List<Candle>, index: Int): Candle? =
    candles.getOrNull(index - 1)?.takeIf { candles[index].openTime - it.openTime == 60_000L }
private fun percent(close: Double, previous: Candle?): Double? =
    previous?.let { (close - it.close) / it.close * 100.0 }?.takeIf { it.isFinite() }

fun lastPriceChange(candles: List<Candle>): PriceChange? {
    val last = candles.lastOrNull() ?: return null
    val previous = previous(candles, candles.lastIndex)
    return PriceChange(last.close, previous?.let { last.close - it.close }, percent(last.close, previous))
}
fun loadedStats(candles: List<Candle>): LoadedStats? {
    if (candles.isEmpty()) return null
    return LoadedStats(candles.size, candles.first().openTime, candles.last().openTime + 60_000L,
        candles.maxOf { it.high }, candles.minOf { it.low }, candles.sumOf { it.volume })
}
fun recentClosedRows(candles: List<Candle>, limit: Int = 20): List<RecentRow> =
    candles.indices.reversed().asSequence().filter { candles[it].isClosed }.take(limit.coerceAtLeast(0)).map { i ->
        val candle = candles[i]
        RecentRow(candle.openTime, candle.close, percent(candle.close, previous(candles, i)), candle.volume)
    }.toList()
fun movingAverageSnapshot(candles: List<Candle>): List<Double?> = listOf(5, 10, 20).map { period ->
    if (candles.isEmpty()) null else movingAverage(candles, period, candles.lastIndex..candles.lastIndex)[candles.lastIndex]
}
