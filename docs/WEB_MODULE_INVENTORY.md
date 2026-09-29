# Inventário atual dos módulos web

**Fonte:** árvore e histórico Git local da branch `main`
**Atualizado em:** 2026-09-29

## Histórico observado

A web atual entrou no repositório em 14/09/2026 e evoluiu nas seguintes entregas:

| Commit | Entrega |
|---|---|
| `5e21c81` | SPA Vite consumindo a API WellPaid existente |
| `e1b72ef` | redesign, entitlements e superadmin protegido |
| `d5e823c` | correção do output Vite para deploy Vercel |
| `1238f8b` | landing, public IDs e limitação de cadastros |
| `6bf74fd` | Cloudflare Turnstile no cadastro |
| `c387c13` | dashboard mensal dinâmico e miniaturas de metas |

Portanto, login, sessão, landing, proteção de cadastro, shell e módulos financeiros
existentes devem ser evoluídos, não reconstruídos.

## Módulos e rotas atuais

| Rota | Módulo | Arquivo principal | Leitura atual |
|---|---|---|---|
| `/` | Landing | `web/src/pages/Landing.tsx` | sólido |
| `/login` | Login | `web/src/pages/Login.tsx` | sólido |
| `/registar` | Cadastro | `web/src/pages/Register.tsx` | sólido, com Turnstile |
| `/confirmar-email` | Confirmação | `web/src/pages/ConfirmEmail.tsx` | implementado |
| `/recuperar` | Recuperação | `web/src/pages/Recover.tsx` | implementado |
| `/app` | Dashboard | `web/src/pages/Dashboard.tsx` | implementado, mês dinâmico |
| `/app/despesas` | Despesas | `web/src/pages/Expenses.tsx` | port parcial |
| `/app/receitas` | Receitas | `web/src/pages/Incomes.tsx` | port parcial |
| `/app/metas` | Metas | `web/src/pages/Goals.tsx` | port parcial |
| `/app/investimentos` | Investimentos | `web/src/pages/Investments.tsx` | port parcial |
| `/app/reserva` | Reserva | `web/src/pages/Reserve.tsx` | port parcial |
| `/app/listas` | Compras | `web/src/pages/Shopping.tsx` | port parcial |
| `/app/familia` | Família | `web/src/pages/Family.tsx` | port parcial |
| `/app/definicoes` | Definições | `web/src/pages/Settings.tsx` | port parcial |
| caminho secreto | Superadmin | `web/src/admin/AdminApp.tsx` | implementado; acesso oculto |

## Infraestrutura compartilhada

- `web/src/session.ts`: armazenamento e ciclo de tokens.
- `web/src/guards.tsx`: proteção das rotas autenticadas.
- `web/src/shell.tsx`: navegação principal.
- `web/src/api.ts`: fachada pública temporária das operações da API.
- `web/src/api/http.ts`: transporte HTTP, timeout e refresh de sessão.
- `web/src/api/errors.ts`: erro estruturado compatível com FastAPI.
- `web/src/query-client.ts`: política central de cache e retry.
- `web/src/i18n/`: textos já internacionalizados de autenticação e landing.

## Lacunas transversais

1. Operações da API ainda estão concentradas na fachada `api.ts`.
2. Tipos ainda são manuais; falta geração pelo OpenAPI.
3. TanStack Query está disponível, mas as páginas devem ser migradas gradualmente.
4. Formulários existentes devem adotar React Hook Form + Zod por domínio.
5. Testes E2E ainda não existem.
6. O workflow de qualidade web foi adicionado, mas o deploy atual da Vercel deve
   continuar independente até sua validação no GitHub.
