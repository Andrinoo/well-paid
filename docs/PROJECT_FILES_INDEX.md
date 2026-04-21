# Índice só na raiz do repositório Well Paid

Este documento lista **apenas** ficheiros e pastas que estão **directamente** na raiz do repositório (`Well Paid/`). Serve para perceber o que foi criado ou configurado para **arrancar, fazer deploy, versionar e documentar** o projecto.

**Não** inclui: código dentro de `backend/`, `android-native/`, etc.; dependências ou bibliotecas (`node_modules`, caches de build dentro dos pacotes); ficheiros fonte individuais. Para isso use o README de cada pasta, [ANDROID_API_BACKEND_CONTRACT.md](./ANDROID_API_BACKEND_CONTRACT.md), ou o código-fonte.

---

## Pastas (primeiro nível)

| Nome | Função |
|------|--------|
| [`backend/`](../backend/) | API **FastAPI**, Alembic, testes — contrato JSON alinhado com o cliente Android em `android-native`. |
| [`android-native/`](../android-native/) | App **Android** (Kotlin, Compose, Gradle multi-módulo). |
| [`mobile/`](../mobile/) | App **Flutter** (multi-plataforma). |
| [`admin-console/`](../admin-console/) | **SPA** (Vite + React + TypeScript) para administradores. |
| [`docs/`](../docs/) | Documentação do produto (contratos API, QA, arquivo de planos). |
| [`.cursor/`](../.cursor/) | Regras do Cursor no repo (ex.: [no-env-secrets.mdc](../.cursor/rules/no-env-secrets.mdc)). |
| [`.git/`](../.git/) | Metadados do **Git** (histórico, branches); local ao clone. |
| [`.pytest_cache/`](../.pytest_cache/) | **Cache** gerado pelo pytest ao correr testes; pode apagar-se; não é “código” do produto. |
| [`.vercel/`](../.vercel/) | Metadados de **ligação/deploy Vercel** (projecto ligado ao repo). |

---

## Ficheiros na raiz

| Nome | Função |
|------|--------|
| [`README.md`](../README.md) | Entrada do repositório: como arrancar, variáveis, visão geral. |
| [`app.py`](../app.py) | Aplicação **Flask** legacy (HTML e fluxos antigos); a API principal do APK moderno está em `backend/`. |
| [`.env`](../.env) | Variáveis de ambiente **locais**; não commitar segredos (ver `.gitignore`). |
| [`.gitignore`](../.gitignore) | Padrões de ficheiros/pastas ignorados pelo Git. |
| [`.python-version`](../.python-version) | Versão de Python esperada (ex. **pyenv** / tooling). |
| [`.vercelignore`](../.vercelignore) | Ficheiros excluídos do bundle enviado ao **Vercel**. |
| [`wellpaid-release.jks`](../wellpaid-release.jks) | **Keystore** de assinatura release do Android — tratar como segredo. |

---

## Detalhe além da raiz

Estrutura interna de cada pacote (`backend/app/`, `android-native/app/`, `mobile/lib/`, dependências npm, etc.) **não** está listada aqui. Consulte o README respectivo, a documentação em [`docs/`](./), ou o contrato [ANDROID_API_BACKEND_CONTRACT.md](./ANDROID_API_BACKEND_CONTRACT.md).

*Actualizar esta página quando surgir um novo ficheiro ou pasta de topo no repositório.*
