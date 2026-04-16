# Well Paid — Admin console (web local)

Painel Vite + React para gestão de contas. Não faz parte do APK.

## Arranque

1. API FastAPI a correr (ex.: `uvicorn` na pasta `backend/`) com migrações Alembic aplicadas (`alembic upgrade head`) e pelo menos um utilizador com `is_admin = true`.
2. Copiar `.env.example` para `.env` e ajustar `VITE_API_BASE_URL` se necessário.
3. `npm install` e `npm run dev`.

## Variáveis

| Nome | Descrição |
|------|-----------|
| `VITE_API_BASE_URL` | Em **dev**, pode ficar **vazio**: o Vite faz proxy de `/wellpaid-api` para `http://127.0.0.1:8000` (evita CORS). Para API direta, use ex.: `http://127.0.0.1:8000` e configure `CORS_ORIGINS` no backend. |

Com URL direta, se `CORS_ORIGINS` na API não for `*`, inclua **as duas** origens que o browser pode usar: `http://localhost:5173` e `http://127.0.0.1:5173`.

## Funções

- Listagem com filtro, paginação, ativar/desativar conta, coluna **Última actividade** (`last_seen_at`).
- **Exportar CSV** — exporta todas as linhas que correspondem ao filtro actual (vários pedidos à API se necessário).
- Gestão de **avisos e dicas financeiras** para exibição na Home do app (`/admin/announcements` + `/announcements/active`).

## Deixar sempre aberto no Windows

1. Usa o script `start-admin-console.ps1` na pasta `admin-console` para iniciar rápido:
   - `powershell -ExecutionPolicy Bypass -File .\start-admin-console.ps1`
2. No **Task Scheduler**, cria uma tarefa:
   - Trigger: `At log on`
   - Action: `powershell.exe`
   - Arguments: `-ExecutionPolicy Bypass -File "D:\Projects\Well Paid\admin-console\start-admin-console.ps1"`
   - Start in: `D:\Projects\Well Paid\admin-console`
3. Mantém o atalho desse comando no desktop para reiniciar manualmente quando quiser.
