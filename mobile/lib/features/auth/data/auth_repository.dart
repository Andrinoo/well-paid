import 'package:dio/dio.dart';

import '../domain/token_pair.dart';

class ForgotPasswordResult {
  ForgotPasswordResult({required this.message, this.devResetToken});

  final String message;
  final String? devResetToken;
}

class AuthRepository {
  AuthRepository(this._dio);

  final Dio _dio;

  Future<TokenPair> register({
    required String email,
    required String password,
    String? fullName,
    String? phone,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      '/auth/register',
      data: {
        'email': email,
        'password': password,
        if (fullName != null && fullName.trim().isNotEmpty)
          'full_name': fullName.trim(),
        if (phone != null && phone.trim().isNotEmpty) 'phone': phone.trim(),
      },
    );
    return TokenPair.fromJson(res.data!);
  }

  Future<TokenPair> login({
    required String email,
    required String password,
  }) async {
    final res = await _dio.post<Map<String, dynamic>>(
      '/auth/login',
      data: {'email': email, 'password': password},
    );
    return TokenPair.fromJson(res.data!);
  }

  Future<void> logout(String refreshToken) async {
    await _dio.post<void>(
      '/auth/logout',
      data: {'refresh_token': refreshToken},
    );
  }

  Future<ForgotPasswordResult> forgotPassword(String email) async {
    final res = await _dio.post<Map<String, dynamic>>(
      '/auth/forgot-password',
      data: {'email': email.trim()},
    );
    final data = res.data!;
    return ForgotPasswordResult(
      message: data['message'] as String? ?? 'Pedido enviado.',
      devResetToken: data['dev_reset_token'] as String?,
    );
  }

  Future<void> resetPassword({
    required String token,
    required String newPassword,
  }) async {
    await _dio.post<void>(
      '/auth/reset-password',
      data: {'token': token.trim(), 'new_password': newPassword},
    );
  }
}
