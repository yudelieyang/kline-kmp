package io.github.yudelieyang.kline.data

import io.github.yudelieyang.kline.domain.Candle
import io.github.yudelieyang.kline.domain.Interval
import kotlinx.serialization.json.*

sealed interface BinanceMessage {
    data class Kline(val value: KlineVersion, val eventTime: Long) : BinanceMessage
    data object Shutdown : BinanceMessage
    data object Unknown : BinanceMessage
    data object Malformed : BinanceMessage
    data object Invalid : BinanceMessage
    data object WrongStream : BinanceMessage
}

private fun candle(time: Long, open: String, high: String, low: String, close: String, volume: String, closed: Boolean): Candle {
    val text = "openTime,open,high,low,close,volume,isClosed\n$time,$open,$high,$low,$close,$volume,$closed"
    return parseCandlesCsv(text, Interval.ONE_MINUTE).single()
}

fun parseBinanceMessage(text: String, symbol: MarketSymbol = MarketSymbol.BTCUSDT): BinanceMessage {
    val obj = try { Json.parseToJsonElement(text).jsonObject } catch (_: IllegalArgumentException) { return BinanceMessage.Malformed }
    val type = try { obj["e"]?.jsonPrimitive?.content } catch (_: IllegalArgumentException) { return BinanceMessage.Malformed }
    if (type == "serverShutdown") return BinanceMessage.Shutdown
    if (type != "kline") return BinanceMessage.Unknown
    return try {
        val k = obj.getValue("k").jsonObject
        fun field(name: String) = k.getValue(name).jsonPrimitive.content
        if (field("s") != symbol.name || field("i") != "1m") return BinanceMessage.WrongStream
        val trades = field("n").toLong().also { require(it >= 0) }
        val closed = field("x").toBooleanStrict()
        val item = candle(field("t").toLong(), field("o"), field("h"), field("l"), field("c"), field("v"), closed)
        BinanceMessage.Kline(KlineVersion(item, trades), obj.getValue("E").jsonPrimitive.content.toLong())
    } catch (_: Exception) { BinanceMessage.Invalid }
}

fun parseSnapshot(text: String, serverTime: Long): List<KlineVersion> {
    val rows = Json.parseToJsonElement(text).jsonArray
    return rows.map { element ->
        val row = element.jsonArray
        fun field(i: Int) = row[i].jsonPrimitive.content
        val trades = field(8).toLong().also { require(it >= 0) }
        KlineVersion(candle(field(0).toLong(), field(1), field(2), field(3), field(4), field(5), field(6).toLong() < serverTime), trades)
    }.also { list -> require(list.isNotEmpty() && list.zipWithNext().all { (a, b) -> a.candle.openTime < b.candle.openTime }) }
}
