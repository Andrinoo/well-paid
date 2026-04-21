# Plano mestre Well Paid — fonte única de verdade

## Onde está o plano mestre

O **mapa único** do sistema (Android nativo + API, navegação, fluxos, backend, checklist de “recriar” o produto) vive no **Cursor Plan** criado pelo utilizador, com título **“Plano mestre do sistema Well Paid”** (ficheiro `*.plan.md` na pasta de planos do Cursor). O mesmo conteúdo ampliado (plano mestre + restantes planos Cursor exportados + contrato API + QA + índice da raiz) está também versionado em [**WELL_PAID_DOCUMENTACAO_UNIFICADA.md**](./WELL_PAID_DOCUMENTACAO_UNIFICADA.md) para leitura offline no repositório.

- Caminho típico no Windows: `C:\Users\<utilizador>\.cursor\plans\plano_mestre_well_paid_<id>.plan.md`.
- No repositório Well Paid, a pasta [`.cursor`](../.cursor/) contém sobretudo **regras** (ex.: `rules/no-env-secrets.mdc`); os **planos** `.plan.md` ficam no perfil do Cursor (fora do Git), por isso este ficheiro lista-os abaixo para não perder o rasto.
- Esse documento **substitui** planos antigos dispersos como referência principal para onboarding, QA e auditoria.
- Evitar **duas fontes de verdade** sem sincronizar: alterações grandes ao plano mestre — no **Cursor Plan** *ou* no ficheiro unificado em `docs/`, e alinhar o outro quando fizer sentido.

### Planos Cursor já criados (pasta `~\.cursor\plans`)

Lista dos ficheiros `.plan.md` encontrados no perfil do utilizador **andri** (actualize se usar outra máquina ou utilizador). Cada linha: **ficheiro** → **nome no frontmatter** → **resumo do `overview`**.

| Ficheiro | Nome | O que é |
|----------|------|--------|
| `plano_mestre_well_paid_b05ef76d.plan.md` | Plano mestre Well Paid | Mapa único do sistema (Android + API), navegação, domínios, backend, checklist para recriar o produto. **Canónico.** |
| `apk_performance_e_travamentos_1da547c7.plan.md` | APK performance e travamentos | Diagnosticar tela branca/travamentos (listas de compras, metas), Baseline Profiles, R8, Compose/rede. |
| `legacy_app.py_para_stack_atual_05475281.plan.md` | Legacy app.py para stack atual | O que reaproveitar do Flask legado sem duplicar FastAPI; pesquisa de preços, grocery, opcional histórico. |
| `reservas_e_metas_v2_f0c5c643.plan.md` | Reservas e Metas v2 | Múltiplos planos de reserva de emergência, metas com URL/preço referência, migrações, Android + l10n. |
| `auditoria_partilha_despesas_3b28cce6.plan.md` | Auditoria partilha despesas | Revisão da feature partilha familiar (Android + API), lacunas, melhorias. |
| `despesa_partilhada_pt_e_ux_f80a3008.plan.md` | Despesa partilhada PT e UX | Textos PT-BR, modo Valor vs %, teclado monetário, validação API. |
| `family_financial_events_f3d7581c.plan.md` | Family financial events | Tabela append-only de eventos financeiros entre membros + API de leitura. |
| `recusa_partilha_e_assumir_9378707b.plan.md` | Recusa partilha e assumir | Recusa pelo parceiro, assumir parcela/compra, API e Android. |
| `despesas_partilhadas_e_a_receber_fbcf3c8a.plan.md` | Despesas partilhadas e a receber | Modelo de partilhas, receivables, cobertura, ecrã “a receber”. |
| `próximas_features_well_paid_27744ec2.plan.md` | Próximas features Well Paid | Backlog: FCM, admin suporte, export CSV, telemetria, placement UI. |
| `tela_de_avisos_admin_40b98a86.plan.md` | Tela de Avisos Admin | Módulo de avisos/dicas, backend, admin console, consumo no app, autostart Windows. |
| `redesign_admin_console_9636bd96.plan.md` | Redesign Admin Console | Refactor modular, tema SaaS, tabelas, dashboard. |
| `swipe_navegação_abas_android_f20371f6.plan.md` | Swipe navegação abas Android | Gestos horizontais no `MainShellScreen` / listas de compras. |
| `admin_console_passos_compassados_043ec41c.plan.md` | Admin console passos compassados | Evolução do admin em etapas (filtros users, famílias, métricas, …). |
| `admin_console_escopo_56660c70.plan.md` | Admin console escopo | Escopo inicial: JWT admin, CORS, SPA Vite, fase métricas. |
| `verificação_despesas_por_etapas_8e97e781.plan.md` | Verificação despesas por etapas | Checklist por etapas (backend pytest + Flutter); referência a `PROMPT_VERIFICACAO_*` noutro repo. |

**Planos removidos da pasta Cursor (tarefas concluídas ou redundantes):** `doc_unificado_planos_460cab44.plan.md` (documentação unificada já em [`WELL_PAID_DOCUMENTACAO_UNIFICADA.md`](./WELL_PAID_DOCUMENTACAO_UNIFICADA.md)), `índice_só_raiz_276937cc.plan.md` (substituído por [`PROJECT_FILES_INDEX.md`](./PROJECT_FILES_INDEX.md)), `verificação_despesas_por_etapas_b943890b.plan.md` (duplicado vazio do plano de verificação acima).

## Documentação no repositório (complementar)

| Documento | Função |
|-----------|--------|
| [WELL_PAID_DOCUMENTACAO_UNIFICADA.md](./WELL_PAID_DOCUMENTACAO_UNIFICADA.md) | **Leitura única:** plano mestre (Parte I), planos temáticos Cursor (Parte II), contrato API + checklist E2E + índice da raiz (Parte III). |
| [ANDROID_API_BACKEND_CONTRACT.md](./ANDROID_API_BACKEND_CONTRACT.md) | Cruzamento DTOs Retrofit ↔ rotas FastAPI (`backend/app`) e nota sobre `app.py` legacy. |
| [E2E_QA_CHECKLIST.md](./E2E_QA_CHECKLIST.md) | Testes manuais derivados das secções de navegação, Main, settings e domínios. |
| [PROJECT_FILES_INDEX.md](./PROJECT_FILES_INDEX.md) | **Só** ficheiros e pastas na **raiz** do repositório (config, produto, tooling); não descreve bibliotecas nem árvores internas dos módulos. |
| [FLUXOGRAMAS_UI_E_NAVEGACAO.md](./FLUXOGRAMAS_UI_E_NAVEGACAO.md) | Fluxogramas Mermaid: login → Main → domínios (despesas, metas, listas, definições), app lock e `savedStateHandle`. |

## Planos ou notas antigas

Se existirem ficheiros de plano antigos no repositório ou em chats:

1. Tratar o **Plano mestre** (Cursor) como canónico para o **estado actual** do produto.
2. Arquivar notas obsoletas com uma linha no topo: `Superseded by: Plano mestre Well Paid (Cursor Plan)` e data.
3. Para contratos de API, preferir sempre **OpenAPI** (`/docs` no deploy) + [ANDROID_API_BACKEND_CONTRACT.md](./ANDROID_API_BACKEND_CONTRACT.md).

## Relação com `app.py` (raiz)

A raiz [`app.py`](../app.py) é a stack **Flask** legacy. O cliente Android alinha-se com a API **FastAPI** em [`backend/app/main.py`](../backend/app/main.py). Detalhes em [ANDROID_API_BACKEND_CONTRACT.md](./ANDROID_API_BACKEND_CONTRACT.md).
