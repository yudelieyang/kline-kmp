package io.github.yudelieyang.kline.state

import io.github.yudelieyang.kline.data.CandleSource
import io.github.yudelieyang.kline.domain.Candle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CandleStateTest {
    private fun source(load: suspend () -> List<Candle>) = object : CandleSource {
        override suspend fun loadHistory(): List<Candle> = load()
        override fun updates(): Flow<Candle> = emptyFlow()
    }

    @Test
    fun publishesLoadedAndErrorStates() = runBlocking {
        val data = listOf(Candle(1762041600000, 1.0, 1.0, 1.0, 1.0, 0.0, true))
        val loaded = CandleStateHolder(source { data })
        loaded.load()
        assertEquals(CandleState.Loaded(data), loaded.state.value)
        val failed = CandleStateHolder(source { error("broken CSV") })
        failed.load()
        assertEquals(CandleState.Error("broken CSV"), failed.state.value)
    }

    @Test
    fun cancellationIsNotReportedAsAnError() = runBlocking {
        val holder = CandleStateHolder(source { throw CancellationException("cancelled") })
        assertFailsWith<CancellationException> { holder.load() }
        assertEquals(CandleState.Loading, holder.state.value)
    }

    @Test
    fun cancelledLoadDoesNotPublishReturnedData() = runBlocking {
        val holder = CandleStateHolder(source {
            currentCoroutineContext().cancel()
            emptyList()
        })
        launch { holder.load() }.join()
        assertEquals(CandleState.Loading, holder.state.value)
    }
}
