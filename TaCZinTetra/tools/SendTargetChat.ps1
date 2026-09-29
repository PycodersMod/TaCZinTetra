param(
    [Parameter(Mandatory = $true)][int]$ProcessId,
    [Parameter(Mandatory = $true)][string]$Command
)

$process = Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId"
if ($null -eq $process -or $process.Name -ne 'java.exe' -or
    $process.CommandLine -notlike '*--launchTarget forgeclientuserdev*' -or
    $process.CommandLine -notlike '*TaCZinTetra*') {
    throw "拒绝操作：PID 不是本项目的 Forge 客户端。"
}

Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class TargetWindow {
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern bool IsWindow(IntPtr hWnd);
    [DllImport("user32.dll")] public static extern void keybd_event(byte key, byte scan, uint flags, UIntPtr extra);
    public const uint UNICODE=0x0004, KEYUP=0x0002;
    public static void SendText(string text) {
        foreach (char c in text) {
            keybd_event(0, (byte)c, UNICODE, UIntPtr.Zero);
            keybd_event(0, (byte)c, UNICODE | KEYUP, UIntPtr.Zero);
        }
    }
    public static void SendEnter() {
        keybd_event(0x0D, 0, 0, UIntPtr.Zero);
        keybd_event(0x0D, 0, KEYUP, UIntPtr.Zero);
    }
}
'@

$window = Get-Process -Id $ProcessId -ErrorAction Stop
$handle = $window.MainWindowHandle
if ($handle -eq [IntPtr]::Zero -or -not [TargetWindow]::IsWindow($handle)) {
    throw "拒绝操作：目标客户端尚未提供有效窗口句柄。"
}

if (-not [TargetWindow]::SetForegroundWindow($handle)) {
    throw "拒绝操作：无法激活目标窗口。"
}
Start-Sleep -Milliseconds 250
[TargetWindow]::SendText('t')
Start-Sleep -Milliseconds 100
[TargetWindow]::SendText($Command)
[TargetWindow]::SendEnter()
