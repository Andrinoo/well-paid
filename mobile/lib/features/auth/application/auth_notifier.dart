import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/network_providers.dart';
import '../../../core/storage/token_storage.dart';
import '../../app_lock/application/app_lock_notifier.dart';
import '../data/auth_repository.dart';
import '../domain/token_pair.dart';
import 'auth_state.dart';
import 'router_refresh.dart';

final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => AuthRepository(ref.watch(authDioProvider)),
);

final authNotifierProvider =
    StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(
    ref: ref,
    storage: ref.watch(tokenStorageProvider),
    repository: ref.watch(authRepositoryProvider),
  );
});

class AuthNotifier extends StateNotifier<AuthState> {
  AuthNotifier({
    required Ref ref,
    required TokenStorage storage,
    required AuthRepository repository,
  })  : _ref = ref,
        _storage = storage,
        _repository = repository,
        super(const AuthState(hydrated: false)) {
    _hydrate();
  }

  final Ref _ref;
  final TokenStorage _storage;
  final AuthRepository _repository;

  Future<void> _hydrate() async {
    final access = await _storage.readAccess();
    final refresh = await _storage.readRefresh();
    state = AuthState(
      hydrated: true,
      accessToken: access,
      refreshToken: refresh,
    );
    _ref.read(routerRefreshProvider).ping();
  }

  Future<void> login(String email, String password) async {
    final pair = await _repository.login(email: email, password: password);
    await _storage.writePair(
      accessToken: pair.accessToken,
      refreshToken: pair.refreshToken,
    );
    state = AuthState(
      hydrated: true,
      accessToken: pair.accessToken,
      refreshToken: pair.refreshToken,
    );
    await _ref.read(appLockNotifierProvider.notifier).onLoggedIn();
    _ref.read(routerRefreshProvider).ping();
  }

  Future<RegisterResult> register({
    required String email,
    required String password,
    String? fullName,
    String? phone,
  }) async {
    return _repository.register(
      email: email,
      password: password,
      fullName: fullName,
      phone: phone,
    );
  }

  /// Após `POST /auth/verify-email` — grava tokens e entra na sessão.
  Future<void> completeEmailVerification(TokenPair pair) async {
    await _storage.writePair(
      accessToken: pair.accessToken,
      refreshToken: pair.refreshToken,
    );
    state = AuthState(
      hydrated: true,
      accessToken: pair.accessToken,
      refreshToken: pair.refreshToken,
    );
    await _ref.read(appLockNotifierProvider.notifier).onLoggedIn();
    _ref.read(routerRefreshProvider).ping();
  }

  /// Após `POST /auth/refresh` bem-sucedido (interceptor Dio).
  void applyRefreshedTokens(String access, String refresh) {
    state = AuthState(
      hydrated: true,
      accessToken: access,
      refreshToken: refresh,
    );
    _ref.read(routerRefreshProvider).ping();
  }

  /// Refresh falhou ou tokens em falta: limpa sessão sem chamar API de logout.
  Future<void> sessionExpiredAfterRefreshFailure() async {
    await _storage.clear();
    state = const AuthState(hydrated: true);
    _ref.read(routerRefreshProvider).ping();
  }

  Future<void> logout() async {
    final rt = state.refreshToken;
    if (rt != null && rt.isNotEmpty) {
      try {
        await _repository.logout(rt);
      } catch (_) {}
    }
    await _storage.clear();
    state = const AuthState(hydrated: true);
    _ref.read(routerRefreshProvider).ping();
  }
}
