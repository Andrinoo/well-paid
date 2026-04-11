/// API base URL (compile-time via `--dart-define=API_BASE_URL=...`).
///
/// - Android emulator → default `http://10.0.2.2:8000` (localhost do PC).
/// - APK release (telefone real, API na Vercel): **só a origem** — HTTPS, **sem** barra no fim e
///   **sem** path (não uses `.../auth` nem `.../api`). Ex.:
///   `flutter build apk --release --dart-define=API_BASE_URL=https://well-paid-psi.vercel.app`
/// - Windows desktop: `flutter run -d windows --dart-define=API_BASE_URL=http://127.0.0.1:8000`
/// - Telemóvel na Wi‑Fi (backend local): `http://IP_DO_PC:8000`
class ApiConfig {
  ApiConfig._();

  static const String _raw = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8000',
  );

  /// Origem normalizada (trim, sem `/` final). Usada pelo Dio e por [apiUri].
  static String get baseUrl {
    var s = _raw.trim();
    while (s.endsWith('/')) {
      s = s.substring(0, s.length - 1);
    }
    return s;
  }

  /// Path absoluto da API (ex. `/auth/login`). Evita erros de junção base+path no Dio.
  static Uri apiUri(String path) {
    final p = path.startsWith('/') ? path : '/$path';
    return Uri.parse('$baseUrl$p');
  }
}
