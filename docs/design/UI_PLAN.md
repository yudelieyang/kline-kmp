# UI Plan (M4.5)

This is a plan only. Jun confirmed this plan with the adjustments below on 2026-11-03. The source of design decisions is `docs/design/UI_SPEC.md`; no dependency or data-layer changes are planned. The UI design files are currently untracked.

## 1. Proposed files

All paths below are relative to the repository root. `shared/src/commonMain/kotlin/io/github/yudelieyang/kline/` is abbreviated as `shared/.../kline/`.

### New

- `shared/.../kline/ui/theme/KlineColors.kt` — define light/dark tokens, chart palettes and `LocalKlineColors`.
- `shared/.../kline/ui/theme/KlineTheme.kt` — provide Material color schemes and the selected palette.
- `shared/.../kline/ui/theme/KlineTypography.kt` — define the specified text styles and tabular numeric settings.
- `shared/.../kline/ui/model/ConnectionStatus.kt` — parse connection status and classify reasons without Compose.
- `shared/.../kline/ui/model/MarketSummary.kt` — calculate price changes, loaded statistics, closed rows and MA snapshots.
- `shared/.../kline/ui/model/Banner.kt` — derive banner state from source mode, connection and data.
- `shared/.../kline/ui/model/DisplayFormat.kt` — provide UTC, signed change and duration formatting as pure functions.
- `shared/.../kline/ui/TopBar.kt` — lay out the instrument heading, status and theme action.
- `shared/.../kline/ui/StatusChip.kt` — display and describe connection status and its warning indicator.
- `shared/.../kline/ui/ThemeToggleButton.kt` — draw the theme switch icon and expose its accessible action.
- `shared/.../kline/ui/PriceBlock.kt` — display current price, previous-candle change and loaded-range statistics.
- `shared/.../kline/ui/LoadedRangeNote.kt` — show the truthful loaded data interval and source.
- `shared/.../kline/ui/MaRow.kt` — show the three latest moving averages.
- `shared/.../kline/ui/StatusBanner.kt` — display the localized status and source-switch action.
- `shared/.../kline/ui/ChartPlaceholder.kt` — draw the empty-data skeleton outside the chart.
- `shared/.../kline/ui/RecentCandleList.kt` — show recent closed candles in a bounded scrolling list.
- `shared/.../kline/ui/ConnectionDetailsSheet.kt` — show source, raw status, times, URLs and message counters.
- `shared/src/commonMain/composeResources/values/strings.xml` — add the default English resources.
- `shared/src/commonMain/composeResources/values-zh/strings.xml` — add corresponding Chinese resources.
- `shared/src/commonTest/kotlin/io/github/yudelieyang/kline/ui/model/UiModelTest.kt` — test status parsing, derived data, banner selection and formatting in one file.
- `docs/screenshots/` — add the specified Android and Desktop verification images after actual capture.

### Modified

- `shared/.../kline/App.kt` — own UI-only theme/source selection, collect the existing live holder, load/cache CSV and arrange the screen with measured banner/list heights.
- `shared/.../kline/chart/CandleChart.kt` — make only the five changes allowed by UI_SPEC section 5; retain gesture, viewport and drawing calculations.
- `androidApp/src/main/kotlin/io/github/yudelieyang/kline/MainActivity.kt` — apply the app-selected system-bar style.
- `androidApp/src/main/AndroidManifest.xml` — request portrait orientation on the existing activity.
- `desktopApp/src/main/kotlin/io/github/yudelieyang/kline/main.kt` — set the initial window size to 480 × 900 dp while retaining resizing.
- local review notes (not published) — append actual weak spots after the three-question review, or pending questions if Jun defers it.

`priceLabel` and `volumeLabel` are already public in `chart/ChartLabels.kt`; reuse them without moving or changing them. No changes are planned to `LiveMarket.kt`, `data/`, `indicators/`, build scripts, dependencies, existing test assertions, or chart gestures.

## 2. Status reason inventory

Searched `data` and `state` for `error(`, `IllegalStateException(`, `MergeResult.Conflict`, and also checked `require`, `check`, the reader/snapshot completion messages and status catches. Lines below refer to the current files under `shared/src/commonMain/kotlin/io/github/yudelieyang/kline/`. Classification follows section 4.1 in its specified order, after `Disconnected: ` is removed. `; retry N` is a status suffix, removed only at the end.

| Source and line | Reason or emitted status | Section 4.1 result |
|---|---|---|
| `data/LiveMerge.kt:25` | `Closed candle has fewer trades than an open version` | DataConflict |
| `data/LiveMerge.kt:29` | `Equal versions have different candle values` | DataConflict |
| `data/LiveMerge.kt:50` | `Snapshot and buffered updates have a time gap` | TimeGap |
| `data/LiveMerge.kt:62` | `History contains a time gap` | TimeGap |
| `state/LiveMarket.kt:103-104` | `Message queue overflow` | Overflow |
| `state/LiveMarket.kt:139,173` | `Older candle remains unclosed; resynchronization required` | Resync |
| `state/LiveMarket.kt:162` | `Snapshot buffer overflow` | Overflow |
| `state/LiveMarket.kt:165` | `Live data has a time gap` | TimeGap |
| `state/LiveMarket.kt:181` | `Unexpected symbol or interval` | WrongStream |
| `state/LiveMarket.kt:107,109` | `WebSocket closed`, `WebSocket error` | SocketClosed |
| `state/LiveMarket.kt:119-121` | `REST request timed out`; `REST: ${e.message}` | RestTimeout; RestError |
| `state/LiveMarket.kt:197` | `Connection timed out` | ConnectTimeout (direct status, not `Disconnected:`) |
| `state/LiveMarket.kt:42,193-195` | `Server requested shutdown` is caught specially and becomes `Server restarting; reconnecting` | ServerRestart (direct status) |
| `state/LiveMarket.kt:51-52` | missing/blank exception message becomes `Connection error` | Other |
| `data/LiveMerge.kt:48` | `Empty snapshot` from `require` | Other if it reaches status unwrapped; snapshot parsing normally rejects an empty snapshot before reconciliation |
| `state/LiveMarket.kt:86` | `Session counter exhausted` from `check` | Other if classified; this check is outside the inner connection catch, so normally it terminates `run` rather than writing status |
| `state/LiveMarket.kt:109,121,132` | Dynamic reader exception message, `REST: ${e.message}`, or `ended.await()` | classify the resulting message; arbitrary reader errors default to Other, `REST:` to RestError unless an earlier rule matches |
| `data/LiveMerge.kt:59`; `state/LiveMarket.kt:60,159,169` | Dynamic `error(result.reason)` / `error(merged.reason)` | propagated conflict reason, currently the two DataConflict entries above |

**Known reasons outside the named reason patterns:** `Connection error`, and potentially `Empty snapshot` or arbitrary network/exception messages map to `Other`. The raw status remains visible in details. `require`/parser failures without explicit literal messages can be wrapped as `REST: ...` and then match RestError, subject to the specified matching order.

## 3. API research

- **Material3 bottom sheet on Desktop:** the [official Compose Multiplatform Material3 API](https://kotlinlang.org/api/compose-multiplatform/material3/androidx.compose.material3/-modal-bottom-sheet.html) documents `ModalBottomSheet`. The locally cached exact `material3-desktop-1.12.0-alpha03-sources.jar` contains the common `ModalBottomSheet` and a Skiko actual `ModalBottomSheetDialog` implemented using Compose `Dialog`. The app uses `ModalBottomSheet`; it was opened and visually checked on Desktop without the separate dialog fallback.
- **Android system bars:** the [official `SystemBarStyle` reference](https://developer.android.com/reference/androidx/activity/SystemBarStyle) specifies `light(scrim, darkScrim)` (dark icons where supported; fallback dark scrim for light icons) and `dark(scrim)` (light icons). Pass the selected style for both arguments of [`enableEdgeToEdge`](https://developer.android.com/reference/androidx/activity/EdgeToEdge), using suitable transparent/light/dark scrims and verifying on the device. These APIs were added in Activity 1.8.0, so are present in the configured 1.13.0. The current online docs mark them deprecated starting in 1.14.0-alpha03, not in the configured version; no upgrade is planned.
- **Compose resources:** [official resource setup](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-setup.html) and [localization guidance](https://kotlinlang.org/docs/multiplatform/compose-localize-strings.html) support `commonMain/composeResources/values/strings.xml` and `values-zh/strings.xml`, with the default as fallback and `stringResource` access. The existing resources dependency is already configured.
- **Desktop system dark theme:** the exact locally cached Compose Foundation 1.12.2 source routes `isSystemInDarkTheme()` on Skiko through `isUiSystemInDarkTheme()`. The [official resource-environment documentation](https://kotlinlang.org/docs/multiplatform/compose-resource-environment.html) describes a system theme environment on Desktop, but does not establish reliable Windows detection or live-change behavior for this exact build. On this Windows machine, changing the application color setting from dark to light and back changed the running app correspondingly while the manual override was unset; the original dark setting was restored.

## 4. Planned tests

Each item describes coverage, not a claimed test count. Use `kotlin.test` in common tests, with no new dependency.

- `parseConnectionStatus_baseStatuses` — every section 4.1 status row, including unknown strings and preservation of raw input.
- `parseConnectionStatus_retryAndSemicolons` — suffix retry overrides phase/attempt, preserves classified reason, and does not split the semicolon inside the older-unclosed-candle reason.
- `parseConnectionStatus_reasonPatterns` — each named reason, both overflow capitalizations, both WebSocket messages, REST cases, priority ordering and both exact DataConflict strings and a partial match classified as Other.
- `lastPriceChange_emptyAndContiguous` — empty, one candle, rising, falling and unchanged adjacent candles.
- `lastPriceChange_gapAndZero` — gaps yield null change/percent and a zero previous close never yields an infinite percentage.
- `loadedStats_emptyAndRange` — empty result, actual count, first/last boundaries, extrema and total volume.
- `recentClosedRows_orderAndLimit` — newest first, excludes open candle, less than and more than 20 rows, and the oldest displayed row uses its predecessor outside the display window.
- `recentClosedRows_gapsAndZero` — discontinuities and non-finite percentage are represented as null.
- `movingAverageSnapshot_windowsAndGaps` — MA5/10/20 match the existing indicator, including insufficient data and time gaps.
- `bannerFor_liveAndDisconnected` — live source with Live has no banner; all non-live phases select Warning with correct data and time.
- `bannerFor_offline` — offline source derives bounds from sample statistics; loading and error behavior follows the agreed resolution below.
- `utcFormatting_dayAndYearBoundaries` — UTC seconds/date across midnight, month/year boundaries and leap day.
- `signedFormatting_values` — positive, negative, zero, two-decimal percentages and fixed decimal separator.
- `formatSpan_boundaries` — 0 minutes, 59 minutes, 8 hours 22 minutes, and more than one day.

## 5. Confirmed decisions

1. Label the list time column `时间 UTC` and retain `openTime` as its value.
2. Keep `bannerFor` pure; the UI shows an information banner and skeleton during offline loading, or an information banner on failure, both with a return-to-live button.
3. Use the new localized `banner_reconnecting` for Connecting with old data and for Reconnecting without an attempt or ServerRestart reason, followed by stopped-at text when available.
4. Use the generic `Data has a time gap` / `数据出现时间缺口` wording.
5. Classify the two exact conflicting-data strings as `DataConflict`, not partial matches; cover both and a partial match in tests.
6. The plan resides in `docs/design/`; explicitly stage only the intended files and exclude local design inputs.

The Desktop sheet and manual theme switch were displayed successfully. The initial Windows system dark setting was reflected by the app; runtime system-theme changes were not tested. English locale was verified with `_JAVA_OPTIONS=-Duser.language=en -Duser.country=US`; `JAVA_TOOL_OPTIONS` alone did not override the Gradle-launched application locale.

## 6. Commit and verification plan

After Jun confirms, implement in order from the prompt and keep every commit buildable:

1. Theme plus the five permitted chart changes, including the minimal `App.kt` wiring needed for compilation; run both test tasks before committing.
2. Pure functions, tests, resources, screen components, offline flow and platform entry changes; run both test tasks before committing.
3. Design files and actual screenshots, and review notes only if applicable; run both test tasks before committing. Include `UI_PLAN.md` as part of the design documentation if Jun confirms this plan.

Commands (actual output and XML test counts will be recorded when run):

```text
gradlew.bat :shared:jvmTest
gradlew.bat :shared:testAndroidHostTest
```

Capture the specified Android and Desktop states. Record device size, density, font scale and night mode before changing them; restore the observed original values, including density override 600 if still current, and verify by querying again. Do not commit `.codex-attachments/` or unrelated untracked files. Perform the three short code questions only after tests and screenshots, as directed by the task prompt.
