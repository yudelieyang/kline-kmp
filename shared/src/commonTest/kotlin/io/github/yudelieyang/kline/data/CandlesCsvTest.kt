package io.github.yudelieyang.kline.data

import io.github.yudelieyang.kline.domain.Interval
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CandlesCsvTest {
    private val header = "openTime,open,high,low,close,volume,isClosed"
    private val row = "1762041600000,10,12,9,11,0,true"
    private val interval = Interval.ONE_MINUTE

    private fun parse(vararg rows: String) = parseCandlesCsv((listOf(header) + rows).joinToString("\n"), interval)

    @Test
    fun parsesNormalRowsAndAllowsGaps() {
        val result = parse(row, "1762041720000,11,13,10,12,1.5,false")
        assertEquals(2, result.size)
        assertEquals(10.0, result[0].open)
        assertEquals(0.0, result[0].volume)
        assertTrue(result[0].isClosed)
        assertFalse(result[1].isClosed)
    }

    @Test
    fun acceptsBomCrLfAndFinalNewline() {
        assertEquals(1, parseCandlesCsv("\uFEFF$header\r\n$row\r\n", interval).size)
    }

    @Test
    fun rejectsIncorrectHeader() {
        val error = assertFailsWith<CsvParseException> { parseCandlesCsv("wrong\n$row", interval) }
        assertEquals(1, error.lineNumber)
        assertEquals("wrong", error.rawLine)
    }

    @Test
    fun rejectsBadFieldsWithLocation() {
        val invalid = listOf(
            "1762041600000,10,abc,9,11,0,true" to "high",
            "1762041600000,10,NaN,9,11,0,true" to "high",
            "1762041600000,10,Infinity,9,11,0,true" to "high",
            "1762041600000,10,12,9,11,0,TRUE" to "isClosed",
            "1762041600000,0,12,9,11,0,true" to "open",
            "1762041600000,10,0,9,11,0,true" to "high",
            "1762041600000,10,12,0,11,0,true" to "low",
            "1762041600000,10,12,9,0,0,true" to "close",
            "1762041600000,10,12,9,11,-1,true" to "volume",
            "1762041600000,10,10,9,11,0,true" to "high",
            "1762041600000,10,12,11,10,0,true" to "low",
            "not-a-time,10,12,9,11,0,true" to "openTime",
        )
        for ((badRow, column) in invalid) {
            val earlierRow = row.replace("1762041600000", "1762041540000")
            val error = assertFailsWith<CsvParseException> { parse(earlierRow, badRow) }
            assertEquals(3, error.lineNumber)
            assertEquals(column, error.column)
            assertEquals(badRow, error.rawLine)
        }
    }

    @Test
    fun rejectsWrongColumnCountAndBlankLines() {
        for (badRow in listOf("", "1,2,3", "$row,extra")) {
            val error = assertFailsWith<CsvParseException> { parse(row, badRow, row) }
            assertEquals(3, error.lineNumber)
        }
    }

    @Test
    fun validatesTimeRangeAlignmentAndOrder() {
        val invalid = listOf(
            "99999960000" to "outside supported millisecond range",
            "10000000000000" to "outside supported millisecond range",
            "1762020000" to "outside supported millisecond range",
            "1762041600000000" to "outside supported millisecond range",
            "1762041600001" to "not aligned to interval",
            "1762041600000" to "must be strictly increasing",
            "1762041540000" to "must be strictly increasing",
        )
        for ((time, reason) in invalid) {
            val error = assertFailsWith<CsvParseException> { parse(row, "$time,10,12,9,11,0,true") }
            assertEquals(3, error.lineNumber)
            assertEquals("openTime", error.column)
            assertEquals(reason, error.reason)
        }
        assertEquals(1, parse("100000020000,10,12,9,11,0,true").size)
        assertEquals(1, parse("9999999960000,10,12,9,11,0,true").size)
    }

    @Test
    fun acceptsSmallestPositiveDouble() {
        assertEquals(1, parse("1762041600000,4.9E-324,4.9E-324,4.9E-324,4.9E-324,0,true").size)
    }
}
