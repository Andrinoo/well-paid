<#
.SYNOPSIS
  Grava o Base64 do .jks (keystore.properties) num ficheiro de texto UMA linha, para colar no secret GitHub sem depender do clipboard.

  Gera tambem part1/part2 (2500+resto) para dois secrets se a colagem no GitHub truncar (~500 chars).

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
$part1File = Join-Path $androidRoot "gh-keystore-base64-part1-for-github.txt"
$part2File = Join-Path $androidRoot "gh-keystore-base64-part2-for-github.txt"
$splitAt = 2500

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

$p1 = if ($b64.Length -le $splitAt) { $b64 } else { $b64.Substring(0, $splitAt) }
$p2 = if ($b64.Length -le $splitAt) { "" } else { $b64.Substring($splitAt) }
[IO.File]::WriteAllText($part1File, $p1, $utf8NoBom)
[IO.File]::WriteAllText($part2File, $p2, $utf8NoBom)

$len = $b64.Length
Write-Host ""
Write-Host "OK: escrevi $len caracteres em:" -ForegroundColor Green
Write-Host "  $outFile" -ForegroundColor Cyan
Write-Host "  $part1File ($($p1.Length) chars)" -ForegroundColor Cyan
Write-Host "  $part2File ($($p2.Length) chars)" -ForegroundColor Cyan
Write-Host ""
Write-Host "Se o GitHub truncar colagens longas, usa DOIS secrets (PART1 depois PART2, sem espaco entre eles no CI):" -ForegroundColor Yellow
Write-Host "  ANDROID_KEYSTORE_BASE64_PART1 = conteudo de part1 (ficheiro acima)" -ForegroundColor White
Write-Host "  ANDROID_KEYSTORE_BASE64_PART2 = conteudo de part2" -ForegroundColor White
Write-Host "Podes apagar ou esvaziar ANDROID_KEYSTORE_BASE64 se tinha valor errado; o workflow prefere PART1+PART2." -ForegroundColor DarkGray
Write-Host ""
Write-Host "Passwords: cola storePassword / keyPassword / keyAlias nos outros 3 secrets." -ForegroundColor White
Write-Host "Ficheiros estao no .gitignore - nao faz commit." -ForegroundColor DarkGray
Write-Host ""
