# Fase 1 — Backend admin + console web local

## Objetivo

Disponibilizar rotas protegidas `/admin/*` na API FastAPI e um painel web (Vite + React + TypeScript) executável em `localhost`, com o mesmo fluxo de login por e-mail e palavra-passe que a app, restrito a utilizadores com `is_admin = true`.

## Pré-requisitos

- Python 3.11+ e dependências do `backend/` instaladas (`pip install -r requirements.txt`).
- PostgreSQL acessível pela `DATABASE_URL` (local ou remota).
- Node.js 20+ (para o painel).

## Artefatos do repositório

| Área | Caminhos |
|------|----------|
| Migração Alembic | `backend/alembic/versions/018_users_is_admin.py` |
| Modelo | `backend/app/models/user.py` — campo `is_admin` |
| Segurança JWT | `backend/app/core/security.py` — claim `adm` no access token |
| Auth | `backend/app/api/routes/auth.py` — login/refresh para admins sem e-mail verificado |
| Dependência | `backend/app/api/deps.py` — `get_current_admin_user` |
| Rotas admin | `backend/app/api/routes/admin.py` |
| Schemas | `backend/app/schemas/admin.py` |
| Registo da app | `backend/app/main.py` |
| Painel | `admin-console/` (Vite) |

## Passos de execução (ordem)

### 1. Base de dados

1. A partir da pasta `backend/`, com `.env` válido:
   ```bash
   alembic upgrade head
   ```
2. Promover um utilizador existente a administrador (SQL ou cliente SQL), por exemplo:
   ```sql
   UPDATE users SET is_admin = true WHERE email = 'seu-email@exemplo.com';
   ```
   Não versionar e-mails reais em documentação.

### 2. API local

1. `uvicorn app.main:app --reload --host 127.0.0.1 --port 8000` (com cwd = `backend/` ou `PYTHONPATH` configurado conforme o projeto).
2. Verificar `GET http://127.0.0.1:8000/docs` e os endpoints `/admin/me`, `/admin/users`.

### 3. CORS

- Se `CORS_ORIGINS` **não** for `*`, incluir a origem do painel, por exemplo:
  `http://localhost:5173`
- Reiniciar o servidor após alterar `.env`.

### 4. Painel admin

1. `cd admin-console && npm install`
2. Copiar `.env.example` para `.env` e definir `VITE_API_BASE_URL` (ex.: `http://127.0.0.1:8000`).
3. `npm run dev` — abrir o URL indicado no terminal (tipicamente `http://localhost:5173`).
4. Entrar com credenciais de um utilizador que tenha `is_admin = true`.

## Verificação

- [ ] `alembic upgrade head` conclui sem erro.
- [ ] `POST /auth/login` com admin devolve `access_token` cujo payload JWT inclui `"adm": true` (decodificar em jwt.io apenas em ambiente de teste).
- [ ] `GET /admin/users` com `Authorization: Bearer …` devolve lista paginada.
- [ ] Utilizador **sem** `is_admin` não acede a `/admin/*` (403).
- [ ] Painel: login → tabela de utilizadores carrega.

## Estado da implementação (código)

- Migração `018` e campo `users.is_admin` no modelo.
- Rotas: `GET /admin/me`, `GET /admin/users`, `PATCH /admin/users/{id}` (corpo `{ "is_active": true|false }`).
- Pacote frontend: [`admin-console/`](../../admin-console/) — `npm run dev` após `npm install` e `.env` a partir de `admin-console/.env.example`.
- Testes backend existentes: `pytest` continua a passar (sem novos testes dedicados ao admin nesta entrega).

## Deploy (produção)

1. Fazer deploy do código do `backend/` (ex.: Vercel) após merge.
2. Correr `alembic upgrade head` contra a **base de dados de produção** (processo seguro já usado pelo projeto).
3. Atualizar variáveis no painel (ex.: `CORS_ORIGINS` se o admin local falar com a API de produção).
4. Promover admin em produção via SQL controlado.
5. O painel continua local — **não** implica novo deploy do APK.

## Notas

- Contas admin podem entrar mesmo com `email_verified_at` nulo (apenas para não bloquear operação interna); a app móvel continua a exigir e-mail verificado nas rotas normais.
- Não expor rotas admin sem HTTPS em redes não confiáveis.
