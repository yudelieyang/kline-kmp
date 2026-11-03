package io.github.yudelieyang.kline.data

import kotlin.test.*

class BinanceMessagesTest {
    private val valid = """{"e":"kline","E":1762041601000,"k":{"t":1762041600000,"s":"BTCUSDT","i":"1m","o":"10","h":"12","l":"9","c":"11","v":"2","n":5,"x":false}}"""

    @Test fun classifiesMessagesAndValidatesCandle() {
        val message = assertIs<BinanceMessage.Kline>(parseBinanceMessage(valid))
        assertEquals(5L, message.value.trades)
        assertEquals(1762041601000L, message.eventTime)
        assertEquals(BinanceMessage.Malformed, parseBinanceMessage("{"))
        assertEquals(BinanceMessage.Unknown, parseBinanceMessage("""{"e":"futureEvent"}"""))
        assertEquals(BinanceMessage.Shutdown, parseBinanceMessage("""{"e":"serverShutdown"}"""))
        assertEquals(BinanceMessage.WrongStream, parseBinanceMessage(valid.replace("BTCUSDT", "ETHUSDT")))
        assertEquals(BinanceMessage.Invalid, parseBinanceMessage(valid.replace("\"h\":\"12\"", "\"h\":\"NaN\"")))
    }

    @Test fun snapshotUsesServerTimeAndRejectsBadOrdering() {
        val first = """[1762041600000,"10","12","9","11","2",1762041659999,"0",5]"""
        val second = """[1762041660000,"10","12","9","11","2",1762041719999,"0",6]"""
        val result = parseSnapshot("[$first,$second]", 1762041700000)
        assertTrue(result.first().candle.isClosed)
        assertFalse(result.last().candle.isClosed)
        assertFailsWith<IllegalArgumentException> { parseSnapshot("[$second,$first]", 1762041700000) }
    }
}
