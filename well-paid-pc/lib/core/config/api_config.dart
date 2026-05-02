import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Resolução da URL base da API (ordem de prioridade):
///
/// 1. `--dart-define=API_BASE_URL=...` (CI / override explícito)
/// 2. [dotenv] em `assets/app.env` (valor público; alinhado ao deploy Vercel)
/// 3. Origem por defeito `https://well-paid-psi.vercel.app`
///
/// O `.env` na raiz do repo é para o **servidor** (Neon, SECRET_KEY, SMTP, etc.);
/// não é lido automaticamente pelo Flutter. Mantém API do cliente só com dados
/// não secretos em `assets/app.env`.
///
/// **Web:** pedidos a localhost são substituídos por Vercel excepto com
/// `ALLOW_LOCAL_API_ON_WEB=true`. Ver README.
class ApiConfig {
  ApiConfig._();

  static const String _defaultProductionOrigin =
      'https://well-paid-psi.vercel.app';

  static const String _fromDefine = String.fromEnvironment('API_BASE_URL');

  static const bool _allowLocalApiOnWeb = bool.fromEnvironment(
    'ALLOW_LOCAL_API_ON_WEB',
    defaultValue: false,
  );

  /// Origem normalizada (trim, sem `/` final). Usada pelo Dio e por [apiUri].
  static String get baseUrl {
    final fromDefine = _fromDefine.trim();
    if (fromDefine.isNotEmpty) {
      return _finalizeUrl(fromDefine);
    }

    final fromDot =
        dotenv.env['API_BASE_URL']?.trim() ?? dotenv.env['PUBLIC_API_ORIGIN']?.trim();
    if (fromDot != null && fromDot.isNotEmpty) {
      return _finalizeUrl(fromDot);
    }

    return _defaultProductionOrigin;
  }

  static String _finalizeUrl(String s) {
    var t = _stripTrailingSlashes(s);
    if (kIsWeb && !_allowLocalApiOnWeb && _looksLikeLocalApi(t)) {
      t = _defaultProductionOrigin;
    }
    return t;
  }

  static bool _looksLikeLocalApi(String s) {
    final withScheme = s.contains('://') ? s : 'http://$s';
    final uri = Uri.tryParse(withScheme);
    if (uri == null || uri.host.isEmpty) return false;
    return uri.host == 'localhost' ||
        uri.host == '127.0.0.1' ||
        uri.host == '[::1]';
  }

  static String _stripTrailingSlashes(String s) {
    var t = s.trim();
    while (t.endsWith('/')) {
      t = t.substring(0, t.length - 1);
    }
    return t;
  }

  static Uri apiUri(String path) {
    final p = path.startsWith('/') ? path : '/$path';
    return Uri.parse('$baseUrl$p');
  }
}
