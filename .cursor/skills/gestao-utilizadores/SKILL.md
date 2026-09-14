---
name: gestao-utilizadores
description: >-
  Painel oculto de superuser Well Paid: planos Free/trial/pago, módulos,
  PIX manual, SUPERADMIN_PATH, catálogo de módulos e Alembic para versão
  Android. Usar ao adicionar módulos, libertar contas, alterar entitlements,
  painel admin secreto, PIX, trial, is_free_plan ou is_superuser.
---

# Gestão de utilizadores (superuser)

Painel **invisível** no site público. Não há link, sitemap, robots (além de `/sa.html`) nem entrada no `App.tsx`.

## Segredos — nunca no Git

- Caminho do painel: `SUPERADMIN_PATH` só em `.env` local e nas env vars da Vercel (web). **Nunca** `VITE_SUPERADMIN_PATH`.
- API do painel: `SUPERADMIN_API_PREFIX` (código default `/sa-api`) no backend e, no build do `sa.html`, a mesma env. Injectada só em `window.__WP_SA__` no HTML admin.
- Superuser: `SUPERUSER_EMAIL` na migration `052` (conta já existente).
- **Nunca** commitar `.env`, o caminho real, ou colar o path em README/chat/PR.

## Planos

| Estado | Quem | Acesso |
|---|---|---|
| **Free** (`is_free_plan`) | Todos os utilizadores **já existentes** na 052. Novos só se o superuser marcar a caixa. Staff/testes que não pagam. | Módulos básicos; não expira. |
| **Trial** | Conta nova, 24 h a contar da **confirmação de e-mail**. | Módulos básicos. |
| **Pago** | PIX gerado no painel + botão **Liberar** (nome de quem pagou). `paid_at` agora, `due_at` +30 dias, 1499 cêntimos BRL. | Enquanto `due_at` > agora. |
| **Expirado** | Trial/pago acabou, não é Free nem superuser. | `is_active=false`, sessões revogadas, **dados mantidos**. |

Webhook de pagamento **não** liberta acesso. Só o botão Liberar.

## Módulos

Fonte de verdade: `backend/app/services/module_catalog.py`.

Básicos (plano 14,99): `dashboard`, `payables`, `incomes`. `settings` está sempre ligado.

Superuser ignora gates.

### Adicionar um módulo (ex. carteira)

1. Entrada em `MODULE_CATALOG` (`id`, `included_in_basic`, `price_cents`).
2. Gate na rota: `Depends(require_module("id"))`.
3. Checkbox no painel (catálogo já vem da API `/catalog` + `/users/{id}/modules`).
4. Label i18n em `web/src/admin/AdminApp.tsx` (`MODULE_LABEL`).
5. Alembic: se não houver schema, revision no-op `NNN_bump_android_version_marker.py` (o APK deriva do head Alembic). Se houver schema, revision normal.

## Ficheiros

- API painel: `backend/app/api/routes/admin.py` + `superadmin_billing.py`, montados em `superadmin_api_prefix_path`.
- Auth: `get_current_superuser` responde **404** (não 403) se não for superuser.
- SPA: `web/sa.html` + `web/src/admin-main.tsx`. Acesso directo a `/sa.html` → 404. Rewrite do `SUPERADMIN_PATH` em `web/vite.config.ts` (dev) e `web/middleware.ts` (Vercel).
- Produção FastAPI: `/docs` e OpenAPI desligados.

## PIX

`backend/app/services/pix.py`: Mercado Pago se `MERCADO_PAGO_ACCESS_TOKEN` existir; senão registo pendente sem QR. Libertação sempre manual no painel.
