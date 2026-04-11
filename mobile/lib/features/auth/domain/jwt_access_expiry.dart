import 'dart:convert';

/// Lê o `exp` (segundos Unix UTC) do payload de um JWT, sem validar assinatura.
int? readJwtExpSeconds(String token) {
  try {
    final parts = token.split('.');
    if (parts.length != 3) return null;
    var payload = parts[1];
    payload = payload.replaceAll('-', '+').replaceAll('_', '/');
    final mod = payload.length % 4;
    if (mod != 0) {
      payload += '=' * (4 - mod);
    }
    final bytes = base64Decode(payload);
    final map = jsonDecode(utf8.decode(bytes)) as Map<String, dynamic>;
    final exp = map['exp'];
    if (exp is int) return exp;
    if (exp is num) return exp.toInt();
    return null;
  } catch (_) {
    return null;
  }
}

/// `true` se o access token já expirou ou expira dentro de [skew] (renovar antes do warmup).
bool accessTokenNeedsProactiveRefresh(
  String access, {
  Duration skew = const Duration(minutes: 2),
}) {
  final exp = readJwtExpSeconds(access);
  if (exp == null) return false;
  final expiry = DateTime.fromMillisecondsSinceEpoch(exp * 1000, isUtc: true);
  final now = DateTime.now().toUtc();
  return !now.isBefore(expiry.subtract(skew));
}
