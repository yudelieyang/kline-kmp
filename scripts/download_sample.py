"""Download a fixed BTCUSDT sample; run manually from the project root."""

import csv
import datetime
import json
from pathlib import Path
from urllib.request import urlopen


KLINES_URL = "https://data-api.binance.vision/api/v3/klines?symbol=BTCUSDT&interval=1m&limit=500"
TIME_URL = "https://data-api.binance.vision/api/v3/time"
OUTPUT = Path("shared/src/commonMain/composeResources/files/btcusdt_1m_sample.csv")


def get_json(url):
    with urlopen(url, timeout=30) as response:
        return json.load(response)


def main():
    server_time = get_json(TIME_URL)["serverTime"]
    rows = get_json(KLINES_URL)
    local_time = datetime.datetime.now().astimezone().isoformat()
    if not isinstance(rows, list) or not rows or not isinstance(server_time, int):
        raise ValueError("Invalid Binance response")
    if any(not isinstance(row, list) or len(row) < 7 for row in rows):
        raise ValueError("Invalid kline row")
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    with OUTPUT.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file, lineterminator="\n")
        writer.writerow(["openTime", "open", "high", "low", "close", "volume", "isClosed"])
        for row in rows:
            writer.writerow([row[0], *row[1:6], str(row[6] < server_time).lower()])
    print(f"URL: {KLINES_URL}")
    print(f"Server time (milliseconds): {server_time}")
    print(f"Local time: {local_time}")
    print(f"Rows: {len(rows)}")
    print(f"Last candle closed: {rows[-1][6] < server_time}")
    print(f"CSV: {OUTPUT}")


if __name__ == "__main__":
    main()
