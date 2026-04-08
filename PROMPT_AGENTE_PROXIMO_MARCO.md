# Prompt para agente — Well Paid (marco: Dashboard MVP)

Copia o bloco entre `---INÍCIO---` e `---FIM---` para a primeira mensagem de um **novo chat/agente**. Trabalha **uma fase de cada vez**; só avança quando a fase anterior estiver fechada e documentada.

---

## ---INÍCIO---

### Papel

És um agente de implementação no repositório **Well Paid** (Flutter + FastAPI + PostgreSQL/Neon). Objetivo deste marco: **dashboard móvel** alinhado a `Telas.txt` §5.4, com código e documentação de qualidade profissional.

### Leitura obrigatória (por ordem, antes de codar)

1. `Ordems 1.md` — regras imutáveis (dinheiro em centavos, segurança, estrutura, sem segredos em `.md`).
2. `Telas.txt` — §5.4 Dashboard (especificação detalhada atualizada) e §6 endpoints relevantes.
3. `backend/.env.example` — variáveis; **nunca** commits nem colar `.env` real em chats ou docs.
4. Estrutura atual: `backend/app/` (rotas, models, schemas) e `mobile/lib/` (features, router).

### Regras de execução (melhores práticas para IA)

- **Uma fase por vez:** conclui, valida mentalmente ou com comandos, atualiza notas curtas no que pedirmos (ex.: comentário no PR ou secção no fim deste ficheiro **só se** o utilizador pedir commit de notas).
- **Escopo mínimo:** não refatorar ficheiros não relacionados; não adicionar dependências sem necessidade clara.
- **Segredos:** `DATABASE_URL`, SMTP, `SECRET_KEY` apenas em `.env` local; em exemplos usar placeholders como em `.env.example`.
- **Contrato API primeiro:** schemas Pydantic + respostas documentadas antes de UI complexa.
- **Mobile:** Riverpod/Dio/GoRouter como no projeto; valores monetários `int` centavos na camada de domínio.
- **Após cada fase:** lista objetiva do que foi feito + ficheiros tocados + como testar (comando ou passos), em **bullet points** na resposta ao utilizador.

### Fases (executar em sequência; parar ao fim de cada uma e reportar)

**Fase 1 — Desenho do contrato (só documentação no código)**  
- Definir endpoints REST (ou extensão dos existentes) para o dashboard: agregado **despesas por categoria no mês**, **total pendente**, **lista curta pendentes**, **próximos vencimentos** (conforme `Telas.txt` §5.4).  
- Escrever schemas Pydantic de request/response (datas ISO, `amount_cents`, `year`/`month` ou intervalo).  
- Se ainda não existirem modelos de despesa/categoria no BD, esboçar modelo mínimo compatível com §5.5–5.6 e `Ordems 1.md` §4.1.  
- **Entrega:** ficheiros de schema/rota **stub** ou OpenAPI gerado atualizado; **sem** UI Flutter ainda.

**Fase 2 — Persistência e migração**  
- Alembic: tabelas mínimas para despesas (e categorias se necessário), alinhadas ao contrato da Fase 1.  
- **Entrega:** migração aplicável; models SQLAlchemy; sem dados sensíveis em fixtures.

**Fase 3 — Implementação backend**  
- Implementar handlers reais, auth JWT onde aplicável, validação e erros HTTP consistentes.  
- Testes: pelo menos testes de integração ou unitários nos cálculos de agregação (centavos, período).  
- **Entrega:** endpoints funcionais documentados no código (docstrings curtas onde ajude).

**Fase 4 — Camada mobile (dados)**  
- Repositório Dio + models Dart (fromJson), providers Riverpod para o dashboard.  
- Tratamento de erro e loading states.  
- **Entrega:** chamadas à API verificáveis (logs ou teste manual descrito).

**Fase 5 — UI Dashboard (por secção, uma sub‑entrega por vez)**  
- 5a: layout scroll + card resumo mês + seletor de período.  
- 5b: donut (`fl_chart` ou já usado no projeto) + legenda + total no centro.  
- 5c: secção “A pagar” + navegação para lista §5.5.  
- 5d: secção “Próximos vencimentos” + urgência visual.  
- 5e: resumo metas (placeholder aceitável se API de metas ainda não existir — indicar TODO explícito).  
- 5f: FAB “+” para fluxo de nova despesa (stub de rota se §5.6 ainda não existir).  
- **Entrega:** UI coerente com tema existente do app; acessibilidade básica (semântica, contrastes).

**Fase 6 — Fecho**  
- Atualizar `Ordems 1.md` §6.0/§6.1 só com **factos** (ex.: “dashboard MVP com API X”).  
- Garantir `Telas.txt` §5.4 ainda condizente; ajustar só se a implementação divergir com acordo explícito no chat.  
- **Entrega:** lista final de verificação manual (checklist curta).

### Critério de sucesso

Utilizador autenticado abre **Home/Dashboard** e vê dados reais (ou vazio bem tratado) para o mês escolhido: rosca por categoria, totais e listas conforme §5.4, sem regressões no login/registo/recuperação de senha.

### Início

Começa pela **Fase 1**. Lê os ficheiros obrigatórios. Pergunta ao utilizador **só** se houver ambiguidade bloqueante (ex.: modelo familiar multi‑utilizador vs individual). Não antecipes Fases 2–6 na mesma resposta.

## ---FIM---

---

## Notas para o humano (opcional)

- Podes colar só o conteúdo entre INÍCIO/FIM no novo chat.  
- Se quiseres um marco **mais estreito** (só API, só UI), diz ao agente para parar após a fase N.  
- Referência visual **não** vai para o código: `Nao usar no projeto somente para exemplo/dashboard.html`.
- **Despesas + roadmap seguinte:** **`PROMPT_VERIFICACAO_DESPESAS_ETAPAS.md`** (Parte A = verificação; Parte B = continuidade) — **uma etapa por sessão**; registo factual em `Ordems 1.md` §6.1–§6.2.
