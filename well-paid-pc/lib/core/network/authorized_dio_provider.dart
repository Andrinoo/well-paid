import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/application/auth_notifier.dart';
import 'dio_client.dart';
import 'network_providers.dart';

/// API autenticada (dashboard, despesas, …) com refresh em 401.
final dioProvider = Provider<Dio>(
  (ref) => createAuthorizedDio(
    readAccessToken: () => ref.read(tokenStorageProvider).readAccess(),
    readRefreshToken: () => ref.read(tokenStorageProvider).readRefresh(),
    onTokensRefreshed: (access, refresh) async {
      await ref.read(tokenStorageProvider).writePair(
            accessToken: access,
            refreshToken: refresh,
          );
      ref.read(authNotifierProvider.notifier).applyRefreshedTokens(access, refresh);
    },
    onSessionExpired: () =>
        ref.read(authNotifierProvider.notifier).sessionExpiredAfterRefreshFailure(),
  ),
);
