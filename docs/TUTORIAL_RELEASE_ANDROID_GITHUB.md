# Tutorial: publicar o APK Android (GitHub Releases + actualização na app)

Este guia cobre: **segredos de assinatura**, **workflow que gera o APK**, **link para a app actualizar**, e **deploy do JSON na API**.

---

## O que vais precisar

- Repositório no GitHub (ex.: `Andrinoo/well-paid`).
- No teu PC: pasta `android-native/` com **`keystore.properties`** e o ficheiro **`.jks`** referenciado por `storeFile` (não vão para o Git — estão no `.gitignore`).
- Opcional mas recomendado: [GitHub CLI](https://cli.github.com/) (`gh`) para criar os segredos em comando.

---

## Parte 1 — Segredos no GitHub (assinatura do APK release)

O workflow [`.github/workflows/android-apk-release.yml`](../.github/workflows/android-apk-release.yml) usa estes **quatro** nomes exactos:

| Secret | Conteúdo |
|--------|-----------|
| `ANDROID_KEYSTORE_BASE64` | O ficheiro `.jks` em Base64 (uma linha). |
| `ANDROID_KEYSTORE_PASSWORD` | Valor de `storePassword` no `keystore.properties`. |
| `ANDROID_KEY_PASSWORD` | Valor de `keyPassword` no `keystore.properties`. |
| `ANDROID_KEY_ALIAS` | Valor de `keyAlias` no `keystore.properties`. |

### Opção A — Script automático (recomendado)

1. Instala o GitHub CLI: [https://cli.github.com/](https://cli.github.com/) (Windows: instalador MSI).
2. Abre um terminal e autentica **uma vez**:
   ```powershell
   gh auth login
   ```
   Escolhe GitHub.com → HTTPS → autentica no browser.
3. Na raiz do repo **Well Paid** (onde está `android-native/`):
   ```powershell
   cd "D:\Projects\Well Paid"
   powershell -ExecutionPolicy Bypass -File .\android-native\scripts\Export-GithubAndroidSecrets.ps1
   ```
4. Se o `origin` não for um URL GitHub, passa o repo à mão:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\android-native\scripts\Export-GithubAndroidSecrets.ps1 -Repo "Andrinoo/well-paid"
   ```
5. Se **não** quiseres usar o `gh`, força só clipboard:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\android-native\scripts\Export-GithubAndroidSecrets.ps1 -ClipboardOnly
   ```
   Depois cola manualmente em **GitHub → repo → Settings → Secrets and variables → Actions**.

### Opção B — Só pelo site GitHub

1. Gera Base64 do `.jks` (PowerShell, na pasta onde está o `.jks`):
   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes(".\o-teu-ficheiro.jks")) | Set-Clipboard
   ```
2. **Settings → Secrets and variables → Actions → New repository secret** — cria os quatro segredos com os nomes da tabela acima.

**Sem** estes segredos o CI ainda pode gerar um APK, mas a assinatura pode não ser a mesma da tua loja “real” (útil só para testes).

---

## Parte 2 — Disparar o build e obter o APK

### Método 1 — Manual (Actions)

1. GitHub → **Actions** → workflow **Android APK release**.
2. **Run workflow** → Branch `main` (ou a que quiseres).
3. Campo **`release_tag`**: usa o formato **`v`** + versão, alinhado ao `versionName` do Android, por exemplo **`v0.1.41`**.
4. **Run workflow**. Espera ficar verde.
5. Vai a **Releases** do repo → abre a release com essa tag → faz **download** do ficheiro `wellpaid-v0.1.41.apk` (o nome segue a tag).

### Método 2 — Por tag no Git

```bash
git checkout main
git pull
git tag v0.1.41
git push origin v0.1.41
```

O push da tag `v*` dispara o mesmo workflow. O APK aparece no **Release** dessa tag.

### URL directo (para o JSON da API)

Depois de publicado, o link típico do asset é:

```text
https://github.com/SEU_USUARIO/SEU_REPO/releases/download/v0.1.41/wellpaid-v0.1.41.apk
```

Substitui `SEU_USUARIO`, `SEU_REPO` e a tag. O resumo do job no GitHub Actions também sugere este URL.

---

## Parte 3 — A app saber que há update (backend)

A app Android lê **`GET {API_BASE_URL}android-update.json`** (a mesma base URL que o login usa).

1. No repo, edita [`backend/app/static/android-update.json`](../backend/app/static/android-update.json).
2. Campos:
   - **`version_code`** — inteiro **maior** que o `versionCode` da APK **já instalada** nos telemóveis (no projeto Android isso vem do Gradle / Alembic; se duvidares, compara com o número em **Definições → Sobre** ou com `BuildConfig.VERSION_CODE` do build que publicaste).
   - **`version_name`** — texto livre (ex. `0.1.41`), para notas / humanos.
   - **`apk_url`** — URL **HTTPS** do asset do GitHub (ou outro host estável).
   - **`release_notes`** — opcional.

Exemplo mínimo:

```json
{
  "version_code": 41,
  "version_name": "0.1.41",
  "apk_url": "https://github.com/Andrinoo/well-paid/releases/download/v0.1.41/wellpaid-v0.1.41.apk",
  "release_notes": "Correções e melhorias."
}
```

3. **Commit + push** e faz **deploy da API** (ex.: Vercel) para o ficheiro novo estar online em  
   `https://<teu-dominio-api>/android-update.json`.

4. Na app: **Definições → Procurar atualizações**. Se `version_code` for maior e `apk_url` for HTTPS válido, aparece o diálogo para descarregar e instalar.

---

## Checklist rápido

- [ ] `gh auth login` (se usas script com `gh`)
- [ ] Quatro secrets criados no GitHub
- [ ] Workflow **Android APK release** concluído com sucesso
- [ ] `android-update.json` com `version_code` e `apk_url` correctos
- [ ] API redeployada
- [ ] Teste “Procurar actualizações” no telemóvel

---

## Problemas frequentes

| Sintoma | O que verificar |
|--------|------------------|
| Workflow falha no `assembleRelease` | Logs do job; `wellpaid.api.release.url` no `gradle.properties`; falta de segredos se exigires assinatura release. |
| `gh secret set` falha | `gh auth status`; permissões no repo (admin ou secrets). |
| A app diz que não há update | `version_code` tem de ser **>** ao instalado; `apk_url` tem de começar por `https://`. |
| Download falha | URL público (release **público** ou token não aplicável ao browser da app); GitHub Releases normalmente OK. |

Para a linha de versão visível no login/definições, vê a secção **“Versão visível no APK”** em [`WELL_PAID_DOCUMENTACAO_UNIFICADA.md`](./WELL_PAID_DOCUMENTACAO_UNIFICADA.md).
