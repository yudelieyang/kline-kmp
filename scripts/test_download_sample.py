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
                patch.object(download_sample, "get_json", side_effect=[{"serverTime": 250}, rows]) as request, \
                contextlib.redirect_stdout(io.StringIO()):
            download_sample.main()
        output = "".join(call.args[0] for call in file().write.call_args_list)
        result = list(csv.reader(io.StringIO(output)))
        self.assertEqual(result[1], ["100", "10.00", "12.00", "9.00", "11.00", "0.000", "true"])
        self.assertEqual(result[2][-1], "false")
        self.assertEqual(result[3][-1], "false")
        self.assertEqual([call.args[0] for call in request.call_args_list], [download_sample.TIME_URL, download_sample.KLINES_URL])


if __name__ == "__main__":
    unittest.main()
