import 'dart:convert';
import 'dart:math';

import 'package:crypto/crypto.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// PIN da app (local) + preferência de biometria — não confundir com senha do servidor.
class AppLockStorage {
  static const _kPinHash = 'well_paid_app_lock_pin_hash';
  static const _kPinSalt = 'well_paid_app_lock_pin_salt';
  static const _kBioPref = 'well_paid_app_lock_biometric_pref';

  final FlutterSecureStorage _storage = const FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  Future<bool> get hasPinConfigured async {
    final h = await _storage.read(key: _kPinHash);
    return h != null && h.isNotEmpty;
  }

  Future<bool> get biometricPreferred async {
    final v = await _storage.read(key: _kBioPref);
    return v == '1';
  }

  Future<void> setBiometricPreferred(bool value) async {
    if (value) {
      await _storage.write(key: _kBioPref, value: '1');
    } else {
      await _storage.delete(key: _kBioPref);
    }
  }

  static String _hashPin(String saltB64, String pin) {
    final saltBytes = base64Decode(saltB64);
    final pinBytes = utf8.encode(pin);
    final combined = <int>[...saltBytes, ...pinBytes];
    return sha256.convert(combined).toString();
  }

  Future<void> saveNewPin(String pin) async {
    final rnd = Random.secure();
    final saltBytes = List<int>.generate(16, (_) => rnd.nextInt(256));
    final saltB64 = base64Encode(saltBytes);
    final hash = _hashPin(saltB64, pin);
    await _storage.write(key: _kPinSalt, value: saltB64);
    await _storage.write(key: _kPinHash, value: hash);
  }

  Future<bool> verifyPin(String pin) async {
    final salt = await _storage.read(key: _kPinSalt);
    final stored = await _storage.read(key: _kPinHash);
    if (salt == null || stored == null) return false;
    final computed = _hashPin(salt, pin);
    if (computed.length != stored.length) return false;
    var diff = 0;
    for (var i = 0; i < computed.length; i++) {
      diff |= computed.codeUnitAt(i) ^ stored.codeUnitAt(i);
    }
    return diff == 0;
  }

  Future<void> clearPinAndPreferences() async {
    await _storage.delete(key: _kPinHash);
    await _storage.delete(key: _kPinSalt);
    await _storage.delete(key: _kBioPref);
  }
}
