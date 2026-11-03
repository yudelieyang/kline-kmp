package io.github.yudelieyang.kline.state

import io.github.yudelieyang.kline.data.*
import io.github.yudelieyang.kline.domain.Candle
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.*
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.TimeMark

data class LiveState(
    val candles: List<Candle> = emptyList(),
    val status: String = "Connecting",
    // Latest exchange event time among accepted valid messages; never decreases.
    val lastUpdate: Long? = null,
    val malformed: Int = 0,
    val invalid: Int = 0,
    val unknown: Int = 0,
    val ignored: Int = 0,
)

private sealed interface Event {
    val session: Long
    data class Message(override val session: Long, val value: BinanceMessage) : Event
    data class Snapshot(override val session: Long, val values: List<KlineVersion>) : Event
}

private class ServerShutdown : Exception("Server requested shutdown")

private fun websocketMessages(socket: DefaultClientWebSocketSession, symbol: MarketSymbol) = flow {
    for (frame in socket.incoming) if (frame is Frame.Text) emit(parseBinanceMessage(frame.readText(), symbol))
}

internal fun isCurrentSession(eventSession: Long, currentSession: Long) = eventSession == currentSession

internal fun disconnectedStatus(error: Throwable): String =
    "Disconnected: ${error.message?.takeIf { it.isNotBlank() } ?: "Connection error"}"

internal fun liveStateAfterMerge(state: LiveState, result: MergeResult, eventTime: Long): LiveState = when (result) {
    is MergeResult.Merged -> state.copy(
        candles = result.candles.map { it.candle },
        lastUpdate = state.lastUpdate?.let { maxOf(it, eventTime) } ?: eventTime,
    )
    MergeResult.Ignored -> state.copy(ignored = state.ignored + 1)
    is MergeResult.Conflict -> error(result.reason)
}

internal fun shouldResetFailures(connectedAt: TimeMark?, received: Boolean): Boolean =
    received && connectedAt != null && connectedAt.elapsedNow() >= 60.seconds

internal fun lastAcceptedBufferedTime(current: Long?, accepted: List<KlineVersion>, times: Map<Long, Long>): Long? =
    (accepted.mapNotNull { times[it.candle.openTime] } + listOfNotNull(current)).maxOrNull()

class LiveMarketHolder(
    private val symbol: MarketSymbol = MarketSymbol.BTCUSDT,
    private val wsUrl: String = websocketUrl("wss://data-stream.binance.vision:443", symbol),
    private val restUrl: String = "https://data-api.binance.vision",
) {
    private val mutableState = MutableStateFlow(LiveState())
    val state = mutableState.asStateFlow()

    suspend fun run() {
        val client = HttpClient(CIO) {
            expectSuccess = true
            install(WebSockets) { pingIntervalMillis = 20_000 }
        }
        var failures = 0
        var session = 0L
        var previousVersions = emptyList<KlineVersion>()
        try {
            while (currentCoroutineContext().isActive) {
                check(session < Long.MAX_VALUE) { "Session counter exhausted" }
                val currentSession = ++session
                var connectedAt: TimeMark? = null
                var received = false
                var requestedShutdown = false
                mutableState.value = mutableState.value.copy(status = if (failures == 0) "Connecting" else "Reconnecting ($failures)")
                try {
                    coroutineScope {
                        val socket = withTimeout(10.seconds) { client.webSocketSession(wsUrl) }
                        connectedAt = TimeSource.Monotonic.markNow()
                        socket.timeoutMillis = 10_000
                        val events = Channel<Event>(1024)
                        val ended = CompletableDeferred<String>()
                        val reader = launch {
                            try {
                                websocketMessages(socket, symbol).collect { message ->
                                    if (!events.trySend(Event.Message(currentSession, message)).isSuccess) {
                                        ended.complete("Message queue overflow")
                                        throw IllegalStateException("Message queue overflow")
                                    }
                                }
                                ended.complete("WebSocket closed")
                            } catch (e: CancellationException) { throw e }
                            catch (e: Exception) { ended.complete(e.message ?: "WebSocket error") }
                        }
                        val snapshot = launch {
                            try {
                                val values = withTimeout(10.seconds) {
                                    val time = Json.parseToJsonElement(client.get("$restUrl/api/v3/time").bodyAsText()).jsonObject.getValue("serverTime").jsonPrimitive.content.toLong()
                                    parseSnapshot(client.get(snapshotUrl(restUrl, symbol)).bodyAsText(), time)
                                }
                                events.send(Event.Snapshot(currentSession, values))
                            } catch (e: TimeoutCancellationException) { ended.complete("REST request timed out") }
                            catch (e: CancellationException) { throw e }
                            catch (e: Exception) { ended.complete("REST: ${e.message}") }
                        }
                        var versions = emptyList<KlineVersion>()
                        var synced = false
                        val buffer = mutableListOf<KlineVersion>()
                        val bufferedEventTimes = mutableMapOf<Long, Long>()
                        mutableState.value = mutableState.value.copy(status = "Synchronizing")
                        try {
                            while (true) {
                                val event = select<Event?> {
                                    ended.onAwait { null }
                                    events.onReceive { it }
                                } ?: throw IllegalStateException(ended.await())
                                if (!isCurrentSession(event.session, currentSession)) continue
                                when (event) {
                                    is Event.Snapshot -> {
                                        val reconciliation = reconcileSnapshotDetails(previousVersions, event.values, buffer)
                                        versions = reconciliation.candles
                                        if (versions.any { !it.candle.isClosed && it.candle.openTime < versions.last().candle.openTime - 120_000L })
                                            error("Older candle remains unclosed; resynchronization required")
                                        previousVersions = versions
                                        buffer.clear()
                                        synced = true
                                        val lastAccepted = lastAcceptedBufferedTime(mutableState.value.lastUpdate, reconciliation.acceptedBuffered, bufferedEventTimes)
                                        bufferedEventTimes.clear()
                                        mutableState.value = mutableState.value.copy(candles = versions.map { it.candle }, status = "Live", lastUpdate = lastAccepted)
                                        logSynchronizedCandles(versions.takeLast(10).map { it.candle })
                                    }
                                    is Event.Message -> when (val message = event.value) {
                                        is BinanceMessage.Kline -> {
                                            received = true
                                            if (!synced) {
                                                when (val result = mergeKline(buffer.sortedBy { it.candle.openTime }, message.value, false)) {
                                                    is MergeResult.Merged -> {
                                                        buffer.clear()
                                                        buffer.addAll(result.candles)
                                                        val time = message.value.candle.openTime
                                                        bufferedEventTimes[time] = maxOf(bufferedEventTimes[time] ?: message.eventTime, message.eventTime)
                                                    }
                                                    is MergeResult.Conflict -> error(result.reason)
                                                    MergeResult.Ignored -> Unit
                                                }
                                                if (buffer.size > 60) error("Snapshot buffer overflow")
                                            } else {
                                                val newest = versions.lastOrNull()?.candle?.openTime
                                                if (newest != null && message.value.candle.openTime > newest + 60_000L) error("Live data has a time gap")
                                                val result = mergeKline(versions, message.value)
                                                when (result) {
                                                    is MergeResult.Merged -> versions = result.candles
                                                    is MergeResult.Conflict -> error(result.reason)
                                                    MergeResult.Ignored -> Unit
                                                }
                                                if (versions.any { !it.candle.isClosed && it.candle.openTime < versions.last().candle.openTime - 120_000L })
                                                    error("Older candle remains unclosed; resynchronization required")
                                                previousVersions = versions
                                                mutableState.value = liveStateAfterMerge(mutableState.value, result, message.eventTime)
                                            }
                                        }
                                        BinanceMessage.Malformed -> mutableState.value = mutableState.value.copy(malformed = mutableState.value.malformed + 1)
                                        BinanceMessage.Invalid -> mutableState.value = mutableState.value.copy(invalid = mutableState.value.invalid + 1)
                                        BinanceMessage.Unknown -> mutableState.value = mutableState.value.copy(unknown = mutableState.value.unknown + 1)
                                        BinanceMessage.WrongStream -> error("Unexpected symbol or interval")
                                        BinanceMessage.Shutdown -> throw ServerShutdown()
                                    }
                                }
                            }
                        } finally {
                            reader.cancelAndJoin()
                            snapshot.cancelAndJoin()
                            events.cancel()
                            socket.cancel()
                        }
                    }
                } catch (e: ServerShutdown) {
                    requestedShutdown = true
                    mutableState.value = mutableState.value.copy(status = "Server restarting; reconnecting")
                } catch (e: TimeoutCancellationException) {
                    mutableState.value = mutableState.value.copy(status = "Connection timed out")
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { mutableState.value = mutableState.value.copy(status = disconnectedStatus(e)) }
                if (requestedShutdown) continue
                if (shouldResetFailures(connectedAt, received)) failures = 0
                val wait = reconnectDelayMillis(failures, Random.nextDouble(0.5, 1.0))
                failures++
                mutableState.value = mutableState.value.copy(status = "${mutableState.value.status}; retry $failures")
                delay(wait)
            }
        } finally { client.close() }
    }
}
