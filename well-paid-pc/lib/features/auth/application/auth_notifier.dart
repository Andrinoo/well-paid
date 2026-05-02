import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/navigation/list_data_warmup.dart';
import '../../../core/network/network_providers.dart';
import '../../../core/storage/token_storage.dart';
import '../../app_lock/application/app_lock_notifier.dart';
import '../data/auth_repository.dart';
import '../domain/jwt_access_expiry.dart';
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
    var access = await _storage.readAccess();
    var refresh = await _storage.readRefresh();

    if (refresh != null && refresh.isNotEmpty) {
      final a = access;
      final staleAccess = a == null ||
          a.isEmpty ||
          accessTokenNeedsProactiveRefresh(a);
      if (staleAccess) {
        try {
          final pair = await _repository.refresh(refreshToken: refresh);
          await _storage.writePair(
            accessToken: pair.accessToken,
            refreshToken: pair.refreshToken,
          );
          access = pair.accessToken;
          refresh = pair.refreshToken;
        } catch (_) {
          /* Interceptor trata 401 nos pedidos seguintes ou limpa sessão. */
        }
      }
    }

    state = AuthState(
      hydrated: true,
      accessToken: access,
      refreshToken: refresh,
    );
    _ref.read(routerRefreshProvider).ping();
    if (access != null && access.isNotEmpty) {
      scheduleShellDataWarmup(_ref);
    }
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
    scheduleShellDataWarmup(_ref);
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
    scheduleShellDataWarmup(_ref);
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
