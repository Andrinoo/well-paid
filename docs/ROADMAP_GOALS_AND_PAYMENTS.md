# Roadmap: metas (goals), pagamentos em despesas e gamificação

Documento de planeamento alinhado ao código em `backend/` e `mobile/` (abril 2026). Serve para priorizar trabalho em conjunto; não substitui issues/PRs.

**Implementado (2026-04-09):** M1 (valor inicial na UI + registo em `goal_contributions` na criação), M2/M3 (API + ecrã detalhe com histórico e contribuição), M4 parcial (eliminar bloqueado com saldo; arquivar via `is_active`), P1/P2 (confirmar pagar + mensagem 409), P3 (`paid_at` em despesas). Pendente: M6 offline para contribuições, M7 gamificação no dashboard, P4 destaque de parcelas na UI, enriquecer `pending_preview` com dados de parcela.

---

## 1. Estado atual (verificado no código)

### 1.1 Metas — backend

- Modelo `Goal`: `title`, `target_cents`, `current_cents`, `is_active`, timestamps.
- `POST /goals` aceita **`current_cents` na criação** (valor inicial ≥ 0).
- `POST /goals/{id}/contribute` soma `amount_cents` a `current_cents` — **não existe tabela de histórico**; só se infere “algo mudou” por `updated_at`.
- `PUT /goals/{id}` permite alterar título, alvo, saldo atual e `is_active`.
- `DELETE /goals/{id}` é **eliminação física** (perde título, alvo e todo o progresso).

### 1.2 Metas — mobile

- `GoalsRepository.createGoal` já envia `current_cents`, mas **`NewGoalPage` só pede título + meta** — o utilizador não consegue dizer “já tenho 2.875,00” na UI (fica sempre 0).
- **Não há ecrã** que chame `POST .../contribute` nem lista de contribuições com data/hora.
- Lista de metas e dashboard mostram preview simples (`GoalSummaryItem` / linhas).

### 1.3 Despesas — pagamentos

- `POST /expenses/{id}/pay` só permite se `status == pending`; passa a `paid`. **Não grava `paid_at`** (apenas inferência por estado/`updated_at` se existir no mixin).
- Parcelas: cada linha pode ser uma parcela; pagar marca **essa** linha. Lógica agregada (`installment_plan_has_paid`) aparece no detalhe para contexto do plano.
- Mobile: `payExpense` em lista, detalhe e dashboard; erros 409 tratados genericamente via mensagem de rede.

---

## 2. Roadmap sugerido — Metas

Ordem recomendada: **dados e API primeiro**, depois UI, depois dashboard/gamificação.

| Fase | Entrega | Notas |
|------|---------|--------|
| **M1** | **Valor inicial na criação** | Campo opcional “Já tenho (R$)” em `NewGoalPage` + validação `0 ≤ inicial ≤ alvo` (ou permitir acima do alvo com mensagem “meta já ultrapassada?” — decisão de produto). Backend já suporta. |
| **M2** | **Histórico de contribuições** | Nova tabela `goal_contributions` (ex.: `id`, `goal_id`, `amount_cents`, `recorded_at` timestamptz default now, opcional `note` curta). `POST /contribute` cria linha **e** atualiza `current_cents` (transação). `GET /goals/{id}/contributions` paginado. Migração Alembic nova. |
| **M3** | **Ecrã detalhe da meta** | Mostrar barra progresso, **falta** (`target - current`), botão **“Adicionar contribuição”** (sheet ou página), lista cronológica do histórico com data/hora localizada. |
| **M4** | **Cancelar vs apagar** | **Cancelar** = `is_active: false` (meta arquivada, visível em separador “Concluídas / Arquivadas” ou filtro). **Apagar** = política explícita: (A) só se `current_cents == 0`, ou (B) confirmação forte + texto “irá perder o histórico”, ou (C) soft-delete com retenção. Documentar escolha no README/API. |
| **M5** | **Meta concluída** | Quando `current_cents >= target_cents`: celebrar (snackbar, ícone, opcional `completed_at`), opcional auto-arquivar ou manter ativa até o utilizador fechar. |
| **M6** | **Sincronização offline (mobile)** | Estender fila em `GoalsLocalStore` para `contribute` (e eventualmente edições), alinhado ao padrão de despesas. |
| **M7** | **Gamificação / dashboard** | Ver secção 4. |

---

## 3. Roadmap sugerido — Reforço de pagamentos (despesas)

| Fase | Entrega | Notas |
|------|---------|--------|
| **P1** | **Confirmação antes de pagar** | Diálogo com valor (e parcela “3/12” se aplicável) para evitar toques acidentais; acessível e localizado. |
| **P2** | **Mensagens 409 claras** | Mapear `detail` da API para strings l10n (“Já está paga”, etc.). |
| **P3** | **Auditoria opcional** | Coluna `paid_at` (nullable) preenchida em `/pay` — útil para relatórios e lista “pagas recentemente”. |
| **P4** | **Parcelas na UI** | Em lista/detalhe, realçar **número da parcela** e total do plano quando `installment_total > 1`; opcional ação “ver plano” (outras parcelas do mesmo `installment_group_id`). |
| **P5** | **Pagamento parcial (futuro)** | Só se houver requisito de produto; implica modelo novo (pagamentos múltiplos por despesa ou subdivisão de linhas). Fora do MVP de “reforço”. |

---

## 4. Gamificação e dashboard (metas)

Ideias compatíveis com o que já existe (`_DashboardHeroCarousel`, donut de categorias, secção metas):

1. **Card “Metas ativas”** (paridade com reserva de emergência): progresso global ou da **meta em destaque**, CTA “Contribuir” e “Ver todas”.
2. **Donut / anel por meta**: percentagem `current/target`; setas ◀ ▶ ou `PageView` para **várias metas ativas** (máx. N no carrossel do hero).
3. **Marcos**: 25 % / 50 % / 75 % / 100 % com microcopy ou ícone (sem soar infantil — tom “Well Paid”).
4. **Streak ou consistência (opcional)**: “Contribuiu em 3 meses seguidos” — requer histórico (M2) e regras simples.

**Dependência:** M2 + M3 tornam a gamificação crível (dados reais por contribuição).

---

## 5. Outras sugestões (produto / UX / técnico)

- **Metas partilhadas na família**: hoje `Goal` é por `owner_user_id`; família vê metas dos peers na listagem, mas contribuições são só do dono. Decidir se cônjuge pode contribuir na mesma meta (modelo + permissões).
- **Notas em contribuições**: campo opcional “Ex.: 13º salário” para contexto no histórico.
- **Exportar / backup**: CSV ou PDF do histórico de contribuições (valor agregado para confiança).
- **Objetivos com prazo**: `target_date` opcional + ordenação no dashboard (“o que vence primeiro”).
- **Ligação despesa → meta (avançado)**: ao pagar ou ao registar poupança, opcional “atribuir X à meta Y” — automatiza contribuições a partir de fluxos reais.
- **Testes de API**: testes para `contribute`, limites (`current > target`), delete/cancel com histórico.

---

## 6. Resumo de prioridades

1. **Curto prazo:** M1 (valor inicial na UI) + P1 (confirmar pagamento) + P2 (erros claros).  
2. **Médio prazo:** M2 + M3 (histórico + ecrã detalhe + contribuir).  
3. **Política e limpeza:** M4 (cancelar / apagar com regras).  
4. **Engajamento:** M7 / secção 4 após histórico existir.  
5. **Despesas mais ricas:** P3–P4 conforme necessidade de relatórios e clareza de parcelas.

Quando uma fase for concluída, atualizar esta tabela ou referenciar o PR/issue correspondente.
