param(
  [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$adminUrl = "http://localhost:5173"

Write-Host "Starting Well Paid admin console..."
Set-Location $projectRoot

if (-not $NoBrowser) {
  Start-Process $adminUrl | Out-Null
}

npm run dev -- --host
