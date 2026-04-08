# Ideia futura: Savings & reservas (rascunho para amadurecer)

Não é especificação implementada — só orientação de produto para evoluir o Well Paid além de **proventos** e **metas** actuais.

## Problema que resolve

Famílias precisam de **clareza** entre: dinheiro para o mês, dinheiro “não tocável”, e posição em investimentos — sem confundir com despesas do dia-a-dia ou duplicar entradas.

## Pilares possíveis

1. **Reserva de emergência**  
   Meta dedicada ou “balde” com regra simples (ex.: alvo em meses de despesas médias). Não é investimento arriscado; é liquidez.

2. **Metas (já existem)**  
   Extensão: etiquetas *curto / médio / longo prazo*, ou limite de retirada sem confirmação extra.

3. **Investimentos (registo)**  
   Posição agregada (montante, instituição opcional, tipo muito genérico: “ETF”, “PPR”, “depósito”). **Sem recomendações financeiras** — só visão e evolução no tempo (manual ou import futuro).

4. **Transferências internas**  
   Movimento “da conta corrente para reserva” como evento distinto de **despesa** e de **provento** (evita contar poupança como gasto).

## Princípios

- Valores sempre em **centavos** (inteiro), como no resto da app.  
- Separar **entrada de dinheiro** (proventos) de **alocação** (o que guardaste).  
- UX simples para casal: o que é visível para ambos e o que é opcionalmente privado (decisão futura).

Quando fores implementar, alinha com `Ordems 1.md` (branch `feature/…`, SemVer MINOR se a API crescer de forma compatível).
