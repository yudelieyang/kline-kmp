# M3 plan: live market data

M3 has two separately reviewed stages. Complete M3a, run tests and verify on Desktop and the iQOO 15, then commit and stop for approval before M3b. Do not start M4.

## M3a

- Add Android INTERNET permission and the approved Ktor CIO, WebSockets, serialization and coroutine-test dependencies. First run a temporary CIO WebSocket smoke test on Desktop and the iQOO 15; stop if Android TLS fails. The handshake timeout is 10 seconds.
- Use the Binance market-data REST and WebSocket endpoints together for BTCUSDT 1m. Never mix markets. A session first subscribes to WebSocket, buffers updates, fetches the REST snapshot, then reconciles them before applying new updates directly. Check the snapshot and buffer timestamps rather than assuming their capacities imply coverage.
- Keep trade counts and source/version information in the live data layer, without changing CSV candle fields. Compare updates in a pure function. Handle duplicates, conflicting equal versions and closed-versus-open inconsistencies explicitly rather than silently selecting contradictory data.
- Support a three-period mutable window: reconcile later updates or insert missing candles inside it, ignore and count ordinary older WebSocket events. If a gap or contradiction needs M3b repair capabilities, report it honestly and resynchronize rather than pretending it is repaired. Actual missing-data repair is M3b.
- A single state-processing coroutine serializes snapshot, WebSocket, timer and request-result events. Session generations prevent cancelled or replaced sessions from publishing stale results. Bound each connection's event queue to 1024 and the pre-snapshot buffer to 60 distinct open times; overflow invalidates the affected synchronization and causes backoff, never silent loss.
- Reconnect using base delays of 1, 2, 4, 8, 16 and at most 30 seconds, with a 0.5–1.0 random multiplier. Reset failures after 60 seconds of stability and at least one business message. Continue retrying transient failures. Preserve actual HTTP errors rather than asserting that every 403 is permanent.
- Configure client ping every 20 seconds, pong timeout 10 seconds and REST timeout 10 seconds. Handle server ping using the selected engine's default WebSocket behavior; do not infer disconnection from business-message silence alone. Handle `serverShutdown` as a control event, not malformed data.
- Parse and validate individual kline fields separately from CSV sequence ordering. Count malformed JSON, invalid candles and unknown event types separately; a wrong symbol or interval invalidates the connection. Persistent bad-message thresholds are M3b.
- Expose source, connection and last valid update time through StateFlow. Continue to show the last known data with a truthful disconnected or stale indication. Display live data on Desktop and iQOO 15.
- Test pure version merge, three-period window, buffer transition, generation filtering, reconnect backoff and cancellation with deterministic time/randomness. Run JVM and Android host tests and inspect XML counts. Verify actual updates on Desktop and iQOO 15; commit and stop.
- Verify recovery on iQOO 15 by disconnecting the network for at least two minutes, then restoring it. Check the status and update time, and compare the synchronized candle open times with REST; note that REST does not provide an `isClosed` flag and that closure changes over time.

## M3b — only after explicit approval

- Add an explicit repair registry, up to 120 pending or failed periods, for both missing and unclosed candles. Schedule bounded background REST repairs, batch requests by time range within each endpoint's 1000-row maximum, and report pending/failed counts. After five attempts or five minutes, leave the entry failed; retry on a later reconnection or manual request. Use 2, 4, 8... seconds capped at 60 seconds with jitter and a 10-second REST timeout.
- Respect `Retry-After` for REST 429/418, including values longer than local backoff; share cooldown across this app's REST requests and disable manual retry until it ends. Display connection, REST cooldown and repair statuses independently.
- Before the 24-hour disconnection, use a candidate connection: maintain the current connection while the candidate subscribes, buffers and fetches its own snapshot; promote it only after synchronization. Stop the old reader, join it and drain accepted events before closing its channel. Use per-connection bounded channels and distinguish candidate failures from the active connection. Provide a configurable rotation time or debug trigger to test this.
- Add the user-controlled Binance.US and offline CSV source selection, clear old-market state on source change, and label the source and snapshot time. Do not automatically switch sources or silently show offline data as live.
- Limit in-memory history to 2000 candles softly and 3000 hard. Trim old data in batches of 500; defer soft trimming while the user views affected candles, and explain a forced hard-limit trim. Maintain the visible anchor on insertions and clamp it when old data is removed.
- Add the bad-message threshold: more than five invalid messages within 60 seconds triggers a visible data error and resynchronization; persistent failure remains visible after three cycles. Add tests for the threshold and non-kline control messages.
- Test rate limits, gaps, late repairs, concurrent actions, overflow and dual-connection rotation. Verify live updates on Desktop and phone. Read XML test counts, commit, and conduct the post-implementation code review.

## Limits

Both REST sources can lag. A successful request does not prove a candle is final, and gaps with no observable evidence cannot always be detected or recovered. A connection transition reduces disruption but cannot prove zero loss. Do not report unmeasured memory or performance figures.
