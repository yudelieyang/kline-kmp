package io.github.yudelieyang.kline.chart

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong

private fun fixed(value: Double, decimals: Int): String {
    val negative = value < 0
    val magnitude = abs(value)
    val scale = 10.0.pow(decimals)
    var whole = floor(magnitude).toLong()
    var fraction = ((magnitude - whole) * scale).roundToLong()
    if (fraction == scale.toLong()) { whole++; fraction = 0 }
    val sign = if (negative && (whole != 0L || fraction != 0L)) "-" else ""
    return if (decimals == 0) "$sign$whole" else "$sign$whole.${fraction.toString().padStart(decimals, '0')}"
}

fun scientific(value: Double): String {
    if (value == 0.0) return "0"
    val magnitude = abs(value)
    var exponent = floor(log10(magnitude)).toInt()
    var mantissa = if (exponent < -300) magnitude * 1e300 / 10.0.pow(exponent + 300)
    else magnitude / 10.0.pow(exponent)
    if ((mantissa * 100).roundToLong() >= 1000) { mantissa /= 10; exponent++ }
    val sign = if (value < 0) "-" else ""
    return "$sign${fixed(mantissa, 2)}e$exponent"
}

fun priceLabel(value: Double): String =
    if (abs(value) >= 70_368_744_177_664.0) scientific(value) else fixed(value, 2)

fun movingAveragePanelLabel(period: Int, index: Int, value: Double?): String = when {
    value != null -> "MA$period ${priceLabel(value)}"
    index < period - 1 -> "MA$period -- (insufficient history)"
    else -> "MA$period -- (missing period)"
}

fun volumeLabel(value: Double): String {
    if (value == 0.0) return "0"
    if (value < 0.0001 || value >= 1e15) return scientific(value)
    val divisor = when {
        value >= 1e9 -> 1e9
        value >= 1e6 -> 1e6
        value >= 1e3 -> 1e3
        else -> 1.0
    }
    val suffix = when (divisor) { 1e9 -> "B"; 1e6 -> "M"; 1e3 -> "K"; else -> "" }
    val scaled = value / divisor
    val decimals = (2 - floor(log10(scaled)).toInt()).coerceIn(0, 8)
    val label = fixed(scaled, decimals)
    return (if (decimals > 0) label.trimEnd('0').trimEnd('.') else label) + suffix
}

private val timeSteps = listOf(1, 2, 5, 10, 15, 30, 60, 120, 240, 360, 720, 1440)

fun timeStepMinutes(slotPx: Float, minimumSpacingPx: Float): Int =
    timeSteps.firstOrNull { it * slotPx >= minimumSpacingPx } ?: timeSteps.last()

fun utcLabel(timeMs: Long, dateOnly: Boolean = false): String {
    val minutes = timeMs / 60_000
    val minutesOfDay = (minutes % 1440).toInt()
    if (!dateOnly && minutesOfDay != 0) {
        return "${(minutesOfDay / 60).toString().padStart(2, '0')}:${(minutesOfDay % 60).toString().padStart(2, '0')}"
    }
    var days = (minutes / 1440).toInt()
    var year = 1970
    fun leap(y: Int) = y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)
    while (true) {
        val yearDays = if (leap(year)) 366 else 365
        if (days < yearDays) break
        days -= yearDays
        year++
    }
    val months = intArrayOf(31, if (leap(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 0
    while (days >= months[month]) { days -= months[month]; month++ }
    return "${(month + 1).toString().padStart(2, '0')}-${(days + 1).toString().padStart(2, '0')}"
}

fun utcDateTimeLabel(timeMs: Long): String {
    val day = timeMs - timeMs % 86_400_000L
    val minute = (timeMs % 86_400_000L) / 60_000L
    return "${utcLabel(day, true)} ${(minute / 60).toString().padStart(2, '0')}:${(minute % 60).toString().padStart(2, '0')} UTC"
}
