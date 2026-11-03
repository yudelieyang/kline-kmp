# UI verification follow-up

## Screenshot inspection

All nine original Android screenshots were opened and inspected for the literal text `-0.00` and `-0.00%`. None contained either string. Five showed a negative change rendered as unsigned `0.00%` by the earlier build; those were retaken after correcting the formatter. A rounded-to-zero negative value now displays `0.00`, while its color still indicates the direction of the underlying value.

| Android screenshot | Original inspection | Action |
|---|---|---|
| `android-360dp.png` | No literal negative zero; negative change with unsigned zero percentage | Retaken |
| `android-dark-live.png` | No literal negative zero; negative change with unsigned zero percentage | Retaken |
| `android-details-sheet.png` | No literal negative zero | Retained |
| `android-fontscale-1.3.png` | No literal negative zero; negative change with unsigned zero percentage | Retaken |
| `android-light-crosshair.png` | No literal negative zero | Retained |
| `android-light-live.png` | No literal negative zero; list showed unsigned zero in the down color | Retaken |
| `android-light-reconnecting.png` | No literal negative zero | Retained |
| `android-light-recovered.png` | No literal negative zero; negative change with unsigned zero percentage | Retaken after a new disconnect and recovery |
| `android-offline.png` | No literal negative zero | Retained |

The retained four images predate this formatter correction. The five replacements were captured with the corrected formatter; afterward the offline loading-state selection was refactored without a visual change. The replacement images were opened and checked; no literal negative zero was seen.

The Desktop images `desktop-light-en.png` and `desktop-dark-zh.png` were captured with `scripts/capture-window.ps1` and visually inspected. They show the English light and Chinese dark app windows respectively. The script brings the chosen process window to the foreground, fits it within the desktop work area when needed, and captures its visible frame. It requires the window to be visible and unobscured.

## Planned test coverage

All six test functions are in `shared/src/commonTest/kotlin/io/github/yudelieyang/kline/ui/model/UiModelTest.kt`:

- `ConnectionStatusTest.baseStatusesAndRetry`
- `ConnectionStatusTest.reasons`
- `MarketSummaryTest.changeAndStats`
- `MarketSummaryTest.rowsAndAverages`
- `BannerTest.liveAndOffline`
- `DisplayFormatTest.timeAndSpan`

| UI_PLAN section 4 coverage point | Actual test function and evidence |
|---|---|
| `parseConnectionStatus_baseStatuses` | `ConnectionStatusTest.baseStatusesAndRetry`: every specified base status, unknown input, raw string |
| `parseConnectionStatus_retryAndSemicolons` | `ConnectionStatusTest.baseStatusesAndRetry`: reason containing a semicolon followed by `; retry 3`; checks phase, attempt and reason |
| `parseConnectionStatus_reasonPatterns` | `ConnectionStatusTest.reasons`: all patterns, priority, both exact DataConflict values, partial match as Other |
| `lastPriceChange_emptyAndContiguous` | `MarketSummaryTest.changeAndStats`: empty, one item, rising, falling, unchanged |
| `lastPriceChange_gapAndZero` | `MarketSummaryTest.changeAndStats`: time gap and zero previous close |
| `loadedStats_emptyAndRange` | `MarketSummaryTest.changeAndStats`: empty, count, bounds, extrema and sum |
| `recentClosedRows_orderAndLimit` | `MarketSummaryTest.rowsAndAverages`: latest-first, excludes open candle, 20-row limit and short list; oldest displayed row is time 60000 and has exactly 100% change against the undisplayed time-0 candle |
| `recentClosedRows_gapsAndZero` | `MarketSummaryTest.rowsAndAverages`: gap and zero previous close |
| `movingAverageSnapshot_windowsAndGaps` | `MarketSummaryTest.rowsAndAverages`: expected 5/10/20 values, insufficient history and gap |
| `bannerFor_liveAndDisconnected` | `BannerTest.liveAndOffline`: Live, all four non-live phases, data and stopped time |
| `bannerFor_offline` | `BannerTest.liveAndOffline`: sample bounds, missing statistics, and pure loading/failure/ready selection; actual rendered resource text is not covered by an automated UI test |
| `utcFormatting_dayAndYearBoundaries` | `DisplayFormatTest.timeAndSpan`: leap-day rollover and year rollover |
| `signedFormatting_values` | `DisplayFormatTest.timeAndSpan`: positive, negative, zero and rounded negative zero |
| `formatSpan_boundaries` | `DisplayFormatTest.timeAndSpan`: 0m, 59m, 8h 22m and 1d 2h 5m |

## Desktop and device observations

- Desktop uses Material3 `ModalBottomSheet`; the app displayed it normally during manual inspection. No separate Desktop dialog fallback was used.
- On this Windows machine, with no in-app theme override, changing `AppsUseLightTheme` from 0 to 1 and broadcasting the theme change made the running app switch from dark to light; restoring 0 changed it back. The original value 0 was restored. `SystemUsesLightTheme` was not changed.
- On the connected iQOO 15, airplane mode left Wi-Fi enabled. Wi-Fi was disabled separately for the interruption and then restored. The final device queries showed physical size `1440x3168`, physical density `640`, override density `600`, font scale `1.0`, night mode `auto`, airplane mode `0`, and Wi-Fi enabled.
