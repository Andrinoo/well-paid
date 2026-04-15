# Admin console Well Paid

Documentação de **processo e execução** por fase do console web de administração (fora do APK).

| Fase | Documento | Estado |
|------|-----------|--------|
| 1 — API admin + SPA local | [PHASE_01_BACKEND_AND_CONSOLE.md](PHASE_01_BACKEND_AND_CONSOLE.md) | Implementado (código base) |
| 2 — Métricas de uso / tempo | [PHASE_02_USAGE_METRICS.md](PHASE_02_USAGE_METRICS.md) | Parcial (`last_seen_at` + coluna no admin) |

## Visão rápida

- **Fase 1:** backend FastAPI com `is_admin`, rotas `/admin/*`, JWT com claim de admin; painel Vite/React local com login e listagem de contas.
- **Fase 2:** instrumentação adicional (BD e opcionalmente APK) para “tempo de uso” ou última atividade — ver documento dedicado.
