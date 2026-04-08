# Execução por etapas — despesas e continuidade do roadmap

Documento em **duas partes**: (A) **conferência** do que já foi implementado; (B) **continuidade** após as etapas de verificação, alinhada a `Ordems 1.md` §6.1–§6.2 e `Telas.txt`.

**Regra:** **uma etapa por sessão** (ou por mensagem a um agente). Não pedir “verificar ou fazer tudo” de uma vez.

**Como usar:** copia só o bloco da **etapa** escolhida (entre `---ETAPA X---` e o fim dessa secção) para o chat/agente, ou segue o checklist manualmente.

| Parte | Etapas | Natureza |
|--------|--------|----------|
| **A — Verificação** | 1 a 7 | Só **conferir** o que existe (agente não alarga escopo salvo correção de gap a pedido explícito). |
| **B — Continuidade** | 8 a 13 | **Planeamento + implementação** incremental (cada etapa fecha um fatia do marco). |

**Leitura de contexto (opcional, antes da Etapa 1):** `Ordems 1.md` §4.1 (centavos), §6.1–§6.2; `backend/app/schemas/expense.py`, `backend/app/api/routes/expenses.py`, `mobile/lib/features/expenses/`.

---

## Parte A — Verificação (conferência)

---

## ---ETAPA 1 — Backend: modelo e migração---

### Objetivo

Confirmar que a base de dados e o modelo alinham com parcelamento e metadados de recorrência.

### Checklist

- Migração Alembic `004` (ou equivalente) aplicada: colunas `installment_total`, `installment_number`, `installment_group_id`, `recurring_frequency` em `expenses`.
- Modelo SQLAlchemy e `ExpenseResponse` expõem os mesmos campos na API.
- Regra de negócio documentada no código ou schema: **não** combinar `installment_total > 1` com `recurring_frequency` definida no **create** (validador Pydantic).

### Comandos sugeridos (ajustar caminho se necessário)

```text
cd backend
alembic current
pytest tests/ -q --tb=no -k expense
```

*(Se não existirem testes com `-k expense`, indicar “sem testes nomeados”; a etapa continua válida com revisão de ficheiros.)*

### Critério de fecho

Lista objetiva: ficheiros revistos + confirmação da regra parcelas vs recorrência + estado da migração.

---

## ---ETAPA 2 — Backend: criação parcelada e resposta---

### Objetivo

Validar o comportamento de `POST /expenses` quando `installment_total > 1`.

### Checklist

- São criadas **N** linhas com o mesmo `installment_group_id`, números 1..N e valores coerentes (soma ≈ total em centavos, conforme implementação `_split_amounts_cents`).
- Datas de competência/vencimento avançam por mês (ou conforme `_add_months`) de forma consistente.
- A resposta HTTP do create devolve **apenas a primeira parcela**; o cliente deve invalidar lista/dashboard após criar.

### Critério de fecho

Descrição do comportamento observado (teste manual ou teste automatizado) + referência ao endpoint.

---

## ---ETAPA 3 — Backend: CRUD geral e pay---

### Objetivo

Garantir que o fluxo mínimo da API está íntegro para uma despesa simples e para uma parcela isolada.

### Checklist

- `GET /expenses` com filtros (mês/ano, status) devolve `installment_*` e `recurring_frequency` quando aplicável.
- `PUT /expenses/{id}` atualiza campos esperados; recorrência em linha de parcela de plano: política clara (ex.: `null` no update para parcelas).
- `POST /expenses/{id}/pay` e `DELETE` comportam-se como documentado (401 sem JWT, 404 id inexistente).

### Critério de fecho

Matriz curta: operação → resultado esperado → OK/Falha.

---

## ---ETAPA 4 — Mobile: lista e atalhos---

### Objetivo

Conferir UX na lista de despesas sem abrir ainda novo/editar.

### Checklist

- Card/barra no **corpo** da lista com atalho **Nova despesa** + atualizar (além do ícone na app bar, se existir).
- **FAB** “Nova” visível e navega para `/expenses/new`.
- Lista com **padding** inferior adequado para não ficar sob o FAB.
- Em cada tile: chip **Parcela X/Y** quando `installmentTotal > 1`; chip/texto de recorrência quando `recurringLabelPt` não é nulo.

### Comando sugerido

```text
cd mobile
flutter analyze lib/features/expenses/presentation/expense_list_page.dart
```

### Critério de fecho

Screenshots opcionais; confirmação checklist + analyze limpo.

---

## ---ETAPA 5 — Mobile: nova despesa---

### Objetivo

Validar o formulário de criação alinhado ao backend.

### Checklist

- Parcelas: intervalo permitido (ex. 1–24), texto de ajuda coerente.
- Recorrência: só quando parcela única; desativada ou oculta com múltiplas parcelas.
- Interruptores **Já paga** / **Tem vencimento** (ou equivalente) e envio de `due_date` / `status` corretos.
- Após criar parcelado: lista/dashboard atualizam (invalidação de providers).

### Critério de fecho

Passos manuais numerados (1 criar 3 parcelas → 2 ver lista) + resultado.

---

## ---ETAPA 6 — Mobile: detalhe e edição---

### Objetivo

Conferir ecrãs de detalhe e edição para parcelas e metadado de recorrência.

### Checklist

- **Detalhe:** linhas para parcelas (X de Y) e recorrência quando existirem.
- **Editar:** banner informativo em plano parcelado (“alterações só a esta linha”).
- **Editar:** dropdown de recorrência ausente ou sem efeito indevido em linhas de parcela; `PUT` envia `recurring_frequency` coerente em despesa única.

### Comando sugerido

```text
flutter analyze lib/features/expenses/presentation/expense_detail_page.dart lib/features/expenses/presentation/expense_edit_page.dart
```

### Critério de fecho

Checklist preenchido + analyze sem issues nesses ficheiros.

---

## ---ETAPA 7 — Rede e sessão (opcional)---

### Objetivo

Se o projeto inclui refresh automático no Dio, validar que 401 dispara refresh e que falha de refresh trata sessão expirada sem loop.

### Checklist

- Pedidos autenticados usam cliente com interceptor de refresh.
- Após operações de despesa (lista, criar, editar), não há regressão de login.

### Critério de fecho

Nota breve de revisão de `dio_client` / providers de rede (sem colar tokens).

---

## Parte B — Continuidade (após Parte A)

Executar **em ordem** a partir da **Etapa 8**, salvo decisão explícita de adiar um marco (ex. offline-first antes de recorrência automática). Cada etapa pode incluir **implementação**; manter difs pequenos e testáveis.

---

## ---ETAPA 8 — Fecho da conferência e gaps---

### Objetivo

Consolidar o resultado das Etapas 1–7 e fechar falhas antes de novo desenvolvimento.

### Checklist

- Resumo por etapa: OK / gap (lista curta).
- Para cada gap: tarefa única (ficheiro ou endpoint) + critério de aceitação.
- **Só nesta etapa** (ou a pedido explícito): corrigir bugs bloqueantes encontrados na Parte A.

### Critério de fecho

Documento ou comentário de projeto com “verificação despesas: concluída” ou lista de gaps remanescentes priorizada.

---

## ---ETAPA 9 — Recorrência automática (geração de despesas)---

### Objetivo

Sair do metadado `recurring_frequency` “só na UI” para **criação automática** de linhas futuras (ou modelo equivalente: template + instâncias), conforme `Ordems 1.md` §6.1 (“ainda não implementado”).

### Checklist

- Decisão registada: ex. job diário/semanal no servidor, ou geração lazy na primeira consulta do mês; idempotência (não duplicar o mesmo período).
- Contrato API: como marcar/editar/cancelar série; impacto em `PUT/DELETE` de uma linha vs série inteira.
- Migração ou campos novos se necessário (ex. `recurring_series_id`, `generated_until`).
- Testes mínimos no backend para a regra de geração (centavos, datas ISO).

### Critério de fecho

Comportamento descrito + primeira versão implementável merged ou PR com escopo fechado da Etapa 9.

---

## ---ETAPA 10 — Hive / offline-first (despesas)---

### Objetivo

Alargar `Ordems 1.md` §4.4 ao fluxo de despesas: escrita local primeiro, sync com conflitos básicos.

### Checklist

- Modelo local (Hive) alinhado a `ExpenseItem` / campos da API.
- Fila `sync_status`: criar/editar/pagar offline → envio quando online.
- Reconciliação com resposta do servidor (IDs, `updated_at`); estratégia documentada (ex. LWW).
- Não regressar: login, lista online-only atual continua a funcionar até feature flag ou migração suave.

### Critério de fecho

Cenário manual: “modo avião → criar despesa → online → aparece no servidor e no dashboard”.

---

## ---ETAPA 11 — Metas (API + dashboard)---

### Objetivo

Substituir placeholder de metas no dashboard por `GET /goals` (ou contrato definido em `Telas.txt` §5.7) + UI mínima.

### Checklist

- Schemas + rotas + migração se precisar de tabelas novas.
- Mobile: provider + cartão de metas no dashboard com dados reais ou estado vazio tratado.
- Testes ou verificação manual documentada.

### Critério de fecho

Dashboard sem TODO crítico para “metas” ou TODO explícito com link ao ticket da Etapa 11.

---

## ---ETAPA 12 — Família e sync (núcleo Telas §6)---

### Objetivo

Evoluir de utilizador isolado para **família** (`families`, `family_members`) e regras de partilha de despesas, conforme `Ordems 1.md` §4.5 e `Telas.txt` §6.

### Checklist

- Modelo de dados e RLS ou filtro por `family_id` em despesas/dashboard.
- Convite / entrada no grupo (MVP: código ou link; QR pode ser sub-etapa).
- Despesas `is_shared` / divisão: contrato e UI mínima.

### Critério de fecho

Dois utilizadores na mesma família veem o mesmo conjunto de despesas (teste manual descrito).

---

## ---ETAPA 13 — Biometria / PIN (Telas §5.1)---

### Objetivo

Proteger abertura do app ou ações sensíveis após o núcleo financeiro estável (`Ordems 1.md` §6.2 item 3).

### Checklist

- `local_auth` (ou PIN local) integrado ao fluxo de arranque; fallback e recuperação (sessão JWT) definidos.
- Não armazenar segredos em texto claro; alinhar a `flutter_secure_storage` existente.

### Critério de fecho

Utilizador ativa biometria/PIN, fecha app, reabre: pede desbloqueio antes de mostrar dados sensíveis.

---

## Notas para agentes de IA

- **Parte A (Etapas 1–7):** não implementar melhorias novas; **verificar**, reportar gaps e corrigir **só** o que falhar nessa etapa **se o utilizador pedir**.
- **Parte B (Etapas 8–13):** cada mensagem deve **focar numa única etapa**; ao fechar, atualizar `Ordems 1.md` §6.1/§6.2 com **factos** (sem segredos).
- **Ordem sugerida:** 8 → 9 ou 10 (priorizar 10 se offline for mais urgente que recorrência automática) → 11 → 12 → 13. Ajustar com o utilizador se o produto exigir outra sequência.
