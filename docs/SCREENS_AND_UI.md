# Telas, navegação e padrões de UI (Well Paid)

Documento de referência para **ecrãs já existentes** e **telas que ainda serão criadas**. Complementa `README.md` e o roadmap em `Ordems*.md` (local). Código de referência: `mobile/lib/`.

---

## 1. Internacionalização (idioma da interface)

- **Locales:** `pt` (cópia alinhada a pt-BR) e `en` (inglês), via **gen-l10n** (`mobile/l10n/app_pt.arb`, `app_en.arb`).
- **Persistência:** preferência local (ex.: `shared_preferences`); o `MaterialApp` usa `locale` dinâmico (Riverpod).
- **Regra para texto novo:** nunca hardcoded em PT na UI — adicionar chave nos **dois** ARB e gerar (`flutter gen-l10n`).

---

## 2. Configurações

- **Entrada:** ícone de **engrenagem** discreto na AppBar (padrão comum de apps), por exemplo no dashboard / home — último item em `actions`.
- **Rota:** `/settings` (GoRouter).
- **Conteúdo mínimo:** seletor de idioma (pt ↔ en); outras preferências seguem neste ecrã.

---

## 3. Padrão: lista mensal + criar registo (despesas e proventos)

Objetivo: **uma ação primária clara**, sem duplicar o mesmo fluxo em dois sítios (cartão + FAB).

### 3.1 Lista de despesas (`ExpenseListPage`)

| Zona | Comportamento |
|------|----------------|
| **AppBar** | Título localizado (`expensesTitle`), botão atualizar lista. |
| **Barra de mês** | Setas mês anterior / seguinte; tooltips localizados (`periodPrevMonth`, `periodNextMonth`). |
| **Cartão sob o mês** | `FilledButton.tonalIcon`: ícone **`Icons.receipt_long_outlined`** (associado a despesa/recibo), rótulo **`expensesNewLong`** (“Nova despesa”). À direita, refresh da lista (`expensesRefreshList`). |
| **Sem FAB** | Não usar `FloatingActionButton` adicional com “+” / “Nova” — o cartão já cumpre o papel de “nova despesa”. |
| **Filtros** | Chips (todas / pendentes / pagas). |
| **Lista** | Pull-to-refresh; padding inferior moderado (não reservar espaço para FAB). |

**Atalhos externos** (ex.: home → despesas): ao abrir esta lista, o utilizador vê **o mesmo** cartão “Nova despesa”; não deve aparecer um segundo botão flutuante redundante.

### 3.2 Lista de proventos (`IncomeListPage`)

**Paridade visual com despesas:** mesma hierarquia (AppBar → mês → cartão de ação → contexto → lista).

| Zona | Comportamento |
|------|----------------|
| **AppBar** | `incomesTitle`, refresh (`incomesRefresh`). |
| **Barra de mês** | Igual às despesas (tooltips `periodPrevMonth` / `periodNextMonth`). |
| **Cartão** | `FilledButton.tonalIcon`: ícone **`Icons.savings_outlined`**, rótulo **`incomesAddLong`** (“Adicionar provento” / “Add income”). Refresh ao lado (`incomesRefreshList`). |
| **Sem FAB** | Mesma regra que despesas. |
| **Texto de ajuda** | Abaixo do cartão: `incomesListHint` (valores em reais com centavos; mês pela data do provento). |
| **Lista vazia / erro** | `incomesEmpty`, `incomesLoadError` (localizados). |

### 3.3 Regra para **novas** telas “lista + criar”

1. **Um** CTA principal no corpo: preferir cartão com `FilledButton.tonalIcon` e ícone **temático** (não `Icons.add` se existir alternativa clara).
2. **Não** combinar FAB estendido com o mesmo fluxo já exposto no cartão superior.
3. Manter **refresh** visível (AppBar e/ou ícone no cartão, conforme o ecrã).
4. Strings e tooltips via **l10n** desde o primeiro PR do ecrã.

---

## 4. Outros módulos (resumo)

- **Dashboard:** secções (resumo, despesas por categoria, a pagar, vencimentos, metas); textos localizados; gráfico donut com rótulos ARB.
- **Autenticação:** login, registo, recuperação/redefinição — mensagens e validação com `AppLocalizations` onde aplicável.
- **Detalhe / edição** de despesa e provento: títulos e formulários alinhados às chaves `newExpenseTitle`, `editIncomeTitle`, etc.

---

## 5. Evolução deste documento

Ao criar um ecrã novo que liste dados e permita “adicionar”:

- Registar aqui uma linha na secção adequada (ou subsecção nova) com **nome do ficheiro**, **rota** e **padrão de CTA** seguido.
- Se desviar do padrão cartão+temática, documentar **porquê** (ex.: tablet, wizard em passos).
