$ErrorActionPreference = "Stop"
$adb = "C:\Users\HP\Android\Sdk\platform-tools\adb.exe"
$apk = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path -LiteralPath $apk)) {
    throw "Build the debug APK first: gradle assembleDebug"
}

& $adb wait-for-device
do {
    Start-Sleep -Seconds 2
    $boot = (& $adb shell getprop sys.boot_completed).Trim()
} while ($boot -ne "1")

& $adb install -r $apk
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& $adb shell am start -n "com.aistudio.sehatihomecare.sd/com.example.MainActivity"
