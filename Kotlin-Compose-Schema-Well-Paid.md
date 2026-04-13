# Kotlin + Compose Schema — Well Paid

Documento de planeamento para uma **app Android nativa** (Kotlin + Jetpack Compose) que reutiliza o **backend e contratos HTTP atuais** da solução Well Paid. Espelha o comportamento e as áreas já implementadas na app **Flutter** (`mobile/`), hoje o cliente de referência.

**Âmbito:** apenas Android. Não inclui iOS.

---

## 1. Objetivos

- Manter **paridade funcional** com a app Flutter atual (fluxos, ecrãs principais, regras de sessão).
- Consumir a **mesma API** (origem configurável, JSON, Bearer + refresh como hoje).
- Melhorar **previsibilidade de layout** em vários formatos Android usando idiomas nativos (WindowInsets, `WindowSizeClass`, Compose).
- Permitir **evolução independente** do cliente Android sem alterar o servidor (salvo correções de bug ou extensões acordadas).

**Não objetivo:** reescrever o backend nem duplicar regras de negócio no cliente além do que já existe na app atual.

---

## 2. Inventário da app Flutter (espelho funcional)

Fonte: `mobile/lib/app_router.dart`, `mobile/lib/features/**`, `mobile/lib/core/network/dio_client.dart`.

### 2.1 Autenticação e sessão (rotas públicas)

| Rota / fluxo | Ecrã Flutter | Notas para Compose |
|--------------|--------------|-------------------|
| `/login` | `LoginPage` | Email/senha, navegação para registo/recuperação. |
| `/register` | `RegisterPage` | Registo de conta. |
| `/verify-email` | `VerifyEmailPage` | Query: `email`, `token`. |
| `/forgot-password` | `ForgotPasswordPage` | Pedido de reset. |
| `/reset-password` | `ResetPasswordPage` | Token por query `token` ou `extra`. |

**Contrato rede (referência):** cliente autorizado com `Authorization: Bearer`, refresh em `401` exceto rotas de auth listadas em `dio_client.dart` (`/auth/login`, `/auth/register`, `/auth/verify-email`, …).

### 2.2 Bloqueio local (PIN)

| Rota | Ecrã | Notas |
|------|------|--------|
| `/unlock` | `UnlockPage` | Após login se PIN ativo e sessão bloqueada. |
| `/security` | `SecuritySettingsPage` | Definições de PIN/segurança. |

**Lógica de redirect (espelhar):** hidratar estado → se autenticado + PIN ativo + não desbloqueado → `/unlock`; se desbloqueado e em `/unlock` → `/home`; rotas públicas com sessão ativa → `/home`.

### 2.3 Shell principal (bottom navigation)

`MainShell` + `StatefulShellRoute.indexedStack` com **5 ramos**:

| Índice | Path | Ecrã raiz | Sub-rotas (full-screen / root navigator) |
|--------|------|-----------|--------------------------------------------|
| 0 | `/home` | `HomePage` | Dashboard (overview, gráficos donut/fluxo, pendentes, atalhos). |
| 1 | `/expenses` | `ExpenseListPage` | `new`, `:expenseId`, `:expenseId/edit`; query `status`. |
| 2 | `/incomes` | `IncomeListPage` | `new`, `:incomeId`, `:incomeId/edit`. |
| 3 | `/goals` | `GoalsPlaceholderPage` | `new`, `:goalId`. |
| 4 | `/emergency-reserve` | `EmergencyReservePage` | — |

**Extra no shell:** painel rápido (`ShellQuickPanel`) acionado por gesto/toque acima da barra; sincronização de lembretes de metas (`GoalStallReminderService`) — reproduzir ou simplificar em Android (WorkManager + notificações).

### 2.4 Rotas full-screen (fora do shell)

| Path | Ecrã |
|------|------|
| `/settings` | `SettingsPage` |
| `/family` | `FamilyPage` (query `token` convite) |
| `/to-pay` | `ToPayPage` |
| `/shopping-lists` | `ShoppingListsPage` |
| `/shopping-lists/:listId` | `ShoppingListDetailPage` |

### 2.5 Domínios / providers (camada de dados a espelhar)

Módulos com `application/*_providers.dart` e respetivos domínios:

- **Dashboard:** `dashboard_providers.dart` — overview, cashflow, período, categorias.
- **Despesas:** `expenses_providers.dart` — lista, filtros, CRUD, categorias.
- **Proventos:** `incomes_providers.dart`.
- **Metas:** `goals_providers.dart`.
- **Reserva de emergência:** `emergency_reserve_providers.dart`.
- **Família:** `family_providers.dart`.
- **Listas de compras:** `shopping_lists_providers.dart`.

**Configuração API:** `ApiConfig.baseUrl` via compile-time (`API_BASE_URL`); em Android equivalente: `BuildConfig` / `local.properties` / secrets não commitados.

### 2.6 Internacionalização e tema

- Strings: `mobile/lib/l10n/` (template `app_pt.arb`).
- Cores: `well_paid_colors.dart`, Material 3.

**Compose:** `strings.xml` + `values-pt` (e opcionalmente inglês), tema Material 3 alinhado às cores existentes.

### 2.7 Gráficos

- Donut / categorias: `fl_chart` + `category_donut_chart.dart`.
- Fluxo de caixa: `dashboard_cashflow_chart_card.dart`.

**Compose:** bibliotecas candidatas (Vico, YCharts, ou Canvas custom); planear spike de 2–3 dias antes da fase de dashboard.

---

## 3. Arquitetura alvo (Android)

### 3.1 Forma do projeto

- **Opção A (recomendada):** novo módulo `app` Android no monorepo (ou repositório novo) `well-paid-android`, Gradle Kotlin DSL, minSdk / targetSdk alinhados ao projeto atual.
- **Opção B:** manter pasta `mobile/` Flutter e adicionar `android-native/` irmã — dois artefactos até decisão de sunset Flutter Android.

**Módulos sugeridos:**

| Módulo | Conteúdo |
|--------|----------|
| `:app` | Compose, Navigation, Hilt, tema, `Application`. |
| `:core:network` | Retrofit/OkHttp, interceptors (Bearer, refresh), serialização (kotlinx.serialization ou Moshi). |
| `:core:datastore` / `:core:security` | Tokens seguros (EncryptedSharedPreferences ou DataStore + crypto). |
| `:core:model` | DTOs e modelos alinhados ao JSON do backend. |
| `:feature:*` | Um módulo por área (`auth`, `home`, `expenses`, …) ou agrupar em `feature:main` no MVP. |

### 3.2 Padrões de UI e estado

- **UI:** Jetpack Compose + Material 3.
- **Navegação:** Navigation Compose; grafo espelhando shell (nested graphs) + rotas fullscreen.
- **Estado:** ViewModel + `StateFlow`/`UiState`; ou MVI explícito se a equipa preferir contratos de evento únicos.
- **DI:** Hilt.
- **Async:** Coroutines; chamadas rede em `Dispatchers.IO`.

### 3.3 Testes

- Unitários: ViewModels, mappers, use cases.
- UI: Compose Testing (smoke nos fluxos críticos: login → home).
- Contrato: opcional snapshot/OpenAPI se o backend publicar especificação.

---

## 4. Integração com o backend existente

1. **Documentar** endpoints usados pelo Flutter (extrair de `dio` calls / repositórios) numa tabela interna: método, path, body, códigos de erro.
2. **Reutilizar** o mesmo `baseUrl` e paths (`ApiConfig.apiUri` — paths absolutos a partir da origem).
3. **Auth:** persistir `access_token` + `refresh_token`; interceptor de refresh com mutex (como `_refreshInFlight` no Dio).
4. **Erros:** mapear mensagens para UI (equivalente a `messageFromDio` / l10n).
5. **Sem credenciais em repositório:** apenas `.env.example` / `local.properties.example` com placeholders (alinhado às regras do projeto).

---

## 5. Plano de execução por fases

### Fase 0 — Fundação (1–2 semanas)

- [ ] Criar projeto Android, módulos, CI (build + lint + test).
- [ ] Configurar Retrofit + OkHttp + auth interceptor + refresh.
- [ ] Persistência de tokens (segura).
- [ ] Tema Compose (cores Well Paid) + tipografia base.
- [ ] Navigation skeleton: grafo vazio + destinos placeholder.
- [ ] Spike gráficos: prova de conceito com dados mock.

**Entregável:** app instala, chama `GET` autenticado de health ou endpoint simples, tema aplicado.

### Fase 1 — Autenticação e deep links (1–2 semanas)

- [ ] Login, registo, forgot/reset password, verify email (queries).
- [ ] Estado global de sessão (logged in / out); redirect equivalente ao `GoRouter.redirect`.
- [ ] Logout e limpeza de tokens.

**Entregável:** utilizador completa ciclo login → entra no shell vazio.

### Fase 2 — Shell + Home / Dashboard (2–3 semanas)

- [ ] Bottom bar com 5 destinos (espelho `MainShell`).
- [ ] `HomePage`: cabeçalho (receitas/saldo/despesas), seletor de período, `RefreshIndicator` equivalente.
- [ ] Integração providers dashboard + overview.
- [ ] Donut por categorias + legenda (paridade com `CategoryDonutChart`).
- [ ] Gráfico de fluxo de caixa (paridade com `DashboardCashflowChartCard`).
- [ ] Cartão pendentes / atalhos conforme `DashboardScrollContent`.

**Entregável:** paridade visual/funcional aceitável no Início (com tolerâncias documentadas).

### Fase 3 — Despesas (2 semanas)

- [ ] Lista + filtros (`status` na URL na Flutter — espelhar em argumentos).
- [ ] Nova despesa, detalhe, edição.
- [ ] Integração categorias e formatação BRL (igual `brl_cents`).

### Fase 4 — Proventos (1–1,5 semanas)

- [ ] Lista, novo, detalhe, edição.

### Fase 5 — Metas + notificações (1,5–2 semanas)

- [ ] Lista/placeholder, nova meta, detalhe.
- [ ] Lembretes: WorkManager + canais de notificação (paridade com `GoalStallReminderService`).

### Fase 6 — Reserva de emergência (1 semana)

- [ ] Ecrã e chamadas a `emergency_reserve_providers` equivalentes.

### Fase 7 — Definições, família, a pagar (1,5–2 semanas)

- [ ] `SettingsPage`, `FamilyPage` (convite `token`), `ToPayPage`.

### Fase 8 — Listas de compras (1,5 semanas)

- [ ] Lista + detalhe `listId`.

### Fase 9 — Segurança app lock (1 semana)

- [ ] PIN / biometria (BiometricPrompt), ecrã unlock, definições.

### Fase 10 — Polimento e rollout (contínuo)

- [ ] Acessibilidade (TalkBack, tamanhos de texto).
- [ ] Tablets / landscape (opcional `WindowSizeClass`).
- [ ] Performance (baseline profiles), ProGuard/R8 rules para Retrofit.
- [ ] Play Console, assinatura, track interno → produção.

**Ordem das fases 3–8 pode ajustar-se à prioridade de negócio; o inventário acima garante que nada caia fora do âmbito.**

---

## 6. Checklist de paridade (resumo)

- [ ] Auth completa + refresh
- [ ] Redirects PIN / unlock
- [ ] 5 tabs + painel rápido (ou ADR se simplificar)
- [ ] Home + gráficos
- [ ] Despesas CRUD + filtros
- [ ] Proventos CRUD
- [ ] Metas + notificações
- [ ] Reserva emergência
- [ ] Settings, Family, To-pay
- [ ] Shopping lists
- [ ] L10n PT (e EN se existir no produto)

---

## 7. Riscos e decisões

| Risco | Mitigação |
|-------|-----------|
| Paridade de gráficos demorada | Spike antecipado; aceitar biblioteca com menos “polish” no MVP. |
| Drift API sem OpenAPI | Inventário manual de endpoints a partir do Flutter; testes de integração contra staging. |
| Dupla manutenção Flutter + Android | Congelar features novas num cliente ou definir “Flutter sunset” Android com data. |
| Tokens e segredos | Nunca commitar; alinhar com `.cursor/rules` do repositório. |

**Decisões a tomar antes da Fase 0:**

1. Monorepo novo módulo vs repo separado.
2. Kotlin Multiplatform Mobile no futuro ou só Android puro.
3. Nível de paridade obrigatório no MVP (ex.: excluir painel rápido na v1).

---

## 8. Estimativa global (indicativa)

Para **uma equipa pequena** (1–2 devs Android familiarizados com o backend), ordem de grandeza **10–16 semanas** até paridade próxima da app Flutter atual, **sem** contar pausas, design system fino ou rewrites de API. Gráficos e app lock costumam ser os maiores desvios.

---

## 9. Referências no repositório

- Rotas: `mobile/lib/app_router.dart`
- Rede: `mobile/lib/core/network/dio_client.dart`, `mobile/lib/core/config/api_config.dart`
- Features: `mobile/lib/features/*`
- Cores: `mobile/lib/core/theme/well_paid_colors.dart`

---

*Documento gerado para alinhamento de produto e engenharia; ajustar datas e donos de fase no planeamento de sprint.*
