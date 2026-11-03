package io.github.yudelieyang.kline.data

import io.github.yudelieyang.kline.state.LiveMarketHolder
import io.github.yudelieyang.kline.state.LiveState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarketSymbolTest {
    @Test
    fun buildsUrlsForEachSymbol() {
        for (symbol in MarketSymbol.entries) {
            assertEquals("wss://example/ws/${symbol.name.lowercase()}@kline_1m", websocketUrl("wss://example/", symbol))
            assertEquals("https://example/api/v3/klines?symbol=${symbol.name}&interval=1m&limit=500", snapshotUrl("https://example/", symbol))
        }
    }

    @Test
    fun validatesTheCurrentSymbolAndInterval() {
        val message = """{"e":"kline","E":1762041601000,"k":{"t":1762041600000,"s":"ETHUSDT","i":"1m","o":"10","h":"12","l":"9","c":"11","v":"2","n":5,"x":false}}"""
        assertIs<BinanceMessage.Kline>(parseBinanceMessage(message, MarketSymbol.ETHUSDT))
        assertEquals(BinanceMessage.WrongStream, parseBinanceMessage(message, MarketSymbol.BTCUSDT))
        assertEquals(BinanceMessage.WrongStream, parseBinanceMessage(message.replace("1m", "5m"), MarketSymbol.ETHUSDT))
    }

    @Test
    fun newHolderStartsWithEmptyState() {
        for (symbol in MarketSymbol.entries) assertEquals(LiveState(), LiveMarketHolder(symbol = symbol).state.value)
    }
}
