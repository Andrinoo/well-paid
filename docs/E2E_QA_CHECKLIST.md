# Checklist QA manual E2E (Well Paid Android)

Derivado do plano mestre (secções 3–7): navegação, shell principal, definições, segurança e domínios. Executar em **build de debug** com API de staging ou local apontada por `API_BASE_URL`. Marcar cada linha após verificação.

## Pré-requisitos

- Conta de teste (ou registo novo) com email verificável se o fluxo de verificação estiver activo.
- Pelo menos uma família ou utilizador isolado conforme cenário.

---

## 1. Arranque e sessão (cold start)

- [ ] Com token válido guardado: app abre directamente no **Main** (tabs visíveis após loading).
- [ ] Sem token / após limpar dados: app mostra **Login** (ou progresso breve antes).
- [ ] Após logout nas definições: volta a **Login** sem ficar preso no Main.

---

## 2. Grafo de navegação (rotas principais)

### Autenticação

- [ ] **Login** com credenciais correctas → navega para **Main**, back stack limpo até login.
- [ ] **Registo** → fluxo leva a verificação de email se aplicável (`verify_email/...`).
- [ ] **Esqueci password** → pedido enviado / mensagem de sucesso.
- [ ] **Reset password** (deep link com token) → nova password e login.

### Shell e destinos frequentes

- [ ] **Main**: 5 tabs — Início, Despesas, Rendimentos, Metas, Reserva de emergência.
- [ ] Navegar para **nova despesa** e **editar despesa** (`expense_new`, `expense/{id}`); voltar não deixa ecrã em branco.
- [ ] **Plano de prestações** (`installment_plan/{groupId}`) quando aplicável.
- [ ] **Rendimentos** — novo e editar (`income_new`, `income/{id}`).
- [ ] **Metas** — novo, detalhe, editar (`goal_new`, `goal/{id}`, `goal_edit/{id}`).
- [ ] Após **eliminar meta** no editar: regressão ao Main sem segundo pop incorrecto (pilha limpa).
- [ ] `emergency_reserve_dirty` (Main): após criar plano de reserva no ecrã dedicado, o tab agrega dados actualizados.
- [ ] **Listas de compras** — lista, nova, detalhe (`shopping_lists`, `shopping_list_new`, `shopping_list/{listId}`).
- [ ] **Anúncios** (`announcements`).
- [ ] **Receivables** (`receivables`) e badge no shell se existir valor pendente.
- [ ] **Investimentos** (`investments`) pelo atalho no menu expandido da barra inferior.
- [ ] **Nova reserva (planos)** — `emergency_reserve_new` a partir do tab Reserva: ecrã dedicado com top bar navy; após criar, volta ao Main e a lista de planos actualiza.
- [ ] **Definições** (`settings`) e sub-rotas: nome a apresentar, família, segurança, categorias.

### Dirty flags (refrescar listas no Main)

- [ ] Após guardar despesa/rendimento/meta: ao voltar ao Main, lista respectiva actualiza (savedStateHandle `*_dirty`).

---

## 3. Shell principal (Main) — comportamento

- [ ] **Prefetch**: tabs carregam sem erros visíveis após entrada no Main (delays conforme `MainPrefetchTiming`).
- [ ] **Swipe** entre tabs (onde implementado) e voltar ao home.
- [ ] **Atalhos** na barra expandida: despesas pendentes, listas, recados, receivables — abrem destino correcto.
- [ ] **Definições** via ícone → rota `settings`.

---

## 4. Fluxo exemplo: Login → Main → Settings

- [ ] Login → tokens persistem; Main mostra dados do utilizador/dashboard.
- [ ] Abrir **Definições**: nome, família, opções coerentes com API.
- [ ] **Segurança**: biometria / quick login (activar e desactivar; bloqueio ao reabrir se configurado).
- [ ] **Categorias** (gestão): listar e criar categoria de teste.
- [ ] **Logout**: sessão terminada; credenciais não reutilizadas sem novo login.

---

## 5. Segurança e privacidade

- [ ] Com **app lock** activo: ao reabrir app (rota não pública), aparece **AppLock** até desbloquear.
- [ ] **Ocultar valores** (preferência de privacidade): montantes ocultos na UI onde aplicável (`LocalPrivacyHideBalance`).
- [ ] **FLAG_SECURE** / comportamento de ecrã em ecrãs sensíveis (verificação visual rápida se for requisito de produto).

---

## 6. Domínios funcionais (amostra por área)

### Despesas e prestações

- [ ] Listar com filtros de mês/categoria se UI disponível.
- [ ] Criar despesa; marcar como paga se aplicável; partilha/cover request se existir cenário de família.

### Rendimentos

- [ ] Criar e editar rendimento; lista reflecte alterações.

### Metas

- [ ] Lista com progresso; miniatura se `referenceThumbnailUrl` presente.
- [ ] **Detalhe no card (expandir)**: tocar *Detalhes* mostra estado, poupado/objectivo, “ainda a poupar”, preço de referência, link, alternativas, datas criada/actualizada, nota a tocar o cartão para o ecrã completo.
- [ ] Formulário: pesquisa de preços / preview URL se usados; contribuição na meta.

### Listas de compras

- [ ] Criar lista, adicionar item, sugestões de preço (debounce).
- [ ] Completar lista ou alterar estado conforme UI.

### Recados e receivables

- [ ] Anúncios activos visíveis; marcar lido / ocultar sem crash.
- [ ] Receivables: listar e liquidar um item de teste (se dados existirem).

### Reserva de emergência

- [ ] Tab carrega plano/saldo; actualizar meta ou plano sem erro.
- [ ] **Hero (registo geral)**: coluna com texto completo (saldo, meta mensal, progresso 1.º ano, n.º de planos) e barra de progresso, não grelha de quatro “minicards”.
- [ ] **Planos com nome**: cada card mostra todos os campos (estado, saldo, mensal, descrição, validade, tecto, estimativa, contagem, concluído) sem colapsar.
- [ ] **Adicionar reserva** abre ecrã dedicado (não toggle inline *Fechar*).
- [ ] Bloco “**Dicas práticas**” e, com **&gt;1 plano**, secção “Vários planos: recompor” visível.

### Investimentos (novo)

- [ ] Acesso via atalho **Investimentos** no shell abre ecrã sem crash.
- [ ] Cartão de resumo mostra saldo aplicado e rendimento acumulado com valores do backend.
- [ ] Buckets (CDI/CDB/Renda fixa) mostram linha de alocado/rendimento/% ao mês.
- [ ] Botão **Atualizar dados** recarrega sem duplicar layout/erros visuais.
- [ ] Com backend sem acesso à fonte externa (simulação offline), aparece badge de fallback:
  - [ ] PT-BR: `Taxa estimada (fallback temporário)`
  - [ ] EN-US: `Estimated rate (temporary fallback)`
- [ ] Com backend online e fonte disponível, badge de fallback deixa de aparecer.

### Telemetria

- [ ] Arranque da app não falha se endpoint de ping estiver disponível (ver rede / logs em debug).

---

## 7. Regressão rápida pós-release

- [ ] Build **release** com R8: sem crash ao abrir Main e uma lista pesada.
- [ ] Navegação atrás dos principais fluxos (formulários → Main) sem ANR.

---

## 8. Rollout e requisitos não-funcionais (smoke)

Executar após promover build a **staging** e repetir o subconjunto mínimo em **produção** logo após deploy.

| NFR | O que verificar | Meta |
|-----|----------------|------|
| Disponibilidade | `GET /health` 200; login e um tab (ex. Início) carregam | OK sob carga normal de teste |
| Latência percebida | Abrir despesas/rendimentos com lista não vazia; sem ecrã branco prolongado | &lt; ~2 s em rede 4G em condições normais |
| Resiliência | Desligar rede a meio de refresh de lista: mensagem de erro ou retry, sem crash | Sem ANR / crash |
| Dados | Logout e login: não vazamento de sessão de outro utilizador no mesmo dispositivo (limpar apresentação) | Só dados do user actual |
| Investimentos | Ecrã com `Scaffold` navy coerente com Anúncios/Recados; back regressa ao Main | Alinhamento visual |
| L10n | Reserva + metas: PT e EN sem chaves `R.string` em falta (Logcat) | Sem `MissingResource` |

- [ ] Staging: percorridos n.ºs **1, 2 (subconj.), 3, 5, 6 (Reserva+Metas+Invest.)** com build candidato a release.
- [ ] Produção: smoke **login → Main → tab Reserva → Investimentos** + um fluxo de escrita (ex. nova despesa de teste apagada depois).

---

## 9. Segurança e API (lembrete de operações)

- **Nunca** comitar `.env` nem colar credenciais em docs (ver regra do repositório).
- A API expõe **rate limit** (SlowAPI): respostas 429 com mensagem clara; clientes móveis devem tratar sem loop infinito de retry.
- **CORS** vem de `CORS_*` / lista no settings; em produção evitar `*` com credenciais.
- **Auth**: tokens só em tráfego HTTPS; o APK deve usar `API_BASE_URL` com `https` em release.

---

*Última actualização: alinhada ao grafo em `NavRoutes.kt` / `WellPaidNavHost.kt`, ecrã dedicado de reserva, cards de metas expandidos, e notas de rollout / API.*
