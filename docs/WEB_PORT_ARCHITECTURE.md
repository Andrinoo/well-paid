# Port Android para web — arquitetura e critério de conclusão

**Estado:** decisão arquitetural ativa
**Atualizado em:** 2026-09-29

## Objetivo

A aplicação em `web/`, já publicada em `wellpaid.com.br`, passa a ser o cliente principal do WellPaid. O Android em
`android-native/` permanece como referência funcional durante o port, mas novas
regras financeiras devem viver no backend FastAPI, nunca duplicadas no cliente.

## Ecossistema adotado

| Responsabilidade | Tecnologia / fonte de verdade |
|---|---|
| Regras financeiras e autorização | FastAPI + Pydantic + SQLAlchemy |
| Persistência e evolução do schema | PostgreSQL + Alembic |
| Contrato cliente/servidor | OpenAPI produzido pelo FastAPI |
| Cliente principal | React + TypeScript + Vite |
| Rotas | React Router |
| Estado remoto e invalidação | TanStack Query |
| Formulários | React Hook Form + Zod (adoção incremental) |
| Testes unitários web | Vitest + Testing Library |
| Testes de jornada | Playwright |
| Deploy web | Vercel |

Não está prevista migração para Next.js. O produto é uma SPA autenticada e a API
já é um serviço separado; Vite mantém o build e a operação mais simples.

## Limites dos projetos

- `backend/`: API canônica, validação, autorização, cálculos e persistência.
- `web/`: produto web, incluindo a entrada protegida do superadmin.
- `android-native/`: referência de comportamento até a paridade da web.
- `mobile/`, `well-paid-pc/` e `app.py`: legados/congelados durante o port.
- `admin-console/`: candidato a arquivamento após confirmar a paridade com o
  superadmin integrado em `web/`.

## Regras de implementação

1. Valores monetários trafegam e são calculados em centavos inteiros.
2. O backend é a validação definitiva; validação web dá feedback imediato.
3. Erros novos da API devem possuir `code` estável e mensagem humana opcional.
4. Toda consulta deve representar loading, vazio, erro, sucesso e retry.
5. Mutações devem invalidar somente as queries afetadas.
6. Tipos manuais são temporários; o destino é gerar o cliente pelo OpenAPI.
7. Uma tela existente não conta como portada sem operações e casos especiais.

## Critério de conclusão de um fluxo

Um fluxo só recebe estado **Concluído** quando:

- as operações equivalentes do Android existem na web;
- permissões e entitlements são respeitados;
- erros de validação e rede têm feedback recuperável;
- o layout funciona em celular e desktop;
- há teste de integração ou E2E da jornada principal;
- o contrato usado corresponde ao OpenAPI atual.

## Pipeline mínimo

1. Backend: lint, Pytest e validação das migrações.
2. Web: typecheck, Vitest e build Vite.
3. Staging: migrações, smoke test da API e Playwright.
4. Produção: deploy somente depois dos gates anteriores.
