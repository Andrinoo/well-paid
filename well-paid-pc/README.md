# Well Paid PC

Cliente **Flutter para Windows** no mesmo monorepo, contra a API FastAPI em `backend/` (mesmo contrato que o app móvel).

## Requisitos

- Flutter SDK (canal estável), suporte **Windows desktop** activo (`flutter doctor`).
- Por omissão a app fala com a **API em Vercel** (mesmo host que o APK release em `android-native/gradle.properties`).

## Configuração da API

A origem pública do deploy está em **`assets/app.env`** (carregada com `flutter_dotenv` no `main.dart`). Não coloques aí `DATABASE_URL` nem `SECRET_KEY` — o `.env` na **raiz do repositório** é para o **FastAPI** (e continua fora do bundle Flutter).

Ordem de prioridade: `--dart-define=API_BASE_URL` > `assets/app.env` > default em código.

Se no `.env` do servidor quiseres documentar a mesma origem (opcional, só referência humana): `PUBLIC_API_ORIGIN=https://…` — o PC pode também ler esta chave em `assets/app.env` se a duplicares aí (não lê o ficheiro `.env` da raiz em runtime).

Backend **local** (uvicorn) durante desenvolvimento:

```bash
flutter run -d windows --dart-define=API_BASE_URL=http://127.0.0.1:8000
```

Outro deploy (só a origem HTTPS, sem path extra):

```bash
flutter run -d windows --dart-define=API_BASE_URL=https://outro-projeto.vercel.app
```

### Web (Chrome)

Na **web**, pedidos a `127.0.0.1` falham se o backend local não estiver a correr; o código força **Vercel** quando a URL compilada é localhost (exceto se usares `ALLOW_LOCAL_API_ON_WEB=true`). Para API local no browser (precisa CORS no FastAPI):

```bash
flutter run -d chrome --dart-define=ALLOW_LOCAL_API_ON_WEB=true --dart-define=API_BASE_URL=http://127.0.0.1:8000
```

Depois de mudar `dart-define`, faz **`flutter clean`** se o browser continuar com URL antiga.

### “Sem ligação ao servidor” no Chrome

1. Na **consola** do Flutter deve aparecer `[Well Paid] API base URL (resolved): https://well-paid-psi.vercel.app`. Se mostrar `127.0.0.1`, há **override** (`dart-define` ou cache): `flutter clean`, fecha o Chrome, volta a correr.
2. Na **Vercel**, variável `CORS_ORIGINS`: usa `*` ou inclui explicitamente a origem do Flutter web (ex.: `http://localhost:7357` se fixares a porta com `--web-port=7357`).
3. Testa a API no browser: `https://well-paid-psi.vercel.app/health` — deve devolver JSON/status OK.

## Correr

Na pasta `well-paid-pc`:

```bash
flutter pub get
flutter run -d windows
```

## Build release

```bash
flutter build windows
```

O executável fica em `build/windows/x64/runner/Release/`.

## Rotas extra (paridade com Android)

Além do fluxo copiado de `mobile/`, existem rotas dedicadas: anúncios, recebíveis, investimentos, plano de parcelas (`/installment-plan/:groupId`), planos de reserva (`/emergency-plans`, `/emergency-plan/:planId`), nome de exibição, categorias, e `/shopping-lists/new`.

## Testes

```bash
flutter test
```
