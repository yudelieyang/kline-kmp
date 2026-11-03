import contextlib
import csv
import io
from pathlib import Path
import unittest
from unittest.mock import mock_open, patch

import download_sample


class DownloadSampleTest(unittest.TestCase):
    def test_preserves_prices_and_uses_server_time(self):
        rows = [
            [100, "10.00", "12.00", "9.00", "11.00", "0.000", 199],
            [200, "11.00", "13.00", "10.00", "12.00", "2.500", 250],
            [251, "12.00", "14.00", "11.00", "13.00", "1.000", 299],
        ]
        file = mock_open()
        with patch.object(download_sample, "OUTPUT", Path("sample.csv")), \
                patch.object(Path, "open", file), \
                patch("sys.argv", ["download_sample.py"]), \
                patch.object(download_sample, "get_json", side_effect=[{"serverTime": 250}, rows]) as request, \
                contextlib.redirect_stdout(io.StringIO()):
            download_sample.main()
        output = "".join(call.args[0] for call in file().write.call_args_list)
        result = list(csv.reader(io.StringIO(output)))
        self.assertEqual(result[1], ["100", "10.00", "12.00", "9.00", "11.00", "0.000", "true"])
        self.assertEqual(result[2][-1], "false")
        self.assertEqual(result[3][-1], "false")
        self.assertEqual([call.args[0] for call in request.call_args_list], [download_sample.TIME_URL, download_sample.KLINES_URL])

    def test_paginated_closed_candles_and_gap_detection(self):
        def row(time):
            return [time, "1", "1", "1", "1", "1", time + 59999]

        with patch.object(download_sample, "get_json", side_effect=[[row(120000), row(180000)], [row(0), row(60000)]]):
            with contextlib.redirect_stdout(io.StringIO()):
                rows = download_sample.download_closed(4, 240000)
        self.assertEqual([r[0] for r in rows], [0, 60000, 120000, 180000])
        with patch.object(download_sample, "get_json", return_value=[row(0), row(120000)]):
            with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(ValueError):
                download_sample.download_closed(2, 240000)
        with patch.object(download_sample, "get_json", side_effect=[[row(120000)], [row(120000)]]):
            with contextlib.redirect_stdout(io.StringIO()), self.assertRaises(ValueError):
                download_sample.download_closed(2, 240000)


if __name__ == "__main__":
    unittest.main()
