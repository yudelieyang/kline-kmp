# M4 plan: moving averages and crosshair

## Moving averages

- Compute MA5, MA10, and MA20 from candle close prices. Use configurable orange, blue, and purple colors, respectively, for the lines and their legend values.
- Return nullable values aligned with candle indices. A value is absent until there is a complete window of N consecutive one-minute periods. Missing periods also make each affected window absent; never fabricate a zero, shorten the window, or connect across an absent value. Display unavailable values as `--`; distinguish a missing period from insufficient initial history in the crosshair panel.
- Include the current close of an unclosed candle and update its MA as data changes. Draw its line normally, but mark the latest legend and the selected candle's panel as unclosed when applicable. The CSV snapshot's unclosed flag describes its snapshot, not the current market.
- Recalculate the needed range when the data or viewport changes rather than maintaining incremental sums. For visible indices `[start, end)`, compute up to one extra point on each side, clipped to `[0, count)`, and read up to N earlier candles for the left extra point. Draw only relevant visible segments and clip MA lines to the main price panel.
- Sum each window directly; if the sum is non-finite, compute the mean by scaling against the largest window value. Keep the result finite and within the window's minimum and maximum; test extreme finite prices, subnormal positive prices, and rounding boundaries. This is floating-point arithmetic, not exact decimal arithmetic.
- Include the actual visible portions of MA segments in the price range, using interpolated values at the left and right plot boundaries rather than whole offscreen points. Include the visible candles, preserve the existing 5% padding and flat-range handling, and retain measured, constrained axis labels. Candle extrema can still cause the axis to jump as visibility changes.
- Implement the dashed time-gap marker last; do not synthesize candles. Live gaps currently trigger resynchronization and the committed CSV sample is continuous, but other CSV input may contain gaps.

## Crosshair

- Android: long press opens the crosshair; dragging while holding moves it, release retains it, and a tap closes it. Movement beyond touch slop before long press pans the chart; a second finger cancels a pending long press and permits pinch zoom. Once retained, normal pan and zoom remain usable.
- Desktop: pointer hover displays and moves the crosshair, leaving the chart hides it; mouse drag pans with the crosshair hidden, and the wheel zooms. While actively hovering, its horizontal line follows pointer height, including when the price axis changes.
- Snap the vertical line to the nearest candle center, with selection constrained to available data. An Android retained crosshair is bound to candle `openTime` and the chosen price, not an index or screen coordinate. If the candle disappears or moves completely outside the plot, close the crosshair; hide only the horizontal line and price label when the retained price is outside the current price range.
- Position the information panel at the upper side opposite the selected candle, constrained to the main panel. Show UTC month-day and hour:minute, OHLC, change and percentage relative to the immediately previous consecutive candle, volume using existing formatting, MA5/10/20 in their colors, and an unclosed indicator when applicable. Show `--` when a previous candle or MA is unavailable; do not bridge a time gap. Display the crosshair price at the right axis and selected time at the bottom axis.
- When the selected candle is unclosed, update OHLCV, change, and MA values in the panel as live data changes.
- Keep chart and crosshair state in the UI, with the existing state owner providing market data. Use no chart library or new dependency.

## Validation and delivery

- Unit-test MA against hand-computed values, complete and incomplete windows, missing periods, edge-range calculations, extreme values, unclosed updates, axis boundary interpolation, and relevant crosshair selection/coordinates. Check interaction and panel display on both Desktop and the iQOO 15 using the newly built APK; capture screenshots and verify long press, pan, pinch, hover, wheel, dismiss, and retained-selection behavior.
- Run Desktop JVM and Android host tests with `gradlew.bat`, reading test, failure, and skipped counts from their XML reports. Build the Android debug APK. Record commands and observed outcomes without unmeasured performance claims.
- Implement and test moving averages, drawing, price range and legend first; commit them. Then implement and test the crosshair and commit it. Evaluate the optional gap marker last. After device verification, conduct the post-code review one question at a time using actual repository code, and record weak spots in local review notes (not published). Stop after M4; do not start M5 without approval.
