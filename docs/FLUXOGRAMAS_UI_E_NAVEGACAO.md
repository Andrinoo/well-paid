# Fluxogramas — UI e navegação (Well Paid Android)

Este documento descreve os **fluxos de ecrã** da app Android nativa desde o arranque até às áreas autenticadas, com **diagramas Mermaid**. A implementação vive em [`WellPaidNavHost.kt`](../android-native/app/src/main/java/com/wellpaid/navigation/WellPaidNavHost.kt), [`NavRoutes.kt`](../android-native/app/src/main/java/com/wellpaid/navigation/NavRoutes.kt) e [`MainShellScreen.kt`](../android-native/app/src/main/java/com/wellpaid/ui/main/MainShellScreen.kt).

**Legenda geral:** setas indicam navegação `navigate` ou `popBackStack`; rotas entre aspas são valores de `NavRoutes`.

---

## 1. Arranque (cold start) e decisão de rota inicial

O `SessionViewModel` lê tokens; enquanto `startRoute == null` mostra-se apenas um indicador de progresso.

```mermaid
flowchart TD
  start([App abre])
  loading[Indicador de carregamento]
  hasToken{Tem sessão válida?}
  loginRoute["startDestination = login"]
  mainRoute["startDestination = main"]
  start --> loading --> hasToken
  hasToken -->|Não| loginRoute
  hasToken -->|Sim| mainRoute
```

---

## 2. Fluxo de autenticação (rotas públicas)

Nestas rotas o **bloqueio por app lock** não se aplica (`isPublicAuthRoute`).

```mermaid
flowchart LR
  subgraph auth [Rotas públicas]
    login["login"]
    register["register"]
    verify["verify_email/{email}"]
    forgot["forgot_password"]
    reset["reset_password?token="]
  end
  login --> register
  login --> forgot
  register --> verify
  forgot --> reset
  verify --> main["main"]
  login -->|login OK| main
```

**Comportamentos importantes**

- **Login com sucesso:** `navigate(main)` com `popUpTo(login) inclusive` — apaga a pilha até ao login.
- **Registo → verificação:** navega para `verify_email` e remove `register` da pilha.
- **Reset password:** pode voltar ao `login` ou só dar `pop`.

---

## 3. Mapa global: Main como hub

Após autenticação, o **Main** é o centro: tabs, atalhos e navegação para formulários e ecrãs satélite.

```mermaid
flowchart TB
  main["main / MainShellScreen"]
  settings["settings"]
  expNew["expense_new"]
  expId["expense/{id}"]
  inst["installment_plan/{groupId}"]
  incNew["income_new"]
  incId["income/{id}"]
  gNew["goal_new"]
  gDet["goal/{id}"]
  gEdit["goal_edit/{id}"]
  shopL["shopping_lists"]
  shopNew["shopping_list_new"]
  shopId["shopping_list/{listId}"]
  ann["announcements"]
  rec["receivables"]
  inv["investments"]
  emNew["emergency_reserve_new"]
  main --> settings
  main --> expNew
  main --> expId
  main --> inst
  main --> incNew
  main --> incId
  main --> gNew
  main --> gDet
  main --> gEdit
  main --> shopL
  main --> shopNew
  main --> shopId
  main --> ann
  main --> rec
  main --> inv
  main --> emNew
  settings --> display["display_name"]
  settings --> family["family"]
  settings --> security["security"]
  settings --> cats["manage_categories"]
```

**Logout** (desde Settings): `navigate(login)` com `popUpTo(main) inclusive`.

---

## 4. Shell principal (Main): tabs e atalhos

Resumo funcional (detalhe em `MainShellScreen`):

```mermaid
flowchart TB
  shell[MainShellScreen]
  shell --> tabs
  subgraph tabs [Tabs índice 0 a 4]
    t0[Início]
    t1[Despesas]
    t2[Rendimentos]
    t3[Metas]
    t4[Reserva emergência]
  end
  shell --> bar[Barra expandida / atalhos]
  bar --> a1[Despesas pendentes]
  bar --> a2[Listas de compras]
  bar --> a3[Anúncios]
  bar --> a4[Receivables + badge]
  bar --> a5[Investimentos]
  shell --> setIcon[Ícone Definições]
  setIcon --> settingsRoute[settings]
```

- **Gestos:** swipe entre tabs 1–4 e regresso ao Início; ao voltar das listas de compras pode seleccionar-se o tab 0 via `MAIN_SHELL_SELECT_TAB` no `savedStateHandle` do `main`.

---

## 5. Despesas e plano de prestações

```mermaid
flowchart TD
  main["main"]
  expNew["expense_new"]
  expDet["expense/{id}"]
  plan["installment_plan/{groupId}"]
  main --> expNew
  main --> expDet
  main --> plan
  plan --> expDet
  expNew -->|guardar OK| mainDirtyExp[Marca expense_list_dirty no Main]
  expDet -->|guardar OK| mainDirtyExp
  plan -->|plano apagado| mainDirtyExp2[Marca expense_list_dirty]
  mainDirtyExp --> main
  mainDirtyExp2 --> main
```

---

## 6. Rendimentos

```mermaid
flowchart TD
  main["main"]
  incNew["income_new"]
  incDet["income/{id}"]
  main --> incNew
  main --> incDet
  incNew -->|guardar OK| dirty[Marca income_list_dirty no Main]
  incDet -->|guardar OK| dirty
  dirty --> main
```

---

## 7. Metas: detalhe, edição e eliminação

```mermaid
flowchart TD
  main["main"]
  gNew["goal_new"]
  gDet["goal/{id}"]
  gEdit["goal_edit/{id}"]
  main --> gNew
  main --> gDet
  main --> gEdit
  gDet --> gEdit
  gNew -->|guardar OK| d1[goal_list_dirty]
  gEdit -->|guardar OK| d2[goal_detail_refresh no anterior]
  gEdit -->|eliminar meta| popMain[popBackStack até main]
  gDet -->|eliminar na lista| d3[goal_list_dirty]
  d1 --> main
  d2 --> gDet
  popMain --> main
  d3 --> main
```

A eliminação no **editar** usa `popBackStack(Main, inclusive=false)` para evitar ecrã branco quando a pilha é curta.

### 7.1 Reserva: novo plano (ecrã dedicado)

A partir do tab **Reserva** (`EmergencyReserveContent`), o botão **Adicionar reserva** navega para `emergency_reserve_new` com o mesmo `EmergencyReserveViewModel` ancorado no back stack do **Main** (partilhado com o tab). O formulário está em `EmergencyReservePlanFormScreen`. Após **criar plano** com sucesso, marca-se `emergency_reserve_dirty` no `Main` e faz-se `popBackStack` para voltar ao shell; a lista de planos actualiza no tab.

```mermaid
flowchart TD
  main["main / tab 4"]
  new["emergency_reserve_new"]
  main -->|Adicionar reserva| new
  new -->|plano criado| dirty[emergency_reserve_dirty + pop]
  dirty --> main
```

---

## 8. Listas de compras

```mermaid
flowchart TD
  main["main"]
  lists["shopping_lists"]
  newList["shopping_list_new"]
  detail["shopping_list/{listId}"]
  exp["expense/{id}"]
  main --> lists
  lists --> newList
  lists --> detail
  newList -->|criada| detail
  lists -->|swipe para Início| tab0[Tab 0 no Main]
  detail --> exp
```

---

## 9. Anúncios e receivables

```mermaid
flowchart LR
  main["main"]
  ann["announcements"]
  rec["receivables"]
  inv["investments"]
  main --> ann
  main --> rec
  main --> inv
  ann -->|ler/ocultar| dirty[announcements_dirty no Main]
```

---

## 10. Definições (árvore)

```mermaid
flowchart TD
  main["main"]
  set["settings"]
  dn["display_name"]
  fam["family"]
  sec["security"]
  mc["manage_categories"]
  main --> set
  set --> dn
  set --> fam
  set --> sec
  set --> mc
  set -->|logout| login["login"]
  dn -->|guardar| prof[user_profile_dirty no Main]
```

---

## 11. Bloqueio da app (App Lock)

Quando `locked` é verdadeiro e a rota **não** é pública, sobrepõe-se o `AppLockScreen` a todo o `NavHost`.

```mermaid
flowchart TD
  locked{Bloqueado?}
  pub{Rota pública?}
  overlay[AppLockScreen em cima]
  normal[NavHost visível]
  locked -->|Sim| pub
  pub -->|Não| overlay
  pub -->|Sim| normal
  locked -->|Não| normal
```

Rotas públicas: `login`, `register`, `forgot_password`, prefixos `verify_email`, `reset_password`.

---

## 12. Resumo: chaves `savedStateHandle` no Main

| Chave | Quando é escrita |
|--------|-------------------|
| `expense_list_dirty` | Após criar/editar despesa; apagar plano de prestações |
| `income_list_dirty` | Após criar/editar rendimento |
| `goal_list_dirty` | Metas: criar, eliminar no detalhe, eliminar no editar (após pop até Main) |
| `goal_detail_refresh` | Após guardar no editar meta |
| `announcements_dirty` | Interacção em anúncios |
| `emergency_reserve_dirty` | Após criar plano de reserva no ecrã `emergency_reserve_new` |
| `user_profile_dirty` | Nome a apresentar (e similares) |
| `MAIN_SHELL_SELECT_TAB` | Ex.: voltar das listas de compras para o tab Início |

---

## 13. Onde aprofundar

| Tema | Ficheiro |
|------|----------|
| Lista exacta de rotas | [`NavRoutes.kt`](../android-native/app/src/main/java/com/wellpaid/navigation/NavRoutes.kt) |
| Composables e callbacks | [`WellPaidNavHost.kt`](../android-native/app/src/main/java/com/wellpaid/navigation/WellPaidNavHost.kt) |
| Tabs, prefetch, atalhos | [`MainShellScreen.kt`](../android-native/app/src/main/java/com/wellpaid/ui/main/MainShellScreen.kt) |
| Sessão / rota inicial | [`SessionViewModel.kt`](../android-native/app/src/main/java/com/wellpaid/ui/session/SessionViewModel.kt) |

Para o **mapa API ↔ ecrã**, ver [ANDROID_API_BACKEND_CONTRACT.md](./ANDROID_API_BACKEND_CONTRACT.md). Para leitura única com o plano mestre, ver [WELL_PAID_DOCUMENTACAO_UNIFICADA.md](./WELL_PAID_DOCUMENTACAO_UNIFICADA.md).
