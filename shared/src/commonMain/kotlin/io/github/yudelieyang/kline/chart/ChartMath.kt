package io.github.yudelieyang.kline.chart

import io.github.yudelieyang.kline.domain.Candle
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

data class PriceRange(val min: Double, val max: Double) {
    init {
        require(min.isFinite() && max.isFinite() && max > min)
    }
}

fun priceToY(price: Double, range: PriceRange, top: Float, bottom: Float): Float {
    val span = range.max - range.min
    val fraction = if (span.isFinite()) (price - range.min) / span
    else (price / 2 - range.min / 2) / (range.max / 2 - range.min / 2)
    return (bottom.toDouble() - fraction * (bottom.toDouble() - top.toDouble())).toFloat()
}

fun yToPrice(y: Float, range: PriceRange, top: Float, bottom: Float): Double {
    require(bottom > top)
    val fraction = ((bottom - y).toDouble() / (bottom - top).toDouble()).coerceIn(0.0, 1.0)
    return range.min * (1.0 - fraction) + range.max * fraction
}

fun nearestCandleIndex(x: Float, count: Int, width: Float, viewport: ChartViewport): Int? {
    if (count <= 0 || width <= 0f || viewport.slot <= 0f) return null
    return (count - 0.5f + (x - width - viewport.offset) / viewport.slot).roundToInt().coerceIn(0, count - 1)
}

fun visiblePriceRange(candles: List<Candle>, visible: IntRange, additionalValues: List<Double> = emptyList()): PriceRange? {
    if (visible.isEmpty()) return null
    var low = Double.MAX_VALUE
    var high = 0.0
    for (i in visible) {
        low = minOf(low, candles[i].low)
        high = maxOf(high, candles[i].high)
    }
    for (value in additionalValues) {
        low = minOf(low, value)
        high = maxOf(high, value)
    }
    val padding = if (high == low) maxOf(high * 0.001, Double.MIN_VALUE) else (high - low) * 0.05
    val min = (low - padding).coerceAtLeast(-Double.MAX_VALUE)
    val max = (high + padding).coerceAtMost(Double.MAX_VALUE)
    return PriceRange(min, max)
}

data class PriceSegment(val x0: Float, val value0: Double, val x1: Float, val value1: Double)

fun visibleMovingAverageSegments(candles: List<Candle>, values: List<Double?>, visible: IntRange, width: Float, viewport: ChartViewport): List<PriceSegment> {
    if (visible.isEmpty() || width <= 0f) return emptyList()
    val result = mutableListOf<PriceSegment>()
    val start = (visible.first - 1).coerceAtLeast(0)
    val end = (visible.last + 1).coerceAtMost(candles.lastIndex)
    for (i in start until end) {
        val a = values[i] ?: continue
        val b = values[i + 1] ?: continue
        val x0 = candleCenter(i, candles.size, width, viewport.slot, viewport.offset).toDouble()
        val x1 = candleCenter(i + 1, candles.size, width, viewport.slot, viewport.offset).toDouble()
        if (x1 < 0.0 || x0 > width.toDouble()) continue
        fun at(x: Double): Double {
            val t = ((x - x0) / (x1 - x0)).coerceIn(0.0, 1.0)
            return (a * (1.0 - t) + b * t).coerceIn(minOf(a, b), maxOf(a, b))
        }
        val left = maxOf(0.0, x0)
        val right = minOf(width.toDouble(), x1)
        result.add(PriceSegment(left.toFloat(), at(left), right.toFloat(), at(right)))
    }
    return result
}

fun visibleMovingAverageValues(candles: List<Candle>, values: List<Double?>, visible: IntRange, width: Float, viewport: ChartViewport): List<Double> =
    visibleMovingAverageSegments(candles, values, visible, width, viewport).flatMap { listOf(it.value0, it.value1) }

fun plotWidth(canvasWidth: Float, axisWidth: Float): Float = (canvasWidth - axisWidth).coerceAtLeast(0f)

fun visibleRange(count: Int, width: Float, slot: Float, offset: Float): IntRange {
    if (count <= 0 || width <= 0f || slot <= 0f) return IntRange.EMPTY
    val start = (count - ceil((width + offset).toDouble() / slot).toInt()).coerceIn(0, count)
    val end = (count - floor(offset.toDouble() / slot).toInt()).coerceIn(start, count)
    return start until end
}

fun candleCenter(index: Int, count: Int, width: Float, slot: Float, offset: Float): Float =
    width - (count - index - 0.5f) * slot + offset

fun gapMarkerIndices(candles: List<Candle>, visible: IntRange): List<Int> {
    if (visible.isEmpty()) return emptyList()
    val first = visible.first.coerceAtLeast(1)
    val last = (visible.last + 1).coerceAtMost(candles.lastIndex)
    if (first > last) return emptyList()
    return (first..last).filter { candles[it].openTime - candles[it - 1].openTime > 60_000L }
}

fun preserveAnchor(old: List<Candle>, new: List<Candle>, viewport: ChartViewport, width: Float): ChartViewport {
    if (viewport.offset == 0f || old.isEmpty() || new.isEmpty() || width <= 0f) return viewport.clamp(new.size, width)
    val visible = visibleRange(old.size, width, viewport.slot, viewport.offset)
    if (visible.isEmpty()) return viewport.clamp(new.size, width)
    val oldIndex = visible.last
    val newIndex = new.indexOfFirst { it.openTime == old[oldIndex].openTime }
    if (newIndex < 0) return viewport.clamp(new.size, width)
    val shift = (new.size - newIndex) - (old.size - oldIndex)
    return viewport.copy(offset = viewport.offset + shift * viewport.slot).clamp(new.size, width)
}

data class ChartViewport(val offset: Float, val slot: Float) {
    fun clamp(count: Int, width: Float): ChartViewport =
        copy(offset = offset.coerceIn(0f, (count * slot - width).coerceAtLeast(0f)))

    fun pan(dx: Float, count: Int, width: Float): ChartViewport =
        copy(offset = offset + dx).clamp(count, width)

    fun zoom(factor: Float, anchorX: Float, count: Int, width: Float, minSlot: Float, maxSlot: Float): ChartViewport {
        val newSlot = (slot * factor).coerceIn(minSlot, maxSlot)
        val distance = width - anchorX.coerceIn(0f, width)
        val newOffset = (newSlot / slot) * (offset + distance) - distance
        return ChartViewport(newOffset, newSlot).clamp(count, width)
    }
}
