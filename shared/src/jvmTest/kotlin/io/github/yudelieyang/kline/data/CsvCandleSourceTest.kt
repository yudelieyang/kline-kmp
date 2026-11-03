package io.github.yudelieyang.kline.data

import io.github.yudelieyang.kline.domain.Interval
import kline_kmp_app.shared.generated.resources.Res
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CsvSampleJvmTest {
    @Test
    fun loadsCommittedSample() = runBlocking {
        val path = "files/btcusdt_1m_sample.csv"
        val text = Res.readBytes(path).decodeToString()
        val expectedCount = text.lineSequence().drop(1).count { it.isNotEmpty() }
        val source = CsvCandleSource(path, Interval.ONE_MINUTE)
        val candles = source.loadHistory()
        assertEquals(expectedCount, candles.size)
        assertFalse(candles.last().isClosed)
        assertEquals(emptyList(), source.updates().toList())
    }
}
