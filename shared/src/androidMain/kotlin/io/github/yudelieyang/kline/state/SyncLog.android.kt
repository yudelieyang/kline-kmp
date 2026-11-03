package io.github.yudelieyang.kline.state

import android.util.Log
import io.github.yudelieyang.kline.domain.Candle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal actual fun logSynchronizedCandles(candles: List<Candle>) {
    val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val entries = candles.joinToString("; ") {
        "openTime=${utc.format(Date(it.openTime))} openTimeMs=${it.openTime} isClosed=${it.isClosed}"
    }
    Log.i("KlineSync", "Synchronized candles: $entries")
}
