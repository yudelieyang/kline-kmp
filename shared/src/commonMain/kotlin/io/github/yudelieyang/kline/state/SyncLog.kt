package io.github.yudelieyang.kline.state

import io.github.yudelieyang.kline.domain.Candle

internal expect fun logSynchronizedCandles(candles: List<Candle>)
