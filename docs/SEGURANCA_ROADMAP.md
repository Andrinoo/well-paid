# Segurança — estado atual e roadmap (Well Paid)

Este documento descreve o que o projeto **já faz** em termos de segurança (backend FastAPI + app Android nativa), **lacunas típicas** e um **plano em etapas** do mais básico ao mais avançado, em formato de *checklist* para execução incremental.

---

## Regra imutável — Vercel e Neon

**Toda** alteração que exija o painel **Vercel** (variáveis de ambiente, redeploy) ou **Neon** (connection string, branch, credenciais) **tem** de ser acompanhada de **instruções claras e verificáveis** para o utilizador executar no serviço certo. Não basta mencionar “atualiza o env”: é obrigatório seguir ou estender o guia **`docs/VERCEL_E_NEON_OPERACOES.md`** (passo a passo, ordem Segura, validação com `/health`).

- **Imutável:** esta regra não é dispensada por PRs ou por agentes; novos fluxos de deploy reutilizam ou actualizam esse documento.
- **Quem implementa código** que depende de nova variável: documenta o **nome exacto** da variável, valores exemplo **sem segredos**, e um parágrafo “No Vercel: …” com referência à secção certa do guia.

---

## 1. Panorama do que já existe no código

### Backend (FastAPI)

| Área | Situação observada |
|------|-------------------|
| **Autenticação** | JWT access + refresh; `sub` = UUID do utilizador; refresh com `jti` persistido e rotação (refresh invalida o token anterior). |
| **Senhas** | `bcrypt` (cost 12); política de força em `validate_password_strength`. |
| **Tokens sensíveis** | Reset/verificação de e-mail guardados como HMAC-SHA256 (`hash_password_reset_token`), não em texto claro. |
| **Dependência de auth** | `get_current_user` valida Bearer, tipo `access`, utilizador ativo e e-mail verificado (403 se não verificado). |
| **Rate limiting** | `slowapi` com limites em rotas críticas de `/auth` (ex.: login 5/min, registo 10/min, forgot-password 5/min, etc.). |
| **CORS** | Configurável via `CORS_ORIGINS`; valor por defeito `*` (ver secção 2). |
| **Base de dados** | Connection string remota força `sslmode=require` se não estiver definido. |
| **Ambiente** | Rotas de *health* e dev-only: `/health/smtp-effective` retorna 404 fora de `APP_ENV` development — bom padrão. |
| **Documentação API** | Controlada por `EXPOSE_OPENAPI` (predefinição `true` local; em produção definir `false` no Vercel — ver `docs/VERCEL_E_NEON_OPERACOES.md`). |

### Cliente Android

| Área | Situação observada |
|------|-------------------|
| **Tokens** | `EncryptedSharedPreferences` (AES) para access/refresh. |
| **HTTP em claro** | `network_security_config` permite cleartext só para `localhost` / emulador — adequado para dev. |
| **UI** | `DisplayNameSanitizer` / `looksLikeUuid` evita mostrar IDs como nome de saudação. |

---

## 2. Riscos e melhorias prioritárias (resumo)

1. **Superfície de documentação** — Swagger UI (`/docs`) e esquema OpenAPI expõem todos os endpoints; em produção convém restringir ou proteger.
2. **CORS `*`** — Permite qualquer origem; para app só mobile pode ser aceitável, mas para web ou ambientes mistos deve ser lista explícita.
3. **Rate limit só por IP** — Utilizadores atrás do mesmo NAT partilham quota; contas podem ser abusadas em massa de IPs (mitigação parcial). Rotas autenticadas podem combinar IP + identidade.
4. **Cabeçalhos HTTP de segurança** — Não há middleware explícito para `HSTS`, `X-Content-Type-Options`, `X-Frame-Options`, CSP (relevante sobretudo se houver front web na mesma origem).
5. **`/health/db`** — Responde `db_ssl` publicamente; é informação de diagnóstico; avaliar se deve ser interno ou atrás de auth em produção.
6. **Sanitização de conteúdo** — Campos de texto (nomes, descrições) têm limites Pydantic; não há camada explícita anti-XSS no JSON (normal para API JSON consumida por app nativa; relevante se no futuro houver WebView ou admin web).
7. **Segredo JWT** — `SECRET_KEY` deve ser longo e aleatório; rotação de chaves e algoritmos (RS256 com par de chaves) são evolução natural.
8. **Android** — Reforçar com *certificate pinning* (opcional), *screenshot* flags em ecrãs sensíveis, e política de backup/ADB conforme sensibilidade dos dados.

---

## 3. Roadmap por fases (step-by-step)

Marque `[ ]` → `[x]` à medida que implementa. A ordem sugere **impacto / esforço**; ajuste à realidade do produto.

### Fase A — Básico (rápido, alto valor)

- [ ] **A.1** Definir `CORS_ORIGINS` em produção com origens explícitas (não `*`), se existir cliente web ou requisitos de segurança da organização.
- [ ] **A.2** Desativar ou proteger documentação em produção: `docs_url=None`, `redoc_url=None`, `openapi_url=None` no `FastAPI(...)` **ou** middleware que só permita a partir de IPs internos / basic auth / header secreto.
- [ ] **A.3** Garantir `SECRET_KEY` forte (≥ 32 bytes aleatórios) apenas em variáveis de ambiente; nunca no repositório; validar que Vercel/production têm valores reais.
- [ ] **A.4** Rever `APP_ENV`, `EMAIL_VERIFICATION_LOG_TOKEN` e `PASSWORD_RESET_LOG_TOKEN` — **desligados** em produção (evitar tokens em logs).
- [ ] **A.5** Confirmar que `/health/db` (e qualquer health “rico”) é aceitável publicamente; caso contrário, mover para rota interna ou exigir segredo de monitorização.
- [ ] **A.6** Lista de dependências: `pip audit` / GitHub Dependabot / renovate para Python e Gradle; corrigir CVEs críticas com cadência fixa (ex. mensal).

### Fase B — Intermédio (endurecimento da API)

- [ ] **B.1** Adicionar middleware de **security headers** (pelo menos `X-Content-Type-Options: nosniff`; se servir HTML algures, `X-Frame-Options` / CSP).
- [ ] **B.2** Estender rate limiting: chave composta `IP + user_id` em rotas autenticadas (após `get_current_user`), mantendo IP em rotas públicas.
- [ ] **B.3** Padronizar mensagens de erro: evitar detalhes de stack em produção (`DEBUG=false` / handler global que não vaza exceções internas).
- [ ] **B.4** Revisão sistemática de **autorização** (IDOR): confirmar que cada `GET/PATCH/DELETE` por ID verifica `family_peer_user_ids` ou ownership onde aplicável (despesas, listas, metas, etc.).
- [ ] **B.5** Limites de tamanho de corpo de pedido no servidor (Uvicorn/proxy) para evitar payloads enormes em JSON.
- [ ] **B.6** Logging estruturado: registar `request_id`, utilizador (quando autenticado), rota e código HTTP — **sem** passwords nem tokens completos.

### Fase C — Avançado (identidade, infra e conformidade)

- [ ] **C.1** **Rotação de refresh tokens** já existe; avaliar **deteção de reutilização** (token já revogado usado de novo → revogar todos os refresh da conta).
- [ ] **C.2** Migrar de JWT HS256 para **RS256** (ou ES256) com chave privada só no servidor e verificação com chave pública — facilita rotação e separação de serviços.
- [ ] **C.3** **WAF** / proteção na borda (Cloudflare, Vercel Firewall, etc.) para padrões comuns e geo-blocking se necessário.
- [ ] **C.4** Política de **retenção e eliminação** de dados (RGPD): exportação e apagamento de conta; testes automatizados do fluxo.
- [ ] **C.5** **Monitorização de segurança**: alertas para picos de 401/429, falhas de login por conta, integração com SIEM se a escala justificar.
- [ ] **C.6** Pentest ou **OWASP ASVS** checklist como meta de maturidade; corrigir achados críticos antes de *release* major.

### Fase D — Cliente Android (além do armazenamento encriptado)

- [ ] **D.1** Garantir que **release** usa `minifyEnabled` / R8 e que não há secrets em `BuildConfig` público.
- [ ] **D.2** Opcional: **SSL pinning** (OkHttp `CertificatePinner`) contra MITM em redes não confiáveis — equilibrar com complexidade de deploy e expiração de certificados.
- [ ] **D.3** `FLAG_SECURE` em ecrãs com dados financeiros sensíveis (impede screenshots em muitos dispositivos).
- [ ] **D.4** Rever `android:allowBackup` e regras de backup para não exportar `EncryptedSharedPreferences` de forma insegura.
- [ ] **D.5** Deep links (`wellpaid://`) — validar tokens de convite e parâmetros contra abuso e *open redirect* interno.

---

## 4. Verificação contínua (sugestão de rituais)

- [ ] Antes de cada release: rever variáveis de ambiente de produção (sem defaults inseguros).
- [ ] Testes automatizados que cobrem: login falhado, refresh inválido, acesso a recurso de outro utilizador (401/403/404 conforme desenho).
- [ ] Documentar contacto e processo em caso de vulnerabilidade (**security.txt** ou e-mail `security@...`).

---

## 5. Referências rápidas no repositório

| Tema | Onde olhar |
|------|------------|
| JWT e senhas | `backend/app/core/security.py` |
| Utilizador atual e Bearer | `backend/app/api/deps.py` |
| Limites de taxa | `backend/app/core/limiter.py`, decoradores em `backend/app/api/routes/auth.py` |
| CORS e settings | `backend/app/core/config.py`, `backend/app/main.py` |
| OpenAPI / docs em produção | `EXPOSE_OPENAPI` em `config.py`; guia operacional `docs/VERCEL_E_NEON_OPERACOES.md` |
| Auth e fluxos | `backend/app/api/routes/auth.py` |
| Tokens no Android | `android-native/core/datastore/.../EncryptedTokenStorage.kt` |
| Cleartext só dev | `android-native/app/src/main/res/xml/network_security_config.xml` |

### 5.1 Já implementado neste roadmap (código)

- **`EXPOSE_OPENAPI`** — desativa `/docs`, `/redoc`, `/openapi.json` quando `false` (produção: definir no Vercel + redeploy).
- **Cabeçalhos** — `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy` em todas as respostas.
- **`X-Request-ID`** — por pedido, para correlacionar logs e suporte.
- **A.4 (parcial)** — ao arranque, aviso em log se `EMAIL_VERIFICATION_LOG_TOKEN` ou `PASSWORD_RESET_LOG_TOKEN` estiverem ativos fora de `development`.
- **B.6** — linha de log por pedido (`request_id`, método, path, status); `GET /health` só em DEBUG; 4xx/5xx com nível warning/error.
- **B.3** — handler global para exceções não tratadas: produção devolve mensagem genérica + `request_id`; em `development`/`dev`/`local`, `detail` inclui a mensagem da excepção.
- **D.3** — `FLAG_SECURE` na janela principal (`MainActivity`) para mitigar screenshots e gravação de ecrã em toda a app (**novo APK** para utilizadores).
- **A.6 (ferramenta)** — `pip-audit` listado em `backend/requirements-dev.txt`; última corrida local: sem vulnerabilidades conhecidas em `requirements.txt`.

**Pendente (próxima vaga):** A.6 (repetir `pip-audit` em CI ou mensalmente), B.2/B.4/B.5, D.1 (R8 release), A.1/A.5 quando houver requisitos claros. **A.6:** ver `backend/requirements-dev.txt`.

---

## 6. O que não “parte” o sistema — e ordem lógica de execução

“A não partir o sistema” aqui significa: **sem alterar contratos JSON**, **sem invalidar sessões em massa** e **sem bloquear clientes legítimos** com uma configuração mal calibrada. Itens que só exigem **processo**, **variáveis de ambiente** ou **cabeçalhos extra** costumam ser os mais seguros primeiro.

### 6.1 Itens tipicamente seguros (baixo risco de regressão)

| ID | Porque é seguro |
|----|-----------------|
| **A.4** | Só desliga exposição de tokens em **logs** do servidor. A API e a app comportam-se igual. |
| **A.2** | Esconder `/docs`, `/redoc`, `/openapi.json` em **produção** não afeta o cliente móvel (não usa Swagger no fluxo normal). |
| **B.1** | Cabeçalhos de segurança são ignorados por clientes JSON que só parseiam corpo e status. |
| **B.6** | Logging estruturado é **aditivo** (não muda respostas HTTP). Cuidado para não logar segredos. |
| **B.3** | Seguro se em produção apenas **ocultar stack traces** no corpo da resposta; manter mensagens `detail` úteis já existentes (422, 401, etc.). |
| **Sec. 4** (rituais) | Documentação e checklist de release — zero impacto em runtime. |
| **A.6** (só leitura) | Correr `pip audit` / relatório Dependabot **sem** atualizar ainda pacotes — não altera comportamento. |
| **D.3** | `FLAG_SECURE` altera screenshots, não chamadas à API. |
| **D.1** (se já builda em release) | R8/minify — risco é de *build* ou reflexão; se o projeto já compila release, é incremental. |

### 6.2 Itens que exigem cuidado (podem “partir” algo se mal feitos)

| ID | Risco |
|----|--------|
| **A.1** | Lista de origens CORS errada quebra **navegadores** (SPA/admin web). App Android nativa muitas vezes **não** envia `Origin` como um browser; mesmo assim, testar qualquer cliente web. |
| **A.3** | **Trocar** `SECRET_KEY` em produção invalida **todos** os JWT atuais → utilizadores têm de voltar a autenticar-se. Não é bug, mas é efeito em massa. |
| **A.5** | Restringir `/health/db` pode partir **monitorização** ou *dashboards* que já fazem *poll* a esse URL. |
| **B.2** | Limites mais apertados podem gerar **429** para utilizadores legítimos (mesma rede, testes automatizados). |
| **B.4** | Corrigir IDOR muda quem acede a quê — correto, mas pode expor que algum cliente dependia de comportamento errado. |
| **B.5** | Limite de tamanho de corpo pode devolver **413** em anexos/payloads grandes no futuro. |
| **A.6** (atualizar deps) | Subir versões pode introduzir **breaking changes** — sempre com testes. |
| **D.2** | Pinning mal configurado → falhas TLS até ao certificado ser atualizado na app. |

### 6.3 Passo lógico recomendado (só a faixa “segura” + preparação)

Ordem pensada para **ganhos rápidos sem downtime lógico**; cada passo assume o anterior validado em *staging*.

1. **Confirmar ambiente de produção (A.4)** — *parcial no código (avisos ao arranque); falta definir env no Vercel quando promover a produção.*  
   - **Necessário:** Acesso às variáveis no painel (Vercel ou outro).  
   - **Ação:** `APP_ENV=production` (ou equivalente), `EMAIL_VERIFICATION_LOG_TOKEN=false`, `PASSWORD_RESET_LOG_TOKEN=false`.  
   - **Verificar:** Nenhum token de reset/verificação em logs após um fluxo de teste.

2. **Documentação API só onde importa (A.2)** — *feito no código; com `EXPOSE_OPENAPI=false` no Vercel após deploy.*  
   - **Necessário:** Variável tipo `EXPOSE_OPENAPI=false` ou detetar `VERCEL_ENV==production` no código que instancia `FastAPI`.  
   - **Ação:** Condicionar `docs_url`, `redoc_url`, `openapi_url` a `None` em produção.  
   - **Verificar:** `GET /docs` em produção deixa de servir UI; endpoints `/auth/login`, etc., inalterados.

3. **Security headers (B.1)** — *feito.*  
   - **Necessário:** Middleware Starlette/FastAPI ou headers no *edge* (Vercel).  
   - **Ação:** Adicionar pelo menos `X-Content-Type-Options: nosniff`; opcional `Referrer-Policy`.  
   - **Verificar:** App continua a fazer login e chamadas habituais (teste manual ou E2E).

4. **Logging estruturado sem dados sensíveis (B.6)** — *feito (linha por pedido; sem corpo nem Authorization).*  
   - **Necessário:** Middleware que gera `request_id` e loga método, path, status, `user_id` se autenticado.  
   - **Ação:** Garantir *redaction* de `Authorization` e corpos com password.  
   - **Verificar:** Logs úteis em *staging*; nenhum segredo em texto claro.

5. **Handler de erros em produção (B.3)** — *feito.*  
   - **Necessário:** Flag `APP_ENV` ou `DEBUG`.  
   - **Ação:** Handler global: em produção, resposta genérica ou `detail` seguro; não enviar `traceback` ao cliente.  
   - **Verificar:** Erros 422 ainda devolvem erros de validação Pydantic úteis; 401/403 inalterados.

6. **Inventário de dependências (A.6 — fase leitura)** — *ferramenta instalada; repetir periodicamente ou no CI.*  
   - **Necessário:** CI ou comando local.  
   - **Ação:** `python -m pip install -r requirements-dev.txt` (inclui `pip-audit`) e `python -m pip_audit -r requirements.txt` no directório `backend/`; priorizar CVEs **críticas** numa *issue*.  
   - **Verificar:** Nada mudou em runtime até decidir atualizações.

7. **Android: `FLAG_SECURE` (D.3)** — *feito na `MainActivity` (toda a app); exige novo APK.*  
   - **Necessário:** Lista de *composables*/actividades com dados sensíveis.  
   - **Ação:** Aplicar nas telas acordadas.  
   - **Verificar:** UX aceitável (sem screenshots); fluxos de API iguais.

8. **R8 / minificação (D.1)** — quando o *release* já for estável  
   - **Necessário:** `build.gradle`, regras ProGuard se usar reflexão.  
   - **Ação:** Ativar em `release`; corrigir *warnings* de *shrinking*.  
   - **Verificar:** APK release: login, refresh, ecrãs principais.

### 6.4 O que deixar para depois desta faixa (mas na ordem certa)

- **A.3** (reforçar `SECRET_KEY`): fazer com **janela de manutenção** ou aceitar logout geral; gerar chave com `secrets.token_urlsafe(48)` ou similar.  
- **A.1** (CORS): só depois de listar **todas** as origens web reais.  
- **A.5** (health): alinhar com quem monitoriza o *deploy*.  
- **B.2, B.4, B.5**: após testes de carga e testes de autorização.

### 6.5 Redeploy (backend) vs novo APK (Android)

| Passo / item | Redeploy API | Novo APK | Notas |
|--------------|:------------:|:--------:|--------|
| **A.4** (flags de log) | Sim* | Não | *Só alteração de variáveis no painel: em muitos hosts basta guardar env e **redeploy** ou restart para o processo ler valores novos. |
| **A.2** (esconder docs/OpenAPI) | Sim | Não | Código ou condição por env no `FastAPI`. |
| **A.1** (CORS) | Sim | Não | Só backend. |
| **A.3** (`SECRET_KEY`) | Sim* | Não | *Mudança de env + redeploy; utilizadores podem ter de voltar a entrar. |
| **A.5** (health) | Sim | Não | |
| **A.6** (auditoria `pip audit`) | Não | Não | Relatório local/CI sem deploy. **Atualizar** dependências → redeploy. |
| **B.1** (security headers) | Sim | Não | |
| **B.2** (rate limit) | Sim | Não | |
| **B.3** (erros em produção) | Sim | Não | |
| **B.4** (IDOR) | Sim | Não | |
| **B.5** (limite de body) | Sim | Não | Pode ser também no proxy (Vercel/nginx) → redeploy ou alteração de config. |
| **B.6** (logging) | Sim | Não | |
| **C.1–C.6** | Sim / infra | Parcial | Maioria backend ou plataforma; **C.4** pode exigir também alterações na app (fluxo “apagar conta”). |
| **D.1** (R8/minify) | Não | Sim | |
| **D.2** (SSL pinning) | Não | Sim | |
| **D.3** (`FLAG_SECURE`) | Não | Sim | |
| **D.4** (backup rules) | Não | Sim | `AndroidManifest` / XML de backup. |
| **D.5** (deep links) | Talvez | Sim | Validação no backend pode exigir redeploy; links e *intent filters* na app → APK. |
| **Sec. 4** (rituais, `security.txt`) | Opcional | Não | `security.txt` no site/API é ficheiro estático ou rota → pode ser redeploy; é processo, não binário. |

**Resumo:** tudo que mexe em **FastAPI**, **env do servidor**, **proxy** ou **base de dados** → **redeploy** (ou novo deploy) da API. Tudo que mexe em **Kotlin**, **manifest**, **recursos Android** ou **assinatura** → **novo build/APK** (e nova publicação na Play se for produção).

---

## 7. SQL injection — estado no backend

**Conclusão:** o código em `backend/app/` segue o padrão **SQLAlchemy 2.x** (`select`, `update`, modelos ORM, `.where()` com colunas e valores ligados como parâmetros). Não há SQL em *string* construído com `f"…"` ou concatenação a partir de input HTTP. Os únicos `text("…")` são literais fixos (ex.: `SELECT 1`, `SHOW ssl` em *health*).

| Prática | No projeto |
|---------|------------|
| Consultas à API | Filtros e IDs passam pelo ORM / `bindparam` implícito — **parametrizadas**. |
| Migrações Alembic | SQL estático em revisões; não é input de utilizador. |
| Risco residual | **Baixo** enquanto se evitar introduzir SQL cru com input dinâmico. |

**Manter assim (regra de equipa):**

- Preferir sempre `select(Model).where(Model.col == value)` (ou equivalente) em vez de `text(f"SELECT … {user_input}")`.
- Se no futuro for inevitável `text("…")`, usar **ligação explícita de parâmetros** (`:name` + dicionário) e nunca interpolar strings do cliente em SQL.
- Revisão de PR em qualquer uso novo de `text(`, `literal_column` com input externo, ou `execute("…" +`.

---

*Documento gerado com base na estrutura atual do repositório Well Paid; atualize as secções “estado atual” quando implementar mudanças significativas.*
