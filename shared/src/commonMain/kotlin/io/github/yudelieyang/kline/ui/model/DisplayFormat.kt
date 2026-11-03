package io.github.yudelieyang.kline.ui.model

import io.github.yudelieyang.kline.chart.priceLabel

private fun two(n: Long) = n.toString().padStart(2, '0')
fun utcClock(time: Long): String {
    val seconds = time / 1_000 % 86_400
    return "${two(seconds / 3_600)}:${two(seconds / 60 % 60)}:${two(seconds % 60)}"
}
fun utcDate(time: Long): String {
    var days = time / 86_400_000
    var year = 1970L
    fun leap(y: Long) = y % 4L == 0L && (y % 100L != 0L || y % 400L == 0L)
    while (days >= if (leap(year)) 366 else 365) { days -= if (leap(year)) 366 else 365; year++ }
    val months = intArrayOf(31, if (leap(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 0
    while (days >= months[month]) { days -= months[month]; month++ }
    return "$year-${two((month + 1).toLong())}-${two(days + 1)}"
}
fun utcMinute(time: Long) = utcClock(time).take(5)
fun utcShortDateTime(time: Long) = "${utcDate(time).substring(5)} ${utcMinute(time)}"
fun utcFullDateTime(time: Long) = "${utcDate(time)} ${utcClock(time)}"
fun signedNumber(value: Double): String {
    val label = priceLabel(value)
    return if (value > 0) "+$label" else label
}
data class Span(val days: Long, val hours: Long, val minutes: Long)
fun formatSpan(milliseconds: Long): Span {
    val minutes = milliseconds.coerceAtLeast(0) / 60_000
    return Span(minutes / 1440, minutes / 60 % 24, minutes % 60)
}
