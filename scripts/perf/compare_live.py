"""Compare the completed benchmark log snapshot with Binance REST data."""
import datetime
import json
import math
import re
import sys
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import urlopen

ROOT = Path("docs/perf/results/live-run")
EVENT = re.compile(r"^\s*(\d+\.\d+)\s+\d+\s+\d+\s+I KlinePerf: (.*)$")
STEP = 60_000


def parse_log(text):
    events = []
    for line in text.splitlines():
        match = EVENT.match(line)
        if match:
            events.append((float(match.group(1)), match.group(2)))
    starts = [(t, msg) for t, msg in events if msg.startswith("START ")]
    begins = [i for i, (_, msg) in enumerate(events) if msg.startswith("SNAPSHOT_BEGIN ")]
    ends = [i for i, (_, msg) in enumerate(events) if msg.startswith("SNAPSHOT_END ")]
    if len(starts) != 1 or len(begins) != 1 or len(ends) != 1 or begins[0] >= ends[0]:
        raise ValueError("Missing or ambiguous start/snapshot markers")
    start_time = starts[0][0]
    begin, end = begins[0], ends[0]
    match = re.search(r"count=(\d+)$", events[begin][1])
    end_match = re.search(r"count=(\d+)$", events[end][1])
    if not match or not end_match:
        raise ValueError("Missing snapshot count")
    rows = []
    for _, msg in events[begin + 1:end]:
        if msg.startswith("CANDLE "):
            fields = msg.removeprefix("CANDLE ").split(",")
            if len(fields) != 6:
                raise ValueError("Invalid candle log")
            values = [float(x) for x in fields[1:]]
            if not all(math.isfinite(x) for x in values):
                raise ValueError("Non-finite candle value")
            rows.append((int(fields[0]), *values))
    if len(rows) != int(match.group(1)) or len(rows) != int(end_match.group(1)):
        raise ValueError("Incomplete snapshot log")
    times = [r[0] for r in rows]
    if len(rows) != 60 or len(set(times)) != 60 or any(b - a != STEP for a, b in zip(times, times[1:])):
        raise ValueError(f"Snapshot has {len(rows)} rows or non-consecutive/duplicate open times")
    return events, start_time, begin, rows


def compare(rows, rest):
    by_time = {}
    for item in rest:
        if not isinstance(item, list) or len(item) < 6:
            raise ValueError("Invalid REST row")
        time = int(item[0])
        if time in by_time:
            raise ValueError(f"Duplicate REST open time: {time}")
        values = [float(x) for x in item[1:6]]
        if not all(math.isfinite(x) for x in values):
            raise ValueError("Non-finite REST value")
        by_time[time] = values
    names = ("open", "high", "low", "close", "volume")
    matched = 0
    differences = []
    for row in rows:
        actual = by_time.get(row[0])
        if actual is None:
            differences.append(f"{row[0]}: missing from REST")
            continue
        diffs = [f"{name}: app={app!r}, REST={remote!r}" for name, app, remote in zip(names, row[1:], actual) if app != remote]
        if diffs:
            differences.append(f"{row[0]}: " + "; ".join(diffs))
        else:
            matched += 1
    extras = sorted(set(by_time) - {r[0] for r in rows})
    return matched, differences, extras


def utc_now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def main():
    events, start, begin, rows = parse_log((ROOT / "logcat.txt").read_text(encoding="utf-8-sig"))
    url = "https://data-api.binance.vision/api/v3/klines?" + urlencode({
        "symbol": "BTCUSDT", "interval": "1m", "startTime": rows[0][0], "endTime": rows[-1][0], "limit": 60,
    })
    requested = utc_now()
    with urlopen(url, timeout=30) as response:
        body = response.read()
    received = utc_now()
    (ROOT / "rest-response.json").write_bytes(body)
    (ROOT / "rest-request.txt").write_text(f"URL: {url}\nRequested UTC: {requested}\nReceived UTC: {received}\n", encoding="utf-8")
    rest = json.loads(body)
    if not isinstance(rest, list):
        raise ValueError("REST response is not a list")
    matched, differences, extras = compare(rows, rest)
    relevant = events[:begin]
    sessions = [(t, msg) for t, msg in relevant if msg.startswith("SESSION ")]
    syncs = [(t, msg) for t, msg in relevant if msg.startswith("SYNC ")]
    retries = [(t, msg) for t, msg in relevant if msg.startswith("RETRY ")]
    if not syncs:
        raise ValueError("No synchronization recorded")
    last_sync = max(i for i, (_, msg) in enumerate(relevant) if msg.startswith("SYNC "))
    live_times = set()
    for _, msg in relevant[last_sync + 1:]:
        match = re.fullmatch(r"LIVE_CLOSED session=\d+ openTime=(\d+)", msg)
        if match:
            live_times.add(int(match.group(1)))
    after_start = {r[0] for r in rows if r[0] > start * 1000}
    known_live = after_start & live_times
    host_launch = datetime.datetime.fromisoformat((ROOT / "launch-time.txt").read_text(encoding="utf-8-sig").strip()).timestamp()
    after_host_launch = {r[0] for r in rows if r[0] > host_launch * 1000}
    output = [f"Snapshot candles: {len(rows)}", f"Exact matches: {matched}",
              f"Opened after recorded app start: {len(after_start)}",
              f"Opened after start and observed closed from live after last sync: {len(known_live)}",
              f"Opened after host-recorded launch: {len(after_host_launch)}; of those, observed closed from live after last sync: {len(after_host_launch & live_times)}",
              f"Sessions: {len(sessions)}; reconnect sessions: {max(0, len(sessions) - 1)}; syncs: {len(syncs)}; resyncs: {max(0, len(syncs) - 1)}; retries: {len(retries)}",
              "Event timestamps below are device logcat Unix seconds; host and device clocks can differ."]
    output.extend(f"{t:.3f} {msg}" for t, msg in relevant if msg.startswith(("SESSION ", "SYNC ", "RETRY ")))
    output.append("Differences:")
    output.extend(differences or ["None"])
    output.append(f"Extra REST open times: {extras}")
    output.append("Start comparisons use separate device and host clock readings; clock agreement is not assumed. Live provenance is conservative: only accepted closed updates after the last synchronization are counted; resynchronization may restore candles from REST.")
    text = "\n".join(output) + "\n"
    (ROOT / "comparison.txt").write_text(text, encoding="utf-8")
    print(text, end="")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"Comparison incomplete: {exc}", file=sys.stderr)
        raise
