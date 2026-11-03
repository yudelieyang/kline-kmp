"""Download a fixed BTCUSDT sample; run manually from the project root."""

import csv
import argparse
import datetime
import json
from pathlib import Path
from urllib.request import urlopen
from urllib.parse import urlencode


KLINES_URL = "https://data-api.binance.vision/api/v3/klines?symbol=BTCUSDT&interval=1m&limit=500"
TIME_URL = "https://data-api.binance.vision/api/v3/time"
OUTPUT = Path("shared/src/commonMain/composeResources/files/btcusdt_1m_sample.csv")


def get_json(url):
    with urlopen(url, timeout=30) as response:
        return json.load(response)


def download_closed(count, server_time):
    rows = []
    end_time = server_time - 1
    while len(rows) < count:
        query = urlencode({"symbol": "BTCUSDT", "interval": "1m", "limit": min(1000, count - len(rows) + 1), "endTime": end_time})
        url = "https://data-api.binance.vision/api/v3/klines?" + query
        print(f"URL: {url}")
        page = get_json(url)
        if not isinstance(page, list) or not page or any(not isinstance(row, list) or len(row) < 7 for row in page):
            raise ValueError("Invalid or empty kline page")
        if any(not isinstance(row[0], int) or not isinstance(row[6], int) for row in page):
            raise ValueError("Invalid kline timestamps")
        if page[0][0] > end_time or any(b[0] <= a[0] for a, b in zip(page, page[1:])):
            raise ValueError("Kline page does not move backward in time")
        closed = [row for row in page if row[6] < server_time]
        rows = closed + rows
        end_time = page[0][0] - 1
    rows = rows[-count:]
    if len(rows) != count or any(b[0] - a[0] != 60000 for a, b in zip(rows, rows[1:])):
        raise ValueError("Kline count, order, uniqueness or continuity is invalid")
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--benchmark-5000", action="store_true")
    args = parser.parse_args()
    server_time = get_json(TIME_URL)["serverTime"]
    rows = download_closed(5000, server_time) if args.benchmark_5000 else get_json(KLINES_URL)
    output = OUTPUT.with_name("btcusdt_1m_5000.csv") if args.benchmark_5000 else OUTPUT
    local_time = datetime.datetime.now().astimezone().isoformat()
    if not isinstance(rows, list) or not rows or not isinstance(server_time, int):
        raise ValueError("Invalid Binance response")
    if any(not isinstance(row, list) or len(row) < 7 for row in rows):
        raise ValueError("Invalid kline row")
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, lineterminator="\n")
        writer.writerow(["openTime", "open", "high", "low", "close", "volume", "isClosed"])
        for row in rows:
            writer.writerow([row[0], *row[1:6], str(row[6] < server_time).lower()])
    if not args.benchmark_5000:
        print(f"URL: {KLINES_URL}")
    print(f"Server time (milliseconds): {server_time}")
    print(f"Local time: {local_time}")
    print(f"Rows: {len(rows)}")
    print(f"Last candle closed: {rows[-1][6] < server_time}")
    print(f"CSV: {output}")


if __name__ == "__main__":
    main()
