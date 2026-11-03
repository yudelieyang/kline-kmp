param([string]$Adb = 'E:\Android\Sdk\platform-tools\adb.exe')
$ErrorActionPreference = 'Stop'
$PSNativeCommandUseErrorActionPreference = $true
$package = 'io.github.yudelieyang.kline'
$dir = 'docs/perf/results/live-run'
if (Test-Path $dir) { throw "Output already exists: $dir" }
New-Item -ItemType Directory -Force $dir | Out-Null
$log = (Resolve-Path $dir).Path + '\logcat.txt'
$err = (Resolve-Path $dir).Path + '\logcat-errors.txt'
$logProcess = Start-Process -FilePath $Adb -ArgumentList @('logcat', '-v', 'epoch', '-T', '1', 'KlinePerf:I', '*:S') -RedirectStandardOutput $log -RedirectStandardError $err -PassThru -WindowStyle Hidden
try {
    & $Adb shell am force-stop $package
    Get-Date -Format o | Out-File -Encoding utf8 "$dir/launch-time.txt"
    & $Adb shell am start -n "$package/.MainActivity" | Out-File -Encoding utf8 "$dir/start.txt"
    $deadline = [Diagnostics.Stopwatch]::StartNew()
    while ($deadline.Elapsed.TotalSeconds -lt 60) {
        if ($logProcess.HasExited) { throw 'Log capture stopped' }
        if ((Test-Path $log) -and (Select-String -Path $log -Pattern 'KlinePerf.*SYNC session=' -Quiet)) { break }
        Start-Sleep -Seconds 1
    }
    if (-not (Select-String -Path $log -Pattern 'KlinePerf.*SYNC session=' -Quiet)) { throw 'Live synchronization not observed' }
    Get-Date -Format o | Out-File -Encoding utf8 "$dir/meminfo-start-time.txt"
    & $Adb shell dumpsys meminfo $package | Out-File -Encoding utf8 "$dir/meminfo-start.txt"
    $timer = [Diagnostics.Stopwatch]::StartNew()
    Write-Output 'Live synchronized; one-hour interval started.'
    while ($timer.Elapsed.TotalSeconds -lt 3600) {
        if ($logProcess.HasExited) { throw 'Log capture stopped during the run' }
        Start-Sleep -Seconds 5
    }
    Get-Date -Format o | Out-File -Encoding utf8 "$dir/meminfo-end-time.txt"
    & $Adb shell dumpsys meminfo $package | Out-File -Encoding utf8 "$dir/meminfo-end.txt"
    $snapshotDeadline = [Diagnostics.Stopwatch]::StartNew()
    while ($snapshotDeadline.Elapsed.TotalSeconds -lt 120) {
        if ($logProcess.HasExited) { throw 'Log capture stopped before snapshot' }
        if (Select-String -Path $log -Pattern 'KlinePerf.*SNAPSHOT_END' -Quiet) { break }
        Start-Sleep -Seconds 2
    }
    if (-not (Select-String -Path $log -Pattern 'KlinePerf.*SNAPSHOT_END' -Quiet)) { throw 'Snapshot not observed' }
    Write-Output 'Live interval and snapshot completed.'
} finally {
    if (-not $logProcess.HasExited) { Stop-Process -Id $logProcess.Id }
}
