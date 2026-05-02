import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Credenciais opcionais “lembrar login” (Keychain / Keystore). Chaves separadas dos tokens.
class LoginCredentialsStorage {
  static const _kEmail = 'well_paid_saved_login_email';
  static const _kPassword = 'well_paid_saved_login_password';

  final FlutterSecureStorage _storage = const FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  Future<({String? email, String? password})> read() async {
    final email = await _storage.read(key: _kEmail);
    final password = await _storage.read(key: _kPassword);
    return (email: email, password: password);
  }

  Future<void> save({required String email, required String password}) async {
    await _storage.write(key: _kEmail, value: email);
    await _storage.write(key: _kPassword, value: password);
  }

  Future<void> clear() async {
    await _storage.delete(key: _kEmail);
    await _storage.delete(key: _kPassword);
  }
}
