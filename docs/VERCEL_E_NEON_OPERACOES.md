# Operações Vercel e Neon — Well Paid

Este guia é a referência **obrigatória** quando qualquer alteração exigir o painel **Vercel** ou **Neon**. Siga os passos na ordem; não salte validações.

**Ligação:** a regra imutável está em `docs/SEGURANCA_ROADMAP.md` (secção “Regra imutável — Vercel e Neon”).

---

## Convenções do projeto

| Serviço | Uso típico |
|---------|------------|
| **Vercel** | Deploy do backend FastAPI (`backend/`), variáveis de ambiente da API em execução. |
| **Neon** | PostgreSQL gerido; `DATABASE_URL` aponta para o *branch* / instância correta. |

**Nunca** colar passwords ou URLs completas com password em issues, chat ou commits. Use placeholders em documentação.

---

## Parte A — Vercel (variáveis de ambiente e deploy)

### A.1 Abrir o projeto certo

1. Entrar em [https://vercel.com](https://vercel.com) com a conta da equipa.
2. Abrir o projeto do backend (ex.: **well-paid-psi**, conforme `Ordems 2.md`).
3. Ir a **Settings** → **Environment Variables**.

### A.2 Adicionar ou editar uma variável

1. **Key:** nome exato (ex.: `DATABASE_URL`, `SECRET_KEY`, `EXPOSE_OPENAPI`, `APP_ENV`). O backend usa nomes em maiúsculas com underscore, como no Pydantic/`Settings`.
2. **Value:** colar o valor **uma vez**; confirmar que não há espaços extra no início/fim.
3. **Environment:** marcar **Production** (e **Preview** / **Development** só se a equipa usar esses ambientes para o mesmo código).
4. Clicar **Save**.

### A.3 O que acontece depois de guardar

- Alterar variáveis **não** aplica automaticamente ao tráfego já em execução em muitos casos — é preciso um **novo deploy** para o processo ler os valores novos.

### A.4 Disparar redeploy

1. Ir a **Deployments**.
2. No último deploy bem-sucedido, menu **⋯** → **Redeploy**.
3. Confirmar (opcional: “Use existing Build Cache” pode ficar ativo para deploys só de env).
4. Aguardar estado **Ready**.

### A.5 Validar a API após deploy

1. No browser ou terminal: `GET https://<teu-dominio-vercel>.vercel.app/health` → deve responder `{"status":"ok"}` (ou equivalente).
2. Se alterou base de dados: ver secção Neon abaixo e depois repetir o health.
3. Smoke opcional: login na app ou `POST /auth/login` com utilizador de teste.

### A.6 Variáveis frequentes (referência de nomes, não de valores)

| Variável | Notas |
|----------|--------|
| `DATABASE_URL` | Connection string Postgres (muitas vezes copiada do Neon). |
| `SECRET_KEY` | Segredo JWT; mudança invalida tokens já emitidos. |
| `APP_ENV` | `production` em produção; evita tokens de desenvolvimento em respostas. |
| `CORS_ORIGINS` | Lista separada por vírgulas ou `*` (ver documentação de segurança). |
| `EXPOSE_OPENAPI` | `false` em produção para esconder `/docs` e esquema OpenAPI (ver `SEGURANCA_ROADMAP.md`). |
| `SMTP_*`, `MAIL_FROM` | E-mail transacional, se usado. |

### A.7 Activar `EXPOSE_OPENAPI=false` em produção (checklist)

1. **Settings** → **Environment Variables** → **Production**.
2. Criar ou editar: **Key** `EXPOSE_OPENAPI`, **Value** `false` (sem aspas).
3. **Save**.
4. **Deployments** → **Redeploy** no último deploy (ver A.4).
5. Validar no browser: `https://<teu-projeto>.vercel.app/docs` deve devolver **404** (ou página Vercel “not found”, conforme hosting).
6. Validar que a API continua OK: `GET /health` e um login de teste.

---

## Parte B — Neon (base de dados)

### B.1 Abrir o projeto e branch

1. Entrar em [https://console.neon.tech](https://console.neon.tech).
2. Selecionar o **project** que serve o Well Paid.
3. Confirmar o **branch** (ex.: `production` vs `development`) — a `DATABASE_URL` deve corresponder ao branch pretendido.

### B.2 Obter a connection string

1. No Neon: **Connection details** (ou equivalente).
2. Escolher **PostgreSQL** e copiar a URI (modo “comprimido” ou completa conforme a UI).
3. **Não** commitar esta string; colar apenas no Vercel (ou `.env` local).

### B.3 Alterar password ou rotação

1. Se o Neon gerar nova password ou endpoint: gerar nova connection string na consola.
2. Atualizar `DATABASE_URL` no Vercel (Parte A.2–A.4).
3. **Redeploy** obrigatório.
4. Validar: `GET /health` e, se existir, `GET /health/db` em ambiente controlado.

### B.4 Migrações Alembic (quando o código exige esquema novo)

1. Desenvolvimento local: aplicar migrações contra uma BD compatível (ver README do `backend/`).
2. Produção: o processo da equipa deve aplicar migrações ao **mesmo** destino Neon que `DATABASE_URL` do Vercel usa **antes** ou **junto** com o deploy que depende do novo esquema — nunca deploy de código novo sem migração aplicada.

---

## Parte C — Ordem segura para mudanças que envolvem os dois

1. **Neon:** confirmar branch e obter `DATABASE_URL` correta (se mudou).
2. **Vercel:** atualizar `DATABASE_URL` (e outras variáveis necessárias).
3. **Vercel:** **Redeploy**.
4. **Validar:** `/health` → fluxo crítico (login).

Se apenas mudar código Python **sem** variáveis: Git push / deploy habitual; Neon não precisa de passos extra.

---

## Falhas comuns

| Sintoma | O que verificar |
|---------|-------------------|
| 500 em tudo | `DATABASE_URL` errada ou migração em falta; logs de runtime no Vercel (**Deployments** → **Functions** / logs). |
| 401 em massa após deploy | `SECRET_KEY` alterada — utilizadores precisam de novo login. |
| `/docs` ainda visível | `EXPOSE_OPENAPI=false` não definido ou deploy antigo; fazer redeploy após guardar env. |

---

*Mantenha este ficheiro alinhado aos nomes reais de projeto Vercel/Neon da equipa; actualize placeholders se o project name mudar.*
