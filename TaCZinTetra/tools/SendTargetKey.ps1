param(
    [Parameter(Mandatory = $true)][int]$ProcessId,
    [Parameter(Mandatory = $true)][ValidateSet('R','F','LMB','RMB')][string]$InputName
)

$process = Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId"
if ($null -eq $process -or $process.Name -ne 'java.exe' -or
    $process.CommandLine -notlike '*--launchTarget forgeclientuserdev*' -or
    $process.CommandLine -notlike '*TaCZinTetra*') { throw '拒绝操作：PID 不是本项目 Forge 客户端。' }

Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class TargetInput {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool IsWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern void mouse_event(uint flags, uint dx, uint dy, uint data, UIntPtr extra);
    [DllImport("user32.dll")] public static extern void keybd_event(byte key, byte scan, uint flags, UIntPtr extra);
    public const uint LEFTDOWN=0x0002, LEFTUP=0x0004, RIGHTDOWN=0x0008, RIGHTUP=0x0010;
    public const uint KEYUP=0x0002;
}
'@

$window = Get-Process -Id $ProcessId -ErrorAction Stop
if ($window.MainWindowHandle -eq [IntPtr]::Zero -or -not [TargetInput]::IsWindow($window.MainWindowHandle)) {
    throw '拒绝操作：目标客户端没有有效窗口句柄。'
}
if (-not [TargetInput]::SetForegroundWindow($window.MainWindowHandle)) { throw '拒绝操作：无法激活目标窗口。' }
Start-Sleep -Milliseconds 250

if ($InputName -in @('R','F')) {
    $virtualKey = if ($InputName -eq 'R') { 0x52 } else { 0x46 }
    [TargetInput]::keybd_event($virtualKey, 0, 0, [UIntPtr]::Zero)
    [TargetInput]::keybd_event($virtualKey, 0, [TargetInput]::KEYUP, [UIntPtr]::Zero)
} elseif ($InputName -eq 'LMB') {
    [TargetInput]::mouse_event([TargetInput]::LEFTDOWN, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [TargetInput]::mouse_event([TargetInput]::LEFTUP, 0, 0, 0, [UIntPtr]::Zero)
} else {
    [TargetInput]::mouse_event([TargetInput]::RIGHTDOWN, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [TargetInput]::mouse_event([TargetInput]::RIGHTUP, 0, 0, 0, [UIntPtr]::Zero)
}
