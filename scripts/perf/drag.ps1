param(
    [switch]$Trial,
    [string]$Adb = 'E:\Android\Sdk\platform-tools\adb.exe'
)
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
$package = 'io.github.yudelieyang.kline'
$root = 'docs/perf/results/drag'
New-Item -ItemType Directory -Force $root | Out-Null
if (-not $Trial) {
    & $Adb shell getprop ro.product.model | Out-File -Encoding utf8 "$root/device-model.txt"
    & $Adb shell getprop ro.build.version.release | Out-File -Encoding utf8 "$root/android-version.txt"
    & $Adb shell settings get global stay_on_while_plugged_in | Out-File -Encoding utf8 "$root/stay-awake.txt"
    & $Adb shell settings get global low_power | Out-File -Encoding utf8 "$root/power-saving.txt"
    & $Adb shell settings get system peak_refresh_rate | Out-File -Encoding utf8 "$root/peak-refresh-setting.txt"
    & $Adb shell settings get system min_refresh_rate | Out-File -Encoding utf8 "$root/min-refresh-setting.txt"
    & $Adb shell dumpsys battery | Out-File -Encoding utf8 "$root/battery-before.txt"
    & $Adb shell wm size | Out-File -Encoding utf8 "$root/screen-size.txt"
}
$cases = @(
    @{ Count = 500; Width = 8 }, @{ Count = 500; Width = 2 },
    @{ Count = 5000; Width = 8 }, @{ Count = 5000; Width = 2 }
)
if ($Trial) { $cases = @($cases[0]); $rounds = 1 } else { $rounds = 5 }
for ($round = 1; $round -le $rounds; $round++) {
    foreach ($case in $cases) {
        $name = if ($Trial) { 'trial' } else { "round-$round-$($case.Count)-$($case.Width)" }
        $dir = "$root/$name"
        if (Test-Path $dir) { throw "Output already exists: $dir" }
        New-Item -ItemType Directory $dir | Out-Null
        & $Adb shell am force-stop $package
        & $Adb shell am start -n "$package/.MainActivity" --ez perf_mode true --ei perf_count $case.Count --ei perf_width_dp $case.Width | Out-File -Encoding utf8 "$dir/start.txt"
        Start-Sleep -Seconds 12
        & $Adb shell uiautomator dump /sdcard/kline-perf-window.xml | Out-File -Encoding utf8 "$dir/ui-dump-command.txt"
        & $Adb shell cat /sdcard/kline-perf-window.xml | Out-File -Encoding utf8 "$dir/ui.xml"
        if (-not (Select-String -Path "$dir/ui.xml" -Pattern "离线样例 $($case.Count) 根|Offline sample $($case.Count)" -Quiet)) {
            throw "Offline chart readiness could not be verified: $dir"
        }
        Start-Sleep -Seconds 2
        & $Adb shell dumpsys battery | Out-File -Encoding utf8 "$dir/battery.txt"
        & $Adb shell dumpsys display | Out-File -Encoding utf8 "$dir/display-before.txt"
        & $Adb shell screencap -p /sdcard/kline-perf.png
        & $Adb pull /sdcard/kline-perf.png "$dir/before.png"
        & $Adb shell dumpsys gfxinfo $package reset | Out-File -Encoding utf8 "$dir/reset.txt"
        for ($swipe = 0; $swipe -lt 20; $swipe++) {
            if ($swipe % 2 -eq 0) { & $Adb shell input swipe 300 1600 900 1600 400 }
            else { & $Adb shell input swipe 900 1600 300 1600 400 }
            if ($Trial -and $swipe -eq 0) {
                & $Adb shell screencap -p /sdcard/kline-perf.png
                & $Adb pull /sdcard/kline-perf.png "$dir/first-swipe.png"
            }
            if ($swipe -eq 9) {
                & $Adb shell dumpsys display | Out-File -Encoding utf8 "$dir/display-during.txt"
            }
        }
        & $Adb shell dumpsys gfxinfo $package | Out-File -Encoding utf8 "$dir/gfxinfo.txt"
        & $Adb shell dumpsys display | Out-File -Encoding utf8 "$dir/display-after.txt"
        Write-Output "Completed $name"
    }
}
