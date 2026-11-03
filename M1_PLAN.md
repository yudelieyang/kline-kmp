# M1 plan

- Use `Double` for OHLCV and `Long` Unix milliseconds for candle open time.
- Support the one-minute interval, passed explicitly to the CSV source.
- Read the committed Binance sample as a Compose resource, without a UI or timed replay.
- Load history atomically as a list; CSV updates are an empty flow.
- Require the documented CSV header, strict ordering, aligned timestamps, finite positive prices, nonnegative volume, and consistent high and low values. Reject the first invalid line with its location and content.
- Allow a UTF-8 BOM and CRLF; reject empty lines except the final newline.
- Keep sample generation manual; determine the closed flag using Binance server time.
- Test embedded examples and load the committed sample on Desktop and Android host. Record Gradle XML test counts.
- For M3, the market-data Binance stream is the primary candidate and Binance.US the fallback; confirm the official documentation and reachability then.
