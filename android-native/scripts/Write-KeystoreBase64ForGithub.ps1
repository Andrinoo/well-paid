<#
.SYNOPSIS
  Grava o Base64 do .jks (keystore.properties) num ficheiro de texto UMA linha — para colar no secret GitHub sem depender do clipboard.

  O GitHub web as vezes trunca colagens longas; abre o ficheiro no VS Code, Ctrl+A, Ctrl+C, cola no secret e confirma que o tamanho bate.

.EXAMPLE
  cd "D:\Projects\Well Paid"
  powershell -ExecutionPolicy Bypass -File .\android-native\scripts\Write-KeystoreBase64ForGithub.ps1
#>
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..") | Select-Object -ExpandProperty Path
$androidRoot = Join-Path $repoRoot "android-native"
$propsPath = Join-Path $androidRoot "keystore.properties"
$outFile = Join-Path $androidRoot "gh-keystore-base64-for-github.txt"

if (-not (Test-Path -LiteralPath $propsPath)) {
    Write-Error "Nao encontrei $propsPath"
}

$props = @{}
Get-Content -LiteralPath $propsPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $eq = $line.IndexOf("=")
    if ($eq -lt 1) { return }
    $props[$line.Substring(0, $eq).Trim()] = $line.Substring($eq + 1).Trim()
}

$storeRel = $props["storeFile"]
$jksPath = if ([IO.Path]::IsPathRooted($storeRel)) { $storeRel } else { Join-Path $androidRoot $storeRel }
if (-not (Test-Path -LiteralPath $jksPath)) {
    Write-Error "Nao encontrei o keystore: $jksPath"
}

$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($jksPath)).Trim()
# UTF-8 sem BOM = uma linha continua; evita editores a acrescentarem marca.
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[IO.File]::WriteAllText($outFile, $b64, $utf8NoBom)

$len = $b64.Length
Write-Host ""
Write-Host "OK: escrevi $len caracteres em:" -ForegroundColor Green
Write-Host "  $outFile" -ForegroundColor Cyan
Write-Host ""
Write-Host "Confirma no VS Code (barra de estado ou (Get-Content -Raw).Length) = $len" -ForegroundColor Yellow
Write-Host "GitHub: Settings -> Secrets -> ANDROID_KEYSTORE_BASE64 -> Update -> apaga tudo -> cola do ficheiro (Ctrl+A no ficheiro, Ctrl+C)." -ForegroundColor White
Write-Host "Este ficheiro esta no .gitignore — nao faz commit." -ForegroundColor DarkGray
Write-Host ""
