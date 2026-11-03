package io.github.yudelieyang.kline.data

enum class MarketSymbol(val baseAsset: String) {
    BTCUSDT("BTC"), ETHUSDT("ETH"), SOLUSDT("SOL")
}

fun websocketUrl(base: String, symbol: MarketSymbol): String =
    "${base.trimEnd('/')}/ws/${symbol.name.lowercase()}@kline_1m"

fun snapshotUrl(base: String, symbol: MarketSymbol): String =
    "${base.trimEnd('/')}/api/v3/klines?symbol=${symbol.name}&interval=1m&limit=500"
