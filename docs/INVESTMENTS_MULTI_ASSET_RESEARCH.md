# Plano de pesquisa para tela de investimentos

## Contexto atual no projeto

A tela de investimentos hoje ja possui:

- busca de tickers (`/investments/tickers/search`) com suporte a `instrument_type`,
- debounce no Android de `350ms` (form) e `300ms` (busca global),
- roteamento de dados em `market_data_router` com foco principal em `stocks`,
- classificacao por heuristica no app (`inferInstrumentType`) que ainda tende a marcar ticker B3 como `stocks`.

Para atingir o objetivo de cobertura ampla (Acoes, FIIs, BDRs, ETFs e Tesouro Direto), o desenho abaixo separa claramente pesquisa, classificacao e consulta detalhada por tipo.

## 1) Mapeamento de provedores gratuitos (com validacao)

### `brapi.dev`

- Cobertura observada: cotacoes/listagens do mercado brasileiro com boa utilidade para busca e quote inicial.
- Pontos fortes: simples para comecar, integracao leve.
- Risco: limites de plano gratuito e variacao de cobertura para dados fundamentalistas mais profundos.
- Uso recomendado: provider principal para `search/suggestions` e `quote` de ativos listados.

### `dadosdemercado.com.br` (documentacao publica)

- Cobertura observada: endpoints dedicados para FIIs e Tesouro.
- Pontos fortes: orientado a classes de ativos que precisamos ampliar (FII/Tesouro).
- Risco: alguns endpoints exigem token; validar limite gratuito operacional antes de producao.
- Uso recomendado: complementar FIIs (dividendos/proventos) e Tesouro (preco/taxa/historico).

### `usebolsai.com` (free tier)

- Cobertura observada: fundamentos, historico, dividendos, acoes e FIIs, com cota diaria gratuita.
- Pontos fortes: pacote unificado de fundamentos + proventos.
- Risco: limite diario baixo para escala sem cache.
- Uso recomendado: provider principal de fundamentos/proventos no MVP multiasset, com cache agressivo.

### `developers.b3.com.br`

- Cobertura observada: catalogo oficial de APIs B3.
- Pontos fortes: fonte oficial e referencia de dominio.
- Risco: para Tesouro e Area do Investidor ha restricoes B2B e nao ha acesso direto para pessoa fisica em varios cenarios.
- Uso recomendado: base normativa e futura evolucao enterprise, nao depender como unico caminho gratuito imediato.

### `tesourodireto.com.br` (JSON/CSV publicos)

- Cobertura observada: feeds/arquivos publicos usados pela comunidade para preco/taxa e historicos.
- Pontos fortes: dados oficiais/publicos amplamente usados para pesquisa.
- Risco: estabilidade e disponibilidade podem variar; necessita camada de resiliencia e validacao de termos.
- Uso recomendado: fallback para Tesouro no modo gratuito, sempre com cache, retry e monitoramento.

## 2) Contrato de dados unificado (proposta)

Padrao para todo ativo:

- `assetType`: `stock | fii | bdr | etf | treasury`
- `ticker`: codigo pesquisavel (ex.: `PETR4`, `MXRF11`, `BOVA11`)
- `displayName`: nome amigavel do ativo
- `source`: provider que respondeu
- `confidence`: confianca do roteador/classificador (0.0-1.0)
- `asOf`: timestamp da referencia

### `AssetSearchResult`

Retorno do autocomplete e lista curta de pesquisa:

- `ticker`, `displayName`, `assetType`, `source`, `confidence`
- `badges`: lista de marcadores de UI (ex.: `FII`, `ETF`, `TESOURO`)
- `searchKey`: texto normalizado para rank

### `AssetDetails`

Resposta detalhada por tipo com envelope comum:

- `asset`: metadados basicos (`ticker`, `displayName`, `assetType`)
- `quote`: bloco padrao (`lastPrice`, `changePercent`, `currency`, `asOf`)
- `fundamentals`: objeto opcional por tipo
  - `stock/bdr`: `pl`, `pvp`, `roe`, `evEbitda`, `dy`
  - `fii`: `pvp`, `dy12m`, `vacancy` (quando disponivel), `navPerShare`
  - `etf`: `aum`, `adminFee`, `trackingIndex` (quando disponivel)
  - `treasury`: `buyRate`, `sellRate`, `buyPrice`, `sellPrice`, `maturityDate`
- `error`: erro de provider, quando houver

### `AssetPayout`

Unificacao de proventos:

- `assetType`, `ticker`, `eventType` (`dividend | jcp | rendimento_fii | treasury_coupon`)
- `exDate`, `paymentDate`, `valuePerUnit`, `currency`
- `source`, `asOf`

## 3) Comportamento da busca unificada

## Fluxo recomendado

1. Usuario digita no mesmo campo.
2. Entrada passa por normalizacao (`trim`, uppercase para ticker, remocao de ruido).
3. Debounce aplicado.
4. Chamada `search/suggestions`.
5. Classificador escolhe `assetType`.
6. UI mostra sugestoes com badge de tipo.
7. Ao selecionar, carregar `asset/details` e `asset/proventos` conforme tipo.

## Parametros de UX/performance

- Debounce alvo inicial: `600ms` (faixa aceitavel `450-700ms`).
- `minLength` para query de API: `3`.
- `Enter` executa busca imediata (bypass debounce).
- Cancelamento de chamadas antigas com `AbortController` (frontend web) / `collectLatest` + cancelacao de job (Android).
- Regra `last-write-wins` com `requestId` monotonic para evitar que resposta antiga sobrescreva resultado novo.

## Ajustes recomendados no estado atual do app

- Aumentar debounce global do Android de `300ms` para `600ms` e o do form de `350ms` para `600ms`.
- Subir `minLength` local de `2` para `3` em busca global e formulario.
- Trocar classificacao simplista de ticker como `stocks` por lookup deterministico via `ticker_cache`/catalogo de instrumentos.
- Manter heuristica apenas como fallback e nunca decisao final em casos ambiguos (ex.: sufixo `11`).

## 4) Arquitetura de classificacao por tipo

## Catalogo mestre (`asset master`)

Criar base cacheada com:

- `ticker`, `displayName`, `assetType`,
- `primaryProvider`, `fallbackProvider`,
- `updatedAt`, `isActive`.

## Regras de decisao

- Primeira camada: match deterministico pelo catalogo mestre.
- Segunda camada: heuristica por padrao textual somente quando ticker nao estiver no catalogo.
- Em empate/ambiguidade: retornar multiplas sugestoes com `confidence` e badge de tipo, sem forcar tipo unico.

## Roteamento por tipo (provider strategy)

- `stock`: `brapi` (quote/search) + `bolsai` (fundamentos/proventos) + fallback atual.
- `fii`: `dadosdemercado` ou `bolsai` para fundamentos/proventos.
- `bdr`: `brapi` para quote/search; fundamentos conforme disponibilidade.
- `etf`: `brapi` para quote/search; metadados de ETF via provider secundario quando houver.
- `treasury`: feed Tesouro/Dados de Mercado com politica de cache independente da bolsa.

## 5) Politica de atualizacao e cache (modo gratuito)

- Busca (`search/suggestions`): cache de 5 a 15 minutos.
- Cotacao em tela ativa: refresh de 15 a 60 segundos.
- Fundamentos/proventos: refresh de 12 a 24 horas.
- Tesouro: janela dedicada, com horario e periodicidade proprios.
- Fallback por limite: quando provider primario falhar por quota, responder dado de cache + `source=fallback` + `stale=true`.

## 6) Testes e criterios de aceite

- Nenhuma selecao ambigua classificada no tipo errado nos casos de regressao principais (`KLBN11`, FIIs e ETFs conhecidos).
- Busca com digitacao rapida nao dispara respostas fora de ordem.
- Menos de 1% de erro em logs de `search -> details` por classificacao incorreta.
- Tempo de resposta de sugestoes em cache quente abaixo de 300ms.
- Cobertura de testes:
  - classificacao deterministica e fallback,
  - debounce + cancelamento,
  - roteamento por tipo,
  - degradacao de provider (quota/timeout).

## 7) Rollout incremental recomendado

### Fase 1 - Base de busca e classificacao

- Criar contrato unificado.
- Implementar catalogo mestre e badges de tipo.
- Ajustar debounce/minLength/cancelamento.

### Fase 2 - Acoes e FIIs

- Consolidar quote + fundamentos + proventos para `stock` e `fii`.
- Validar cobertura de top tickers e casos de ambiguidade.

### Fase 3 - BDRs e ETFs

- Ativar roteamento especifico para `bdr` e `etf`.
- Revisar metadados e proventos por disponibilidade de provider.

### Fase 4 - Tesouro Direto

- Integrar feed dedicado com cache e regras de resiliencia.
- Ajustar UX para exibicao de taxa/preco/vencimento.

### Fase 5 - Operacao e observabilidade

- Painel com erros por provider, hit-rate de cache e latencia por endpoint.
- Alertas de quota e fallback acionado.

## 8) Material de pesquisa continuo (gratuito)

Manter uma rotina quinzenal de validacao de:

- limites de uso dos provedores gratuitos,
- estabilidade dos endpoints de Tesouro e FIIs,
- aderencia dos campos fundamentais por tipo de ativo,
- custo de migracao para planos pagos caso o volume ultrapasse o free tier.

Esse pacote permite desenvolver de forma abrangente no curto prazo com base gratuita, reduzindo risco de lock-in e evitando retrabalho na classificacao por tipo.
