<#
.SYNOPSIS
  Gera UM ficheiro de texto com todos os valores para os secrets do GitHub:
  ANDROID_KEYSTORE_BASE64 (linha completa) + passwords + alias.

  Usa Repository secrets (nao Variables). APAGA o ficheiro depois. NUNCA facas commit.

.EXAMPLE
  cd "D:\Projects\Well Paid"
  powershell -ExecutionPolicy Bypass -File .\android-native\scripts\Write-GithubSecretsCopyPasteSheet.ps1
#>
[CmdletBinding()]
param(
    [string] $OutFileName = "gh-github-secrets-folha-unica.txt"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..") | Select-Object -ExpandProperty Path
$androidRoot = Join-Path $repoRoot "android-native"
$propsPath = Join-Path $androidRoot "keystore.properties"
$outPath = Join-Path $androidRoot $OutFileName

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

foreach ($k in @("storeFile", "storePassword", "keyPassword", "keyAlias")) {
    if (-not $props.ContainsKey($k) -or [string]::IsNullOrWhiteSpace($props[$k])) {
        Write-Error "Falta '$k' em keystore.properties"
    }
}

$storeRel = $props["storeFile"]
$jksPath = if ([IO.Path]::IsPathRooted($storeRel)) { $storeRel } else { Join-Path $androidRoot $storeRel }
if (-not (Test-Path -LiteralPath $jksPath)) {
    Write-Error "Nao encontrei o keystore: $jksPath"
}

$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($jksPath)).Trim()
try {
    [void][Convert]::FromBase64String($b64)
} catch {
    Write-Error "Base64 gerado invalido: $($_.Exception.Message)"
}

$sp = $props["storePassword"]
$kp = $props["keyPassword"]
$ka = $props["keyAlias"]

$lines = @(
    "================================================================================",
    "FOLHA UNICA - todos os secrets GitHub (Actions -> Repository SECRETS, nao Variables).",
    "APAGA este ficheiro depois de colar. NAO COMMITES.",
    "================================================================================",
    "",
    "Copia SO o texto entre VALUE_START e VALUE_END (sem essas linhas) para cada secret.",
    "",
    "--- SECRET: ANDROID_KEYSTORE_BASE64 ---",
    "VALUE_START",
    $b64,
    "VALUE_END",
    "",
    "--- SECRET: ANDROID_KEYSTORE_PASSWORD ---",
    "VALUE_START",
    $sp,
    "VALUE_END",
    "",
    "--- SECRET: ANDROID_KEY_PASSWORD ---",
    "VALUE_START",
    $kp,
    "VALUE_END",
    "",
    "--- SECRET: ANDROID_KEY_ALIAS ---",
    "VALUE_START",
    $ka,
    "VALUE_END",
    "",
    "================================================================================",
    "Opcional: remove ANDROID_KEYSTORE_BASE64_PART1 e PART2 se ja nao precisares.",
    "Se a colagem do Base64 truncar no browser, usa gh secret set ou Write-KeystoreBase64ForGithub.ps1 (part1/part2).",
    "Base64: $($b64.Length) caracteres (validado localmente).",
    "================================================================================"
)

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$body = $lines -join "`r`n"
[IO.File]::WriteAllText($outPath, $body, $utf8NoBom)

Write-Host ""
Write-Host "OK: folha unica gerada ($($b64.Length) chars Base64):" -ForegroundColor Green
Write-Host "  $outPath" -ForegroundColor Cyan
Write-Host ""
Write-Host "4 secrets: ANDROID_KEYSTORE_BASE64 + PASSWORD + KEY_PASSWORD + KEY_ALIAS" -ForegroundColor Yellow
Write-Host ""
