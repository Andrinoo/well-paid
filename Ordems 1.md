# Well Paid - CURSOR INSTRUCTIONS (IMUTÁVEIS)

## 1. IDENTIDADE DO PROJETO

**Nome:** Well Paid  
**Tipo:** App mobile de finanças familiar (casal/família pequena)
**Stack:** Flutter + FastAPI + PostgreSQL (Neon)
**Público:** 2-5 membros, brasileiros, controle de gastos compartilhados

**Objetivo:** Aplicativo para casal controlar despesas juntos, com segurança, offline-first, custo zero.

---

## 2. REGRAS GLOBAIS (SEMPRE SEGUIR - ECONOMIA DE TOKENS)

### 2.1 Formato de Respostas
- Respostas diretas, sem introduções do tipo "Claro! Aqui está..."
- Não explicar código óbvio ou repetitivo
- Não sugerir "posso ajudar com mais algo" desnecessariamente
- Prefira bullet points a parágrafos longos
- Máximo 150 linhas por resposta, a menos que explicitamente solicitado mais

### 2.2 Código (quando solicitado)
- Apenas o código relevante, sem comentários óbvios
- Usar `// ...` para indicar partes não modificadas
- Sem exemplo de uso a menos que pedido
- Sem explicação do que o código faz (o código fala por si)

### 2.3 Proibido
- **Commit de `.env`** (ou variantes com segredos) e **segredos em documentos** (`.md`, specs, exemplos com URL/senha/`SECRET_KEY` reais) — só placeholders; credenciais apenas em `.env` local, fora do Git
- Respostas com mais de 200 linhas sem necessidade
- Múltiplas opções de implementação (escolha a melhor e mostre só ela)
- Despedidas longas ("Boa sorte!", "Qualquer dúvida...")
- Markdown desnecessário em respostas curtas

---

## 3. ARQUITETURA DO PROJETO (O QUE VOCÊ PRECISA SABER)

### 3.1 Backend (FastAPI)
- **Linguagem:** Python **3.14.3** (versão fixa do projeto)
- **Framework:** FastAPI (não Flask)
- **Servidor:** Uvicorn
- **Banco:** PostgreSQL via Neon (gratuito)
- **ORM:** SQLAlchemy 2.0+
- **Validação:** Pydantic v2
- **Migrações:** Alembic
- **Autenticação:** JWT (access 1h, refresh 30d)
- **Hash de senha:** bcrypt (custo 12) ou Argon2
- **Rate limiting:** slowapi (5 tentativas login → bloqueio 15min)

### 3.2 Frontend (Flutter)
- **Linguagem:** Dart
- **Framework:** Flutter **3.41+** (instalado: 3.41.6 stable; mín. doc: 3.16+)
- **Gerenciamento de estado:** Riverpod (preferencial) ou BLoC
- **Banco local:** Hive (não SQLite)
- **Cliente HTTP:** Dio com interceptors para refresh token
- **Navegação:** GoRouter
- **Armazenamento seguro:** flutter_secure_storage (tokens)
- **Biometria:** local_auth
- **Gráficos:** fl_chart
- **QR Code:** qr_flutter
- **Notificações:** flutter_local_notifications
- **Tarefas background:** workmanager

### 3.3 Banco de Dados (PostgreSQL)
- **Serviço gratuito:** Neon (neon.tech) - 512MB, sem cartão
- **Alternativa:** Render PostgreSQL (1GB, dorme após 7 dias)
- **Tabelas principais:** users, families, family_members, expenses, goals, sync_log
- **Regra de ouro:** Todos valores monetários como INTEGER (centavos). Ex: amount_cents = 35000 para R$ 350,00

---

## 4. REGRAS TÉCNICAS IMTÁVEIS

### 4.1 Valores Monetários (CRÍTICO)
- **Nunca** use float/double para dinheiro em qualquer camada
- Backend: `amount_cents: Integer` no banco e schemas
- Frontend: `int amountCents` nos modelos
- Exibição: formata para `R$ 1.234,56` apenas na UI
- Cálculos: sempre em centavos (inteiros)

### 4.2 Datas
- Backend: ISO (YYYY-MM-DD) no banco e API
- Frontend: exibir como DD/MM/YYYY
- Armazenamento local: ISO string

### 4.3 Segurança (OBRIGATÓRIO)
- Senhas: mínimo 8 chars, 1 maiúscula, 1 número, 1 especial
- Hash: bcrypt (nunca texto plano)
- **Senha do utilizador (conta Well Paid):** no servidor guarda-se só **hash bcrypt** (unidirecional). Não usar cifra reversível para “criptografar e recuperar” a senha de login; recuperação = fluxo dedicado (e-mail com token).
- **E-mail transacional:** envio por **SMTP** (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`, `MAIL_FROM` no `.env`). Não é obrigatório integrar provedor só por API REST se o mesmo serviço expuser SMTP.
- **Credenciais Neon (`DATABASE_URL`):** apenas em **`.env`** local, fora do Git. Proteção = não commitar + permissões do SO (ex.: BitLocker) ou gestor de segredos em produção; **nunca** documentar a URL com palavra-passe em `.md`.
- Tokens: JWT com refresh rotation
- Armazenar tokens apenas em `flutter_secure_storage`
- Comunicação: apenas HTTPS (TLS 1.2+)
- Rate limiting por IP: 100 req/minuto

### 4.4 Offline-First (OBRIGATÓRIO)
- App salva localmente primeiro (Hive)
- Sync em background quando internet disponível
- Campo `sync_status`: 0=synced, 1=pending, 2=conflict
- Campo `updated_at` em todas tabelas para controle de mudanças
- Resolução de conflitos: "Last Write Wins" + UI manual quando necessário

### 4.5 Compartilhamento Familiar
- Tabela `families` para grupos
- Tabela `family_members` para associação
- Despesa tem `is_shared` e `shared_with_id`
- Convite via QR Code ou link mágico
- Divisão padrão: 50/50 ou porcentagem customizável

### 4.6 Estrutura de Pastas (OBRIGATÓRIA)

```
Well Paid/                        # raiz do workspace
├── backend/                      # FastAPI + Alembic
├── mobile/                       # Flutter (MVP: login/registo + home placeholder; nome alternativo: app/)
├── .env                          # credenciais locais (nunca commitar)
├── .env.example                  # modelo sem segredos
├── .gitignore
├── .python-version               # 3.14.3
├── Ordems 1.md
├── Telas.txt
├── Fastapi.txt
└── Frontend.txt
```

**Backend:** módulos por domínio (ex.: `app/api`, `app/models`, `app/schemas`, `app/core`).

**Mobile:** código Dart/Flutter conforme pacotes definidos em §3.2.

---

## 5. AMBIENTE INSTALADO (ESTADO ATUAL — maquina dev)

*Atualizar esta secção quando versões ou caminhos mudarem.*

| Componente | Versão / detalhe | Caminho ou nota |
|------------|------------------|-----------------|
| Windows | 11 (25H2) | — |
| Git | 2.53.0.windows.2 | `D:\Dev\Git` (no PATH) |
| Python | **3.14.3** (fixo projeto) | Instalado no **C:** (exceção à regra “ferramentas no D:”) |
| Flutter | **3.41.6** stable | `D:\dev\flutter` — `bin` no PATH |
| Dart (bundled) | 3.11.4 | com Flutter |
| Android SDK | **36.1.0** | `%LOCALAPPDATA%\Android\Sdk` — `flutter config --android-sdk` aponta aqui |
| Android Studio | instalado | uso: SDK, emulador, SDK Manager |
| cmdline-tools + licenças Android | OK | necessário para `flutter doctor --android-licenses` |
| Visual Studio (C++ desktop) | **não** instalado | **não** necessário para build **Android** |
| Docker | **não** instalado | — |
| Neon (PostgreSQL) | em uso | projeto criado; conexão testada (ex.: DBeaver) |
| PostgreSQL local | **não** instalado | desenvolvimento via Neon + `.env` |
| Cursor | IDE principal | workspace: `D:\Projects\Well Paid` |

**Segredos:** URL/credenciais Neon apenas em **`.env`** (ver `.env.example`). Nunca em `.md` nem commit.

**Discos:** regra da equipa — ferramentas grandes no **D:** (`Git`, `Flutter`); **Python** ficou no **C:** por decisão.

---

## 6. ORDEM DE TRABALHO (PROXIMA FASE)

### 6.0 Registo do que já foi validado (dev)

- **Recuperação de senha (e-mail transacional):** envio **SMTP** testado com **Zoho Mail** (`SMTP_HOST=smtp.zoho.com`, porta TLS 587 ou SSL 465 conforme `.env`; credenciais **só** em `.env`). Entrega ao destino confirmada. Foi **prova técnica**; decisão de **custo/produção** (manter Zoho, trocar de plano ou outro provedor) fica para quando o produto for rentável.
- **Contexto:** correio no domínio passou de Umbler para Zoho (MX/DNS na Cloudflare); problemas anteriores de entrega (ex. Gmail com Umbler) documentados na conversa do projeto, não neste ficheiro.

### 6.1 O que está fechado (MVP em curso)

- **Conta e sessão:** fluxos alinhados a `Telas.txt` §5.2–5.3 — registo, login, refresh/logout, recuperação/redefinição de senha; app `mobile/` com Dio, `flutter_secure_storage`, Riverpod, GoRouter (incl. tratamento de 401 com refresh onde aplicável).
- **Dashboard (§5.4 — primeira entrega):** backend com tabelas `categories` (seed MVP) e `expenses`, migrações Alembic `003` + **`004` (parcelas / grupo / metadado de recorrência)**, agregação em **`GET /dashboard/overview?year=&month=`** (JWT); mobile com período mensal, resumo em BRL, rosca `fl_chart`, blocos A pagar / próximos vencimentos / metas (placeholder + TODO até `GET /goals`). Testes Python: `tests/test_dashboard_aggregates.py` (centavos / período).
- **Despesas (API + app):** `GET/POST/PUT/DELETE /expenses`, **`POST /expenses/{id}/pay`**, `due_date`, `status`; criação com **parcelamento** (várias linhas, `installment_group_id`) e **metadado** `recurring_frequency` (sem misturar com parcelas no create, conforme validador). Mobile: lista §5.5 com filtros, **atalhos no contentor da lista + FAB**, chips de parcela/recorrência nos tiles, fluxos **nova / detalhe / editar / pagar / eliminar**, formulário com parcelas, vencimento e recorrência (UI alinhada ao contrato).
- **Execução incremental:** **`PROMPT_VERIFICACAO_DESPESAS_ETAPAS.md`** — Parte A (Etapas 1–7) conferência do módulo despesas; Parte B (Etapas 8–13) continuidade do roadmap; **uma etapa por sessão**.
- **Verificação despesas: concluída (Etapa 8):** Parte A fechada com **Etapas 1–7 = OK**, sem gaps bloqueantes. Estado resumido: E1 migração/modelo/validação parcelas x recorrência OK; E2 create parcelado (split centavos, datas mensais, retorno 1.ª parcela) OK; E3 CRUD + pay + filtros OK; E4 lista/FAB/chips/padding OK; E5 nova despesa (parcelas/recorrência/status/vencimento + invalidação providers) OK; E6 detalhe/edição para parcelas/recorrência OK; E7 Dio com refresh de 401 sem loop OK.
- **Recorrência automática (Etapa 9 — versão inicial):** backend passou a gerar despesas futuras de forma **lazy** e **idempotente** ao listar por mês (`GET /expenses?year=&month=`), com suporte a série (`recurring_series_id`) e cursor de geração (`recurring_generated_until`) via migração Alembic `005`. Contrato de controlo incluído: **`POST /expenses/{id}/recurrence/stop`** para parar a recorrência no item âncora. Testes adicionados em `tests/test_recurrence_service.py`.
- **Offline-first despesas (Etapa 10 — versão inicial):** mobile com `Hive` para cache local (`expenses_cache`) e fila de sync (`expenses_sync_queue`), fallback de listagem local quando API indisponível e enfileiramento de operações **create/update/pay/delete** para reenvio automático posterior.
- **Metas (Etapa 11 — versão inicial):** backend com tabela `goals` (Alembic `006`), rotas `GET/POST/PUT/DELETE /goals` e `POST /goals/{id}/contribute`; dashboard passou a devolver `goals_preview` com dados reais. Mobile: ecrã de metas deixou de ser placeholder puro e passou a listar metas reais via `GET /goals`.
- **Ainda não implementado** (próximo trabalho útil): família e sync conforme `Telas.txt` §6.

### 6.2 Próximo marco sugerido

1. **Próxima etapa ativa: Etapa 12** (família e sync núcleo) e depois Etapa 13 (biometria), salvo ajuste de prioridade.
2. **Hive / offline-first** alargado (Ordems §4.4) — corresponde à **Etapa 10** do documento de execução.
3. **§5.1** biometria/PIN — **Etapa 13** do mesmo documento.

### 6.3 Regras gerais (mantidas)
- **Não refazer:** instalação de Git, Flutter, Android SDK ou explicação de setup — já feito.
- **Perguntas ao utilizador:** modo orientação até pedir implementação; respostas curtas (§2).

---