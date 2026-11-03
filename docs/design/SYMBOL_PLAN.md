# Symbol switching plan

Status: Jun confirmed the plan and selected the top-bar dropdown. Code questions are paused until this feature is complete.

## Scope and data source

- Offer BTCUSDT, ETHUSDT and SOLUSDT; start with BTCUSDT. BNBUSDT is optional and excluded from the initial scope unless Jun asks for it.
- Keep Binance market-data bases configurable. Pure functions build the WebSocket stream using a lowercase symbol and the REST kline URL using an uppercase symbol. The details sheet receives the same URLs used by the holder.
- A direct availability check on this computer returned two 12-field kline rows for each candidate. The command actually run was:

```powershell
$symbols = 'BTCUSDT','ETHUSDT','SOLUSDT','BNBUSDT'
foreach ($symbol in $symbols) {
    $url = "https://data-api.binance.vision/api/v3/klines?symbol=$symbol&interval=1m&limit=2"
    try {
        $response = Invoke-RestMethod -Uri $url -TimeoutSec 12
        Write-Output "$symbol HTTP OK; rows=$($response.Count); first row fields=$($response[0].Count)"
    } catch {
        Write-Output "$symbol FAILED: $($_.Exception.Message)"
    }
}
```

Output: `BTCUSDT`, `ETHUSDT`, `SOLUSDT`, and `BNBUSDT` each reported `HTTP OK; rows=2; first row fields=12`. This confirms these REST requests at the time of checking, not future availability or WebSocket connectivity. [Binance's official REST documentation](https://developers.binance.com/en/docs/products/spot/rest-api) identifies the market-data-only base.

## Design and implementation

- Selector form: tap the top-bar symbol to open a dropdown. Keep it usable within 360 dp, with a minimum 48 dp touch target and no new dependency.
- Use a small fixed symbol representation with display name and base asset. Parameterize `BinanceMessages.kt` so the expected symbol is checked for each received kline; preserve the existing interval check and merge behavior.
- Recreate `LiveMarketHolder` for each selected live symbol. Cancel the old holder's effect, isolate collection by symbol so no previous `StateFlow` value appears during the transition, and begin with empty candles and a fresh status. Existing candles and updates from the previous holder must never enter the new holder.
- Key the chart by both source mode and displayed symbol so its remembered viewport and crosshair reset on either change; do not alter the chart's gesture or merge logic.
- The only offline sample is BTCUSDT. When viewing it, display BTCUSDT and BTC units, disable the selector with an accessible explanation, and keep the previously selected live symbol so returning to live returns to that symbol. The sample is never presented as ETH or SOL data.
- Update English and Chinese volume labels to use the displayed base asset, including the list and chart context; keep the existing chart drawing and gestures unless Jun approves a further change. Preserve the rest of the UI specification.
- Update relevant BTCUSDT-only statements in `AGENTS.md` and `README.md`, retaining the existing BTC sample description. README already contains an unrelated uncommitted icon note; preserve it and stage only the symbol-related README changes.

## Files and checks

- Expected changes: shared app, symbol/URL pure functions, live holder, Binance message parser, top bar and volume labels, English/Chinese resources, focused common tests, `AGENTS.md`, `README.md`, this plan, and symbol-related acceptance screenshots. Exact files depend on the selector decision.
- Unit tests: URLs and case for all selected symbols; matching and mismatched WebSocket messages including interval; independent fresh holder state; relevant offline/displayed-symbol and unit behavior. Verify chart reset by opening the crosshair and switching symbols on the device, including two candles sharing the same open time.
- Run `gradlew.bat :shared:jvmTest` and `gradlew.bat :shared:testAndroidHostTest`; report counts and failures from their XML reports. Also assemble the Android app and verify Desktop still builds.
- On iQOO 15, capture one live image per supported symbol, a crosshair-open-before/switch-after pair, and a 360 dp selector image. Compare multiple **closed** candles per symbol against the matching REST response at the same open times, accounting for the UI's displayed precision. Record and restore the actual original screen size, density, font scale, theme and network settings; airplane mode on this device may leave Wi-Fi on.
- Stage only explicit files/hunks related to this feature. Preserve existing uncommitted icon and Desktop launcher edits, the unrelated README icon note, and untracked files. Complete tests and acceptance, commit, report, and stop; resume the deferred code questions only afterward at Jun's direction.

## Acceptance results

- iQOO 15, 1440 px wide at density 640 (360 dp): `symbol-selector-360dp.png` shows all three menu items on one line. `symbol-btcusdt.png`, `symbol-ethusdt.png`, and `symbol-solusdt.png` show live data; `symbol-crosshair-before.png` shows the ETH selection before switching, and `symbol-solusdt.png` shows SOL after switching with no crosshair. `symbol-offline-btc.png` shows the BTC sample after selecting SOL. The accessible selector description says selection is unavailable offline. SOL's details showed the `solusdt@kline_1m` stream and the `symbol=SOLUSDT` REST URL.
- Compared screenshot rows with `GET /api/v3/klines?symbol=<symbol>&interval=1m&startTime=<2026-11-03T06:25:00Z in milliseconds>&limit=4`. At 06:25–06:28 UTC, REST close values respectively were BTC 76605.24, 76652.01, 76659.38, 76661.73; ETH 2249.22, 2250.63, 2250.92, 2250.77; SOL 91.61, 91.67, 91.66, 91.65. Volumes in those rows also matched the screenshots at displayed precision. BTC 06:28 is visible in the selector screenshot; the other BTC rows are visible in the BTC screenshot.
- Device settings before and after: size 1440x3168, density override 600, font scale 1.0, night mode 0, airplane mode 0, Wi-Fi on. Density 640 and Wi-Fi off were temporary.
- `gradlew.bat :shared:jvmTest :shared:testAndroidHostTest :androidApp:assembleDebug :desktopApp:compileKotlin` completed successfully. XML totals: `shared/build/test-results/jvmTest/` 50 tests, `shared/build/test-results/testAndroidHostTest/` 49 tests, both with zero failures, errors or skips.
