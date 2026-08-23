$ErrorActionPreference = "Stop"
$emulator = "D:\AndroidSdkLite\emulator\emulator.exe"
$adb = "C:\Users\HP\Android\Sdk\platform-tools\adb.exe"
$env:ANDROID_AVD_HOME = "D:\AndroidAvd"

if (-not (Test-Path -LiteralPath $emulator)) {
    throw "Light emulator is not installed at $emulator"
}

& $adb start-server | Out-Null
$running = & $adb devices | Select-String "emulator-"
if (-not $running) {
    Start-Process -FilePath $emulator -ArgumentList @(
        "-avd", "SehatiLite",
        "-no-audio",
        "-no-boot-anim",
        "-gpu", "swiftshader_indirect",
        "-no-snapshot",
        "-memory", "2048",
        "-cores", "2"
    )
}

Write-Host "Starting SehatiLite. Wait for the Android home screen, then run .\install-debug-apk.ps1"
