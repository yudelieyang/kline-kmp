package io.github.yudelieyang.kline.state

import io.github.yudelieyang.kline.data.CandleSource
import io.github.yudelieyang.kline.domain.Candle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CandleState {
    data object Loading : CandleState
    data class Loaded(val candles: List<Candle>) : CandleState
    data class Error(val message: String) : CandleState
}

class CandleStateHolder(private val source: CandleSource) {
    private val mutableState = MutableStateFlow<CandleState>(CandleState.Loading)
    val state: StateFlow<CandleState> = mutableState.asStateFlow()

    suspend fun load() {
        mutableState.value = CandleState.Loading
        try {
            val candles = source.loadHistory()
            currentCoroutineContext().ensureActive()
            mutableState.value = CandleState.Loaded(candles)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            mutableState.value = CandleState.Error(error.message ?: "Unable to load candles")
        }
    }
}
