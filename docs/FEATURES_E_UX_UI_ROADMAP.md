# Well Paid — ideias de funcionalidades, telas e direcções de UX/UI

Este documento resume o **estado atual** do cliente Android (Compose) e da API, e propõe **evoluções de produto** com foco em **funcionalidade**, **experiência de utilização (UX)** e **interface (UI)**. Serve de brainstorming alinhado ao produto: finanças familiar (casal/pequena família), valores em centavos, partilha por agregado e idiomas pt/en.

---

## 1. Baseline — o que o projecto já cobre

| Área | Situação atual (resumo) |
|------|---------------------------|
| **Navegação principal** | *Bottom bar* com 5 separadores: Início (dashboard), Despesas, Proventos, Metas, Reserva de emergência. |
| **Início** | Visão do mês, cashflow, gráficos (donut categorias, série temporal), atalhos e pendentes. |
| **Despesas / proventos** | Listas + formulários; despesas com recorrência, parcelamento e partilha familiar (contratos já no backend). |
| **Metas** | Lista, detalhe, formulário. |
| **Reserva de emergência** | Aba dedicada com lógica de acumulação. |
| **Listas de compras** | Fluxo próprio (lista, detalhe, criar/editar), acessível a partir do *shell* (atalho). |
| **Família** | Convites, agregado, papéis (conceito já documentado no repositório). |
| **Conta** | Registo, verificação de e-mail, login, recuperação de senha, definições (incl. nome de saudação). |
| **Identidade visual** | Material 3, paleta navy/dourado/cream, componentes reutilizáveis (datas, cartões, gráficos). |

Esta base permite priorizar melhorias que **encaixam** no modelo de dados e na API existentes, versus funcionalidades que exigem **novos contratos** ou integrações externas.

---

## 2. Funcionalidades que poderiam ser agregadas (por tema)

### 2.1 Orçamento e controlo de gastos

- **Orçamento mensal por categoria** — definir um teto em R$ (centavos) por categoria; comparar com o gasto real do mês e mostrar barra de progresso ou alerta visual no dashboard e na lista de despesas.
- **“Quanto ainda posso gastar”** — número simples derivado de (rendimento mensal − compromissos fixos − despesas já registadas), opcionalmente por categoria.
- **Regras de alerta** — quando uma categoria ultrapassa X% do orçamento antes do fim do mês (notificação ou *banner* na app).

**Valor para o utilizador:** responde à pergunta “estamos dentro do plano?” sem folha de cálculo.

### 2.2 Lembretes e calendário de vencimentos

- **Lembretes locais** (WorkManager + notificações) para despesas com `expense_date` / vencimento próximo, e para metas paradas.
- **Vista “Agenda” ou “Próximos 7 dias”** — lista cronológica de vencimentos e recebimentos esperados (muito dos dados já existem na API de dashboard/despesas).

**UX:** um único ecrã que substitui várias idas ao separador Despesas; **UI:** lista com chips de estado (pago/pendente) e ícones de categoria.

### 2.3 Relatórios e exportação

- **Exportar CSV/PDF** — intervalo de datas, totais por categoria, adequado a IR ou conversa com contabilista.
- **“Resumo do mês” partilhável** — cartão visual (imagem ou PDF) com totais e uma frase gerada (“Gastaram 12% menos em alimentação vs. mês anterior”).

**UI:** ecrã de pré-visualização + botões “Exportar” / “Partilhar” com *bottom sheet* de opções.

### 2.4 Segurança e conforto na app

- **Bloqueio com PIN ou biométria** ao reabrir a app (já mencionado nas ordens do projecto como objectivo).
- **Ocultar valores** (modo “privacidade” em transportes) — toggle que mascara montantes até toque longo ou desbloqueio.

**UX:** fluxo curto na primeira activação; **UI:** ícone de cadeado na *top bar* ou nas definições.

### 2.5 Família e colaboração

- **Feed de actividade mínimo** — “X registou uma despesa em Supermercado” (opt-in por privacidade).
- **Metas e listas de compras com comentários** — opcional, se o backend evoluir.

### 2.6 Inteligência leve (sem depender de LLM obrigatório)

- **Sugestões baseadas em regras** — “Este mês gastaste mais em Y do que a média dos últimos 3 meses”; usa agregações já possíveis com `dashboard` + histórico.
- **Detecção de duplicados** — aviso ao criar despesa muito similar (mesmo valor, mesma categoria, mesmo dia).

### 2.7 Integrações (fase posterior)

- **Importação de extrato** (CSV/OFX) — mapeamento de colunas.
- **Open banking** — só com decisão de produto, compliance e custos.

---

## 3. Novas telas ou destinos bem-vindos

As propostas abaixo **complementam** o *shell* atual; algumas podem ser modais ou *bottom sheets* em vez de ecrãs completos.

| # | Nome sugerido | Função | UX | UI (alinhamento Well Paid) |
|---|----------------|--------|----|------------------------------|
| 1 | **Orçamentos** | Definir e editar tetos por categoria/mês; ver estado vs. real. | Entrada a partir do *dashboard* (“Gerir orçamentos”) ou Definições; fluxo em passos: categoria → valor → confirmar. | Lista com barras de progresso (cores semânticas: ok / atenção / excedido); FAB para adicionar linha de orçamento. |
| 2 | **Agenda de vencimentos** | Lista temporal de próximos pagamentos e recebimentos. | *Pull-to-refresh*; toque abre detalhe da despesa/receita. | *Timeline* vertical ou lista agrupada por dia; mesmo sistema de tipografia e espaçamentos que `HomeDashboardContent`. |
| 3 | **Relatórios / Exportar** | Escolher intervalo, formato, pré-visualizar totais. | *Wizard* simples: datas → categorias incluídas → exportar. | *Cards* com totais; botão primário dourado/navy conforme tema atual. |
| 4 | **Segurança da app** | PIN, biométria, tempo de bloqueio, modo ocultar valores. | Lista de interruptores com explicação curta sob cada um. | Lista Material 3, ícones `Filled.Lock` / `Filled.VisibilityOff`. |
| 5 | **Notificações** | Ligar/desligar tipos de lembrete (vencimentos, metas, família). | Espelhar canais Android; texto claro sobre o que gera notificação. | Igual ao padrão de `SettingsScreen`, com *dividers* e secções. |
| 6 | **Pesquisa global** | Campo único que filtra despesas/proventos por texto, intervalo, categoria. | *Search bar* na *top bar* ou ecrã dedicado; resultados em abas ou lista unificada com *badges* de tipo. | Estado vazio amigável; *skeleton* ao carregar. |
| 7 | **Detalhe de categoria** | Ao tocar numa fatia do donut ou numa linha de orçamento — histórico da categoria, tendência. | Navegação de profundidade 1 a partir do Início. | Gráfico de barras mensal + lista filtrada abaixo. |
| 8 | **Onboarding pós-registo** | 3–4 *slides* opcionais: moeda, convidar parceiro, activar lembretes. | Saltar em qualquer altura; progresso com indicador de passos. | Ilustrações leves ou ícones grandes; botão “Começar” no fim. |

---

## 4. Priorização sugerida

| Prioridade | Itens | Motivo |
|------------|--------|--------|
| **Alta (rápido impacto)** | Lembretes de vencimento + ecrã “Agenda”; bloqueio PIN/biométrica; orçamento simples por categoria se o backend permitir agregação. | Aumenta retenção e confiança sem dependências externas. |
| **Média** | Exportação CSV; pesquisa global; detalhe de categoria; modo ocultar valores. | Diferenciação e poder utilizador. |
| **Baixa / estratégica** | Feed familiar, importação de extrato, open banking, IA conversacional. | Custo de desenvolvimento, compliance ou manutenção mais elevados. |

---

## 5. Princípios de UX/UI a manter em novas entregas

- **Paridade de idiomas** — toda *string* visível em `values/` e `values-pt-rBR/` (ou convenção da equipa), como nas regras do projecto.
- **Valores** — continuar a mostrar formatação monetária na UI e inteiros em centavos no domínio/API.
- **Acessibilidade** — tamanhos de toque ≥ 48dp onde aplicável; contraste dos gráficos com legenda legível.
- **Consistência** — reutilizar `WellPaidScreenDefaults`, paddings horizontais partilhados, e padrão de *top bar* / *scaffold* já usados no *shell* e nas listas.
- **Estados vazios e erro** — ilustrar “sem dados” com mensagem útil e CTA (ex.: “Adicionar primeira despesa”), não só texto técnico.

---

## 6. Nota de implementação

Cada feature acima deve ser desenhada contra **contratos HTTP existentes** ou acompanhada de **tarefas de backend** (novos endpoints, campos ou migrações). Para alterações que mexam em partilha familiar ou RGPD, rever `docs/` de modo família e o roadmap de segurança.

---

*Documento de produto/UX; atualizar quando decisões de *scope* mudarem.*
