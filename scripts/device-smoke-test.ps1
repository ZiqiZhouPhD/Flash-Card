[CmdletBinding()]
param(
    [string]$Serial,
    [string]$AdbPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$packageName = "com.ziqiphyzhou.flashcard"
$testPackageName = "$packageName.test"
$runner = "$testPackageName/androidx.test.runner.AndroidJUnitRunner"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

if (-not $AdbPath) {
    $sdkAdb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path -LiteralPath $sdkAdb) {
        $AdbPath = $sdkAdb
    } else {
        $AdbPath = (Get-Command adb -ErrorAction Stop).Source
    }
}

$deviceLines = @(& $AdbPath devices) | Where-Object { $_ -match "\sdevice$" }
if (-not $Serial) {
    if ($deviceLines.Count -ne 1) {
        throw "Expected exactly one authorized device; found $($deviceLines.Count). Pass -Serial when needed."
    }
    $Serial = ($deviceLines[0] -split "\s+")[0]
}

function Invoke-Adb {
    param([Parameter(Mandatory)][string[]]$Arguments)

    $output = @(& $AdbPath -s $Serial @Arguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: $($Arguments -join ' ')`n$($output -join [Environment]::NewLine)"
    }
    return $output
}

function Get-FirstInstallTime {
    $packageDump = (Invoke-Adb -Arguments @("shell", "dumpsys", "package", $packageName)) -join "`n"
    $match = [regex]::Match($packageDump, "firstInstallTime=([^\r\n]+)")
    if ($match.Success) { return $match.Groups[1].Value.Trim() }
    return $null
}

$firstInstallTimeBefore = Get-FirstInstallTime
$testPackageInstalled = $false

Push-Location $projectRoot
try {
    & (Join-Path $projectRoot "gradlew.bat") test :app:assembleDebug :app:assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw "Gradle validation failed." }

    $appApk = Join-Path $projectRoot "app\build\outputs\apk\debug\app-debug.apk"
    $testApk = Join-Path $projectRoot "app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk"

    Write-Output "Updating $packageName in place on $Serial..."
    Invoke-Adb -Arguments @("install", "-r", "-t", $appApk) | Write-Output
    Invoke-Adb -Arguments @("install", "-r", "-t", $testApk) | Write-Output
    $testPackageInstalled = $true

    $firstInstallTimeAfter = Get-FirstInstallTime
    if ($firstInstallTimeBefore -and $firstInstallTimeAfter -ne $firstInstallTimeBefore) {
        throw "The app's firstInstallTime changed; the installation was not an in-place update."
    }

    Write-Output "Running isolated Room tests and the launch smoke test..."
    $instrumentationOutput = Invoke-Adb -Arguments @("shell", "am", "instrument", "-w", "-r", $runner)
    $instrumentationOutput | Write-Output
    if ($instrumentationOutput -match "FAILURES!!!|INSTRUMENTATION_FAILED") {
        throw "Instrumented tests failed."
    }

    Invoke-Adb -Arguments @("logcat", "-c") | Out-Null
    Invoke-Adb -Arguments @("shell", "am", "force-stop", $packageName) | Out-Null
    Invoke-Adb -Arguments @(
        "shell", "monkey", "-p", $packageName,
        "-c", "android.intent.category.LAUNCHER", "1"
    ) | Out-Null
    Start-Sleep -Seconds 2

    $crashLog = Invoke-Adb -Arguments @("logcat", "-b", "crash", "-d")
    if (($crashLog -join "`n") -match $packageName) {
        throw "The app produced a crash-buffer entry after launch.`n$($crashLog -join [Environment]::NewLine)"
    }

    Write-Output "PASS: tests passed, the app launched, and existing app data was preserved in place."
} finally {
    if ($testPackageInstalled) {
        try {
            Invoke-Adb -Arguments @("uninstall", $testPackageName) | Out-Null
            Write-Output "Removed the temporary test package; the application package and its data remain installed."
        } catch {
            Write-Warning "Could not remove the temporary test package: $_"
        }
    }
    Pop-Location
}
