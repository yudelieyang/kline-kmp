import unittest

from compare_live import compare, parse_log


class LiveComparisonTest(unittest.TestCase):
    def log(self, missing=False):
        lines = ["1000.000 1 1 I KlinePerf: START symbol=BTCUSDT",
                 "1001.000 1 1 I KlinePerf: SYNC session=1",
                 "2000.000 1 1 I KlinePerf: SNAPSHOT_BEGIN status=Live count=60"]
        lines += [f"2001.000 1 1 I KlinePerf: CANDLE {i * 60000},1.0,2.0,0.5,1.5,3.0" for i in range(59 if missing else 60)]
        lines.append("2002.000 1 1 I KlinePerf: SNAPSHOT_END count=60")
        return "\n".join(lines)

    def test_snapshot_and_difference(self):
        _, _, _, rows = parse_log(self.log())
        rest = [[r[0], "1.00", "2", "0.5", "1.5", "3"] for r in rows]
        self.assertEqual(compare(rows, rest), (60, [], []))
        rest[0][4] = "1.6"
        matched, differences, _ = compare(rows, rest)
        self.assertEqual(matched, 59)
        self.assertIn("close:", differences[0])

    def test_incomplete_snapshot_and_duplicate_rest_fail(self):
        with self.assertRaises(ValueError):
            parse_log(self.log(missing=True))
        _, _, _, rows = parse_log(self.log())
        rest = [[r[0], "1", "2", "0.5", "1.5", "3"] for r in rows]
        with self.assertRaises(ValueError):
            compare(rows, rest + [rest[0]])


if __name__ == "__main__":
    unittest.main()
