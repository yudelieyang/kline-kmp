"""Summarize the saved Android gfxinfo outputs; preserve source files separately."""
import re
import statistics
from pathlib import Path

ROOT = Path("docs/perf/results/drag")
PATTERNS = {
    "frames": r"Total frames rendered:\s*(\d+)",
    "jank_percent": r"Janky frames:\s*\d+\s*\(([\d.]+)%\)",
    **{f"p{p}_ms": rf"^{p}th percentile:\s*(\d+)ms" for p in (50, 90, 95, 99)},
}


def main():
    print("round,count,width_dp," + ",".join(PATTERNS))
    groups = {}
    for round_number in range(1, 6):
        for count, width in ((500, 8), (500, 2), (5000, 8), (5000, 2)):
            path = ROOT / f"round-{round_number}-{count}-{width}" / "gfxinfo.txt"
            data = path.read_text(encoding="utf-8-sig")
            values = []
            for name, pattern in PATTERNS.items():
                match = re.search(pattern, data, re.MULTILINE)
                if not match:
                    raise ValueError(f"Missing {name}: {path}")
                values.append(float(match.group(1)))
            groups.setdefault((count, width), []).append(values)
            print(f"{round_number},{count},{width}," + ",".join(f"{v:g}" for v in values))
    for (count, width), rows in groups.items():
        medians = [statistics.median(column) for column in zip(*rows)]
        print(f"median,{count},{width}," + ",".join(f"{v:g}" for v in medians))


if __name__ == "__main__":
    main()
