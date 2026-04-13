# Gráficos Donut e Fluxo (linhas) — especificação visual e funcional para Jetpack Compose

Este documento descreve **apenas** o gráfico de **rosca por categorias** e o de **linhas (fluxo de caixa mensal)** tal como na app Flutter atual, com ênfase no **comportamento das linhas** e **comportamento do donut**, e com **orientações de layout** para uma implementação em **Jetpack Compose** sem cortes, sem overflow indesejado e com **contentores de igual altura** entre as duas abas.

**Código de referência (Flutter):**

- Donut: `mobile/lib/features/dashboard/presentation/category_donut_chart.dart`
- Linhas: `mobile/lib/features/dashboard/presentation/dashboard_cashflow_chart_card.dart`
- Onde entram no ecrã: `mobile/lib/features/dashboard/presentation/dashboard_scroll_content.dart`
- Ecrã pai: `mobile/lib/features/home/presentation/home_page.dart`

**Cores** (hex): ver também `mobile/lib/core/theme/well_paid_colors.dart` e secção de paleta em [VISUAL_DESIGN_WELL_PAID.md](./VISUAL_DESIGN_WELL_PAID.md).

---

## 1. Onde aparecem no produto

| Gráfico | Ecrã | Rota | Contexto |
|--------|------|------|----------|
| **Donut (categorias)** | Início | `/home` | Aba **“Categorias”** do seletor segmentado (ícone pie + texto localizado), dentro de um **pager** horizontal |
| **Linhas (fluxo)** | Início | `/home` | Aba **“Fluxo”** do mesmo pager |

Acima do pager (no mesmo scroll vertical do dashboard):

- Opcional: cartão “pendentes do mês” (só se houver valor a pagar).
- **SegmentedButton** com duas opções: Categorias (tab 0) e Fluxo (tab 1); sincronizado com o pager (swipe ou toque).

Abaixo do header fixo em gradiente navy do Início (`HomePage`), o corpo é fundo **cream** (`#F5F1E8`) e o conteúdo do dashboard faz **pull-to-refresh**.

**Nota:** Na base de código atual, estes dois gráficos **só** são instanciados em `DashboardScrollContent`. O widget de fluxo aceita `embeddedInHomeTabs = false` (com cabeçalho próprio no cartão), mas **na app só é usado o modo embutido** na aba Início.

---

## 2. Geometria do espaço — pager, insetos e alinhamento entre abas

### 2.1 Altura disponível para cada página do pager

O Flutter usa `SliverFillRemaining` + `PageView`: **as duas páginas recebem a mesma altura útil** (viewport restante abaixo do seletor, dentro do scroll).

**Inset inferior por página** (padding do conteúdo da tab):

- `bottomInset = safeInsetBottom + 72` (72 dp reservados para a barra de navegação inferior + handle do painel rápido).

**Padding horizontal** do conteúdo de cada tab: **10 dp** à esquerda e direita.

### 2.2 Requisito para Compose: contentores com a mesma altura

**Objetivo:** As duas abas devem partilhar **exatamente a mesma altura de “slot”** (a altura que o `HorizontalPager` ou `Row`+`Box` atribui a cada página). Dentro de cada página:

1. Um **único cartão branco** (equivalente ao `Card` Flutter: raio **20**, contorno navy ~6% alpha, elevação/sombra discreta) deve **preencher esse slot** (`Modifier.fillMaxHeight()` ou `height(slotHeight)`), para o utilizador perceber **duas vistas equivalentes**.
2. **Cada gráfico vive na sua própria composable / sub-árvore** (ex.: `DonutDashboardCardContent` vs `CashflowDashboardCardContent`), mas o **invólucro** (cartão + padding interno `10, 6, 10, 8` como no Flutter) deve ser **simétrico entre abas**.

### 2.3 Evitar cortes e overflow

| Risco | Mitigação em Compose |
|--------|----------------------|
| Linhas ou pontos do gráfico cortados nas bordas | No Flutter o `LineChart` usa `clipData: none` (não recorta à caixa). Em Compose, **não** uses `clip(RoundedCornerShape)` no mesmo `Box` que desenha o canvas das linhas, ou usa **padding interno** no desenho; garante que o **touch** usa a mesma área que o desenho. |
| Legenda do donut + grelha a exceder a altura | O cartão tem altura **fixa** (slot); o **conteúdo interno** deve usar **`verticalScroll`** (ou `LazyColumn`) **dentro** do cartão, **exceto** se calculares alturas e garantires que cabem (difícil em todos os tamanhos). Recomendação: **área superior do donut com `weight(1f)` + legenda scrollável** ou **scroll no cartão inteiro** com cabeçalho do gráfico “sticky” opcional. |
| Fluxo: muitos meses + painel + rodapé + insights | No Flutter a aba Fluxo usa **`SingleChildScrollView`** com altura do filho **intrínseca**: o cartão pode ser **mais alto** que o viewport e o utilizador faz scroll. Para **igualar altura do cartão** ao donut (requisito acima), em Compose: cartão com **`fillMaxHeight()`** e **scroll interno** (`verticalScroll` / `LazyColumn`) para o corpo do fluxo, de forma a **nunca** estourar o pai. |
| Texto dos eixos a sair | Rótulos Y com `FittedBox` / `scaleToFit` ou fonte reduzida; reserva fixa à esquerda (`≥ 42 dp`, escala com fontScale). |

### 2.4 Paridade Flutter atual (honestidade útil)

- Na aba **Categorias**, o cartão ocupa o espaço **Expanded**; a nota de rodapé (`dashHomeCategoriesFootnote`) fica **fora** do cartão, abaixo dele, ainda dentro da página do pager.
- Na aba **Fluxo**, o conteúdo está dentro de **scroll**; o **cartão branco** pode crescer em altura com o conteúdo. Para Compose com **cartões da mesma altura**, trata o scroll **no interior** do cartão de fluxo, não aumentando o cartão em relação ao slot.

---

## 3. Gráfico donut (despesas por categoria)

### 3.1 Função de dados (regra de negócio)

1. Entrada: lista de `CategorySpend` (chave de categoria, nome, valor em centavos) e total mensal de despesas.
2. **Ordenação:** por `amountCents` **decrescente**.
3. **Máximo de 5 fatias visíveis.** Se existirem mais categorias, as restantes são **somadas** numa única fatia com `categoryKey == 'outros'` e nome = string localizada (`chartCategoryOther`).
4. **Índice selecionado** `selectedSliceIndex` referencia a lista **já agregada** (0 = maior fatia após ordenação).
5. Ao mudar **mês** ou **dados** (total ou lista), o Flutter **repor** a fatia selecionada para **0**; se o índice ficar fora dos limites, ajusta para o último válido.

### 3.2 Comportamento de interação

- **Toque numa fatia:** altera a seleção; **feedback háptico** leve (`selectionClick`) quando o índice muda.
- **Toque na faixa da categoria selecionada** (card colorido): navega para lista de despesas filtrada por essa categoria (callback `onViewCategoryExpenses`).
- **Toque num tile da grelha** (outras categorias): apenas seleciona essa fatia (não navega).
- **Botão texto** “ver despesas da categoria” (quando callback existe): mesma navegação que a faixa.
- **Sem dados** (`monthExpenseTotalCents == 0` ou sem linhas úteis): donut mostra anel **placeholder**; texto central explica; **CTA** `FilledButton.tonalIcon` para registar despesa (quando callback existe).

### 3.3 Aparência visual (detalhe)

**Estrutura em camadas (de fundo a frente):**

1. **Círculo de fundo** no tamanho `side × side` (ver §3.4): gradiente radial cream → creamMuted; borda navy ~14% alpha; sombra suave (preto ~10% alpha, deslocamento Y positivo).
2. **Highlight** interno: elipse/círculo ligeiramente menor, gradiente branco no topo (alpha ~0.26 × “depth”) a transparente em baixo — efeito “lente”.
3. **Camada sombra do donut:** segundo `PieChart` **deslocado** `Offset(2g, 4g)` dp (g = escala, ver §3.4), **sem toque**. Fatias com cor = `blend(black 38%, cor base)`, borda preta suave.
4. **Camada principal:** fatias com gradiente linear **topLeft → bottomRight** (clarear com branco 20% → base → escurecer com preto 28%), com alpha animada pela intro.
5. **Centro:** texto (mês se existir, rótulo “total despesas”, valor BRL); `FittedBox` para não transbordar o buraco.

**Parâmetros de referência (dp na escala “g = 1”, com `ref = 238`):**

- Raio da fatia (espessura do anel): `sliceRadius = 54` × g.
- Raio do buraco central: `centerHole = 58` × g.
- **Espaço entre fatias** (`sectionsSpace`): **2.2** dp quando há dados e animação avançou; **0** no placeholder.
- **Ângulo inicial:** `-90°` (topo) com offset animado `(1 - introT) * 6°` (sombra) e `(1 - introT) * 6° × depth` (principal).
- **Fatia selecionada:** raio aumenta `+10g` dp face às outras; borda entre fatias **cream** com alpha maior se selecionada; espessura de borda maior se selecionada (`2.5g` vs `1.8g` × depth).

**Paleta cíclica das fatias (base sólida antes do gradiente):**

`#1B4D6F`, `#C9A94E`, `#2A7A6E`, `#3D5A80`, `#B85C4A`, `#6B6560`.

**Estado vazio:** um único anel navy muito transparente (~12–20% alpha conforme intro).

**Animação de entrada:** 800 ms, curva `easeOutCubic`; ao mudar mês/dados/categorias, a animação **reinicia**.

### 3.4 Dimensionamento do quadrado do donut (`side`)

Fórmula equivalente à Flutter:

- `w` = largura máxima disponível; `h` = altura máxima disponível (área **Expanded** acima da legenda quando há dados).
- `maxByWidth = min(420, w * 0.99)`
- `sideW = clamp(w, 158, maxByWidth)` — nota: em Dart `w.clamp(lo, hi)` com `lo=158` força largura mínima 158 quando o pai é estreito (comportamento a replicar com cuidado em telas estreitas).
- `side = min(sideW, h)` — o donut é um **quadrado inscrito**: nunca ultrapassa a altura disponível.

Escalar todo o desenho: `g = side / 238`.

### 3.5 Legenda (com dados)

1. **Faixa superior** (“categoria selecionada”): fundo cor base ~14% alpha, cantos 12; ícone em **swatch** quadrado com gradiente e borda; nome; valor BRL; linha com percentagem do total.
2. **Divisor** 1 px, navy 10% alpha.
3. **Texto hint** centrado, pequeno (~9.5 sp), navy ~48% alpha.
4. **Grelha 2 colunas** das **outras** categorias (não a selecionada): `crossAxisSpacing = 8`, `mainAxisSpacing = 6`, altura de cada linha **66 / 72 / 76 dp** conforme altura de layout ≥ 380 / 480 (ve `layoutH`).
5. Cada tile: cantos 10; fundo selecionado colorido ~12% alpha vs navy ~3%; ícone swatch compacto 18; texto duas linhas máx. com elipse.

**Swatch:** quadrado cantos 7–8; gradiente diagonal; ícone cream; borda mais espessa se categoria selecionada; sombra suave se selecionada.

---

## 4. Gráfico de linhas (fluxo de caixa mensal)

### 4.1 Função de dados

- Séries por índice de mês `0..n-1`, cada uma em **centavos inteiros**:
  - **Receitas** (`incomeCents`)
  - **Despesas pagas** (`expensePaidCents`)
  - **Despesas previstas** (`expenseForecastCents`)
- Eixo X: índices discretos dos meses; rótulos inferiores com **mês abreviado** conforme locale (`DateFormat.MMM`).
- Eixo Y: **0 .. maxY**, com `maxY = max(valores visíveis) * 1.24` (margem superior ~24% para não cortar curvas/pontos).

### 4.2 Comportamento das linhas (crítico para Compose)

**Ordem de desenho das séries** (empilhamento): a ordem em `lineBarsData` é a ordem de pintura — na implementação atual, **receitas**, depois **pagas**, depois **previsão** (previsão fica por cima onde se sobrepõem).

**Receitas e despesas pagas (sólidas):**

- Cor: receitas `#2A7A6E`; pagas `#B85C4A`.
- **Largura da linha:** 2.8 dp.
- **Curva:** `isCurved = true` com `curveSmoothness = 0.32`, **`preventCurveOverShooting = true`** (evita que a spline “dispare” para fora do intervalo entre pontos).
- **Sem “reduzir movimento”** (`reduceMotion`): curva ativa. Com redução: linha **reta** entre pontos (`isCurved = false`).
- **Pontos:** círculo raio **5**, preenchimento = cor da série, contorno branco **1.5** dp.
- **Área por baixo da linha:** gradiente vertical, cor com alpha **0.22** no topo → **0.02** em baixo (só para séries **não** tracejadas).

**Previsão (tracejada):**

- Cor `#C9A94E` (gold).
- Mesma espessura 2.8 dp.
- **Dash:** padrão `[7, 5]` (7 desenhado, 5 espaço), repetido.
- **Curva:** igual às outras (smooth / linear conforme reduce motion).
- **Pontos:** círculo raio **4.5**, **interior transparente**, só **traço** da cor da série, **2** dp — aspeto “anel”.
- **Sem preenchimento** sob a linha (`belowBarData` desligado).

**Grelha e eixos:**

- Linhas horizontais: **4** intervalos (`maxY/4`), cor navy **6%** alpha, 1 dp; **sem** linhas verticais.
- Borda só **inferior e esquerda**, navy **12%** alpha.
- **Clip:** desativado (`FlClipData.none`) — linhas/pontos podem ultrapassar ligeiramente a caixa de plotagem; em Compose, reserva **padding** no canvas.

**Eixo X — padding horizontal “virtual”:**

- `minX = 0 - xPad`, `maxX = (n-1) + xPad` com `xPad = 0.25` se `n <= 1`, senão `0.58` se `n > 14`, senão `0.48` — evita que o primeiro/último ponto fique colado à borda.

**Rótulos X:**

- `interval = 2` se `n > 12`, senão `1` (mostra um sim, um não em eixos longos).
- Tamanho fonte ~9 sp (telas estreitas) ou ~10 sp.

**Rótulos Y:**

- Intervalos alinhados a `maxY/4`.
- Texto compacto: `R$ X`, `R$ Xk`, `R$ XM` conforme magnitude (valores em **reais**, entrada em centavos).

### 4.3 Toque e painel de detalhe por mês

- **Toque no gráfico** ativo; `touchSpotThreshold ≈ 22` dp.
- Quando há vários spots próximos, **não** usar o mais próximo em distância 2D genérica: escolher o spot com **maior Y** entre os candidatos — assim, se uma linha está em zero e outra no pico, o mês refletido no painel é o do **pico** (comportamento intencional no Flutter).
- **Tooltip do gráfico:** itens sempre `null` (não mostra tooltip nativo); o feedback é o **painel inferior**.
- **Painel:** fundo navy ~4.5% alpha, borda 10% alpha, cantos 12, padding `10, 8, 10, 10`.
  - Título = mês formatado.
  - Se o utilizador **nunca** tocou no gráfico (`touchedMonthIndex == null`), mostra linha de hint “toque no gráfico” (string localizada).
  - Lista uma linha por **série visível**: barra vertical 3×16 dp na cor da série; nome; valor BRL desse mês.
- **Ao alternar visibilidade** de uma série (legenda), repor `touchedMonthIndex` a **null** e o painel volta ao mês “padrão”.

**Mês padrão (sem toque):** índice com **maior soma** `income + paid + forecast` entre as séries **atualmente visíveis** — evita mostrar o último mês se estiver tudo a zero.

### 4.4 Legenda (chips tocáveis)

- Faixa fixa **30** dp de altura; três colunas com `Expanded` + `FittedBox` scale down.
- Cada chip: borda navy (alpha maior se ativo); fundo navy ~4% (ativo) vs ~2% (inativo); mini-linha 12×2 (sólida ou tracejada igual à série); label ~9.5 sp, peso 600, alpha reduzido se inativo.
- **Toque:** alterna `_showIncome` / `_showPaid` / `_showForecast`.
- Se **todas** desligadas, mostra estado vazio textual (`dashCashflowEmpty`).

### 4.5 Barra de opções (acima do gráfico, dentro do cartão)

- Ícone `arrowsClockwise` + **Switch** (modo janela **dinâmica** vs **fixa**); thumb/track **gold** quando ativo.
- Texto à direita descreve o modo (strings localizadas).
- Secção **previsão:** label curto + `IconButton` setas esquerda/direita para mudar meses de previsão **1–12**; número centrado; **timer de 5 s** repõe o valor se o utilizador só “pré-visualizou” com as setas (comportamento fino — replicar se quiseres paridade total).

### 4.6 Altura do canvas do gráfico (embedded na aba Início)

Quando `embeddedInHomeTabs == true` e é passada `homeTabViewportHeight = H` (altura máxima da **página** do pager, do `LayoutBuilder`):

```
chartHeight = clamp(
  min(H * 0.44, H - 162),
  min = 152,
  max = 600
)
```

Se `H` não for válida (> 120): fallback com `shortestSide` e `screenHeight`:

- Embedded sem H: `max(shortSide * 0.42, screenH * 0.28)` clamp **160–400**.
- Não embedded (uso teórico): `max(shortSide * 0.36, screenH * 0.195)` clamp **186–276**.

**Compose:** calcula `H` com `BoxWithConstraints` no **mesmo slot** de altura que o donut; aplica a fórmula para a **área do LineChart**; o resto (legenda 30 + espaçamentos + painel + rodapé + insights) entra no **scroll interno** do cartão se necessário.

### 4.7 Animação

- Duração **380** ms, curva `easeOutCubic` ao mudar dados; com **reduce motion**, duração zero e curva linear.

### 4.8 Conteúdo extra só na aba Início (embedded)

- **Rodapé** após o corpo: divisor; texto totais previsão + saldo (saldo verde `#2A7A6E` ou coral `#B85C4A` conforme sinal).
- **Cartão de insights** (picos de despesa paga e de receita): fundo gold ~11% alpha, borda navy ~8%, cantos 14; ícone `chartLine`; dois blocos de texto se houver picos.

### 4.9 Estados loading / erro

- Loading: `CircularProgressIndicator` fino ou ícone estático se reduce motion.
- Erro: mensagem + botão “tentar novamente”.

---

## 5. Checklist rápido para o implementador Compose

- [ ] Pager com **duas páginas** de **igual altura**; inset inferior **safe + 72**; padding horizontal **10**.
- [ ] **Cartão** branco por aba: **mesma altura** (`fillMaxHeight` no slot), cantos **20**, padding interno **10, 6, 10, 8**.
- [ ] Donut: agregação **top 5 + outros**; animação intro; sombra deslocada **(2g, 4g)**; seleção com **raio +10g**; toque vs navegação conforme §3.2.
- [ ] Linhas: três séries com regras **curva / dash / área / pontos** exatamente como §4.2; eixo Y **×1.24**; sem clip agressivo; toque → **máximo Y** entre spots; painel sincronizado.
- [ ] **Scroll interno** onde o conteúdo exceder o cartão; **sem** overflow que quebre o layout pai.
- [ ] Acessibilidade: rótulos semânticos equivalentes aos `Semantics` Flutter nos gráficos e sumários.

---

*Documento derivado do comportamento do código Flutter em **abril de 2026**.*
