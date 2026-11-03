package io.github.yudelieyang.kline.indicators

import io.github.yudelieyang.kline.domain.Candle
import kotlin.test.*

class MovingAverageTest {
    private fun candle(index: Int, close: Double, closed: Boolean = true) =
        Candle(index * 60_000L, close, close, close, close, 0.0, closed)

    @Test fun calculatesHandCheckedWindowsAndHandlesGaps() {
        val data = listOf(candle(0, 1.0), candle(1, 2.0), candle(2, 3.0), candle(4, 4.0), candle(5, 5.0), candle(6, 6.0))
        assertEquals(listOf(null, null, 2.0, null, null, 5.0), movingAverage(data, 3))
        assertEquals(listOf(null, null, null, null, null, 5.0), movingAverage(data, 3, 5..5))
        assertFailsWith<IllegalArgumentException> { movingAverage(data, 0) }
    }

    @Test fun handlesExtremeFiniteAndPositivePrices() {
        fun mean(values: List<Double>): Double = movingAverage(values.mapIndexed { i, v -> candle(i, v) }, values.size).last()!!
        for (values in listOf(List(5) { Double.MAX_VALUE }, List(5) { Double.MIN_VALUE }, listOf(Double.MIN_VALUE, 1.0, Double.MAX_VALUE, 2.0, 3.0), List(5) { 0.1 })) {
            val result = mean(values)
            assertTrue(result.isFinite() && result > 0.0)
            assertTrue(result in values.min()..values.max())
        }
        assertEquals(Double.MAX_VALUE, mean(List(5) { Double.MAX_VALUE }))
        assertEquals(Double.MIN_VALUE, mean(List(5) { Double.MIN_VALUE }))
        assertEquals(0.1, mean(List(5) { 0.1 }))
    }

    @Test fun unclosedCandleUsesCurrentClose() {
        val data = listOf(candle(0, 1.0), candle(1, 2.0), candle(2, 3.0, false))
        assertEquals(2.0, movingAverage(data, 3).last())
        assertEquals(3.0, movingAverage(data.dropLast(1) + candle(2, 6.0, false), 3).last())
    }
}
