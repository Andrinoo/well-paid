# Arquitectura híbrida: rendimentos, histórico e rendibilidade (esboço)

Documento de **direcção** para evolução do Well Paid, sem impor desenho de implementação imediata. O produto hoje trata **rendimentos e investimentos** de forma **manual** (APIs existentes) com simulação onde aplica o caso (ex. investimentos, taxas).

## Objectivos

1. **Rendimentos**: continuar a permitir registo manual com qualidade; preparar o terreno para **sugestão ou import** de movimentos (ficheiro, PSD2, ou parceiro) sem duplicar linhas.
2. **Histórico real** vs simulado: separar o que vem de **transacções reais** do que vem de **projeccões** (dashboard, “evolução” de investimentos), para a UI e relatórios não misturarem fontes.
3. **Idempotência e auditoria**: toda a ingestão automática deveria carregar chaves externas (`external_id` / hash) e registo de origem, para reprocessar com segurança.

## Princípios

- **Fonte de verdade**: a API continua a ser a autoridade; o cliente móvel só orquestra.
- **Híbrido** = o utilizador pode corrigir, categorizar e apagar; importações alimentam **propostas** ou filas com revisão, conforme a maturidade do produto.
- **Não confundir** reserva de emergência (adesões internas) com rendimentos/reinvestimento—já hoje são domínios distintos no backend; manter essa clareza no modelo de eventos se surgirem *jobs* de reconciliação.

## Componentes a considerar (fases futuras)

| Camada | Papel |
|--------|--------|
| Conectores (opcional) | Ingest de extratos (CSV) ou de API de contas; mapear para o schema de `incomes` / movimentos. |
| Deduplicação | Hash por (data, valor, descrição, conta) ou id externo. |
| Job assíncrono | Filas (Redis, cron, ou fila do PaaS) para não bloquear o request móvel. |
| Vistas materializadas / snapshots | Onde a app precisar de “série histórica” de rendimento real por mês, sem recalcular tudo a cada *refresh*. |

## Relação com investimentos

- **Posições e rendimentos** mostrados na app: hoje podem assentar em taxas/estimativas; a evolução “real” exige *cashflows* (aportes, resgates) e *mark-to-market* ou rendimento creditado, conforme a fonte.
- O documento de contrato [ANDROID_API_BACKEND_CONTRACT.md](./ANDROID_API_BACKEND_CONTRACT.md) e os endpoints em `incomes`, `investments` são o ponto de partida para alinhar futuras colunas (ex. `ingestion_source`).

## Riscos e governança

- **Dados pessoais e PSD2** (Europa): consentimento, DPA, e minimização (só o necessário). Nada disto comita segredos no repositório.
- **Rate limits e abuso** na API: já existem mecanismos de limite (SlowAPI) em `main.py`; conectores devem respeitar os mesmos limites e não multiplicar chamadas a partir de um único dispositivo.

*Este ficheiro não substitui o plano de produto detalhado; serve para alinhar fases 5+ do roadmap (ingest híbrida) com a documentação existente.*
