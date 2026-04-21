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
- [ ] **Listas de compras** — lista, nova, detalhe (`shopping_lists`, `shopping_list_new`, `shopping_list/{listId}`).
- [ ] **Anúncios** (`announcements`).
- [ ] **Receivables** (`receivables`) e badge no shell se existir valor pendente.
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
- [ ] Formulário: pesquisa de preços / preview URL se usados; contribuição na meta.

### Listas de compras

- [ ] Criar lista, adicionar item, sugestões de preço (debounce).
- [ ] Completar lista ou alterar estado conforme UI.

### Recados e receivables

- [ ] Anúncios activos visíveis; marcar lido / ocultar sem crash.
- [ ] Receivables: listar e liquidar um item de teste (se dados existirem).

### Reserva de emergência

- [ ] Tab carrega plano/saldo; actualizar meta ou plano sem erro.

### Telemetria

- [ ] Arranque da app não falha se endpoint de ping estiver disponível (ver rede / logs em debug).

---

## 7. Regressão rápida pós-release

- [ ] Build **release** com R8: sem crash ao abrir Main e uma lista pesada.
- [ ] Navegação atrás dos principais fluxos (formulários → Main) sem ANR.

---

*Última actualização: alinhada ao grafo em `NavRoutes.kt` / `WellPaidNavHost.kt` e ao plano mestre Well Paid.*
