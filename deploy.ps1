$package = "com.im_a_hero.daemon"
$service = "com.im_a_hero.daemon.TelemetryService"
$apkPath = ".\android_host\build\outputs\apk\debug\HeroNodeZero-debug.apk"

Write-Host "[1/4] Kompilacja (Gradle)..." -ForegroundColor Yellow
Set-Location .\android_host
.\gradlew.bat assembleDebug
$exitCode = $LASTEXITCODE
Set-Location ..
if ($exitCode -ne 0) { exit }

Write-Host "[2/4] Wstrzykiwanie (ADB)..." -ForegroundColor Yellow
adb install -r $apkPath
if ($LASTEXITCODE -ne 0) { exit }

Write-Host "[3/4] Lamanie zabezpieczen..." -ForegroundColor Yellow
adb shell appops set $package SYSTEM_ALERT_WINDOW allow

Write-Host "[4/4] Aktywacja Radaru Frustracji..." -ForegroundColor Yellow
adb shell settings put secure enabled_accessibility_services "$package/$service"
adb shell settings put secure accessibility_enabled 1

Write-Host "SUKCES! WEZEL ZERO UZBROJONY!" -ForegroundColor Green