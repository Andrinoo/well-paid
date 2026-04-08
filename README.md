# Well Paid

Monorepo com **API FastAPI** (`backend/`) e **app Flutter** (`mobile/`) para gestão de finanças pessoais.

Repositório: [github.com/Andrinoo/well-paid](https://github.com/Andrinoo/well-paid)

## Requisitos

- **Python** 3.12+ (o projeto referencia `3.14.3` em `.python-version`)
- **PostgreSQL** acessível (URL no formato esperado pelo SQLAlchemy; ver `.env.example`)
- **Flutter** SDK estável, com toolchain para as plataformas que fores usar

## Variáveis de ambiente

1. Copia `backend/.env.example` para **`backend/.env`** ou para **`.env` na raiz do repositório** (a raiz sobrepõe valores se ambos existirem — ver `backend/app/core/config.py`).
2. Preenche pelo menos:
   - `DATABASE_URL` — ex.: `postgresql+psycopg://USER:SENHA@HOST/NOME_BD`
   - `SECRET_KEY` — chave longa e aleatória
3. Opcional: SMTP (`SMTP_*`, `MAIL_FROM`) para recuperação de senha por e-mail. Sem SMTP, podes usar `APP_ENV=development` para obter `dev_reset_token` na API de “esqueci a palavra-passe” (só para desenvolvimento).

Não commits ficheiros `.env` com valores reais.

## Backend

Na pasta `backend/`:

```bash
python -m venv .venv
# Windows: .venv\Scripts\activate
# macOS/Linux: source .venv/bin/activate

pip install -r requirements.txt
alembic upgrade head
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

A API fica em `http://127.0.0.1:8000` (documentação interativa em `/docs`).

### Migrações (Alembic)

Com o `.env` configurado e o ambiente virtual ativo, a partir de `backend/`:

```bash
alembic upgrade head
```

## Mobile (Flutter)

Na pasta `mobile/`:

```bash
flutter pub get
```

A URL base da API vem de `API_BASE_URL` (compile-time). O default em código aponta para o **emulador Android** (`http://10.0.2.2:8000`). Para outros alvos, usa `--dart-define`:

| Alvo | Exemplo |
|------|--------|
| Android emulator | `flutter run` (default) |
| Windows / Chrome / iOS simulator | `flutter run -d windows --dart-define=API_BASE_URL=http://127.0.0.1:8000` |
| Telemóvel na mesma Wi‑Fi | `flutter run --dart-define=API_BASE_URL=http://IP_DO_PC:8000` |

Mais detalhes em `mobile/lib/core/config/api_config.dart`.

## Testes (backend)

A partir de `backend/`:

```bash
pytest
```

## Estrutura

```
backend/     # FastAPI, Alembic, modelos e testes
mobile/      # App Flutter
.cursor/     # Regras do Cursor (ex.: não expor segredos em docs)
```
