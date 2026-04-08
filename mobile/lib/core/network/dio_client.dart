import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../config/api_config.dart';

typedef AccessTokenReader = Future<String?> Function();
typedef RefreshTokenReader = Future<String?> Function();
typedef TokensRefreshed = Future<void> Function(String access, String refresh);
typedef SessionExpired = Future<void> Function();

const String _kAuthRetriedKey = 'well_paid_auth_retry';

/// Cliente só para rotas de conta (sem Bearer automático).
Dio createAuthDio() {
  final dio = Dio(
    BaseOptions(
      baseUrl: ApiConfig.baseUrl,
      connectTimeout: const Duration(seconds: 20),
      receiveTimeout: const Duration(seconds: 20),
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
    ),
  );
  if (kDebugMode) {
    dio.interceptors.add(
      LogInterceptor(
        requestHeader: true,
        requestBody: true,
        responseHeader: false,
        responseBody: true,
        error: true,
        logPrint: (o) => debugPrint(o.toString()),
      ),
    );
  }
  return dio;
}

Future<String>? _refreshInFlight;

bool _skip401Refresh(RequestOptions o) {
  if (o.extra[_kAuthRetriedKey] == true) return true;
  final path = o.uri.path;
  return path.endsWith('/auth/login') ||
      path.endsWith('/auth/register') ||
      path.endsWith('/auth/refresh') ||
      path.endsWith('/auth/logout') ||
      path.endsWith('/auth/forgot-password') ||
      path.endsWith('/auth/reset-password');
}

/// Dio com Bearer + refresh automático em 401 (exceto rotas de auth).
Dio createAuthorizedDio({
  required AccessTokenReader readAccessToken,
  required RefreshTokenReader readRefreshToken,
  required TokensRefreshed onTokensRefreshed,
  required SessionExpired onSessionExpired,
}) {
  final dio = Dio(
    BaseOptions(
      baseUrl: ApiConfig.baseUrl,
      connectTimeout: const Duration(seconds: 20),
      receiveTimeout: const Duration(seconds: 20),
      headers: {'Accept': 'application/json'},
    ),
  );

  final refreshClient = Dio(
    BaseOptions(
      baseUrl: ApiConfig.baseUrl,
      connectTimeout: const Duration(seconds: 20),
      receiveTimeout: const Duration(seconds: 20),
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json',
      },
    ),
  );

  Future<String> performRefresh() async {
    final rt = await readRefreshToken();
    if (rt == null || rt.isEmpty) {
      throw DioException(
        requestOptions: RequestOptions(path: '/auth/refresh'),
        message: 'Sem refresh token',
        type: DioExceptionType.unknown,
      );
    }
    final res = await refreshClient.post<Map<String, dynamic>>(
      '/auth/refresh',
      data: {'refresh_token': rt},
    );
    final data = res.data;
    if (data == null) {
      throw StateError('Resposta de refresh vazia');
    }
    final access = data['access_token'] as String?;
    final newRt = data['refresh_token'] as String?;
    if (access == null || newRt == null) {
      throw StateError('Tokens em falta na resposta de refresh');
    }
    await onTokensRefreshed(access, newRt);
    return access;
  }

  Future<String> lockedRefresh() async {
    if (_refreshInFlight != null) {
      return _refreshInFlight!;
    }
    _refreshInFlight = performRefresh();
    try {
      return await _refreshInFlight!;
    } finally {
      _refreshInFlight = null;
    }
  }

  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await readAccessToken();
        if (token != null && token.isNotEmpty) {
          options.headers['Authorization'] = 'Bearer $token';
        }
        handler.next(options);
      },
      onError: (err, handler) async {
        final response = err.response;
        if (response?.statusCode != 401) {
          return handler.next(err);
        }
        final opts = err.requestOptions;
        if (_skip401Refresh(opts)) {
          return handler.next(err);
        }
        try {
          await lockedRefresh();
        } catch (_) {
          await onSessionExpired();
          return handler.next(err);
        }
        final token = await readAccessToken();
        if (token == null || token.isEmpty) {
          await onSessionExpired();
          return handler.next(err);
        }
        opts.headers['Authorization'] = 'Bearer $token';
        opts.extra[_kAuthRetriedKey] = true;
        try {
          final clone = await dio.fetch(opts);
          return handler.resolve(clone);
        } on DioException catch (e) {
          return handler.next(e);
        }
      },
    ),
  );

  if (kDebugMode) {
    dio.interceptors.add(
      LogInterceptor(
        requestHeader: true,
        requestBody: true,
        responseHeader: false,
        responseBody: true,
        error: true,
        logPrint: (o) => debugPrint(o.toString()),
      ),
    );
  }

  return dio;
}

/// Em debug, imprime no consola tudo o que ajuda a diagnosticar falhas de API.
void logDioException(DioException e, StackTrace? stack) {
  if (!kDebugMode) return;
  debugPrint('═══ Well Paid API erro ═══');
  debugPrint('URL base: ${ApiConfig.baseUrl}');
  debugPrint('Pedido: ${e.requestOptions.method} ${e.requestOptions.uri}');
  debugPrint('Tipo: ${e.type}');
  debugPrint('Mensagem: ${e.message}');
  debugPrint('HTTP: ${e.response?.statusCode}');
  debugPrint('Corpo: ${e.response?.data}');
  if (stack != null) debugPrint(stack.toString());
  debugPrint('════════════════════════');
}

String? messageFromDio(Object error) {
  if (error is! DioException) return error.toString();

  switch (error.type) {
    case DioExceptionType.connectionTimeout:
    case DioExceptionType.sendTimeout:
    case DioExceptionType.receiveTimeout:
      return 'Tempo esgotado. Confirma que o backend está a correr e que o URL da API está certo.';
    case DioExceptionType.connectionError:
      return 'Sem ligação ao servidor. Backend em http://0.0.0.0:8000? No emulador use 10.0.2.2; no Windows use 127.0.0.1 (--dart-define).';
    case DioExceptionType.badResponse:
      break;
    default:
      break;
  }

  final data = error.response?.data;
  if (data is Map && data['detail'] != null) {
    final d = data['detail'];
    if (d is String) return d;
    if (d is List) {
      return d.map((e) {
        if (e is Map && e['msg'] != null) return e['msg'].toString();
        return e.toString();
      }).join('\n');
    }
  }
  return error.message ?? 'Erro de rede';
}
