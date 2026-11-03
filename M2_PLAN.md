# M2 plan: static candlestick chart

- Load the committed CSV through one common state holder using StateFlow: loading, loaded count, or an error. Check cancellation with `ensureActive()` before publishing the result.
- Draw a handwritten Compose Canvas chart with only the visible candles and volume bars. Use red for close >= open and green otherwise; define replaceable colors together.
- Put pure chart calculations in `commonMain` under `chart`. Map prices with Double arithmetic before converting to Float. Reject invalid price ranges and handle flat prices and floating-point extremes.
- Use a right-anchored pixel offset and the candle slot width to calculate a clamped, left-closed/right-open visible range. Include partially visible candles. Calculate the price range from visible highs and lows with 5% padding, or 0.1% of price for a flat range.
- Deduct the right label column before calculating plot width and visible indices. Measure the column from the full dataset's minimum and maximum once so it stays fixed while dragging; cap it at 30% of canvas width and use scientific notation when needed.
- Store the viewport in Compose UI state. Drag moves content in the same direction. Pinch and mouse-wheel zoom anchor at the gesture centroid or pointer, with width 2–40 dp, initially 8 dp. Clamp width before computing the effective zoom ratio; only a data-edge offset clamp may move the anchor. Test both constraints.
- Allocate chart and volume panels 3:1 after an 8 dp gap; reserve a bottom time axis. Draw five price ticks, maximum volume, and UTC labels. Volume zero draws no bar; all-zero volume avoids division. Price labels use two decimals when suitable; volume uses three significant digits and K/M/B, with scientific notation for extremes.
- Choose UTC time intervals from 1, 2, 5, 10, 15, 30 minutes; 1, 2, 4, 6, 12 hours; and 1 day based on label space. Midnight and daily ticks show month-day. Use standard-library date calculations and test leap days and year boundaries.
- Test mappings, visible ranges including reserved axis width, viewport clamping, formatting, and UTC dates on JVM and Android host. Read actual counts from Gradle XML.
- Verify desktop drag and wheel zoom, and load, drag, pinch zoom and a screenshot on the iQOO 15 physical phone. No emulator or performance measurements.
- After tests pass, commit; then conduct the post-implementation code review, record real weak spots, and commit the notes. Stop after M2.
