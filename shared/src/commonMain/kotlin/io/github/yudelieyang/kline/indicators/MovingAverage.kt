package io.github.yudelieyang.kline.indicators

import io.github.yudelieyang.kline.domain.Candle
import io.github.yudelieyang.kline.domain.Interval

fun movingAverage(candles: List<Candle>, period: Int, indices: IntRange = candles.indices): List<Double?> {
    require(period > 0)
    val values = MutableList<Double?>(candles.size) { null }
    for (i in indices) {
        if (i !in candles.indices || i < period - 1) continue
        val start = i - period + 1
        if (candles[i].openTime - candles[start].openTime != (period - 1L) * Interval.ONE_MINUTE.durationMs) continue
        var sum = 0.0
        var min = Double.MAX_VALUE
        var max = 0.0
        for (j in start..i) {
            val close = candles[j].close
            sum += close
            min = minOf(min, close)
            max = maxOf(max, close)
        }
        val mean = if (sum.isFinite()) sum / period else {
            var scaled = 0.0
            for (j in start..i) scaled += candles[j].close / max
            (scaled / period) * max
        }
        values[i] = mean.coerceIn(min, max)
    }
    return values
}
