# Savings & reservas (Well Paid)

## Fase 1 — implementada (reserva de emergência simples)

- **Modelo:** uma reserva por **família** (`family_id`) ou por **utilizador sem família** (`solo_user_id`).
- **Meta mensal:** `monthly_target_cents` (PUT `/emergency-reserve`).
- **Saldo:** `balance_cents` aumenta com **acréscimos idempotentes por mês civil** (`emergency_reserve_accruals`), desde `tracking_start` até ao mês corrente, **desde que** a meta mensal seja > 0.
- **Nota de produto:** numa única execução, os meses em falta usam o **mesmo** `monthly_target_cents` actual (ver comentário em `app/services/emergency_reserve.py`). Não substitui saldo bancário; copy na app deixa isso explícito.
- **Dashboard:** `GET /dashboard/overview` inclui `emergency_reserve_balance_cents` e `emergency_reserve_monthly_target_cents` (após aplicar acréscimos).
- **Cliente:** cartão no dashboard, definições em Definições, rota `/emergency-reserve`.
- **Permissões família:** quando a reserva está no escopo da família, **apenas o titular (`owner`)** pode alterar a meta mensal (`PUT /emergency-reserve`).
- **Resiliência de ambiente:** se as tabelas da reserva ainda não existirem na BD local (migração pendente), `GET /emergency-reserve` devolve estado default (sem 500) e `PUT /emergency-reserve` devolve `503` com instrução para executar `python -m alembic upgrade head`.

## Fase 2 — por definir (não implementado)

- **Transferências internas** (movimento para reserva sem contar como despesa).
- **Registo de investimentos** (posição manual, tipos genéricos).
- **“Sugestões” de mercado** (acções/fundos “em alta” via APIs): **exige estudo jurídico** (marketing de instrumentos financeiros / MIFID na UE) antes de qualquer implementação; não faz parte da Fase 1.

## Pilares que permanecem válidos

1. **Reserva de emergência** — Fase 1 cobre meta mensal + saldo interno; evoluções: alvo em meses de despesas, categorias “essenciais”.
2. **Metas (já existem)** — objectivos genéricos; a reserva é um módulo à parte para clareza.
3. **Investimentos (registo)** — futuro; sem recomendações.
4. **Transferências internas** — futuro; evita dupla contagem.

## Princípios

- Valores em **centavos** (inteiro).
- Separar **proventos** (entradas registadas) de **alocação** (reserva na app).
- UX para casal: reserva partilhada ao nível da família quando `family_id` existe.

Alinha implementações com `Ordems 1.md` (branch `feature/…`, SemVer MINOR quando a API crescer de forma compatível).
