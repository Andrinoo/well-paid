/// API base URL.
///
/// - Android emulator → `10.0.2.2` = localhost do PC (default abaixo).
/// - Windows desktop → precisas de Visual Studio (C++ desktop) + Modo programador;
///   corre: `flutter run -d windows --dart-define=API_BASE_URL=http://127.0.0.1:8000`
/// - iOS simulator / Chrome → `127.0.0.1` com o mesmo `--dart-define`.
/// - Telemóvel na Wi‑Fi → IP do PC na LAN.
class ApiConfig {
  ApiConfig._();

  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8000',
  );
}
