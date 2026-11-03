param(
    [string]$Adb = 'E:\Android\Sdk\platform-tools\adb.exe'
)
$ErrorActionPreference = 'Stop'
$package = 'io.github.yudelieyang.kline'
$activity = "$package/.MainActivity"
$output = 'docs/perf/results'
New-Item -ItemType Directory -Force $output | Out-Null
$cases = @(
    @{ Name = 'offline-500-8'; Count = 500; Width = 8 },
    @{ Name = 'offline-500-2'; Count = 500; Width = 2 },
    @{ Name = 'offline-5000-8'; Count = 5000; Width = 8 },
    @{ Name = 'offline-5000-2'; Count = 5000; Width = 2 },
    @{ Name = 'live' }
)
foreach ($case in $cases) {
    & $Adb shell am force-stop $package
    if ($case.Name -eq 'live') {
        & $Adb shell am start -n $activity
    } else {
        & $Adb shell am start -n $activity --ez perf_mode true --ei perf_count $case.Count --ei perf_width_dp $case.Width
    }
    Start-Sleep -Seconds 12
    & $Adb shell screencap -p /sdcard/kline-perf.png
    & $Adb pull /sdcard/kline-perf.png "$output/$($case.Name).png"
}
