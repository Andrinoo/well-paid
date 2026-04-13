# Run Gradle with a JDK (Android Studio JBR or common paths).
# Usage: .\gradlew-with-jdk.ps1 :app:assembleDebug

$candidates = @(
    "D:\Dev\Android\Sdk\jbr",
    "$env:LOCALAPPDATA\Programs\Android Studio\jbr",
    "$env:ProgramFiles\Android\Android Studio\jbr",
    "${env:ProgramFiles(x86)}\Android\Android Studio\jbr",
    "C:\Program Files\Android\Android Studio\jbr",
    "$env:LOCALAPPDATA\Android\Sdk\jbr"
)

$found = $null
foreach ($dir in $candidates) {
    if ($dir -and (Test-Path (Join-Path $dir "bin\java.exe"))) {
        $found = $dir
        break
    }
}

if (-not $found) {
    Write-Host "JDK not found (bin\java.exe). Set JAVA_HOME to your jbr folder." -ForegroundColor Red
    Write-Host "Android Studio: Settings - Build - Gradle - Gradle JDK (copy path)." -ForegroundColor Yellow
    Write-Host "Example:  `$env:JAVA_HOME = 'D:\Dev\Android\Sdk\jbr'" -ForegroundColor Cyan
    exit 1
}

$env:JAVA_HOME = $found
$env:Path = "$found\bin;$env:Path"
Write-Host "JAVA_HOME = $found" -ForegroundColor Green

$gradlew = Join-Path $PSScriptRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Host "gradlew.bat not found in $PSScriptRoot" -ForegroundColor Red
    exit 1
}

& $gradlew @args
exit $LASTEXITCODE
