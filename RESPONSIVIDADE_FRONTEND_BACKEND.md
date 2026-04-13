# Responsividade, UX adaptativa e alinhamento frontend ↔ backend

Documento de visão geral sobre **como o sistema lida com ecrãs, densidade, acessibilidade e scroll** na app Flutter (`mobile/`), e **como o backend condiciona** tamanho de payloads e comportamento das telas. Não substitui especificações pontuais em [GRAFICOS_DONUT_FLUXO_COMPOSE.md](./GRAFICOS_DONUT_FLUXO_COMPOSE.md), [GRAFICO_FLUXO_MODO_DINAMICO.md](./GRAFICO_FLUXO_MODO_DINAMICO.md) ou [CABECALHOS_TABS_PRINCIPAIS.md](./CABECALHOS_TABS_PRINCIPAIS.md).

**Âmbito:** a experiência principal é **telefone em portrait**; não há breakpoints dedicados a tablet/desktop com layouts alternativos — a adaptação é **contínua** (constraints, `FittedBox`, `LayoutBuilder`, `MediaQuery`).

---

## 1. Princípios gerais

1. **Conteúdo fluido:** listas e formulários usam scroll vertical; evita-se altura intrínseca infinita dentro de regiões com altura limitada (ex.: `SliverFillRemaining` + `PageView` no Início).
2. **Texto e toque:** rótulos com `maxLines` + `ellipsis` onde o espaço é apertado; áreas de toque respeitam `IconButton` / chips com limites mínimos quando possível.
3. **Escala de fonte do sistema:** o gráfico de fluxo ajusta **espaço reservado aos eixos** com `MediaQuery.textScalerOf` para reduzir sobreposição quando o utilizador aumenta o texto.
4. **Movimento reduzido:** `MediaQuery.disableAnimations` (ou `maybeOf`) desativa ou simplifica animações (curvas do gráfico de linhas, loaders substituídos por ícones estáticos em alguns casos).
5. **Teclado e safe areas:** `viewInsets` e `padding` do sistema evitam que campos fiquem tapados (auth com scroll, metas com padding inferior).
6. **Dados e rede:** o backend **limita** pré-visualizações no overview; listas mensais vêm **completas** por mês — a “responsividade” do volume é **scroll + cache**, não paginação por página na lista de despesas.

---

## 2. Melhorias e padrões no frontend (por área)

### 2.1 Início (`HomePage`) e dashboard

| Tema | Comportamento |
|------|----------------|
| **Header financeiro** | Três colunas com `Expanded` (1 : 2 : 1); valores e rótulos com `maxLines: 1` e `ellipsis`; modo `compact` reduz fonte (~9,5 / 11 sp) para caber em ecrãs estreitos. |
| **Seletor de mês no header** | Variante `dark` + `dense` + `ultraDense`: ícones menores, padding mínimo, label `MM/AAAA` com tamanho fixo 13 — menos altura útil consumida. |
| **Corpo do dashboard** | `RefreshIndicator` + `CustomScrollView` com scroll sempre ativo (`AlwaysScrollableScrollPhysics`) para pull-to-refresh mesmo com pouco conteúdo. |
| **Pager Categorias / Fluxo** | `SliverFillRemaining(hasScrollBody: true)` + `PageView`: corrigiu **layout que falhava em release** quando uma página usava scroll com altura intrínseca infinita; cada página recebe **altura consistente**. |
| **Inset inferior** | `MediaQuery.padding.bottom + 72` para não esconder conteúdo atrás da barra de navegação e do handle do painel rápido. |
| **Tab Fluxo** | `LayoutBuilder` passa `constraints.maxHeight` para o cartão de cashflow → **altura do gráfico** derivada da viewport real (`min(H*0.44, H-162)` clamp), evitando “comer” ecrãs baixos (comentário explícito no código). |
| **Overview em reload** | `skipLoadingOnReload: true` no `when` do overview — ao refrescar, **não** pisca para skeleton completo se já houver dados, mantendo a hierarquia visual estável. |

**Backend associado:** `GET /dashboard/overview?year=&month=` devolve um único objeto agregado; o header só precisa deste payload (não compõe totais no cliente). Pré-visualizações são **capped** no servidor (ver §3).

### 2.2 Gráfico donut (`CategoryDonutChart`)

- `LayoutBuilder` + fórmula `side = min(clamp(w, 158, min(420, w*0.99)), h)` — o anel **cresce** com o espaço até um teto, mas **nunca** excede a altura disponível.
- Legenda: altura dos tiles da grelha **66 / 72 / 76** dp conforme a altura de layout (`layoutH ≥ 380` / `480`).
- Centro do donut: `FittedBox` + `scaleDown` para texto dentro do buraco.
- Estado sem dados: coluna com `Expanded` + CTA sem quebrar o cartão.

**Backend:** categorias e totais vêm do mesmo overview; agregação “top 5 + outros” é **no cliente**.

### 2.3 Gráfico de linhas (fluxo)

- `shortestSide`, `width`, `height` para `chartHeight` quando não há altura de tab explícita.
- `textScaler` para `leftAxisReserved` e `bottomAxisReserved`.
- Rótulos de eixo com `FittedBox` / `scaleDown`.
- Legenda em três colunas com `Expanded` + `FittedBox` por chip.
- `FlClipData.none()` (equivalente conceitual: não cortar linhas na caixa).

**Backend:** `GET /dashboard/cashflow` devolve vetores paralelos; o número de pontos varia com janela dinâmica/fixe e `forecast_months` — o eixo adapta-se ao comprimento devolvido.

### 2.4 Autenticação (`AuthShell`)

- Largura útil: `min(380, w - 2*sidePad)` com `sidePad` **por breakpoints de largura** (`<360`, `<520`, `<900`, senão 36).
- Altura: gaps entre logo, wordmark e cartão **menores** se `h < 700` / `< 640` / `< 780`.
- `CustomScrollView` + `resizeToAvoidBottomInset` + `keyboardDismissBehavior: onDrag` — teclado não bloqueia o formulário em ecrãs curtos.
- Wordmark: `FittedBox(scaleDown)` para não estourar horizontalmente.
- Padding inferior: `max(24, safeBottom + 16)`.

**Backend:** independente do layout; fluxos login/registo usam os mesmos endpoints.

### 2.5 Despesas e proventos (listas)

- **AppBar** padrão tema; barra de mês em faixa `creamMuted`.
- Listas com `ListView` / estados loading com `LinearProgressIndicator`; texto de erro centrado com padding.
- Filtro por categoria (despesas): faixa destacada; alinhado a `category_id` na API.
- **Sincronização de mês** com o dashboard: `syncListFiltersWithDashboardPeriod` e `warmMonthlyListsForDashboardPeriod` ao mudar período ou ao mudar de tab no shell — **menos espera** ao abrir listas.

**Backend:** `GET /expenses` com `year`, `month`, `status`, `category_id` — resposta é **lista completa** do mês (ordenada); projeções de recorrentes entram na mesma resposta quando aplicável. O cliente **faz scroll**; não há paginação offset no endpoint mensal.

### 2.6 A pagar (`ToPayPage`)

- Itens com `LayoutBuilder`: se `maxWidth < 420`, layout **empilhado** (descrição em cima, valor + switch alinhados à direita em baixo); caso contrário **linha** com `Expanded` + coluna à direita — melhora legibilidade em telemóveis estreitos.

**Backend:** listagem de pendentes com regras próprias (incl. projeções em alguns modos); volume pode crescer — UI continua a ser lista scrollável.

### 2.7 Metas

- `Padding` com `MediaQuery.viewInsets.bottom` no contexto de formulários (ex. detalhe) para teclado.
- Lista com refresh; estado vazio centrado; cartão agregado no topo quando há metas ativas.

**Backend:** `GET` de metas com `limit` query (cap em 500 no router) — listas muito grandes são teoricamente possíveis; a UI em lista simples continua a depender de scroll.

### 2.8 Reserva de emergência

- `skipLoadingOnReload` em fluxos async para estabilidade visual.
- Formulários em `ListView` com padding fixo.

**Backend:** acréscimos com `limit` query (1–60) na listagem de meses — payload limitado; alinhado a uma lista scrollável compacta.

### 2.9 Shell inferior (`MainShell`)

- `AnimatedSize` / `reduceMotion` (duração ~1 ms) no painel rápido expandido.
- `NavigationBar` com tema custom (indicador gold, labels que mudam peso quando selecionados).

**Backend:** atalhos navegam para rotas que disparam os mesmos providers (to-pay, shopping lists).

### 2.10 Listas de compras (detalhe)

- Uso de `MediaQuery.viewInsetsOf(context).bottom` para ajustar padding com teclado aberto (itens / edição).

---

## 3. Backend: o que afeta “cabe” no ecrã e percepção de performance

| Área | Comportamento | Impacto no frontend |
|------|----------------|---------------------|
| **`GET /dashboard/overview`** | Totais e categorias **sem limite artificial** na soma; `pending_preview` **limit(5)**; `upcoming_due` **limit(20)**; `goals_preview` **limit(3)**. | Cartões de resumo usam estes subconjuntos — a UI **não** deve assumir “todas as despesas pendentes” no overview, só pré-visualização. |
| **`GET /dashboard/cashflow`** | Série mensal com histórico variável (8 meses dinâmicos ou 6 fixos) + previsão. | Largura do eixo X e densidade de rótulos adaptam-se ao **n** devolvido (`xInterval` se `n > 12`). |
| **`GET /expenses`** (mês) | Lista completa do mês + lógica de recorrentes projetadas. | Scroll longo em meses carregados; cache Riverpod/Hive reduz idas à rede ao pré-aquecer ±4 meses (`warmNineMonthExpenseIncomeCaches`). |
| **Família** | Dados agregados por `family_peer_user_ids`. | Mesma UI; mais linhas possíveis nas listas — reforça importância de scroll e performance de item. |
| **Rate limit (auth)** | Limites por minuto em rotas sensíveis. | Erros tratados com mensagens; não alteram layout, podem mostrar snackbar/dialog. |

---

## 4. Estratégias transversais “melhorias aplicadas”

1. **Warmup após login / mudança de tab:** `scheduleShellDataWarmup`, `warmGlobalReferenceData`, `warmMonthlyListsForDashboardPeriod`, `warmNineMonthExpenseIncomeCaches`, prefetch de overview em janela ao mudar mês (`_prefetchWindow` no `PeriodSelectorBar`) — **menos estados de loading** ao navegar.
2. **`skipLoadingOnReload`** em overview, listas, metas, cashflow, reserva — **evita flicker** em refresh.
3. **`keepAlive` + TTL** em vários `FutureProvider` (ex. 3–10 min) — equilíbrio memória vs fluidez ao voltar a uma tab.
4. **Comentários de regressão** no código (ex. `SliverFillRemaining` + `PageView`, altura do cashflow) documentam **porquê** o layout foi feito assim — relevante para manutenção e portes (Compose).

---

## 5. Limitações atuais (transparência)

- **Sem** layout multi-coluna ou master-detail automático em tablets.
- **Paginação server-side** não é usada na lista mensal de despesas/proventos — meses muito densos aumentam payload e tempo de primeira pintura (mitigado em parte por cache).
- Modo **fixo** do cashflow não reancora ao mudar só o mês do header sem reaplicar o pedido (ver doc do modo dinâmico).

---

## 6. Referências de ficheiros

| Tópico | Ficheiros |
|--------|-----------|
| Início / header | `mobile/lib/features/home/presentation/home_page.dart` |
| Dashboard scroll / pager | `mobile/lib/features/dashboard/presentation/dashboard_scroll_content.dart` |
| Período / prefetch | `mobile/lib/features/dashboard/presentation/period_selector_bar.dart` |
| Cashflow responsivo | `mobile/lib/features/dashboard/presentation/dashboard_cashflow_chart_card.dart` |
| Donut | `mobile/lib/features/dashboard/presentation/category_donut_chart.dart` |
| Auth layout | `mobile/lib/features/auth/presentation/widgets/auth_shell.dart` |
| A pagar layout | `mobile/lib/features/expenses/presentation/to_pay_page.dart` |
| Warmup | `mobile/lib/core/navigation/list_data_warmup.dart` |
| Overview caps | `backend/app/services/dashboard.py` |
| Lista despesas | `backend/app/api/routes/expenses.py` (`list_expenses`) |

---

*Documento de síntese; alinhado ao repositório em **abril de 2026**.*
