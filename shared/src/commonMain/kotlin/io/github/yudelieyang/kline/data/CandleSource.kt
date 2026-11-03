package io.github.yudelieyang.kline.data

import io.github.yudelieyang.kline.domain.Candle
import kotlinx.coroutines.flow.Flow

interface CandleSource {
    /** Loads all history; a CSV source throws [CsvParseException] for invalid data. */
    suspend fun loadHistory(): List<Candle>

    fun updates(): Flow<Candle>
}
