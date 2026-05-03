<#
.SYNOPSIS
  Gera um ficheiro de texto com todos os valores para colar nos secrets do GitHub (PART1, PART2, passwords, alias).

  O ficheiro fica em android-native/ e esta no .gitignore. APAGA-O depois de usar. NUNCA facas commit.

.EXAMPLE
  cd "D:\Projects\Well Paid"
  powershell -ExecutionPolicy Bypass -File .\android-native\scripts\Write-GithubSecretsCopyPasteSheet.ps1
#>
[CmdletBinding()]
param(
    [string] $OutFileName = "gh-github-secrets-PASTE-AQUI.txt"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..") | Select-Object -ExpandProperty Path
$androidRoot = Join-Path $repoRoot "android-native"
$propsPath = Join-Path $androidRoot "keystore.properties"
$outPath = Join-Path $androidRoot $OutFileName
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
$p1 = if ($b64.Length -le $splitAt) { $b64 } else { $b64.Substring(0, $splitAt) }
$p2 = if ($b64.Length -le $splitAt) { "" } else { $b64.Substring($splitAt) }

$sp = $props["storePassword"]
$kp = $props["keyPassword"]
$ka = $props["keyAlias"]

$lines = @(
    "================================================================================",
    "APAGA ESTE FICHEIRO DEPOIS DE COLAR NO GITHUB. NAO COMMITES. Contem segredos.",
    "GitHub: Settings -> Secrets and variables -> Actions -> New repository secret",
    "Nomes dos secrets tem de ser EXACTOS (case sensitive).",
    "================================================================================",
    "",
    "Copia o texto ENTRE as linhas VALUE_START e VALUE_END (sem essas linhas) para o secret indicado.",
    "",
    "--- SECRET: ANDROID_KEYSTORE_BASE64_PART1 ---",
    "VALUE_START",
    $p1,
    "VALUE_END",
    "",
    "--- SECRET: ANDROID_KEYSTORE_BASE64_PART2 ---",
    "VALUE_START",
    $p2,
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
    "Opcional: remove o secret ANDROID_KEYSTORE_BASE64 se existir (valor truncado).",
    "O workflow usa PART1+PART2 quando PART1 nao esta vazio.",
    "Base64 total: $($b64.Length) chars | PART1: $($p1.Length) | PART2: $($p2.Length)",
    "================================================================================"
)

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$body = $lines -join "`r`n"
[IO.File]::WriteAllText($outPath, $body, $utf8NoBom)

Write-Host ""
Write-Host "OK: ficheiro gerado (contem segredos - apaga depois):" -ForegroundColor Green
Write-Host "  $outPath" -ForegroundColor Cyan
Write-Host ""
Write-Host "Abre no VS Code e copia cada bloco entre VALUE_START e VALUE_END para o secret indicado." -ForegroundColor Yellow
Write-Host ""
