# Fase 2 — Métricas de uso e tempo

## Objetivo

Fornecer ao console admin dados de **atividade** ou **tempo de utilização** além de `created_at` / `updated_at` em `users`.

## Estado implementado (servidor, sem APK)

- Coluna **`users.last_seen_at`** (nullable), migração `019`.
- Actualização automática em **`_issue_tokens`** (login, refresh de token, confirmação de e-mail): corresponde à última vez que o utilizador obteve um novo par de tokens.
- Tabela **`app_usage_events`** (migração `020`) para eventos de autenticação: `login_success`, `refresh_success`, `verify_email_success`.
- Endpoint **`POST /telemetry/ping`** para APK (evento `app_open`) com deduplicação por dia UTC no backend.
- Endpoint **`GET /admin/usage/summary?days=14`** com:
  - `events_24h`
  - `dau_7d`
  - `mau_30d`
  - série diária (`day`, `events`, `active_users`)
- Coluna **Última actividade** no `admin-console` (lista de contas).
- Cards de resumo no `admin-console` (24h/7d/30d).
- Mini gráfico de barras no `admin-console` para tendência diária (últimos 14 dias).

## Política de baixo custo (Vercel/Neon gratuitos)

- No APK, o envio de `app_open` é limitado localmente para **no máximo 1 pedido por dia**.
- No backend, mesmo que haja pedidos repetidos, só grava **1 registo por utilizador + tipo + dia UTC**.
- Resultado prático: volume diário baixo, adequado para ambiente pessoal gratuito.

**Nota:** não mede “tempo de ecrã”; mede **última sessão renovada / login**.

## Operações no painel (Fase 2+)

- **Exportar CSV** — botão no admin que obtém todas as contas (paginação interna até 100 por pedido) respeitando o filtro de e-mail; ficheiro UTF-8 com BOM para Excel.

## Testes backend

- `tests/test_admin_routes.py` — `/admin/me`, listagem e `PATCH` com overrides de dependências (sem PostgreSQL de teste dedicado).

## Ainda por planear (opcional)

- Tabela de eventos de sessão ou heartbeat na app para métricas mais finas.

## Opções de desenho (a fechar antes de implementar)

1. **`users.last_seen_at`** atualizado por heartbeat da app ou por eventos periódicos.
2. **Tabela `app_usage_events`** (`user_id`, `occurred_at`, `event_type`) para agregações.
3. **`refresh_tokens.created_at`** como proxy de “última renovação de sessão” (não é tempo de ecrã).

## Impacto no APK

- **Obrigatório** se a métrica depender de eventos enviados pela app (heartbeat, foreground).
- **Evitável** se toda a lógica for no servidor (ex.: timestamps em refresh).

## Impacto no backend

- Novas migrações Alembic.
- Novas rotas ou extensão de `GET /admin/users` / relatórios.
- Possível job ou agregações para gráficos.

## Passos de execução (rascunho)

1. Decidir modelo de dados e privacidade (RGPD / retenção).
2. Implementar migrações e API.
3. Se aplicável: alterar cliente Android e publicar nova versão.
4. Estender o `admin-console` com colunas ou páginas de métricas.

Este documento será atualizado quando a Fase 2 entrar em desenvolvimento.
