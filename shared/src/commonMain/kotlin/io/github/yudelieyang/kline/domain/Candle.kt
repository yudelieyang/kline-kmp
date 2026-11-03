package io.github.yudelieyang.kline.domain

data class Candle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val isClosed: Boolean,
)

enum class Interval(val code: String, val durationMs: Long) {
    ONE_MINUTE("1m", 60_000L),
}
