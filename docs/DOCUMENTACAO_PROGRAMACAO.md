# Documentação geral de programação — Well Paid

Este documento descreve linguagens, proporções aproximadas no repositório, frameworks, ferramentas e organização do código. As percentagens de linguagem são calculadas sobre **linhas de texto** dos ficheiros listados por `git ls-files` (código versionado), excluindo binários (imagens, JAR) da contagem de linhas.

---

## 1. Stack em produção (foco do produto)

O produto atual combina **API em Python** e **aplicação Android nativa em Kotlin**.

| Linguagem / tipo | Linhas (aprox.) | Ficheiros | % do subconjunto |
|------------------|-----------------|-----------|------------------|
| **Kotlin** (.kt) | ~17 073 | ~128 | **~63,7%** |
| **Python** (.py) | ~7 998 | ~89 | **~29,8%** |
| **XML** (recursos Android, manifestos) | ~1 049 | ~16 | **~3,9%** |
| **Kotlin Script** (Gradle `.kts`) | ~285 | ~6 | **~1,1%** |
| Outros (scripts, propriedades, ProGuard, exemplos) | ~397 | — | **~1,5%** |

**Subconjunto analisado:** apenas pastas `backend/` e `android-native/` (~**26,8k** linhas de código texto nos ficheiros rastreados).

Interpretação: o cliente móvel (UI + camada de rede local) pesa mais em **linhas** que o servidor; isso é normal em apps com muitas telas Compose e recursos XML.

---

## 2. Repositório completo (inclui legado `mobile/`)

O repositório também contém a pasta **`mobile/`** (cliente **Flutter/Dart** e artefactos **iOS/macOS**), usada como referência ou legado. Incluir essas pastas altera muito as percentagens.

| Categoria | Linhas (aprox.) | % do total |
|-----------|-----------------|------------|
| **Dart** (Flutter — `mobile/`) | ~26 843 | **~43,8%** |
| **Kotlin** (`android-native/`) | ~17 078 | **~27,9%** |
| **Python** (`backend/`) | ~7 998 | **~13,0%** |
| **Nativo Apple** (Xcode, Swift, C/C++, etc. em `mobile/`) | ~3 153 | **~5,1%** |
| **ARB** (traduções Flutter) | ~1 710 | **~2,8%** |
| **XML** | ~1 265 | **~2,1%** |
| Lock files, texto, Markdown, YAML, JSON, outros | ~2 051 | **~3,3%** |

**Total aproximado (linhas texto):** ~**61,3k** (ficheiros binários não entram na soma de linhas).

Para relatórios de “o que usamos no produto atual”, use a **secção 1**; para “o que existe no mono‑repositório”, use esta secção.

---

## 3. Backend (`backend/`)

| Aspeto | Tecnologia |
|--------|------------|
| **Linguagem** | Python 3 |
| **Framework API** | [FastAPI](https://fastapi.tiangolo.com/) |
| **Servidor ASGI** | Uvicorn |
| **Validação / modelos** | Pydantic v2 |
| **ORM** | SQLAlchemy 2 |
| **Migrações** | Alembic |
| **Base de dados** | PostgreSQL (driver [psycopg](https://www.psycopg.org/) v3) |
| **Auth** | JWT (python-jose), passwords com bcrypt |
| **Rate limiting** | slowapi |
| **Testes** | pytest, httpx |
| **Configuração** | pydantic-settings, python-dotenv (variáveis via `.env` — não versionar segredos) |

**Deploy típico:** [Vercel](https://vercel.com/) (serverless) com variáveis de ambiente configuradas no painel; ficheiro `.vercelignore` na raiz reduz upload (ex.: pasta `android-native/`, padrões de segredos).

---

## 4. Android nativo (`android-native/`)

| Aspeto | Tecnologia |
|--------|------------|
| **Linguagem** | Kotlin |
| **UI** | Jetpack Compose, Material 3 |
| **Mínimo / alvo SDK** | minSdk 26, compile/target 35 (ver `app/build.gradle.kts`) |
| **JVM** | 17 |
| **DI** | Hilt (Dagger) + KSP |
| **Navegação** | Navigation Compose |
| **Rede** | Retrofit 2, OkHttp |
| **Serialização** | kotlinx-serialization (JSON) |
| **Armazenamento seguro** | AndroidX Security Crypto (tokens) |
| **Biometria** | AndroidX Biometric |
| **Build** | Gradle com Kotlin DSL (`.kts`) |
| **Wrapper** | `gradlew.bat` + `gradle/wrapper/` (Windows; em Unix pode gerar-se `gradlew` com `gradle wrapper` se necessário) |

Módulos Gradle: `:app`, `:core:model`, `:core:datastore`, `:core:network`.

---

## 5. Cliente legado / paralelo (`mobile/`)

| Aspeto | Tecnologia |
|--------|------------|
| **Framework** | Flutter (Dart) |
| **Plataformas** | Inclui projetos iOS/macOS e ficheiros nativos associados |

Tratar como código legado ou referência, salvo decisão explícita de manter Flutter como alvo principal.

---

## 6. Estrutura de pastas (visão geral)

```
backend/          # API FastAPI, modelos, rotas, serviços, testes, Alembic
android-native/   # App Android (Compose), core model/network/datastore
mobile/           # Flutter + nativo Apple (legado / paralelo)
docs/             # Documentação do projeto
```

---

## 7. Convenções e segurança

- **Segredos:** não commitar `.env`, keystores (`.jks`, `.keystore`), `keystore.properties` com passwords — ver `.gitignore` na raiz e `android-native/.gitignore`.
- **API:** URLs e chaves apenas em variáveis de ambiente ou `local.properties` local (não versionado), conforme exemplos (`local.properties.example`, `backend/.env.example`).

---

## 8. Atualizar as estatísticas

As percentagens dependem do que está em `git`. Após grandes adições/remoções de ficheiros, volte a gerar números com a mesma metodologia (contagem por extensão sobre `git ls-files`) ou ferramentas como [tokei](https://github.com/XAMPPRocky/tokei) / [cloc](https://github.com/AlDanial/cloc).

---

*Documento gerado para o projeto Well Paid. Última atualização alinhada ao estado do repositório em 2026.*
