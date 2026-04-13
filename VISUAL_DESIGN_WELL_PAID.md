# Guia visual — Well Paid (app móvel Flutter)

Documento de referência para **cores, tipografia, ícones, gráficos e padrões de UI** tal como estão implementados no código (`mobile/lib/`). Complementa [SCREENS_AND_UI.md](./SCREENS_AND_UI.md) (fluxos e convenções) e o [schema Kotlin/Compose](./Kotlin-Compose-Schema-Well-Paid.md) (paridade futura).

**Âmbito:** a experiência de produto principal é a **app Flutter** em `mobile/`. Não há frontend web Next.js no repositório; pastas de exemplo (`Nao usar no projeto…`) não fazem parte do visual oficial.

---

## 1. Identidade de cor

### 1.1 Paleta principal (área autenticada / “home”)

Definida em `mobile/lib/core/theme/well_paid_colors.dart` e aplicada no `ColorScheme` em `mobile/lib/main.dart`.

| Token | Hex | Uso |
|--------|-----|-----|
| **Navy** | `#1B2C41` | Primária, texto principal, AppBar, contornos suaves |
| **Navy mid** | `#22344D` | Variação de profundidade |
| **Navy deep** | `#141C2A` | Topo do gradiente do header do Início |
| **Gold** | `#C9A94E` | Acento, saldo em destaque, switch ativo (fluxo de caixa), CTAs primários (fundo do botão preenchido) |
| **Gold pressed** | `#B8943D` | Acento mais escuro (ex.: valores em cartões) |
| **Cream** | `#F5F1E8` | Fundo do `Scaffold`, superfície clara |
| **Cream muted** | `#EAE6DD` | Barras de contexto (mês), barra de navegação inferior |

O tema global usa **Material 3** (`useMaterial3: true`), com `primary` = navy, `secondary` = gold, `surface` = cream, texto sobre superfície = navy e variantes com alpha (~0,62) para texto secundário.

### 1.2 Autenticação (login / registo / recuperação)

| Token | Hex | Uso |
|--------|-----|-----|
| **Login background** | `#000000` | Fundo alinhado ao PNG do logo (evita halo) |
| **Auth card** | `#141C2A` | Cartão do formulário |
| **Auth card border** | `#C9A94E` | Contorno dourado do cartão |
| **Auth field fill** | `#1B2C41` | Preenchimento dos campos |
| **Auth on card** | `#F5F1E8` | Texto principal no cartão |
| **Auth on card muted** / **hint** | `#ADA59A` / `#7A756D` | Texto secundário e hints |

**Cores de marca (referência):** azul `#1E90FF`, roxo `#9400D3`, verde `#32CD32` — documentadas para branding; o dia a dia da UI autenticada gira em torno de navy + cream + gold.

### 1.3 Cores semânticas em gráficos e estados

- **Positivo / receitas (linha de fluxo):** `#2A7A6E`
- **Despesas pagas (linha):** `#B85C4A`
- **Previsão (linha tracejada):** `#C9A94E` (alinhada ao gold)
- **Saldo no rodapé do fluxo:** verde acima se ≥ 0, vermelho-acastanhado `#B85C4A` se negativo
- **Alertas no painel rápido “A pagar”:** crítico `#B00020`; pendente `#F9A825`

### 1.4 Donut — paleta das fatias

Ordem cíclica em `category_donut_chart.dart`: `#1B4D6F`, `#C9A94E`, `#2A7A6E`, `#3D5A80`, `#B85C4A`, `#6B6560` (reutiliza harmonias navy/gold/verde/coral da app).

---

## 2. Tipografia

- **Fontes:** não há famílias custom em `pubspec.yaml`; usa-se a **tipografia padrão do Flutter / Material 3** (`ThemeData.textTheme`).
- **Peso:** muito uso de **w600–w800** em valores monetários, títulos de cartão e rótulos de gráficos.
- **Tamanhos ad hoc:** labels compactas no dashboard (ex. 9,5–11 pt), valores no header do Início com `letterSpacing` ligeiramente negativo para alinhar números.

---

## 3. Forma, elevação e cantos

| Elemento | Padrão |
|----------|--------|
| **Botão preenchido (primário)** | Raio **14**, padding vertical 16 / horizontal 22, sem elevação, fundo gold, texto navy, peso 700 |
| **Campos de texto** | Preenchimento cream-muted (~85% opacidade), raio **14**, borda navy ~14% alpha; foco com borda **gold** 2 px |
| **AppBar** | Navy, texto/branco, sem elevação, título centrado |
| **Cartões de dashboard / fluxo** | `Card` branco, elevação 2, sombra navy ~10% alpha, raio **20**, contorno navy ~6% alpha |
| **Blocos internos** (painel mês, insights) | Raios **12–14**, fundos com navy ou gold em baixa opacidade |

---

## 4. Ícones

### 4.1 Biblioteca principal: **Phosphor** (`phosphor_flutter`)

A navegação e a maior parte dos ecrãs usam **Phosphor Regular** no estado normal e **Phosphor Fill** no separador selecionado (`main_shell.dart`):

| Separador | Regular | Fill |
|-----------|---------|------|
| Início | `house` | `house` |
| Despesas | `receipt` | `receipt` |
| Proventos | `coins` | `coins` |
| Metas | `flag` | `flag` |
| Reserva | `shield` | `shield` |

**Painel rápido** (acima da barra): `invoice` (A pagar), `shoppingCartSimple` (Listas); indicador circular vermelho/amarelo conforme urgência.

**Início (menu ⋮):** `dotsThreeVertical`, `gearSix`, `usersThree`, `lock`, `arrowsClockwise`, `signOut`.

**Gráficos / dashboard:** `chartPie`, `chartLineUp`, `chartLine`, `caretUp` (toggle do painel), `notepad` (pendentes do mês), setas de período `caretLeft` / `caretRight`, etc.

**Listas:** despesas — CTA `receipt`; proventos — `coins`; refresh `arrowsClockwise`; filtro `funnelSimple`; voltar `arrowLeft`.

**Donut por categoria:** mapeamento `categoryKey` → ícones Phosphor (`forkKnife`, `car`, `houseLine`, `firstAid`, `graduationCap`, `gameController`, `user`, `bank`, `squaresFour` para “outros”); CTA vazio `plusCircle`; ver lista `list`.

### 4.2 Material Icons

`uses-material-design: true` está ativo para widgets do SDK; **não** há dependência de ícones nomeados `Icons.*` nos ecrãs principais revistos — a linha visual intencional é **Phosphor + Material components**.

### 4.3 Launcher / PWA

`flutter_launcher_icons`: ícone `assets/images/playstore-icon.png`, fundo adaptativo **`#000000`** (Android) e tema web **`#000000`**.

### 4.4 Imagens

- `well_paid_logo.png`, `login_logo.png` — branding nos fluxos de auth.
- `playstore-icon.png` — ícone da app.

---

## 5. Gráficos

### 5.1 Donut de despesas por categoria (`fl_chart`)

- **Biblioteca:** `fl_chart` (`PieChart`).
- **Comportamento:** até **5 fatias** visíveis; resto agrega em “Outros” (rótulo via l10n).
- **Visual:** fatias com **gradiente** (claro → base → escuro), **sombra** deslocada para efeito 3D suave, bordas entre fatias em cream; fatia selecionada com raio maior e borda mais espessa; animação de entrada ~800 ms.
- **Centro:** mês (se fornecido), “Total despesas”, valor em BRL; estado vazio com mensagem e `FilledButton.tonalIcon` para registar despesa.
- **Legenda:** faixa da categoria selecionada + grelha 2 colunas das restantes; divisor e texto de ajuda a tocar; botão texto para ver despesas da categoria.
- **Semântica:** rótulos A11y para com/sem dados.

### 5.2 Fluxo de caixa mensal (`LineChart`)

- **Biblioteca:** `fl_chart` (`LineChart`).
- **Séries:** receitas (sólida, verde), despesas pagas (sólida, coral), despesa prevista (tracejada, gold); área preenchida com gradiente por baixo das linhas sólidas.
- **Interação:** toque no gráfico escolhe o mês para o painel de detalhe; **chips** de legenda alternam visibilidade de cada série.
- **Controlos:** ícone `arrowsClockwise` + **Switch** (janela dinâmica vs fixa); thumb/track gold quando ativo; setas `caretLeft`/`caretRight` para meses de previsão (1–12).
- **Cartão:** fundo branco, cabeçalho com `chartLineUp` quando não está embutido na tab; estado de carregamento: `CircularProgressIndicator` ou ícone estático se “reduzir movimento”.
- **Rodapé:** totais de previsão e saldo do período com cor semântica.
- **Tab Início:** cartão extra de “insights” (picos) com fundo gold muito suave e ícone `chartLine`.

### 5.3 Navegação entre gráficos no Início

`SegmentedButton` com ícones `chartPie` / `chartLineUp` alterna entre **Categorias** e **Fluxo**; conteúdo em `PageView` com altura preenchida e inset inferior para a barra de navegação.

---

## 6. Ecrãs e hierarquia visual (resumo)

Rotas principais em `mobile/lib/app_router.dart`.

### 6.1 Autenticação (fullscreen, tema escuro)

Login, registo, verificar email, esqueci palavra-passe, redefinir — `AuthShell`: fundo preto, logo centrado, cartão navy com borda gold.

### 6.2 Shell principal (5 separadores)

Fundo da barra: cream-muted quase opaco; indicador do item selecionado: gold ~35% alpha; ícones/labels navy com mais peso quando selecionados. Gestos verticais no “handle” abrem o painel de atalhos.

### 6.3 Início (`/home`)

- **Topo:** gradiente vertical navy deep → navy; colunas receita / saldo (gold) / despesas; seletor de mês em variante **escura** (chevrons gold).
- **Corpo:** cream; pendências do mês em cartão branco com ícone `notepad` se houver total > 0; depois tabs de gráficos conforme secção 5.

### 6.4 Despesas (`/expenses` + detalhe/edição/nova)

AppBar navy padrão do tema; barra de mês em cream-muted; cartão com `FilledButton.tonalIcon` (ícone `receipt`); chips de filtro; lista com padrões de estado (vazio/erro) alinhados ao tema.

### 6.5 Proventos (`/incomes`)

Paridade com despesas; CTA com ícone `coins`.

### 6.6 Metas (`/goals`, novo, detalhe)

Placeholder e fluxos de metas (progresso linear, marcos — ver ficheiros em `features/goals/`).

### 6.7 Reserva de emergência (`/emergency-reserve`)

Acessível também a partir de Definições; componentes em `emergency_reserve_page.dart` e widgets de marcos.

### 6.8 Definições (`/settings`)

Lista com divisores navy suaves; secções de notificações (metas), idioma (pt/en), entradas com trailing `caretRight`.

### 6.9 Outras rotas fullscreen

Família (`/family`), segurança / PIN / biometria (`/security`, `/unlock`), A pagar (`/to-pay`), listas de compras (`/shopping-lists` + detalhe).

---

## 7. Acessibilidade e movimento

- **Semantics** em gráficos, sumários e toggles (ex. fluxo de caixa).
- **Haptic** ao mudar fatia no donut.
- **MediaQuery.disableAnimations** reduz animações do gráfico de linhas e substitui alguns loaders por ícones estáticos.

---

## 8. Ficheiros de referência rápida

| Tema | `mobile/lib/main.dart`, `mobile/lib/core/theme/well_paid_colors.dart` |
| Shell / ícones nav | `mobile/lib/features/shell/presentation/main_shell.dart`, `shell_quick_panel.dart` |
| Início | `mobile/lib/features/home/presentation/home_page.dart` |
| Donut | `mobile/lib/features/dashboard/presentation/category_donut_chart.dart` |
| Fluxo de caixa | `mobile/lib/features/dashboard/presentation/dashboard_cashflow_chart_card.dart` |
| Tabs gráficos | `mobile/lib/features/dashboard/presentation/dashboard_scroll_content.dart` |
| Mês (claro/escuro) | `mobile/lib/features/dashboard/presentation/period_selector_bar.dart` |
| Auth layout | `mobile/lib/features/auth/presentation/widgets/auth_shell.dart` |

---

*Última revisão alinhada ao código em **abril de 2026**.*
