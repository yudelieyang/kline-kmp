# M6-perf plan

Status: approved by Jun, with the additions below.

## Goal and boundaries

Collect reproducible Android measurements for the resume, keeping raw evidence and reporting only results actually obtained. This is an observation of one device and workload, not a general performance guarantee. Keep normal app behavior unchanged, add no dependencies, and do not push without asking Jun first.

## Preparation

1. Add an Android `benchmark` build type based on `release`, signed with the debug key and non-debuggable. Keep minification and R8 disabled, as in the current release configuration; verify and report that setting. Use this build for all measurements. A normal launch of this build remains live; benchmark extras are opt-in and validated.
2. Add an opt-in offline launch mode, with extras selecting the existing 500-row CSV or a new 5,000-row CSV and the initial chart slot width of 8 dp or 2 dp. Ensure the live session is never started in this mode, including during composition, and prevent switching to live within the measured session. Verify both sizes, widths, and normal launch behavior. Width means the chart's candle slot, not the narrower drawn body.
3. Extend the existing Python downloader to obtain real BTCUSDT one-minute rows from Binance's market-data REST endpoint in pages. Fix a server-time cutoff before fetching, exclude unclosed candles, check the requested count, ordering, uniqueness and minute continuity, and fail rather than silently save incomplete data. Preserve the existing sample download behavior. Record the actual command, source, download time and output in README; retain raw download output. If the endpoint is unreachable, report the blocker rather than fabricate data.
4. Record device model, Android version, display resolution, refresh-rate setting mode (fixed or adaptive), reported refresh rate during measurement, power-saving state, charging state and battery temperature, preserving command output and units. Record screen-on and background-app preparation; ask Jun to unlock, approve installation, clear background apps or change device settings when necessary. Keep the screen awake and disable battery saver for measurements, recording any changes and restoring prior settings where possible. Restore and record any refresh-rate setting changes after testing. Do not claim a device state that cannot be verified.

## Drag frame timing

- Run the four combinations of 500 / 5,000 candles and 8 / 2 dp in five rounds, each round containing one repetition of each combination. Record battery temperature for every repetition. Force-stop and launch with extras before each repetition; wait for an explicit data-and-chart-ready signal and a fixed settling period, then reset `dumpsys gfxinfo`.
- Use a saved PowerShell script under `scripts/perf/` with fixed, recorded screen coordinates and swipe duration, calibrated to the actual main plot on the device. Perform 20 alternating swipes per repetition; count each individual `adb shell input swipe` as one swipe. Confirm the gestures pan the chart and avoid the axis, controls and chart boundaries. Record the chosen coordinates and duration, device orientation, screen size, and all commands. If calibration or motion fails, invalidate the repetition rather than report it as a measurement.
- Save complete reset and post-run gfxinfo output separately for every repetition, plus launch and script logs. Extract total frames, janky frame percentage and 50th, 90th, 95th and 99th percentile frame times only where Android actually reports them; do not infer missing metrics. Summarize every repetition and the median of each available metric across the five valid repetitions, retaining the raw values. Gfxinfo covers the app's frames during the measurement window, including idle frames; it is not an isolated per-swipe trace. Its percentile values come from histogram buckets and have limited resolution.

## Live correctness and memory

- Launch the same benchmark build normally in live BTCUSDT mode. Record actual start and end times, connection status, relevant logs and complete `dumpsys meminfo` output at the start and after 60 minutes. Report the two TOTAL PSS readings and their difference, with units and the timing of each capture; two snapshots do not establish a leak or peak memory. Keep the screen on and do not touch the phone during the interval.
- At the end, trigger a benchmark-build-only diagnostic snapshot without restarting the process or interacting with the screen. Capture the last 60 closed candles from the existing state owner, including open time, OHLC and volume, along with snapshot time and connection state. Preserve complete output and detect missing or truncated logs. Diagnostic logging occurs after the ending memory capture so it does not affect that reading.
- Fetch and save the raw REST response for the exact open-time range in the snapshot, with request and response times. Compare by open time and numerical value for each field using a documented conversion consistent with the app's stored numeric representation; do not compare formatted display strings. Report exact matches, missing or duplicate rows and field-level differences. Validate that there are 60 distinct, consecutive closed minutes; if not, report the actual count and gaps instead of claiming a successful 60-candle comparison. A match is against the later REST response, not proof that every intermediate update was correct.
- Record the counts and timestamps of reconnections and resynchronizations. Track provenance of compared candles and report how many opened after this run began and were formed from live updates. If resynchronization occurred, explain that candles may have come from a REST snapshot and agreement with REST does not establish merge correctness.
- If the feed, device, logging, endpoint or uninterrupted run fails, keep the evidence and report the run as incomplete; retry only when feasible. Do not substitute offline data for live correctness.
- Complete preparation and drag measurements before beginning the live run. During the uninterrupted live interval, write the comparison script and documentation without touching the phone.

## Optional cold start

If time permits, use live mode on Wi-Fi and force-stop before each of 10 runs. Log process-start and first actual candle draw using a monotonic clock in the same process; save raw timestamps, connection context and each duration, then report median and range. This measures in-process startup to first draw, not the full time from the host's launch command or display presentation. Record failures and omit unsupported measurements.

## Verification and delivery

- Add focused checks for downloader pagination and validation, benchmark configuration and launch parsing, and comparison logic; run the existing test suite and relevant new checks. Record commands and their actual output, including any failures or skipped checks.
- Save raw command output and logs under `docs/perf/results/`, and write `docs/perf/PERF_RESULTS.md` plus a concise README section with device, refresh rate, build type, R8 status, methods, results and limitations. Include only observed values and calculations reproducible from saved outputs; never estimate or invent numbers. Keep failed and incomplete runs identifiable.
- Review the diff for unrelated files and local-only exclusions, commit only this task's files after measurements and tests, and report changed files, exact commands and real outputs, skipped work and concepts to explain. Ask before pushing `main`; never push `private-history`.
