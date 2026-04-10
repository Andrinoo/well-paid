# Build APK release com URL da API compilada (Vercel / produção).
# Uso (PowerShell, na pasta mobile):
#   .\tool\build_release_apk.ps1 -ApiBaseUrl "https://teu-projeto.vercel.app"
#
# Assinatura: se existir android/key.properties + keystore, usa release real;
# caso contrário usa chave debug (instalação em telemóvel para testes internos).

param(
    [Parameter(Mandatory = $true)]
    [string] $ApiBaseUrl
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

$url = $ApiBaseUrl.Trim().TrimEnd("/")
if ($url -notmatch "^https://") {
    Write-Warning "Recomendado HTTPS em produção (ex.: Vercel). URL atual: $url"
}

flutter build apk --release --dart-define="API_BASE_URL=$url"

Write-Host ""
Write-Host "APK: $root\build\app\outputs\flutter-apk\app-release.apk"
