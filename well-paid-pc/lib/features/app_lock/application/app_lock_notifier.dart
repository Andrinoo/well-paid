import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/storage/app_lock_storage.dart';
import '../../auth/application/router_refresh.dart';
import 'app_lock_state.dart';

final appLockStorageProvider = Provider<AppLockStorage>((ref) => AppLockStorage());

final appLockNotifierProvider =
    StateNotifierProvider<AppLockNotifier, AppLockState>((ref) {
  return AppLockNotifier(
    ref: ref,
    storage: ref.watch(appLockStorageProvider),
  );
});

class AppLockNotifier extends StateNotifier<AppLockState> {
  AppLockNotifier({
    required Ref ref,
    required AppLockStorage storage,
  })  : _ref = ref,
        _storage = storage,
        super(
          const AppLockState(
            hydrated: false,
            pinEnabled: false,
            biometricPreferred: false,
            sessionUnlocked: true,
          ),
        ) {
    _hydrate();
  }

  final Ref _ref;
  final AppLockStorage _storage;

  Future<void> _hydrate() async {
    final pinOn = await _storage.hasPinConfigured;
    final bio = await _storage.biometricPreferred;
    state = AppLockState(
      hydrated: true,
      pinEnabled: pinOn,
      biometricPreferred: bio,
      sessionUnlocked: !pinOn,
    );
    _ref.read(routerRefreshProvider).ping();
  }

  void _ping() => _ref.read(routerRefreshProvider).ping();

  /// Após login/registo com sessão nova: exige desbloqueio se PIN ativo.
  Future<void> onLoggedIn() async {
    final pinOn = await _storage.hasPinConfigured;
    final bio = await _storage.biometricPreferred;
    state = state.copyWith(
      pinEnabled: pinOn,
      biometricPreferred: bio,
      sessionUnlocked: !pinOn,
    );
    _ping();
  }

  void unlockSession() {
    if (!state.pinEnabled) return;
    state = state.copyWith(sessionUnlocked: true);
    _ping();
  }

  void lockSessionIfEnabled() {
    if (!state.pinEnabled) return;
    state = state.copyWith(sessionUnlocked: false);
    _ping();
  }

  Future<bool> verifyAndUnlock(String pin) async {
    final ok = await _storage.verifyPin(pin);
    if (ok) unlockSession();
    return ok;
  }

  Future<void> setNewPin(String pin) async {
    await _storage.saveNewPin(pin);
    state = state.copyWith(
      pinEnabled: true,
      sessionUnlocked: true,
    );
    _ping();
  }

  Future<void> setBiometricPreferred(bool value) async {
    await _storage.setBiometricPreferred(value);
    state = state.copyWith(biometricPreferred: value);
    _ping();
  }

  Future<void> disablePinCompletely() async {
    await _storage.clearPinAndPreferences();
    state = AppLockState(
      hydrated: true,
      pinEnabled: false,
      biometricPreferred: false,
      sessionUnlocked: true,
    );
    _ping();
  }
}
