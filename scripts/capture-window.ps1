param(
    [Parameter(Mandatory = $true)][int]$ProcessId,
    [Parameter(Mandatory = $true)][string]$OutputPath
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public static class CaptureWindowNative {
    [StructLayout(LayoutKind.Sequential)]
    public struct Rect { public int Left, Top, Right, Bottom; }
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out Rect rect);
    [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr hWnd, int command);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr hWnd, int attribute, out Rect rect, int size);
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern bool MoveWindow(IntPtr hWnd, int x, int y, int width, int height, bool repaint);
}
"@
[CaptureWindowNative]::SetProcessDPIAware() | Out-Null
$process = Get-Process -Id $ProcessId
$handle = $process.MainWindowHandle
if ($handle -eq [IntPtr]::Zero) { throw 'The process has no visible main window.' }
[CaptureWindowNative]::ShowWindow($handle, 9) | Out-Null
[CaptureWindowNative]::SetForegroundWindow($handle) | Out-Null
Start-Sleep -Seconds 2
if ([CaptureWindowNative]::GetForegroundWindow() -ne $handle) { throw 'Could not bring the target window to the foreground.' }
$rect = New-Object CaptureWindowNative+Rect
if (-not [CaptureWindowNative]::GetWindowRect($handle, [ref]$rect)) { throw 'Could not read the window bounds.' }
$width = $rect.Right - $rect.Left
$height = $rect.Bottom - $rect.Top
if ($width -le 0 -or $height -le 0) { throw 'The target window has invalid bounds.' }
$workArea = [System.Windows.Forms.Screen]::FromHandle($handle).WorkingArea
if ($width -gt $workArea.Width -or $height -gt $workArea.Height -or $rect.Left -lt $workArea.Left -or $rect.Top -lt $workArea.Top -or $rect.Right -gt $workArea.Right -or $rect.Bottom -gt $workArea.Bottom) {
    $width = [Math]::Min($width, $workArea.Width)
    $height = [Math]::Min($height, $workArea.Height)
    $x = [Math]::Max($workArea.Left, [Math]::Min($rect.Left, $workArea.Right - $width))
    $y = [Math]::Max($workArea.Top, [Math]::Min($rect.Top, $workArea.Bottom - $height))
    if (-not [CaptureWindowNative]::MoveWindow($handle, $x, $y, $width, $height, $true)) { throw 'Could not fit the window inside the work area.' }
    Start-Sleep -Seconds 2
    if (-not [CaptureWindowNative]::GetWindowRect($handle, [ref]$rect)) { throw 'Could not read the resized window bounds.' }
    $width = $rect.Right - $rect.Left
    $height = $rect.Bottom - $rect.Top
}
$frame = New-Object CaptureWindowNative+Rect
if ([CaptureWindowNative]::DwmGetWindowAttribute($handle, 9, [ref]$frame, [Runtime.InteropServices.Marshal]::SizeOf($frame)) -eq 0) {
    $rect = $frame
    $width = $rect.Right - $rect.Left
    $height = $rect.Bottom - $rect.Top
}
$bitmap = [System.Drawing.Bitmap]::new($width, $height)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
try {
    $graphics.CopyFromScreen($rect.Left, $rect.Top, 0, 0, [System.Drawing.Size]::new($width, $height))
    $bitmap.Save([System.IO.Path]::GetFullPath($OutputPath), [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Output "Captured process $ProcessId at $($rect.Left),$($rect.Top), size ${width}x${height}: $OutputPath"
} finally {
    $graphics.Dispose()
    $bitmap.Dispose()
}
