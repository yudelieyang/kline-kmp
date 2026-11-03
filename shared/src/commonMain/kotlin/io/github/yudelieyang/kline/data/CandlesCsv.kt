package io.github.yudelieyang.kline.data

import io.github.yudelieyang.kline.domain.Candle
import io.github.yudelieyang.kline.domain.Interval

class CsvParseException(
    val lineNumber: Int,
    val column: String?,
    val reason: String,
    val rawLine: String,
) : IllegalArgumentException("line $lineNumber${column?.let { ", column $it" } ?: ""}: $reason")

fun parseCandlesCsv(text: String, interval: Interval): List<Candle> {
    val lines = text.removePrefix("\uFEFF").split('\n').map { it.removeSuffix("\r") }
    val header = "openTime,open,high,low,close,volume,isClosed"
    if (lines.first() != header) throw CsvParseException(1, null, "invalid header", lines.first())
    val candles = mutableListOf<Candle>()
    val rows = if (lines.last().isEmpty() && lines.size > 1) lines.dropLast(1) else lines
    for ((index, line) in rows.drop(1).withIndex()) {
        val lineNumber = index + 2
        fun fail(column: String?, reason: String): Nothing =
            throw CsvParseException(lineNumber, column, reason, line)
        val fields = line.split(',')
        if (fields.size != 7) fail(null, "expected 7 columns")
        val time = fields[0].toLongOrNull() ?: fail("openTime", "invalid integer")
        if (time < 100_000_000_000L || time >= 10_000_000_000_000L) fail("openTime", "outside supported millisecond range")
        if (time % interval.durationMs != 0L) fail("openTime", "not aligned to interval")
        if (candles.isNotEmpty() && time <= candles.last().openTime) fail("openTime", "must be strictly increasing")
        fun number(index: Int, name: String): Double {
            val value = fields[index].toDoubleOrNull() ?: fail(name, "invalid number")
            if (!value.isFinite()) fail(name, "not a finite number")
            return value
        }
        val open = number(1, "open")
        val high = number(2, "high")
        val low = number(3, "low")
        val close = number(4, "close")
        val volume = number(5, "volume")
        if (open <= 0) fail("open", "must be positive")
        if (high <= 0) fail("high", "must be positive")
        if (low <= 0) fail("low", "must be positive")
        if (close <= 0) fail("close", "must be positive")
        if (volume < 0) fail("volume", "must be nonnegative")
        if (high < maxOf(open, low, close)) fail("high", "must be the highest price")
        if (low > minOf(open, high, close)) fail("low", "must be the lowest price")
        val closed = fields[6].toBooleanStrictOrNull() ?: fail("isClosed", "expected true or false")
        candles.add(Candle(time, open, high, low, close, volume, closed))
    }
    return candles
}
