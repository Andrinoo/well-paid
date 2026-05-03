<#
.SYNOPSIS
  Prepara os valores para colar nos GitHub Actions secrets (assinatura release do workflow android-apk-release).

.DESCRIPTION
  Lê android-native/keystore.properties (não versionado) e o ficheiro .jks referenciado por storeFile.
  - ANDROID_KEYSTORE_BASE64 → copia para a área de transferência (uma linha).
  - Os outros três segredos: cola um de cada vez com confirmação (clipboard), para não aparecerem no ecrã.

  No GitHub: Settings → Secrets and variables → Actions → New repository secret
  Nomes exactos: ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_PASSWORD, ANDROID_KEY_ALIAS

.EXAMPLE
  cd "D:\Projects\Well Paid"
  pwsh .\android-native\scripts\Export-GithubAndroidSecrets.ps1
#>
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..") | Select-Object -ExpandProperty Path
$androidRoot = Join-Path $repoRoot "android-native"
$propsPath = Join-Path $androidRoot "keystore.properties"

if (-not (Test-Path -LiteralPath $propsPath)) {
    Write-Error "Não encontrei $propsPath. Cria o keystore.properties (como no teu PC de release) e volta a correr o script."
}

$props = @{}
Get-Content -LiteralPath $propsPath | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) { return }
    $eq = $line.IndexOf("=")
    if ($eq -lt 1) { return }
    $k = $line.Substring(0, $eq).Trim()
    $v = $line.Substring($eq + 1).Trim()
    $props[$k] = $v
}

foreach ($required in @("storeFile", "storePassword", "keyPassword", "keyAlias")) {
    if (-not $props.ContainsKey($required) -or [string]::IsNullOrWhiteSpace($props[$required])) {
        Write-Error "Falta a propriedade '$required' em keystore.properties."
    }
}

$storeRel = $props["storeFile"]
$jksPath = if ([IO.Path]::IsPathRooted($storeRel)) {
    $storeRel
} else {
    Join-Path $androidRoot $storeRel
}

if (-not (Test-Path -LiteralPath $jksPath)) {
    Write-Error "Não encontrei o keystore: $jksPath (storeFile=$storeRel)"
}

$bytes = [IO.File]::ReadAllBytes($jksPath)
$b64 = [Convert]::ToBase64String($bytes)
Set-Clipboard -Text $b64

Write-Host ""
Write-Host "OK: ANDROID_KEYSTORE_BASE64 está na área de transferência (${($b64.Length)} caracteres)." -ForegroundColor Green
Write-Host "1) GitHub → repo well-paid → Settings → Secrets and variables → Actions → New secret"
Write-Host "   Nome: ANDROID_KEYSTORE_BASE64   Valor: Ctrl+V"
Write-Host ""

function Copy-SecretToClipboard {
    param([string]$Label, [string]$Value)
    Write-Host "Próximo: $Label — Enter para copiar para a área de transferência (o valor não é mostrado)..."
    Read-Host | Out-Null
    Set-Clipboard -Text $Value
    Write-Host "  Copiado. Cola no GitHub como secret $Label e grava." -ForegroundColor Cyan
    Write-Host ""
}

Copy-SecretToClipboard -Label "ANDROID_KEYSTORE_PASSWORD" -Value $props["storePassword"]
Copy-SecretToClipboard -Label "ANDROID_KEY_PASSWORD" -Value $props["keyPassword"]
Copy-SecretToClipboard -Label "ANDROID_KEY_ALIAS" -Value $props["keyAlias"]

Write-Host "Quando os 4 segredos estiverem criados, dispara o workflow:" -ForegroundColor Yellow
Write-Host "  Actions → Android APK release → Run workflow → release_tag = v0.1.xx (igual ao versionName que queres publicar)"
Write-Host ""
