package io.github.yudelieyang.kline.data

import io.github.yudelieyang.kline.domain.Candle
import io.github.yudelieyang.kline.domain.Interval
import kline_kmp_app.shared.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext

class CsvCandleSource(
    private val resourcePath: String,
    private val interval: Interval,
) : CandleSource {
    override suspend fun loadHistory(): List<Candle> = withContext(Dispatchers.Default) {
        parseCandlesCsv(Res.readBytes(resourcePath).decodeToString(), interval)
    }

    override fun updates(): Flow<Candle> = emptyFlow()
}
