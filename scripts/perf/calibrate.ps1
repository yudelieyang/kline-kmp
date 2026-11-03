param([string]$Adb = 'E:\Android\Sdk\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
$package = 'io.github.yudelieyang.kline'
& $Adb shell am force-stop $package
& $Adb shell am start -n "$package/.MainActivity" --ez perf_mode true --ei perf_count 500 --ei perf_width_dp 8
Start-Sleep -Seconds 12
& $Adb shell screencap -p /sdcard/kline-perf.png
& $Adb pull /sdcard/kline-perf.png docs/perf/results/calibration-before.png
& $Adb shell input swipe 300 1600 900 1600 400
Start-Sleep -Seconds 2
& $Adb shell screencap -p /sdcard/kline-perf.png
& $Adb pull /sdcard/kline-perf.png docs/perf/results/calibration-after.png
