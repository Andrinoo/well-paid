# Contrato Android ↔ API (documentação viva)

Este ficheiro cruza os **interfaces Retrofit** (`android-native/core/network`) e **DTOs Kotlin** (`android-native/core/model`) com a **API FastAPI** em `backend/app`. Mantém-se alinhado ao código; para alterações de contrato, actualize este ficheiro na mesma PR.

## Origem da API e `app.py`

- O APK usa `BuildConfig.API_BASE_URL` (ver `android-native/app/build.gradle.kts`): deve ser a **origem** do deploy FastAPI (ex. `https://….vercel.app/`), **sem** path extra — os paths abaixo são relativos a essa origem.
- O ficheiro na raiz [`app.py`](../app.py) é a aplicação **Flask** legacy (páginas HTML e fluxos antigos). O cliente Android consome a API **REST JSON** servida por [`backend/app/main.py`](../backend/app/main.py).
- **OpenAPI interactiva:** `GET /docs` e `GET /openapi.json` no mesmo host que o FastAPI.

## Mapa por domínio

Legenda: **Retrofit** = método em `core/network`; **DTOs** = ficheiros típicos em `core/model`; **Backend** = router + schemas Pydantic em `backend/app`.

### Autenticação e perfil

| Método | Path | Retrofit | DTOs (Kotlin) | Backend |
|--------|------|----------|---------------|---------|
| POST | `/auth/register` | `AuthApi.register` | `RegisterRequestDto`, `RegisterResponseDto` | `auth.py` → `RegisterRequest`, `RegisterResponse` |
| POST | `/auth/verify-email` | `AuthApi.verifyEmail` | `VerifyEmailRequestDto`, `TokenPairDto` | `VerifyEmailRequest`, `TokenPairResponse` |
| POST | `/auth/resend-verification` | `AuthApi.resendVerification` | `ResendVerificationRequestDto`, `ResendVerificationResponseDto` | `ResendVerificationRequest`, `ResendVerificationResponse` |
| POST | `/auth/login` | `AuthApi.login` | `LoginRequestDto`, `TokenPairDto` | `LoginRequest`, `TokenPairResponse` |
| POST | `/auth/refresh` | `AuthApi.refresh` / `refreshCall` | `RefreshRequestDto`, `TokenPairDto` | `RefreshRequest`, `TokenPairResponse` |
| POST | `/auth/logout` | `AuthApi.logout` | `LogoutRequestDto`, `MessageResponseDto` | `LogoutRequest`, `MessageResponse` |
| POST | `/auth/forgot-password` | `AuthApi.forgotPassword` | `ForgotPasswordRequestDto`, `ForgotPasswordResponseDto` | `ForgotPasswordRequest`, `ForgotPasswordResponse` |
| POST | `/auth/reset-password` | `AuthApi.resetPassword` | `ResetPasswordRequestDto`, `MessageResponseDto` | `ResetPasswordRequest`, `MessageResponse` |
| GET | `/auth/me` | `UserApi.getCurrentUser` | `UserMeDto` | `UserMeResponse` |
| PATCH | `/auth/me` | `UserApi.patchProfile` | `UserProfilePatchDto`, `UserMeDto` | `UserProfilePatch`, `UserMeResponse` |
| POST | `/auth/profile/display-name` | `UserApi.updateDisplayName` | `UserProfilePatchDto`, `UserMeDto` | mesmo `UserMeResponse` |

Paths sem Bearer no header: ver `AuthPaths.kt` (`/auth/login`, `/auth/register`, etc.).

### Dashboard

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| GET | `/dashboard/overview` | `DashboardApi.overview` | `DashboardOverviewDto` | `dashboard.py` → `DashboardOverviewResponse` |
| GET | `/dashboard/cashflow` | `DashboardApi.cashflow` | `DashboardCashflowDto` | `DashboardCashflowResponse` |

### Despesas

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| GET | `/expenses` | `ExpensesApi.listExpenses` | `ExpenseDto` | `ExpenseResponse` (lista) |
| GET | `/expenses/{id}` | `ExpensesApi.getExpense` | `ExpenseDto` | `ExpenseResponse` |
| POST | `/expenses` | `ExpensesApi.createExpense` | `ExpenseCreateDto`, `ExpenseCreateOutcomeDto` | `ExpenseCreate`, `ExpenseCreateOutcome` |
| PUT | `/expenses/{id}` | `ExpensesApi.updateExpense` | `ExpenseUpdateDto`, `ExpenseDto` | `ExpenseUpdate`, `ExpenseResponse` |
| DELETE | `/expenses/{id}` | `ExpensesApi.deleteExpense` | — | query `delete_target`, `delete_scope`, `confirm_delete_paid` |
| POST | `/expenses/{id}/pay` | `ExpensesApi.payExpense` | `ExpensePayDto`, `ExpenseDto` | `ExpensePayRequest`, `ExpenseResponse` |
| POST | `/expenses/{id}/share/cover-request` | `ExpensesApi.requestShareCover` | `ExpenseCoverRequestDto`, `ExpenseDto` | `ExpenseCoverRequest`, `ExpenseResponse` |
| POST | `/expenses/{id}/share/decline` | `ExpensesApi.declineExpenseShare` | `ExpenseShareDeclineDto`, `ExpenseDto` | `ExpenseShareDeclineRequest`, `ExpenseResponse` |
| POST | `/expenses/{id}/share/assume-full` | `ExpensesApi.assumeFullExpenseShare` | `ExpenseDto` | `ExpenseResponse` |

### Rendimentos e categorias

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| GET/POST | `/incomes`, `/incomes/{id}` | `IncomesApi` | `IncomeDto`, `IncomeCreateDto`, `IncomeUpdateDto` | `incomes.py` → `IncomeResponse`, … |
| GET/POST | `/income-categories` | `IncomeCategoriesApi` | `IncomeCategoryDto`, `IncomeCategoryCreateRequest` | `income_categories.py` |

### Categorias de despesa

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| GET/POST | `/categories` | `CategoriesApi` | `CategoryDto`, `CategoryCreateRequest` | `categories.py` → `category_public.py` |

### Metas

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| CRUD | `/goals`, `/goals/{id}` | `GoalsApi` | `GoalDto`, `GoalCreateDto`, `GoalUpdateDto` | `goals.py` → `GoalResponse`, … |
| POST | `/goals/{id}/contribute` | `GoalsApi.contribute` | `GoalContributeDto`, `GoalDto` | `GoalContribute*` (schema em `goal.py`) |
| POST | `/goals/{id}/refresh-reference-price` | `GoalsApi.refreshReferencePrice` | `GoalDto` | `GoalResponse` |
| POST | `/goals/preview-from-url` | `GoalsApi.previewFromUrl` | `GoalPreviewFromUrlRequestDto`, `GoalPreviewFromUrlDto` | `GoalPreviewFromUrlResponse` |
| POST | `/goals/product-search` | `GoalsApi.productSearch` | `GoalProductSearchRequestDto`, `GoalProductSearchResponseDto` | `GoalProductSearchResponse` |

O backend expõe também `GET /goals/{goal_id}/contributions` (histórico); o APK pode obter dados agregados via `getGoal` conforme necessidade.

### Família

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| GET/POST/PATCH/DELETE | `/families/me`, `/families/join`, … | `FamiliesApi` | `FamilyDtos.kt` | `families.py` → `family.py` |

### Reserva de emergência

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| Vários | `/emergency-reserve`, `/emergency-reserve/accruals`, `/emergency-reserve/plans`, … | `EmergencyReserveApi` | `EmergencyReserveDtos.kt` | `emergency_reserve.py` → `emergency_reserve` schemas |

### Valores a receber (receivables)

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| GET | `/receivables` | `ReceivablesApi.listReceivables` | `ReceivablesListDto` | lista + metadados em `receivable.py` |
| POST | `/receivables/{id}/settle` | `ReceivablesApi.settleReceivable` | `SettleReceivableDto`, `ReceivableDto` | `SettleReceivableRequest`, `ReceivableOut` |

### Listas de compras

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| GET/POST/PATCH/DELETE | `/shopping-lists`, `/shopping-lists/{id}`, items, complete | `ShoppingListsApi` | `ShoppingListDtos.kt` | `shopping_lists.py` → `shopping_list.py` |
| POST | `/shopping-lists/price-suggestions` | `ShoppingListsApi.groceryPriceSuggestions` | `GoalProductSearchResponseDto` (reutilizado) | `GoalProductSearchResponse` |

### Anúncios

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| GET | `/announcements/active` | `AnnouncementsApi.listActive` | `AnnouncementListDto` | `AnnouncementListResponse` |
| POST | `/announcements/{id}/read` | `AnnouncementsApi.markRead` | `ApiOkResponse` | resposta OK |
| POST | `/announcements/{id}/hide` | `AnnouncementsApi.hide` | `ApiOkResponse` | idem |

### Telemetria

| Método | Path | Retrofit | DTOs | Backend |
|--------|------|----------|------|---------|
| POST | `/telemetry/ping` | `TelemetryApi.ping` | `TelemetryPingRequestDto`, `TelemetryPingResponseDto` | `telemetry.py` |

## Rotas FastAPI não usadas pelo APK actual

Úteis para consola admin ou futuras features Android:

- `/admin/*` (ex. anúncios), `/health`, `GET /` raiz.
- `/family/financial-events` (`family_financial.py`) — eventos financeiros; **sem** cliente em `core/network` neste repositório.

## Como validar rapidamente

1. Subir ou apontar para o deploy FastAPI; abrir `/docs`.
2. Comparar request/response de um endpoint com o DTO Kotlin correspondente (nomes de campos em `snake_case` na API vs `@SerialName` nos DTOs).
3. Regressão: checklist em [`E2E_QA_CHECKLIST.md`](./E2E_QA_CHECKLIST.md).
