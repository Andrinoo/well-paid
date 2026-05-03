<#
.SYNOPSIS
  Envia os 4 segredos de assinatura Android para o GitHub Actions (ou copia para a área de transferência).

.DESCRIPTION
  Lê android-native/keystore.properties e o .jks referenciado por storeFile.

  Modo A (preferido): se o GitHub CLI (`gh`) estiver instalado e autenticado (`gh auth login`),
  corre `gh secret set` para o repositório remoto origin (GitHub).

  Modo B: se `gh` não existir ou falhar autenticação, usa a área de transferência como antes.

  Nomes dos segredos: ANDROID_KEYSTORE_BASE64, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_PASSWORD, ANDROID_KEY_ALIAS

.PARAMETER Repo
  Override `don/repo` (por defeito detecta a partir de `git remote get-url origin`).

.PARAMETER ClipboardOnly
  Não tenta `gh`; só preenche a área de transferência.

.EXAMPLE
  cd "D:\Projects\Well Paid"
  powershell -ExecutionPolicy Bypass -File .\android-native\scripts\Export-GithubAndroidSecrets.ps1

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File .\android-native\scripts\Export-GithubAndroidSecrets.ps1 -Repo "Andrinoo/well-paid"
#>
[CmdletBinding()]
param(
    [string] $Repo = "",
    [switch] $ClipboardOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..") | Select-Object -ExpandProperty Path
$androidRoot = Join-Path $repoRoot "android-native"
$propsPath = Join-Path $androidRoot "keystore.properties"

if (-not (Test-Path -LiteralPath $propsPath)) {
    Write-Error "Não encontrei $propsPath. Cria o keystore.properties e volta a correr o script."
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
# Uma linha continua; evita newline final se `gh secret set` ler stdin com \n extra.
$b64 = [Convert]::ToBase64String($bytes).Trim()

function Get-GitHubRepoSlug {
    param([string] $Root)
    $git = Get-Command git -ErrorAction SilentlyContinue
    if (-not $git) { return $null }
    $url = & git -C $Root remote get-url origin 2>$null
    if ([string]::IsNullOrWhiteSpace($url)) { return $null }
    if ($url -match 'github\.com[:/]([^/]+)/([^/.]+)') {
        return "$($Matches[1])/$($Matches[2])"
    }
    return $null
}

function Test-GhCli {
    return $null -ne (Get-Command gh -ErrorAction SilentlyContinue)
}

function Test-GhAuth {
    $gh = (Get-Command gh -ErrorAction Stop).Source
    $null = & $gh auth status 2>&1
    return $LASTEXITCODE -eq 0
}

function Invoke-GhSecretSet {
    param(
        [Parameter(Mandatory)] [string] $Slug,
        [Parameter(Mandatory)] [string] $Name,
        [Parameter(Mandatory)] [string] $Value
    )
    $gh = (Get-Command gh -ErrorAction Stop).Source
    $Value | & $gh secret set $Name --repo $Slug
    if ($LASTEXITCODE -ne 0) {
        throw "gh secret set $Name falhou (exit $LASTEXITCODE)."
    }
}

$slug = $Repo.Trim()
if ([string]::IsNullOrWhiteSpace($slug)) {
    $slug = Get-GitHubRepoSlug -Root $repoRoot
}

$useGh = -not $ClipboardOnly -and (Test-GhCli) -and -not [string]::IsNullOrWhiteSpace($slug)

if ($useGh) {
    if (-not (Test-GhAuth)) {
        Write-Warning "gh não está autenticado. Corre: gh auth login   — a usar modo área de transferência."
        $useGh = $false
    }
}

if ($useGh) {
    Write-Host "Modo: GitHub CLI (repo $slug)." -ForegroundColor Cyan
    Write-Host "A enviar segredos para GitHub..." -ForegroundColor Green
    Invoke-GhSecretSet -Slug $slug -Name "ANDROID_KEYSTORE_BASE64" -Value $b64
    Invoke-GhSecretSet -Slug $slug -Name "ANDROID_KEYSTORE_PASSWORD" -Value $props["storePassword"]
    Invoke-GhSecretSet -Slug $slug -Name "ANDROID_KEY_PASSWORD" -Value $props["keyPassword"]
    Invoke-GhSecretSet -Slug $slug -Name "ANDROID_KEY_ALIAS" -Value $props["keyAlias"]
    Write-Host "Concluído: os 4 secrets foram definidos em $slug" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "Modo: area de transferencia (cria os 4 secrets manualmente no GitHub)." -ForegroundColor Yellow
    if (-not [string]::IsNullOrWhiteSpace($slug)) {
        Write-Host "Repo detectado: $slug (confirma que e este no browser)." -ForegroundColor DarkGray
    }
    if ([string]::IsNullOrWhiteSpace($slug) -and -not $ClipboardOnly) {
        Write-Warning "Nao detecei repo GitHub em origin (ou falta gh). Usa -Repo 'Dono/repo' ou instala/auth gh."
    }
    Write-Host "GitHub: Settings -> Secrets and variables -> Actions -> New repository secret (nome exacto, uma vez por secret)." -ForegroundColor White
    Write-Host ""
    Set-Clipboard -Text $b64
    Write-Host ""
    $lenMsg = 'OK: ANDROID_KEYSTORE_BASE64 na area de transferencia ({0} caracteres).' -f $b64.Length
    Write-Host $lenMsg -ForegroundColor Green
    Write-Host "1) New repository secret: ANDROID_KEYSTORE_BASE64 = colar (ja na area de transferencia)."
    Write-Host ""

    function Copy-SecretToClipboard {
        param([string] $Label, [string] $Value)
        Write-Host "New repository secret: $Label - Enter para copiar (valor nao mostrado)..."
        Read-Host | Out-Null
        Set-Clipboard -Text $Value
        Write-Host "  Copiado: cola como $Label" -ForegroundColor Cyan
        Write-Host ""
    }

    Copy-SecretToClipboard -Label "ANDROID_KEYSTORE_PASSWORD" -Value $props["storePassword"]
    Copy-SecretToClipboard -Label "ANDROID_KEY_PASSWORD" -Value $props["keyPassword"]
    Copy-SecretToClipboard -Label "ANDROID_KEY_ALIAS" -Value $props["keyAlias"]
}

Write-Host "Depois: Actions -> Android APK release -> Run workflow -> release_tag = v0.1.xx" -ForegroundColor Yellow
Write-Host ""
