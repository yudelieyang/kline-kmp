package io.github.yudelieyang.kline.chart

import io.github.yudelieyang.kline.domain.Candle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChartMathTest {
    private fun candle(low: Double, high: Double) = Candle(1762041600000, low, high, low, high, 0.0, true)

    @Test fun movingAveragePanelDistinguishesUnavailableReasons() {
        assertEquals("MA5 -- (insufficient history)", movingAveragePanelLabel(5, 3, null))
        assertEquals("MA5 -- (missing period)", movingAveragePanelLabel(5, 4, null))
        assertEquals("MA5 12.50", movingAveragePanelLabel(5, 4, 12.5))
    }

    @Test fun gapMarkersUseActualTimeGaps() {
        val data = listOf(0L, 60_000L, 180_000L, 240_000L).map { candle(1.0, 2.0).copy(openTime = it) }
        assertEquals(listOf(2), gapMarkerIndices(data, 1..2))
        assertEquals(listOf(2), gapMarkerIndices(data, 1..1))
        assertTrue(gapMarkerIndices(data, 2..3).contains(2))
        assertTrue(gapMarkerIndices(data, IntRange.EMPTY).isEmpty())
    }

    @Test fun crosshairMapsPointerToCandleAndPrice() {
        val view = ChartViewport(0f, 10f)
        assertEquals(9, nearestCandleIndex(99f, 10, 100f, view))
        assertEquals(0, nearestCandleIndex(-100f, 10, 100f, view))
        assertEquals(null, nearestCandleIndex(1f, 0, 100f, view))
        val prices = PriceRange(100.0, 200.0)
        assertEquals(200.0, yToPrice(0f, prices, 0f, 100f))
        assertEquals(150.0, yToPrice(50f, prices, 0f, 100f))
        assertEquals(100.0, yToPrice(100f, prices, 0f, 100f))
        assertEquals(50f, priceToY(yToPrice(50f, prices, 0f, 100f), prices, 0f, 100f))
    }

    @Test fun movingAverageAxisUsesClippedBoundaryValues() {
        val candles = List(5) { candle(1.0, 2.0) }
        val values = listOf(null, 10.0, 30.0, 50.0, 90.0)
        val view = ChartViewport(10f, 10f)
        val visible = visibleRange(5, 20f, view.slot, view.offset)
        assertEquals(2 until 4, visible)
        val included = visibleMovingAverageValues(candles, values, visible, 20f, view)
        assertEquals(listOf(20.0, 30.0, 30.0, 50.0, 50.0, 70.0), included)
        assertEquals(listOf(PriceSegment(0f, 20.0, 5f, 30.0), PriceSegment(5f, 30.0, 15f, 50.0), PriceSegment(15f, 50.0, 20f, 70.0)), visibleMovingAverageSegments(candles, values, visible, 20f, view))
        val prices = visiblePriceRange(candles, visible, included)!!
        assertEquals(-2.45, prices.min, 0.00001)
        assertEquals(73.45, prices.max, 0.00001)
        assertTrue(visibleMovingAverageValues(candles, listOf(null, null, 30.0, null, null), visible, 20f, view).isEmpty())
    }

    @Test fun preservesHistoricalAnchorWhenCandlesAreInserted() {
        val old = (0 until 100).map { candle(1.0, 2.0).copy(openTime = it * 60_000L) }
        val view = ChartViewport(200f, 10f)
        val anchor = visibleRange(old.size, 200f, view.slot, view.offset).last
        val x = candleCenter(anchor, old.size, 200f, view.slot, view.offset)
        val newer = old + candle(1.0, 2.0).copy(openTime = 100 * 60_000L)
        val moved = preserveAnchor(old, newer, view, 200f)
        assertEquals(x, candleCenter(anchor, newer.size, 200f, moved.slot, moved.offset))
        val earlier = listOf(candle(1.0, 2.0).copy(openTime = -60_000L)) + old
        val unchanged = preserveAnchor(old, earlier, view, 200f)
        assertEquals(x, candleCenter(anchor + 1, earlier.size, 200f, unchanged.slot, unchanged.offset))
        assertEquals(0f, preserveAnchor(old, newer, ChartViewport(0f, 10f), 200f).offset)
    }

    @Test
    fun mapsPricesWithDoublePrecision() {
        val range = PriceRange(68000.0, 68001.0)
        assertEquals(20f, priceToY(68001.0, range, 20f, 620f))
        assertEquals(620f, priceToY(68000.0, range, 20f, 620f))
        assertEquals(320f, priceToY(68000.5, range, 20f, 620f))
        assertTrue(priceToY(68001.1, range, 20f, 620f) < 20f)
        assertFailsWith<IllegalArgumentException> { PriceRange(1.0, 1.0) }
    }

    @Test
    fun visibleCandlesIncludePartialEdgesAndRespectAxisWidth() {
        assertEquals(400 until 500, visibleRange(500, 1000f, 10f, 0f))
        assertEquals(397 until 498, visibleRange(500, 1000f, 10f, 25f))
        assertEquals(0 until 100, visibleRange(500, 1000f, 10f, 4000f))
        assertEquals(0 until 2, visibleRange(2, 1000f, 10f, 0f))
        assertTrue(visibleRange(0, 1000f, 10f, 0f).isEmpty())
        assertEquals(300f, plotWidth(400f, 100f))
        assertEquals(470 until 500, visibleRange(500, plotWidth(400f, 100f), 10f, 0f))
    }

    @Test
    fun priceRangeUsesVisibleDataAndHandlesFlatValues() {
        val candles = listOf(candle(1.0, 100.0), candle(10.0, 20.0), candle(15.0, 15.0))
        assertEquals(PriceRange(9.5, 20.5), visiblePriceRange(candles, 1..1))
        val flat = visiblePriceRange(candles, 2..2)!!
        assertTrue(flat.min < 15.0 && flat.max > 15.0)
        assertEquals(null, visiblePriceRange(candles, IntRange.EMPTY))
        val tiny = visiblePriceRange(listOf(candle(Double.MIN_VALUE, Double.MIN_VALUE)), 0..0)!!
        assertTrue(tiny.min < tiny.max)
        val huge = visiblePriceRange(listOf(candle(Double.MAX_VALUE, Double.MAX_VALUE)), 0..0)!!
        assertTrue(huge.min < huge.max && huge.max.isFinite())
    }

    @Test
    fun zoomUsesClampedWidthAndOnlyMovesAnchorAtDataEdge() {
        val view = ChartViewport(100f, 10f)
        val zoomed = view.zoom(2f, 200f, 500, 400f, 2f, 40f)
        assertEquals((400f - 200f + view.offset) / view.slot, (400f - 200f + zoomed.offset) / zoomed.slot, 0.0001f)
        val capped = ChartViewport(100f, 40f).zoom(2f, 200f, 500, 400f, 2f, 40f)
        assertEquals(ChartViewport(100f, 40f), capped)
        val beforeCap = ChartViewport(100f, 30f)
        val afterCap = beforeCap.zoom(2f, 200f, 500, 400f, 2f, 40f)
        assertEquals(40f, afterCap.slot)
        assertEquals((200f + beforeCap.offset) / beforeCap.slot, (200f + afterCap.offset) / afterCap.slot, 0.0001f)
        val minCapped = ChartViewport(100f, 2f).zoom(0.5f, 200f, 500, 400f, 2f, 40f)
        assertEquals(ChartViewport(100f, 2f), minCapped)
        val beforeMin = ChartViewport(200f, 3f)
        val afterMin = beforeMin.zoom(0.5f, 200f, 500, 400f, 2f, 40f)
        assertEquals(2f, afterMin.slot)
        assertEquals((200f + beforeMin.offset) / beforeMin.slot, (200f + afterMin.offset) / afterMin.slot, 0.0001f)
        val edge = ChartViewport(0f, 10f).zoom(0.5f, 0f, 500, 400f, 2f, 40f)
        assertEquals(0f, edge.offset)
        assertEquals(0f, view.pan(-1000f, 500, 400f).offset)
        assertEquals(4600f, view.pan(10000f, 500, 400f).offset)
    }

    @Test
    fun formatsNumbersWithoutOverflowOrFalseZeroVolume() {
        assertEquals("0.13", priceLabel(0.125))
        assertEquals("10000000000000.00", priceLabel(1e13))
        assertTrue(priceLabel(70_368_744_177_664.0).contains('e'))
        assertTrue(priceLabel(1e300).contains('e'))
        assertEquals("0", volumeLabel(0.0))
        assertEquals("12.5", volumeLabel(12.48312))
        assertEquals("0.000412", volumeLabel(0.0004123))
        assertEquals("1.25K", volumeLabel(1250.0))
        assertTrue(volumeLabel(Double.MIN_VALUE) != "0")
    }

    @Test
    fun utcDatesAndTickSteps() {
        assertEquals("05:07", utcLabel(1793596032896))
        assertEquals("11-02 05:07 UTC", utcDateTimeLabel(1793596032896))
        assertEquals("02-29", utcLabel(1835395200000))
        assertEquals("12-31", utcLabel(1830211200000))
        assertEquals("01-01", utcLabel(1830297600000))
        assertEquals(2, timeStepMinutes(40f, 64f))
        assertEquals(60, timeStepMinutes(2f, 64f))
    }
}
