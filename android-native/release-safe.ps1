param(
    [switch]$NoKillJava,
    [switch]$SkipGlobalGradleCleanup
)

$ErrorActionPreference = "Stop"

function Write-Step([string]$Message) {
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Remove-DirRobust([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    Write-Step "Cleaning $Path"
    try {
        Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction Stop
        return
    } catch {
        Write-Host "   PowerShell remove failed, trying cmd rmdir..." -ForegroundColor Yellow
    }

    cmd /c "rmdir /s /q `"$Path`""
    if (Test-Path -LiteralPath $Path) {
        throw "Could not remove locked directory: $Path"
    }
}

Write-Step "Stopping Gradle daemons"
& ".\gradlew.bat" --stop | Out-Host

if (-not $NoKillJava) {
    Write-Step "Killing java/javaw/gradle processes (best effort)"
    Get-Process java, javaw, gradle -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 800
}

Write-Step "Removing module build outputs"
$paths = @(
    (Join-Path (Get-Location) "app\build"),
    (Join-Path (Get-Location) "core\model\build"),
    (Join-Path (Get-Location) "core\network\build"),
    (Join-Path (Get-Location) "core\datastore\build"),
    (Join-Path (Get-Location) "baselineprofile\build"),
    (Join-Path (Get-Location) "build")
)

foreach ($path in $paths) {
    Remove-DirRobust -Path $path
}

if (-not $SkipGlobalGradleCleanup) {
    Write-Step "Removing project .gradle cache (optional safety)"
    Remove-DirRobust -Path (Join-Path (Get-Location) ".gradle")
}

Write-Step "Running release build (no daemon)"
& ".\gradlew.bat" "assembleRelease" "--no-daemon" | Out-Host

Write-Step "Done"
