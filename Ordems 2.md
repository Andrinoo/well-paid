# Well Paid — CURSOR INSTRUCTIONS v2 (Kotlin · Jetpack Compose)

**Versão:** 2 (cliente Android nativo)  
**Base conceitual:** alinhada a `Ordems 1.md` (Flutter); onde não há divergência, as **regras de produto e backend** são as mesmas.

**Nome do produto:** Well Paid  
**Tipo:** App mobile de finanças familiar (casal/família pequena)  
**Stack alvo deste documento:** **Kotlin** + **Jetpack Compose** + **FastAPI** + **PostgreSQL** (Neon)  
**Público:** 2–5 membros, brasileiros, controlo de gastos partilhados  

**Objetivo:** Mesmo produto que o cliente Flutter; este ficheiro define como o **cliente Compose** deve ser construído sem violar contratos HTTP, L10n, dinheiro em centavos e deploy.

---

## 1. IDENTIDADE (inalterável)

- Backend único de verdade: repositório `backend/` (FastAPI). O cliente Compose consome os **mesmos** endpoints e modelos JSON (`snake_case` nos payloads) que o app Flutter.
- Valores monetários: **sempre inteiros em centavos** em toda a camada de domínio e serialização; formatação `R$` só na UI.

---

## 2. REGRAS GLOBAIS (SEMPRE SEGUIR)

### 2.1 Formato de respostas (agente)

- Respostas directas; bullets preferíveis a parágrafos longos.
- Código: só o relevante; `// ...` para omitir; sem prolixidade.
- Uma implementação escolhida, não um menu de três arquitecturas.

### 2.2 Proibido

- Commit de `.env` / segredos; credenciais só em ambiente local ou painel (Vercel/Neon), nunca em `.md` com valores reais.
- **Texto hardcoded na UI** (labels, botões, erros, tooltips). Toda string visível: **recursos Android** por locale (`values/` + `values-pt-rBR/` ou `pt` + `en` conforme convenção da equipa), ou **Compose Multiplatform** com `stringResource`. Paridade mínima **pt-BR** e **en** no mesmo PR quando a feature for utilizador-facing.
- Despedidas e markdown decorativo em excesso.

### 2.3 L10n-first (pt/en) — regra primária (cliente Compose)

- **Não** implementar ecrãs Compose com `Text("...")` literal em produção.
- Novas chaves em **dois** conjuntos de recursos (PT + EN) no mesmo ciclo de entrega.
- `stringResource(R.string.*)` (ou equivalente) em `@Composable`.
- Datas por extenso: `Locale` da app (`pt-BR` vs `en-US`) alinhado ao selector de idioma (ver docs `COMPOSE_ADAPTACAO_LOCALE_FORMATADORES_LISTA_COMPRAS.md`).

### 2.4 Redeploy, Vercel, Neon e cliente Android

A **§2.5 de `Ordems 1.md`** (Git → validar backend → push → `vercel deploy` em `backend/` no projeto **well-paid-psi** → migrações → `GET /health`) aplica-se **integralmente** ao backend; **não** se repete aqui por duplicação — cumprir a ordem lá descrita.

**Cliente Android (substitui `--dart-define` do Flutter):**

- **`API_BASE_URL`** (ou nome equivalente): apenas **origem** em produção Well Paid: `https://well-paid-psi.vercel.app` (sem barra final, sem `/api` extra). Definir via **`BuildConfig`** / `local.properties` / flavour **sem** commitar segredos; documentar em `README` só com placeholder.
- Retrofit/OkHttp: `baseUrl` deve juntar paths de forma que **não** resulte em `POST /` na raiz por erro de concatenação (mesmo antipadrão histórico do Dio).
- Pós-deploy: validar `GET /health`, smoke de login.

**APK/AAB:** rebuild quando mudar contrato HTTP, versão publicada, ou primeira vez com `API_BASE_URL` de produção correcto — não obrigatório para mudança só de Python no servidor se o contrato for idêntico.

### 2.5 Alteração das regras de deploy

Só por decisão explícita de arquitectura (como em Ordems 1).

---

## 3. ARQUITECTURA — CLIENTE COMPOSE (O QUE DEFINIR)

### 3.1 Backend (inalterável)

Igual a **§3.1 de `Ordems 1.md`**: FastAPI, Pydantic v2, SQLAlchemy 2, Alembic, JWT, rate limits, etc.

### 3.2 Frontend Android (Jetpack Compose) — stack recomendada

| Área | Escolha |
|------|---------|
| **Linguagem** | Kotlin **2.x** (alinhar `libs.versions.toml` ao projeto) |
| **UI** | Jetpack Compose (Material 3) |
| **Estado** | ViewModel + **StateFlow** / **UiState**; alternativa aceitável: MVI explícito |
| **Injeção** | Hilt ou Koin (uma só, consistente) |
| **Rede** | Retrofit + OkHttp; interceptor de **refresh** do JWT espelhando comportamento do Dio no Flutter |
| **Persistência local** | **Room** (SQL) para cache estruturado; **DataStore** (Preferences) para tokens/flags; opcional fila de sync como tabela Room |
| **Navegação** | **Navigation Compose** com `NavHost`; rotas fullscreen (formulários) fora da bottom bar quando o design o exigir (equivalente a `parentNavigatorKey`) |
| **Locale** | `AppCompatDelegate.setApplicationLocales` ou composição com `CompositionLocal` + `stringResource`; persistir escolha pt/en em DataStore |
| **Segurança tokens** | **EncryptedSharedPreferences** ou **DataStore encrypted** / Android Keystore conforme política da equipa — **nunca** tokens em `SharedPreferences` em claro |
| **Biometria** | AndroidX Biometric / `BiometricPrompt` |
| **Gráficos** | Vico, MPAndroidChart port Compose, ou Canvas — paridade visual com `fl_chart` é objetivo de produto, não obrigatório pixel-perfect na primeira entrega |
| **QR** | ZXing / ML Kit |
| **Notificações** | WorkManager + NotificationManager; canais Android obrigatórios |
| **Min SDK / target** | Definir no módulo (ex.: min 26+ para boa parte de biométrica/notificações) |

### 3.3 Base de dados e domínio

- Mesma regra: **centavos inteiros** nos modelos Kotlin (`Long` ou `Int` conforme convénção; cuidado com overflow em totais muito grandes — `Long` para somas agregadas se necessário).

---

## 4. REGRAS TÉCNICAS IMUTÁVEIS (CLIENTE)

### 4.1 Dinheiro

- **Proibido** `Double`/`Float` para montantes de negócio.
- API já envia `*_cents` como número inteiro; mapear para tipos inteiros Kotlin.

### 4.2 Datas

- API: ISO `YYYY-MM-DD` onde aplicável.
- UI: **DD/MM/AAAA** para utilizador BR; respeitar locale quando UI em EN.

### 4.3 Segurança

- Mesmos requisitos de política de senha no servidor; cliente valida UX, servidor é fonte de verdade.
- TLS-only; pinning opcional (decisão de equipa).

### 4.4 Offline-first

- Paridade conceptual com Flutter: cache + fila + `sync_status` onde o backend e o modelo o permitirem; implementação Room/fila deve espelhar `expenses_sync_queue` / Hive do legado até unificação.

### 4.5 Família e partilha

- Contratos `is_shared`, `shared_with_user_id`, `family_peer_user_ids` **inalterados** — ver `docs/MODO_FAMILIA_BACKEND_E_FRONTEND.md` (secção frontend aplica-se a Compose linha-a-linha em conceito).

### 4.6 Estrutura de pastas do repositório

- **`backend/`** — fonte de verdade API.
- **`mobile/`** — cliente Flutter existente (legado até decisão de arquivo).
- **Cliente Compose:** convenção sugerida **`android-app/`** ou **`compose-client/`** na raiz (definir num PR `chore/` e actualizar README); **não** misturar módulos Gradle dentro de `mobile/` sem decisão explícita.

### 4.7 Ficheiros locais (gitignore)

- Manter política de `Ordems*.md` conforme `.gitignore` do repo; cópias locais de specs podem ficar fora do Git.

---

## 5. AMBIENTE DE DESENVOLVIMENTO (actualizar pela máquina)

Substituir a tabela de **§5 de Ordems 1** por:

| Componente | Nota Compose |
|------------|----------------|
| Android Studio | Hedgehog+ recomendado |
| JDK | 17+ (alinhar Gradle) |
| Kotlin | Versão do catálogo |
| Emulador / dispositivo | API nível alinhado a `minSdk` |

Python, Vercel CLI e Neon seguem necessários para quem toca no **backend**.

---

## 6. ORDEM DE TRABALHO E ROADMAP

- **Prioridade de features, etapas 6.x, cashflow, listas de compras, metas, família:** seguir **§6 de `Ordems 1.md`** como **fonte de verdade do produto**; o cliente Compose deve **atingir paridade funcional** com o Flutter antes de inventar funcionalidades novas.
- Documentação de apoio no repo: `docs/GUIA_RESPONSIVIDADE_PERFORMANCE_SETTINGS_ADESAO_LISTA_COMPRAS.md`, `docs/MODO_FAMILIA_BACKEND_E_FRONTEND.md`, `docs/COMPOSE_ADAPTACAO_LOCALE_FORMATADORES_LISTA_COMPRAS.md`.

### 6.1 Entregas específicas Compose (checklist)

1. **Auth** — login/registo/recuperação com os mesmos endpoints; guardar tokens de forma segura; refresh em 401.
2. **Shell** — bottom navigation de 5 destinos alinhado ao `MainShell` Flutter.
3. **Dashboard** — `GET /dashboard/overview`, `GET /dashboard/cashflow`; gráficos e seletor de mês com mesmas regras de prefetch/warmup **adaptadas** a coroutines + `remember` / `LaunchedEffect` (ver guia de performance).
4. **Despesas / proventos / metas / reserva / listas de compras** — paridade de rotas e `is_mine`.
5. **Família** — ecrã e deep link `wellpaid://join?token=`.
6. **Segurança** — PIN + biométrica + lifecycle lock ao background (`ProcessLifecycleOwner` ou `LifecycleObserver`).
7. **Definições** — idioma, notificações de meta parada, atalhos equivalentes ao menu ⋮.

---

## 7. FEATURES, BRANCHING E VERSIONAMENTO

- **Igual a §7 de `Ordems 1.md`**: `feature/`, `fix/`, SemVer, tags, uma feature dominante por PR.
- **App Android:** `versionCode` / `versionName` em `build.gradle.kts` alinhados a releases; notas de release devem mencionar **versão mínima da API** se houver mudança de contrato.

---

## 8. Build label (equivalente a `WP_V` / `kAppReleaseLabel`)

- Definir **`BuildConfig.BUILD_LABEL`** ou campo visível no ecrã de login no formato acordado, ex.: `WP_V:MAJOR.MINOR-YYYYMMDDHHmmss`, actualizado em releases.
- Manter tabela de histórico no README ou neste ficheiro quando a equipa o versionar no Git.

---

## 9. DIFERENÇAS RESUMIDAS Flutter → Compose (para o agente não confundir)

| Tópico | Flutter (Ordems 1) | Compose (Ordems 2) |
|--------|----------------------|----------------------|
| Strings | `app_*.arb` + `AppLocalizations` | `strings.xml` + `stringResource` |
| Estado | Riverpod | ViewModel + StateFlow |
| Navegação | GoRouter | Navigation Compose |
| Cache lista despesas | Hive | Room + DAO |
| HTTP | Dio | Retrofit |
| Tema | `ThemeData` | `MaterialTheme` |

---

## 10. CONFORMIDADE

- Implementação Compose que **altere** contrato HTTP ou esquema de BD: seguir **Ordems 1** §7 + migrações Alembic no `backend/`.
- Dúvida entre “como era no Flutter” e “idioma Kotlin”: preferir **paridade de comportamento** documentada em `docs/` e endpoints reais a adivinhação.

---

*Fim de Ordems v2 (Compose). Backend e regras partilhadas: ver `Ordems 1.md`.*
